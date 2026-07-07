from __future__ import annotations

import argparse
import hashlib
import json
import math
import random
import re
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Any, Iterable

import unicodedata

MOD_ID = "asteroidmining"
PROFILE_FILE = Path("tools/asteroid_profiles.json")
OUTPUT_DIR = Path("src/generated/resources/data/asteroidmining/asteroids")
JPL_SBDB_QUERY_URL = "https://ssd-api.jpl.nasa.gov/sbdb_query.api"
AU_TO_SCREEN_UNITS = 100.0
DEFAULT_CHUNK_SIZE = 5000
ORBIT_VISIBLE = False
FLUID_BUCKET = 1000
ASTEROID_TEXTURES = (
    f"{MOD_ID}:space/asteroid_1",
    f"{MOD_ID}:space/asteroid_2"
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
    parser = argparse.ArgumentParser(description="Generate asteroid datapack JSON.")
    parser.add_argument(
        "--amount",
        default="all",
        help="Number of asteroids to generate, or 'all'. Default: all.",
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
        help=f"Asteroids per JSON chunk. Default: {DEFAULT_CHUNK_SIZE}",
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


def fetch_sbdb(limit: int | None = None) -> dict[str, Any]:
    params = {
        "sb-kind": "a",  # asteroids only; excludes comets and normal planets
        "fields": ",".join(SBDB_FIELDS),
        "full-prec": "false",
    }
    if limit is not None:
        params["limit"] = str(limit)

    url = f"{JPL_SBDB_QUERY_URL}?{urllib.parse.urlencode(params)}"
    with urllib.request.urlopen(url, timeout=120) as response:
        return json.loads(response.read().decode("utf-8"))


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


def table_rows(sbdb: dict[str, Any]) -> Iterable[dict[str, Any]]:
    fields = sbdb.get("fields") or SBDB_FIELDS
    for row in sbdb.get("data", []):
        yield dict(zip(fields, row))

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


def pick_profile_key(row: dict[str, Any], rng: random.Random, profiles: dict[str, dict[str, Any]]) -> str:
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


def diameter_from_magnitude(absolute_magnitude: float | None, albedo: float | None) -> float | None:
    if absolute_magnitude is None:
        return None

    # Standard asteroid diameter estimate using H and albedo. The default albedo
    # keeps the result in a practical range when SBDB lacks diameter data.
    effective_albedo = min(max(albedo or 0.14, 0.02), 0.6)
    return (1329.0 / math.sqrt(effective_albedo)) * math.pow(10.0, -absolute_magnitude / 5.0)


def base_mass(diameter_km: float | None, absolute_magnitude: float | None, albedo: float | None) -> int:
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
    elif isinstance(bounds, list | tuple) and len(bounds) >= 2:
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


def generate_composition(profile: dict[str, Any], mass: int, rng: random.Random,) -> dict[str, dict[str, int]]:
    items: dict[str, int] = {}
    fluids: dict[str, int] = {}
    chemicals: dict[str, int] = {}

    for resource_id, bounds in profile.get("items", {}).items():
        items[resource_id] = amount_from_weight(rng, mass, bounds)

    for resource_id, bounds in profile.get("fluids", {}).items():
        fluids[resource_id] = amount_from_weight(rng, mass * FLUID_BUCKET, bounds, minimum=FLUID_BUCKET)

    for resource_id, bounds in profile.get("chemicals", {}).items():
        chemicals[resource_id] = amount_from_weight(rng, mass, bounds)

    for resource_id, bounds in profile.get("traces", {}).items():
        value = maybe_add_trace(rng, mass, bounds)
        if value <= 0:
            continue
        if looks_like_chemical_resource(resource_id):
            chemicals[resource_id] = value
        else:
            items[resource_id] = value

    # TODO: add chemicals back someday
    return {
        "items": items,
        "fluids": fluids,
        # "chemicals": chemicals,
    }


def is_supported_asteroid_row(row: dict[str, Any], slug: str) -> bool:
    if slug in EXCLUDED_MAJOR_BODY_SLUGS:
        return False

    designation = str(row.get("pdes") or "").strip()
    if designation.startswith("S/"):
        return False

    return True


def unique_path(base_path: str, seed: str, used_paths: set[str]) -> str:
    path = base_path
    collision_index = 1
    while path in used_paths:
        collision_index += 1
        path = normalized_slug(f"{base_path}_{seed}_{collision_index}", f"asteroid_{len(used_paths)}")
    used_paths.add(path)
    return path


def to_config(row: dict[str, Any], used_paths: set[str], profiles: dict[str, dict[str, Any]], textures: tuple[str, ...]) -> dict[str, Any] | None:
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
        }
    }


def clear_json_files(output_dir: Path) -> None:
    if not output_dir.exists():
        return
    for path in output_dir.glob("*.json"):
        path.unlink()


def write_chunked(configs: list[dict[str, Any]], output_dir: Path, chunk_size: int) -> int:
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
    return files_written


def build_configs(sbdb: dict[str, Any], profiles: dict[str, dict[str, Any]], textures: tuple[str, ...], amount_limit: int | None) -> list[dict[str, Any]]:
    used_paths: set[str] = set()
    configs: list[dict[str, Any]] = []

    for row in table_rows(sbdb):
        config = to_config(row, used_paths, profiles, textures)
        if config is None:
            continue
        configs.append(config)
        if amount_limit is not None and len(configs) >= amount_limit:
            break

    return configs


def main() -> int:
    args = parse_args()
    amount_limit = parse_amount(args.amount)
    profiles = load_profiles(args.profile_file)
    sbdb = fetch_sbdb(amount_limit)
    configs = build_configs(sbdb, profiles, ASTEROID_TEXTURES, amount_limit)

    if args.clear_output_dir:
        clear_json_files(args.output_dir)

    files_written = write_chunked(configs, args.output_dir, args.chunk_size)

    print(f"Generated {len(configs)} asteroids into {files_written} JSON file(s) at {args.output_dir}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
