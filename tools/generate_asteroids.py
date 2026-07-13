from __future__ import annotations

import argparse
import hashlib
import heapq
import shutil
import json
import math
import random
import re
import time
import unicodedata
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Any, Iterable, Iterator

MOD_ID = "asteroidmining"
PROFILE_FILE = Path("tools/asteroid_profiles.json")
OUTPUT_DIR = Path("src/generated/resources/data/asteroidmining/asteroids")
JPL_SBDB_QUERY_URL = "https://ssd-api.jpl.nasa.gov/sbdb_query.api"
AU_TO_SCREEN_UNITS = 100.0
DEFAULT_AMOUNT = 50_000
DEFAULT_CHUNK_SIZE = 5000
DEFAULT_API_PAGE_SIZE = 2500
DEFAULT_SAMPLE_CACHE = Path("tools/sbdb_asteroid_sample_cache.json")
SAMPLE_ALGORITHM_VERSION = 2
DEFAULT_SAMPLE_SEED = "asteroidmining-sbdb-v1"
API_RETRIES = 4
ORBIT_VISIBLE = False
FLUID_BUCKET = 1000
ASTEROID_TEXTURES = (
    f"{MOD_ID}:space/asteroid_1",
    f"{MOD_ID}:space/asteroid_2",
)

SBDB_FIELDS = [
    "spkid",
    "full_name",
    "pdes",
    "name",
    "diameter",
    "H",
    "a",
    "e",
    "i",
    "per",
    "class",
    "neo",
    "pha",
    "rot_per",
    "albedo",
    "spec_T",
    "spec_B",
]

TAXONOMY_PREFIX_TO_PROFILE = {
    "A": "S",
    "B": "C",
    "C": "C",
    "D": "D",
    "E": "M",
    "F": "C",
    "G": "C",
    "K": "S",
    "L": "S",
    "M": "M",
    "P": "D",
    "Q": "S",
    "S": "S",
    "T": "D",
    "V": "V",
    "X": "M",
}

FALLBACK_PROFILE_WEIGHTS = {
    "C": 38,
    "S": 34,
    "M": 10,
    "V": 5,
    "D": 8,
    "ICY": 5,
}

