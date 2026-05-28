import hashlib
import json
import math
import random
import re
import urllib.parse
import urllib.request
from pathlib import Path

MOD_ID = "asteroidmining"
COUNT = 2000

# TODO it's actually without main
OUT_DIR = Path(f"src/main/generated/resources/data/{MOD_ID}/asteroids")
PROFILE_FILE = Path("tools/asteroid_profiles.json")

SBDB_URL = "https://ssd-api.jpl.nasa.gov/sbdb_query.api"

FIELDS = [
    "full_name",
    "diameter",
    "a",
    "e",
    "i",
    "spec_T",
    "spec_B",
    "class"
]

BUCKET = 1000


def fetch_sbdb():
    params = {
        "fields": ",".join(FIELDS),
        "sb-kind": "a",
        "limit": str(COUNT),
        "sort": "full_name"
    }

    url = SBDB_URL + "?" + urllib.parse.urlencode(params)
    with urllib.request.urlopen(url, timeout=90) as response:
        return json.loads(response.read().decode("utf-8"))


def safe_float(value, default):
    try:
        if value is None or value == "":
            return default
        return float(value)
    except Exception:
        return default


def clean_id(name):
    value = name.lower().strip()
    value = re.sub(r"[^a-z0-9_]+", "_", value)
    value = re.sub(r"_+", "_", value).strip("_")
    return f"{value or 'asteroid'}"


def pick_taxonomy(row, rng):
    spec = ((row.get("spec_T") or row.get("spec_B") or "") + "").upper().strip()

    if spec.startswith("C") or spec.startswith("B") or spec.startswith("F") or spec.startswith("G"):
        return "C"
    if spec.startswith("S") or spec.startswith("Q") or spec.startswith("A") or spec.startswith("L") or spec.startswith("K"):
        return "S"
    if spec.startswith("M") or spec.startswith("X") or spec.startswith("E"):
        return "M"
    if spec.startswith("V"):
        return "V"
    if spec.startswith("D") or spec.startswith("P") or spec.startswith("T"):
        return "D"

    # Real datasets are sparse. Fallback distribution approximates useful gameplay variety.
    return rng.choices(
        ["C", "S", "M", "V", "D", "ICY"],
        weights=[38, 34, 10, 5, 8, 5],
        k=1
    )[0]


def base_mass(diameter_km):
    # Gameplay mass, not real kg. Scales sanely for Minecraft.
    d = max(0.05, min(diameter_km, 120.0))
    return int(12000 * math.pow(d, 1.35))


def amount_from_weight(rng, mass, bounds, minimum=1):
    lo, hi = bounds
    weight = rng.uniform(lo, hi)
    noise = rng.uniform(0.82, 1.18)
    return max(minimum, int(mass * weight * noise))


def maybe_add_trace(rng, mass, bounds):
    # Trace materials are not guaranteed.
    if rng.random() > 0.35:
        return 0
    return amount_from_weight(rng, mass, bounds, minimum=1)


def generate_resources(profile, mass, rng):
    items = {}
    fluids = {}
    chemicals = {}

    for rid, bounds in profile.get("items", {}).items():
        items[rid] = amount_from_weight(rng, mass, bounds)

    for rid, bounds in profile.get("fluids", {}).items():
        fluids[rid] = amount_from_weight(rng, mass * BUCKET, bounds, minimum=BUCKET)

    for rid, bounds in profile.get("chemicals", {}).items():
        chemicals[rid] = amount_from_weight(rng, mass, bounds)

    for rid, bounds in profile.get("traces", {}).items():
        value = maybe_add_trace(rng, mass, bounds)
        if value > 0:
            if rid.startswith("mekanism:") and ("slurry" in rid or "hydrogen" in rid or "oxygen" in rid or "dioxide" in rid or "ethene" in rid or "water" in rid):
                chemicals[rid] = value
            else:
                items[rid] = value

    return {
        "items": items,
        "fluids": fluids,
        "chemicals": chemicals
    }


def orbital_speed(a_au):
    # Relative gameplay speed. Inner objects move faster.
    a = max(0.3, a_au)
    return round(max(0.03, min(0.35, 0.28 / math.sqrt(a))), 4)

def stable_seed(value):
    digest = hashlib.sha256(value.encode("utf-8")).digest()
    return int.from_bytes(digest[:8], "big")

def main():
    profiles = json.loads(PROFILE_FILE.read_text(encoding="utf-8"))

    OUT_DIR.mkdir(parents=True, exist_ok=True)

    for old in OUT_DIR.glob("*.json"):
        old.unlink()

    payload = fetch_sbdb()
    fields = payload["fields"]

    written = 0

    for index, raw in enumerate(payload["data"]):
        row = dict(zip(fields, raw))
        name = (row.get("full_name") or f"Generated Asteroid {index}").strip()

        seed = stable_seed(name)
        rng = random.Random(seed)

        diameter = safe_float(row.get("diameter"), rng.uniform(0.2, 12.0))
        semi_major_axis = safe_float(row.get("a"), rng.uniform(1.6, 4.2))
        eccentricity = safe_float(row.get("e"), rng.uniform(0.0, 0.35))
        inclination = safe_float(row.get("i"), rng.uniform(0.0, 25.0))

        taxonomy = pick_taxonomy(row, rng)
        profile = profiles[taxonomy]
        mass = base_mass(diameter)
        full_name = clean_id(name)

        # TODO: missing id inside json
        # TODO: also exclude planets and their moons? (problem: they have own texture)
        # TODO: also texture missing
        # TODO: why game_diameter and diameter_km?
        asteroid = {
            "id": MOD_ID + ":" + full_name,
            "name": name,
            "taxonomy": taxonomy,
            "analogue": profile["analogue"],
            "diameter_km": round(diameter, 4),
            "game_diameter": max(1, min(12, round(math.log2(diameter + 1) + 1))),
            "orbit": {
                "central_body": "Sun",
                "semi_major_axis": round(semi_major_axis * 100.0, 4),
                "eccentricity": round(eccentricity, 5),
                "inclination": round(inclination, 4),
                "orbital_speed": orbital_speed(semi_major_axis),
                "clockwise": True,
                "angle": round((index * 137.508) % 360.0, 3)
            },
            "composition": generate_resources(profile, mass, rng)
        }

        (OUT_DIR / f"{full_name}.json").write_text(
            json.dumps(asteroid, indent=2, sort_keys=False),
            encoding="utf-8"
        )
        written += 1

    print(f"Generated {written} asteroid files in {OUT_DIR}")


if __name__ == "__main__":
    main()