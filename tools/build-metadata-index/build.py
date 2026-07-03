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

RECOMMENDED FLAG: optionally derived from IGDB ratings (total_rating / total_rating_count) per
platform via select_recommended(); baked in as "recommended": true. Needs IGDB_CLIENT_ID and
IGDB_SECRET env vars (free, from a Twitch dev app) — maintainer-side only; if absent, the stage is
skipped and the index builds without the flag. Run `build.py --self-test` to check the policy (no
network).

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

# msgpack is imported lazily inside fetch_rdb_meta so `--self-test` (and the IGDB-only path) work
# without it installed.

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


def select_recommended(entries, rating_threshold=75, min_votes=5, floor=100, cap=200):
    """entries: iterable of (key, rating, votes). Return the set of recommended keys.

    Policy: only games with votes >= min_votes are candidates; rank them by rating desc; include at
    least the top `floor` (or all candidates if fewer) and everyone at/above `rating_threshold`,
    capped at `cap`. Quality decides; floor/cap keep the set neither too sparse nor too large.
    """
    ranked = sorted(
        [(k, r) for (k, r, v) in entries if r is not None and (v or 0) >= min_votes],
        key=lambda kr: kr[1], reverse=True,
    )
    n_pass = sum(1 for _, r in ranked if r >= rating_threshold)
    n = max(n_pass, min(floor, len(ranked)))  # floor: at least top-`floor` (bounded by size)
    n = min(n, cap)                            # soft cap
    return {k for k, _ in ranked[:n]}


def _self_test():
    # threshold selects high-rated
    assert select_recommended([("a", 90, 50), ("b", 80, 50), ("c", 60, 50)],
                              rating_threshold=75, min_votes=5, floor=1, cap=100) == {"a", "b"}
    # min_votes drops obscure games
    assert select_recommended([("a", 99, 1)], min_votes=5, floor=0, cap=100) == set()
    # floor: few pass threshold -> include the top-floor anyway
    assert select_recommended([("a", 70, 50), ("b", 65, 50), ("c", 60, 50)],
                              rating_threshold=90, min_votes=5, floor=2, cap=100) == {"a", "b"}
    # cap: many pass -> exactly cap
    big = [(f"g{i}", 80 + (i % 10), 50) for i in range(300)]
    assert len(select_recommended(big, rating_threshold=75, min_votes=5, floor=100, cap=200)) == 200
    # fewer candidates than floor -> all candidates
    assert select_recommended([("a", 70, 9), ("b", 50, 9)],
                              rating_threshold=90, min_votes=5, floor=100, cap=200) == {"a", "b"}
    print("select_recommended self-test OK")


def get(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": "gobe-metadata-builder"})
    with urllib.request.urlopen(req) as r:
        return r.read()


def get_with(req: urllib.request.Request) -> bytes:
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
    import msgpack
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


IGDB_PLATFORM = {"snes": 19, "nes": 18, "n64": 4, "arcade": 52}
IGDB_TOKEN_URL = "https://id.twitch.tv/oauth2/token"
IGDB_GAMES_URL = "https://api.igdb.com/v4/games"


def igdb_token(client_id: str, secret: str) -> str:
    body = urllib.parse.urlencode({
        "client_id": client_id, "client_secret": secret, "grant_type": "client_credentials",
    }).encode()
    req = urllib.request.Request(IGDB_TOKEN_URL, data=body, method="POST")
    with urllib.request.urlopen(req) as r:
        return json.loads(r.read())["access_token"]


def igdb_recommended(tag: str, client_id: str, token: str) -> set:
    """Return the set of normalized keys recommended for this system, or empty on any failure."""
    platform = IGDB_PLATFORM.get(tag)
    if platform is None:
        return set()
    best: dict[str, tuple[float, int]] = {}  # key -> (rating, votes)
    offset = 0
    while True:
        q = (f"fields name,total_rating,total_rating_count; "
             f"where platforms = ({platform}) & total_rating != null & total_rating_count >= 5; "
             f"sort total_rating desc; limit 500; offset {offset};")
        req = urllib.request.Request(
            IGDB_GAMES_URL, data=q.encode(),
            headers={"Client-ID": client_id, "Authorization": f"Bearer {token}",
                     "User-Agent": "gobe-metadata-builder"}, method="POST",
        )
        try:
            rows = json.loads(get_with(req))
        except Exception as e:
            print(f"  !! IGDB {tag} failed: {e}", file=sys.stderr)
            return set()
        if not rows:
            break
        for row in rows:
            key = normalize(row.get("name", ""))
            if not key:
                continue
            rating = row.get("total_rating")
            votes = row.get("total_rating_count", 0)
            prev = best.get(key)
            if prev is None or (rating or 0) > prev[0]:
                best[key] = (rating or 0, votes or 0)
        offset += 500
        if len(rows) < 500:
            break
    recommended = select_recommended((k, r, v) for k, (r, v) in best.items())
    print(f"  {tag}: IGDB recommended = {len(recommended)}", flush=True)
    return recommended


def main():
    repo_root = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
    out_dir = os.path.join(repo_root, "app", "src", "main", "assets", "metadata")
    os.makedirs(out_dir, exist_ok=True)

    # IGDB auth once (maintainer-side only). Without creds, the recommended flag is skipped and the
    # index still builds normally.
    client_id = os.environ.get("IGDB_CLIENT_ID")
    secret = os.environ.get("IGDB_SECRET")
    igdb_tok = None
    if client_id and secret:
        try:
            igdb_tok = igdb_token(client_id, secret)
        except Exception as e:
            print(f"  !! IGDB auth failed, recommended flag skipped: {e}", file=sys.stderr)
    else:
        print("  (no IGDB_CLIENT_ID/IGDB_SECRET; recommended flag skipped)", file=sys.stderr)

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
        # Merge the IGDB-derived recommended flag (tag even games without boxart/players).
        if igdb_tok:
            for key in igdb_recommended(tag, client_id, igdb_tok):
                index.setdefault(key, {})["recommended"] = True
        path = os.path.join(out_dir, f"{tag}.json")
        with open(path, "w", encoding="utf-8") as f:
            json.dump(index, f, ensure_ascii=False, separators=(",", ":"))
        print(f"  wrote {path}: {len(index)} keys ({os.path.getsize(path)//1024} KB)", flush=True)


if __name__ == "__main__":
    if "--self-test" in sys.argv:
        _self_test()
        sys.exit(0)
    main()