# TODO add all stuff from AsteroidProvider.java automatically?
#  Or should we separately also generate planets inside this and remove AsteroidProvider instead completely?
EXCLUDED_MAJOR_BODY_SLUGS = {
    "mercury",
    "venus",
    "earth",
    "moon",
    "luna",
    "mars",
    "phobos",
    "deimos",
    "jupiter",
    "io",
    "europa",
    "ganymede",
    "callisto",
    "saturn",
    "titan",
    "enceladus",
    "mimas",
    "iapetus",
    "rhea",
    "uranus",
    "titania",
    "oberon",
    "neptune",
    "triton",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate chunked asteroid datapack JSON.")
    parser.add_argument(
        "--amount",
        default=str(DEFAULT_AMOUNT),
        help=(
            "Number of asteroids to generate, selected deterministically from the "
            f"complete asteroid result set, or 'all'. Default: {DEFAULT_AMOUNT}."
        ),
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=OUTPUT_DIR,
        help=f"Directory for generated JSON files. Default: {OUTPUT_DIR}",
    )
    parser.add_argument(
        "--profile-file",
        type=Path,
        default=PROFILE_FILE,
        help=f"Asteroid resource profile JSON. Default: {PROFILE_FILE}",
    )
    parser.add_argument(
        "--chunk-size",
        type=int,
        default=DEFAULT_CHUNK_SIZE,
        help=f"Asteroids per generated JSON chunk. Default: {DEFAULT_CHUNK_SIZE}.",
    )
    parser.add_argument(
        "--api-page-size",
        type=int,
        default=DEFAULT_API_PAGE_SIZE,
        help=(
            "Maximum rows fetched per deterministic sampling stratum. Smaller values "
            "spread the sample over more catalog regions but require more API calls. "
            f"Default: {DEFAULT_API_PAGE_SIZE}."
        ),
    )
    parser.add_argument(
        "--sample-cache",
        type=Path,
        default=DEFAULT_SAMPLE_CACHE,
        help=(
            "Cache the selected raw JPL rows so later runs use exactly the same "
            f"asteroids without contacting JPL. Default: {DEFAULT_SAMPLE_CACHE}."
        ),
    )
    parser.add_argument(
        "--refresh-sample",
        action="store_true",
        help="Ignore and replace the deterministic sample cache.",
    )
    parser.add_argument(
        "--no-sample-cache",
        action="store_true",
        help="Do not read or write the deterministic sample cache.",
    )
    parser.add_argument(
        "--sample-seed",
        default=DEFAULT_SAMPLE_SEED,
        help=(
            "Stable seed used to choose the deterministic asteroid sample. "
            f"Default: {DEFAULT_SAMPLE_SEED!r}."
        ),
    )
    parser.add_argument(
        "--clear-output-dir",
        action="store_true",
        help="Delete existing generated *.json files in output-dir before writing.",
    )
    return parser.parse_args()


def parse_amount(amount: str) -> int | None:
    if amount.lower() == "all":
        return None
    try:
        count = int(amount)
    except ValueError as exc:
        raise SystemExit("--amount must be an integer or 'all'") from exc
    if count < 0:
        raise SystemExit("--amount must be non-negative")
    return count


def _request_sbdb(params: dict[str, str], description: str) -> dict[str, Any]:
    url = f"{JPL_SBDB_QUERY_URL}?{urllib.parse.urlencode(params)}"

    last_error: Exception | None = None
    for attempt in range(API_RETRIES):
        try:
            with urllib.request.urlopen(url, timeout=120) as response:
                payload = json.loads(response.read().decode("utf-8"))
            if not isinstance(payload, dict):
                raise ValueError("JPL SBDB response was not a JSON object")
            return payload
        except urllib.error.HTTPError as exc:
            last_error = exc
            if exc.code not in {429, 500, 502, 503, 504}:
                break
        except (urllib.error.URLError, TimeoutError, json.JSONDecodeError, ValueError) as exc:
            last_error = exc

        if attempt + 1 < API_RETRIES:
            delay = 2**attempt
            print(
                f"JPL request failed ({description}); retrying in {delay}s: {last_error}",
                flush=True,
            )
            time.sleep(delay)

    raise SystemExit(f"Failed JPL SBDB request ({description}): {last_error}")


def _base_sbdb_params() -> dict[str, str]:
    # Requiring a defined semimajor axis prevents selecting rows that cannot be
    # represented by the heliocentric screen model.
    return {
        "sb-kind": "a",
        "sb-cdata": json.dumps({"AND": ["a|DF"]}, separators=(",", ":")),
        "full-prec": "false",
    }


def fetch_sbdb_count() -> int:
    params = _base_sbdb_params()
    payload = _request_sbdb(params, "catalog count")
    try:
        return int(payload["count"])
    except (KeyError, TypeError, ValueError) as exc:
        raise SystemExit(f"JPL SBDB count response was invalid: {payload!r}") from exc


def fetch_sbdb_page(
        offset: int,
        page_size: int,
        *,
        sort: str = "a,spkid",
) -> dict[str, Any]:
    """Fetch one page from a stable server-side ordering."""
    if page_size <= 0:
        raise SystemExit("--api-page-size must be larger than 0")

    params = _base_sbdb_params()
    params.update(
        {
            "fields": ",".join(SBDB_FIELDS),
            "sort": sort,
            "limit": str(page_size),
            "limit-from": str(offset),
        }
    )
    return _request_sbdb(params, f"offset {offset}, limit {page_size}")


def table_rows(sbdb: dict[str, Any]) -> Iterable[dict[str, Any]]:
    fields = sbdb.get("fields") or SBDB_FIELDS
    for row in sbdb.get("data") or []:
        yield dict(zip(fields, row))


def iter_all_sbdb_rows(page_size: int) -> Iterator[dict[str, Any]]:
    """Stream the complete catalog. This is intentionally used only for --amount all."""
    offset = 0
    total_count = fetch_sbdb_count()

    while offset < total_count:
        request_size = min(page_size, total_count - offset)
        print(
            f"Fetching complete JPL catalog: {offset:,}/{total_count:,} rows...",
            flush=True,
        )
        page = fetch_sbdb_page(offset, request_size)
        raw_rows = page.get("data") or []
        if not raw_rows:
            break
        yield from table_rows(page)
        offset += len(raw_rows)


def _stratified_request_plan(
        total_count: int,
        amount: int,
        max_page_size: int,
        sample_seed: str,
) -> list[tuple[int, int, int, int]]:
    """
    Return (stratum_index, stratum_count, offset, limit) requests.

    The catalog is sorted by semimajor axis and split into equal-count strata.
    A deterministic cyclic interval is selected inside every stratum. Across
    possible seeds, every row in a stratum has the same inclusion probability.
    """
    if amount < 0:
        raise ValueError("amount must be non-negative")
    if amount > total_count:
        amount = total_count
    if amount == 0:
        return []
    if max_page_size <= 0:
        raise SystemExit("--api-page-size must be larger than 0")

    stratum_count = max(1, math.ceil(amount / max_page_size))
    requests: list[tuple[int, int, int, int]] = []

    for index in range(stratum_count):
        stratum_start = (index * total_count) // stratum_count
        stratum_end = ((index + 1) * total_count) // stratum_count
        stratum_size = stratum_end - stratum_start

        take_start = (index * amount) // stratum_count
        take_end = ((index + 1) * amount) // stratum_count
        take = take_end - take_start
        if take <= 0:
            continue
        if take > stratum_size:
            take = stratum_size

        local_start = stable_seed_int(sample_seed, "stratum", index) % stratum_size
        first_limit = min(take, stratum_size - local_start)
        requests.append(
            (index, stratum_count, stratum_start + local_start, first_limit)
        )

        wrapped_limit = take - first_limit
        if wrapped_limit:
            requests.append((index, stratum_count, stratum_start, wrapped_limit))

    return requests


def fetch_stratified_sbdb_rows(
        amount: int,
        page_size: int,
        sample_seed: str,
) -> tuple[list[dict[str, Any]], int]:
    """Fetch a fast, deterministic sample spread across orbital distance."""
    total_count = fetch_sbdb_count()
    target = min(amount, total_count)
    plan = _stratified_request_plan(total_count, target, page_size, sample_seed)

    print(
        f"JPL has {total_count:,} usable asteroid rows. Fetching {target:,} rows "
        f"through {len(plan):,} stratified request(s)...",
        flush=True,
    )

    rows: list[dict[str, Any]] = []
    for request_number, (index, stratum_count, offset, limit) in enumerate(plan, 1):
        print(
            f"Sampling stratum {index + 1}/{stratum_count} "
            f"(request {request_number}/{len(plan)}, {len(rows):,}/{target:,} rows)...",
            flush=True,
        )
        page = fetch_sbdb_page(offset, limit, sort="a,spkid")
        rows.extend(table_rows(page))

    if len(rows) < target:
        raise SystemExit(
            f"JPL returned only {len(rows):,} of the requested {target:,} sampled rows. "
            "Run again or lower --amount."
        )
    return rows[:target], total_count


def load_sample_cache(
        cache_file: Path,
        amount: int,
        sample_seed: str,
) -> list[dict[str, Any]] | None:
    if not cache_file.exists():
        return None
    try:
        with cache_file.open("r", encoding="utf-8") as handle:
            payload = json.load(handle)
    except (OSError, json.JSONDecodeError):
        return None

    if not isinstance(payload, dict):
        return None
    if payload.get("algorithmVersion") != SAMPLE_ALGORITHM_VERSION:
        return None
    if payload.get("amount") != amount or payload.get("sampleSeed") != sample_seed:
        return None
    if payload.get("fields") != SBDB_FIELDS:
        return None

    rows = payload.get("rows")
    if not isinstance(rows, list) or len(rows) != amount:
        return None
    if not all(isinstance(row, dict) for row in rows):
        return None
    return rows


def write_sample_cache(
        cache_file: Path,
        rows: list[dict[str, Any]],
        amount: int,
        sample_seed: str,
        catalog_count: int,
) -> None:
    cache_file.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "algorithmVersion": SAMPLE_ALGORITHM_VERSION,
        "amount": amount,
        "sampleSeed": sample_seed,
        "catalogCountAtCreation": catalog_count,
        "sort": "a,spkid",
        "fields": SBDB_FIELDS,
        "rows": rows,
    }
    temporary = cache_file.with_suffix(cache_file.suffix + ".tmp")
    with temporary.open("w", encoding="utf-8") as handle:
        json.dump(payload, handle, ensure_ascii=False, separators=(",", ":"))
        handle.write("\n")
    temporary.replace(cache_file)

