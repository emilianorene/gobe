#!/usr/bin/env python3
"""
Build Gobe's per-system metadata index from libretro sources.

Two sources, merged by a normalized name key:
  - BOX ART NAMES: the libretro-thumbnails repos (Named_Boxarts/<name>.png). These filenames
    are exactly what Coil must request, so box art is authoritative here.
  - PLAYER COUNTS: the libretro database .rdb "users" field (MessagePack inside a RARCHDB
    container).

Output: app/src/main/assets/metadata/<tag>.json
    { "<normalized name>": { "boxart": "<canonical thumbnail name>", "players": N }, ... }
The normalized key uses the SAME algorithm as the app's NameNormalizer, so a game's cleaned
displayName matches the index key.

Requires: msgpack (python3 -m venv venv && venv/bin/pip install msgpack)
Run:      venv/bin/python tools/build-metadata-index/build.py
Uses the unauthenticated GitHub API (4 tree calls, well under the 60/hr limit).
"""
import json
import os
import re
import sys
import urllib.parse
import urllib.request

import msgpack

RDB_BASE = "https://raw.githubusercontent.com/libretro/libretro-database/master/rdb/"
GH_TREE = "https://api.github.com/repos/libretro-thumbnails/{repo}/git/trees/master?recursive=1"

# tag -> (libretro RDB name, libretro-thumbnails repo name)
SYSTEMS = {
    "snes": ("Nintendo - Super Nintendo Entertainment System", "Nintendo_-_Super_Nintendo_Entertainment_System"),
    "nes": ("Nintendo - Nintendo Entertainment System", "Nintendo_-_Nintendo_Entertainment_System"),
    "n64": ("Nintendo - Nintendo 64", "Nintendo_-_Nintendo_64"),
    "arcade": ("MAME", "MAME"),
}

TAG_RE = re.compile(r"[\(\[].*?[\)\]]")
LEADING_ARTICLE_RE = re.compile(r"^(a|an|the)\s+")
TRAILING_ARTICLE_RE = re.compile(r",\s*(a|an|the)$")
NONALNUM_RE = re.compile(r"[^a-z0-9]")


def normalize(name: str) -> str:
    """Must match com.gobe.tv.data.metadata.NameNormalizer."""
    s = TAG_RE.sub(" ", name).lower().strip()
    s = TRAILING_ARTICLE_RE.sub("", s)
    s = LEADING_ARTICLE_RE.sub("", s)
    return NONALNUM_RE.sub("", s)


def region_score(name: str) -> int:
    n = name.lower()
    base = 4
    if "(usa" in n:
        base = 0
    elif "(world" in n:
        base = 1
    elif "(europe" in n or "(eur" in n:
        base = 2
    elif "(japan" in n or "(jpn" in n:
        base = 3
    penalty = 0
    for bad in ("(unl", "(pirate", "(beta", "(proto", "(demo", "(sample", "(aftermarket", "(hack"):
        if bad in n:
            penalty += 10
    return base + penalty


def get(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": "gobe-metadata-builder"})
    with urllib.request.urlopen(req) as r:
        return r.read()


def fetch_boxart_names(repo: str) -> dict:
    """normalized -> best-region canonical box-art name (without .png)."""
    out: dict[str, tuple[int, str]] = {}
    try:
        data = json.loads(get(GH_TREE.format(repo=repo)))
    except Exception as e:
        print(f"  !! thumbnails {repo} failed: {e}", file=sys.stderr)
        return {}
    if data.get("truncated"):
        print(f"  !! thumbnails {repo} tree truncated — box-art coverage may be partial", file=sys.stderr)
    n = 0
    for node in data.get("tree", []):
        path = node.get("path", "")
        if not (path.startswith("Named_Boxarts/") and path.endswith(".png")):
            continue
        name = path[len("Named_Boxarts/"):-len(".png")]
        key = normalize(name)
        if not key:
            continue
        n += 1
        score = region_score(name)
        prev = out.get(key)
        if prev is None or score < prev[0]:
            out[key] = (score, name)
    print(f"  boxart {repo}: {n} files -> {len(out)} keys", flush=True)
    return {k: v[1] for k, v in out.items()}


def fetch_rdb_meta(rdb_name: str) -> dict:
    """normalized -> {players, genre, year} from the RDB (best-effort; fields may be missing)."""
    out: dict[str, dict] = {}
    try:
        data = get(RDB_BASE + urllib.parse.quote(rdb_name + ".rdb"))
    except Exception as e:
        print(f"  !! rdb {rdb_name} failed: {e}", file=sys.stderr)
        return {}
    unpacker = msgpack.Unpacker(raw=False, strict_map_key=False)
    unpacker.feed(data[16:])
    n = 0
    for obj in unpacker:
        if not (isinstance(obj, dict) and isinstance(obj.get("name"), str)):
            continue
        key = normalize(obj["name"])
        if not key:
            continue
        cur = out.setdefault(key, {})
        users = obj.get("users")
        if isinstance(users, int) and users >= 1:
            cur["players"] = max(cur.get("players", 0), users)
        genre = obj.get("genre")
        if isinstance(genre, str) and genre and "genre" not in cur:
            cur["genre"] = genre
        year = obj.get("releaseyear")
        if isinstance(year, int) and year > 0 and "year" not in cur:
            cur["year"] = year
        n += 1
    print(f"  rdb {rdb_name}: {n} entries -> {len(out)} keys", flush=True)
    return out


def main():
    repo_root = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
    out_dir = os.path.join(repo_root, "app", "src", "main", "assets", "metadata")
    os.makedirs(out_dir, exist_ok=True)
    for tag, (rdb_name, repo) in SYSTEMS.items():
        print(f"[{tag}]", flush=True)
        boxart = fetch_boxart_names(repo)
        rdb = fetch_rdb_meta(rdb_name)
        keys = set(boxart) | set(rdb)
        index = {}
        for k in keys:
            meta = {}
            if k in boxart:
                meta["boxart"] = boxart[k]
            r = rdb.get(k)
            if r:
                if "players" in r:
                    meta["players"] = r["players"]
                if "genre" in r:
                    meta["genre"] = r["genre"]
                if "year" in r:
                    meta["year"] = r["year"]
            if meta:
                index[k] = meta
        path = os.path.join(out_dir, f"{tag}.json")
        with open(path, "w", encoding="utf-8") as f:
            json.dump(index, f, ensure_ascii=False, separators=(",", ":"))
        print(f"  wrote {path}: {len(index)} keys ({os.path.getsize(path)//1024} KB)", flush=True)


if __name__ == "__main__":
    main()