def normalized_slug(value: str, fallback: str) -> str:
    normalized = unicodedata.normalize("NFKD", value or fallback)
    ascii_value = normalized.encode("ascii", "ignore").decode("ascii")
    slug = re.sub(r"[^a-z0-9._-]+", "_", ascii_value.lower())
    slug = re.sub(r"_+", "_", slug).strip("_")
    return slug or fallback


def parse_float(value: Any, default: float | None = None) -> float | None:
    if value in (None, "", "null"):
        return default
    try:
        return float(value)
    except (TypeError, ValueError):
        return default


def stable_seed_int(*parts: Any) -> int:
    seed_text = "|".join(str(part) for part in parts)
    digest = hashlib.sha256(seed_text.encode("utf-8")).digest()
    return int.from_bytes(digest[:8], "big")


def deterministic_sample_rank(identity: str, sample_seed: str) -> int:
    """Return an order-independent, effectively collision-free sample rank."""
    seed_text = f"{sample_seed}|{identity}|sample"
    digest = hashlib.sha256(seed_text.encode("utf-8")).digest()
    return int.from_bytes(digest, "big")


def deterministic_angle(seed: str) -> float:
    integer = stable_seed_int(seed, "angle") & 0xFFFFFFFF
    return round((integer / 0xFFFFFFFF) * 360.0, 3)


def scaled_diameter(diameter_km: float | None, absolute_magnitude: float | None) -> int:
    if diameter_km is not None and diameter_km > 0:
        # Ceres-sized bodies become ~4 px at zoom 1, tiny bodies stay clickable,
        # and very large outliers are capped so the UI remains sane.
        return max(1, min(8, int(round(math.log10(diameter_km + 1.0) * 1.35))))

    # Very rough fallback from H. It is not a real diameter; it just prevents
    # unknown-size asteroids from disappearing from the generated data.
    if absolute_magnitude is not None:
        return 1 if absolute_magnitude >= 14 else 2
    return 1


def orbital_speed(a_au: float | None) -> float:
    if a_au is None or a_au <= 0:
        return 0.0
    # Current hand-authored data uses Earth as ~0.298, i.e. km/s divided by 100.
    return round((29.78 / math.sqrt(a_au)) / 100.0, 5)


def clean_name(raw_name: str, designation: str | None, spkid: str | None) -> str:
    value = (raw_name or designation or spkid or "asteroid").strip()
    value = re.sub(r"\s+", " ", value)
    return value


def asteroid_identity(row: dict[str, Any]) -> str:
    spkid = str(row.get("spkid") or "").strip()
    designation = str(row.get("pdes") or "").strip()
    name = clean_name(str(row.get("full_name") or row.get("name") or ""), designation, spkid)
    return spkid or designation or name


def pick_texture(seed: str, textures: tuple[str, ...]) -> str:
    rng = random.Random(stable_seed_int(seed, "texture"))
    return textures[rng.randrange(len(textures))]


def load_profiles(profile_file: Path) -> dict[str, dict[str, Any]]:
    if not profile_file.exists():
        raise SystemExit(f"Profile file not found: {profile_file}")

    with profile_file.open("r", encoding="utf-8") as handle:
        profiles = json.load(handle)

    if not isinstance(profiles, dict) or not profiles:
        raise SystemExit(f"Profile file must contain a non-empty JSON object: {profile_file}")
    return profiles


def pick_profile_key(
        row: dict[str, Any],
        rng: random.Random,
        profiles: dict[str, dict[str, Any]],
) -> str:
    spec = str(row.get("spec_T") or row.get("spec_B") or "").upper().strip()
    if spec:
        mapped = TAXONOMY_PREFIX_TO_PROFILE.get(spec[:1])
        if mapped in profiles:
            return mapped

    orbit_class = str(row.get("class") or "").upper().strip()
    albedo = parse_float(row.get("albedo"))
    a_au = parse_float(row.get("a"))

    if (orbit_class in {"CEN", "TNO"} or (a_au is not None and a_au >= 5.5)) and "ICY" in profiles:
        return "ICY"
    if albedo is not None:
        if albedo < 0.08 and "C" in profiles:
            return "C"
        if albedo > 0.28 and "S" in profiles:
            return "S"

    available_weighted = [
        (profile_key, weight)
        for profile_key, weight in FALLBACK_PROFILE_WEIGHTS.items()
        if profile_key in profiles
    ]
    if available_weighted:
        keys, weights = zip(*available_weighted)
        return rng.choices(list(keys), weights=list(weights), k=1)[0]

    return sorted(profiles)[0]


def diameter_from_magnitude(
        absolute_magnitude: float | None,
        albedo: float | None,
) -> float | None:
    if absolute_magnitude is None:
        return None

    # Standard asteroid diameter estimate using H and albedo. The default albedo
    # keeps the result in a practical range when SBDB lacks diameter data.
    effective_albedo = min(max(albedo or 0.14, 0.02), 0.6)
    return (1329.0 / math.sqrt(effective_albedo)) * math.pow(10.0, -absolute_magnitude / 5.0)


def base_mass(
        diameter_km: float | None,
        absolute_magnitude: float | None,
        albedo: float | None,
) -> int:
    effective_diameter = diameter_km or diameter_from_magnitude(absolute_magnitude, albedo) or 0.2

    # Gameplay mass, not real kg. Scales sanely for Minecraft.
    clamped_diameter = max(0.05, min(effective_diameter, 120.0))
    return int(12_000 * math.pow(clamped_diameter, 1.35))


def amount_from_weight(
        rng: random.Random,
        mass: int,
        bounds: Any,
        minimum: int = 1,
) -> int:
    if isinstance(bounds, (int, float)):
        low = high = float(bounds)
    elif isinstance(bounds, (list, tuple)) and len(bounds) >= 2:
        low = float(bounds[0])
        high = float(bounds[1])
    else:
        raise ValueError(f"Invalid profile bounds: {bounds!r}")

    weight = rng.uniform(min(low, high), max(low, high))
    noise = rng.uniform(0.82, 1.18)
    return max(minimum, int(mass * weight * noise))


def maybe_add_trace(rng: random.Random, mass: int, bounds: Any) -> int:
    # Trace materials are intentionally not guaranteed.
    if rng.random() > 0.35:
        return 0
    return amount_from_weight(rng, mass, bounds, minimum=1)


def looks_like_chemical_resource(resource_id: str) -> bool:
    if not resource_id.startswith("mekanism:"):
        return False
    return any(
        marker in resource_id
        for marker in (
            "slurry",
            "hydrogen",
            "oxygen",
            "dioxide",
            "ethene",
            "water",
            "chlorine",
            "sulfur",
            "uranium_oxide",
        )
    )


def composition_entry(
        resource_id: str,
        amount: int,
        resource_type: str,
) -> dict[str, Any]:
    entry: dict[str, Any] = {
        "resource": {"id": resource_id},
        "amount": amount,
        "type": resource_type,
    }

    namespace = resource_id.partition(":")[0]

    if namespace not in {"minecraft", MOD_ID}:
        entry["neoforge:conditions"] = [
            {
                "type": "neoforge:mod_loaded",
                "modid": namespace,
            }
        ]

    return entry


def generate_composition(
        profile: dict[str, Any],
        mass: int,
        rng: random.Random,
) -> list[dict[str, Any]]:
    """Generate the codec-compatible flat composition entry list."""
    items: dict[str, int] = {}
    fluids: dict[str, int] = {}
    chemicals: dict[str, int] = {}

    # Sort resource IDs so profile key reordering does not alter random draws.
    for resource_id, bounds in sorted(profile.get("items", {}).items()):
        items[resource_id] = amount_from_weight(rng, mass, bounds)

    for resource_id, bounds in sorted(profile.get("fluids", {}).items()):
        fluids[resource_id] = amount_from_weight(
            rng,
            mass * FLUID_BUCKET,
            bounds,
            minimum=FLUID_BUCKET,
        )

    for resource_id, bounds in sorted(profile.get("chemicals", {}).items()):
        chemicals[resource_id] = amount_from_weight(rng, mass, bounds)

    for resource_id, bounds in sorted(profile.get("traces", {}).items()):
        value = maybe_add_trace(rng, mass, bounds)
        if value <= 0:
            continue
        if looks_like_chemical_resource(resource_id):
            chemicals[resource_id] = value
        else:
            items[resource_id] = value

    composition = [
        composition_entry(resource_id, amount, "item")
        for resource_id, amount in sorted(items.items())
    ]
    composition.extend(
        composition_entry(resource_id, amount, "fluid")
        for resource_id, amount in sorted(fluids.items())
    )

    composition.extend(
        composition_entry(resource_id, amount, "chemical")
        for resource_id, amount in sorted(chemicals.items())
    )
    return composition


def is_supported_asteroid_row(row: dict[str, Any], slug: str) -> bool:
    if slug in EXCLUDED_MAJOR_BODY_SLUGS:
        return False

    designation = str(row.get("pdes") or "").strip()
    if designation.startswith("S/"):
        return False

    return True


def is_generatable_asteroid_row(row: dict[str, Any]) -> bool:
    spkid = str(row.get("spkid") or "").strip()
    designation = str(row.get("pdes") or "").strip()
    name = clean_name(str(row.get("full_name") or row.get("name") or ""), designation, spkid)
    seed = spkid or designation or name
    slug = normalized_slug(name, f"asteroid_{seed}")

    if not is_supported_asteroid_row(row, slug):
        return False

    a_au = parse_float(row.get("a"))
    return a_au is not None and a_au > 0


def select_asteroid_rows(
        rows: Iterable[dict[str, Any]],
        amount_limit: int | None,
        sample_seed: str,
) -> list[dict[str, Any]]:
    """
    Select deterministically from the supplied rows using SHA-256 ranks.

    For normal limited runs, the supplied rows have already been selected by the
    fast semimajor-axis-stratified fetch. The hash rank only stabilizes output order.
    """
    if amount_limit == 0:
        return []

    if amount_limit is None:
        selected = [row for row in rows if is_generatable_asteroid_row(row)]
        selected.sort(
            key=lambda row: (
                deterministic_sample_rank(asteroid_identity(row), sample_seed),
                asteroid_identity(row),
            )
        )
        return selected

    heap: list[tuple[int, int, dict[str, Any]]] = []
    sequence = 0

    for row in rows:
        if not is_generatable_asteroid_row(row):
            continue

        identity = asteroid_identity(row)
        rank = deterministic_sample_rank(identity, sample_seed)
        entry = (-rank, sequence, row)
        sequence += 1

        if len(heap) < amount_limit:
            heapq.heappush(heap, entry)
            continue

        worst_rank = -heap[0][0]
        if rank < worst_rank:
            heapq.heapreplace(heap, entry)

    selected = [entry[2] for entry in heap]
    selected.sort(
        key=lambda row: (
            deterministic_sample_rank(asteroid_identity(row), sample_seed),
            asteroid_identity(row),
        )
    )
    return selected


def unique_path(base_path: str, seed: str, used_paths: set[str]) -> str:
    path = base_path
    collision_index = 1
    while path in used_paths:
        collision_index += 1
        path = normalized_slug(
            f"{base_path}_{seed}_{collision_index}",
            f"asteroid_{len(used_paths)}",
        )
    used_paths.add(path)
    return path


def to_config(
        row: dict[str, Any],
        used_paths: set[str],
        profiles: dict[str, dict[str, Any]],
        textures: tuple[str, ...],
) -> dict[str, Any] | None:
    spkid = str(row.get("spkid") or "").strip()
    designation = str(row.get("pdes") or "").strip()
    name = clean_name(str(row.get("full_name") or row.get("name") or ""), designation, spkid)

    seed = spkid or designation or name
    base_path = normalized_slug(name, f"asteroid_{seed or len(used_paths)}")
    if not is_supported_asteroid_row(row, base_path):
        return None
    path = unique_path(base_path, seed, used_paths)

    a_au = parse_float(row.get("a"))
    eccentricity = min(max(parse_float(row.get("e"), 0.0) or 0.0, 0.0), 0.999)
    diameter_km = parse_float(row.get("diameter"))
    absolute_magnitude = parse_float(row.get("H"))
    albedo = parse_float(row.get("albedo"))

    if a_au is None or a_au <= 0:
        # The screen model is heliocentric and needs a semi-major axis. Skip
        # incomplete rows instead of creating invalid configs at the origin.
        return None

    rng = random.Random(stable_seed_int(seed, "composition"))
    profile_key = pick_profile_key(row, rng, profiles)
    profile = profiles[profile_key]
    mass = base_mass(diameter_km, absolute_magnitude, albedo)

    semi_major_axis = round(a_au * AU_TO_SCREEN_UNITS, 5)
    semi_minor_axis = round(semi_major_axis * math.sqrt(1.0 - eccentricity * eccentricity), 5)

    return {
        "id": f"{MOD_ID}:{path}",
        "name": name,
        "texture": pick_texture(seed, textures),
        "diameter": scaled_diameter(diameter_km, absolute_magnitude),
        "composition": generate_composition(profile, mass, rng),
        "orbit": {
            "centralBodyName": "Sun",
            "semiMajorAxis": semi_major_axis,
            "semiMinorAxis": semi_minor_axis,
            "orbitalSpeed": orbital_speed(a_au),
            "isClockwise": True,
            "startingAngleDegrees": deterministic_angle(seed),
            "isOrbitVisible": ORBIT_VISIBLE,
            "rotateAroundItself": True,
        },
    }


def clear_json_files(output_dir: Path) -> None:
    if not output_dir.exists():
        return

    entries = list(output_dir.iterdir())
    contains_only_generated_json = all(
        entry.is_file() and entry.suffix.lower() == ".json"
        for entry in entries
    )
    if contains_only_generated_json:
        # Removing a generated-only directory in one operation is much faster
        # than unlinking 50,000 files one by one, especially on Windows.
        shutil.rmtree(output_dir)
        return

    # Preserve any unrelated files in a custom output directory.
    for path in output_dir.glob("*.json"):
        path.unlink()



def write_chunked(
        configs: list[dict[str, Any]],
        output_dir: Path,
        chunk_size: int,
) -> int:
    if chunk_size <= 0:
        raise SystemExit("--chunk-size must be larger than 0")
    output_dir.mkdir(parents=True, exist_ok=True)
    files_written = 0
    for index in range(0, len(configs), chunk_size):
        chunk = configs[index : index + chunk_size]
        path = output_dir / f"asteroids_{files_written:05d}.json"
        with path.open("w", encoding="utf-8") as handle:
            json.dump({"asteroids": chunk}, handle, ensure_ascii=False, indent=2)
            handle.write("\n")
        files_written += 1
        print(
            f"Wrote chunk {files_written:,} containing {len(chunk):,} asteroids "
            f"({min(index + len(chunk), len(configs)):,}/{len(configs):,})...",
            flush=True,
        )
    return files_written


def build_configs(
        rows: Iterable[dict[str, Any]],
        profiles: dict[str, dict[str, Any]],
        textures: tuple[str, ...],
        amount_limit: int | None,
        sample_seed: str,
) -> list[dict[str, Any]]:
    used_paths: set[str] = set()
    selected_rows = select_asteroid_rows(rows, amount_limit, sample_seed)
    configs: list[dict[str, Any]] = []

    for row in selected_rows:
        config = to_config(row, used_paths, profiles, textures)
        if config is not None:
            configs.append(config)

    return configs


def main() -> int:
    args = parse_args()
    amount_limit = parse_amount(args.amount)
    profiles = load_profiles(args.profile_file)

    if args.api_page_size <= 0:
        raise SystemExit("--api-page-size must be larger than 0")

    if amount_limit == 0:
        rows: Iterable[dict[str, Any]] = []
    elif amount_limit is None:
        print(
            "--amount all requires scanning the complete JPL catalog and can take a long time.",
            flush=True,
        )
        rows = iter_all_sbdb_rows(args.api_page_size)
    else:
        cached_rows: list[dict[str, Any]] | None = None
        if not args.no_sample_cache and not args.refresh_sample:
            cached_rows = load_sample_cache(
                args.sample_cache,
                amount_limit,
                args.sample_seed,
            )

        if cached_rows is not None:
            print(
                f"Loaded the same {len(cached_rows):,} asteroid rows from "
                f"sample cache {args.sample_cache}",
                flush=True,
            )
            rows = cached_rows
        else:
            sampled_rows, catalog_count = fetch_stratified_sbdb_rows(
                amount_limit,
                args.api_page_size,
                args.sample_seed,
            )
            rows = sampled_rows
            if not args.no_sample_cache:
                print(f"Saving deterministic sample cache to {args.sample_cache}...", flush=True)
                write_sample_cache(
                    args.sample_cache,
                    sampled_rows,
                    amount_limit,
                    args.sample_seed,
                    catalog_count,
                )

    configs = build_configs(
        rows,
        profiles,
        ASTEROID_TEXTURES,
        amount_limit,
        args.sample_seed,
    )

    if args.clear_output_dir:
        print(f"Clearing generated output directory {args.output_dir}...", flush=True)
        clear_json_files(args.output_dir)

    files_written = write_chunked(configs, args.output_dir, args.chunk_size)

    print(
        f"Generated {len(configs):,} asteroids into {files_written:,} chunked "
        f"JSON file(s) at {args.output_dir}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())