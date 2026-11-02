"""Builds src/main/resources/com/skillinghelper/money/money-methods.tsv, the Money Making plugin's method list.

Everything comes from the OSRS Wiki (CC BY-NC-SA 3.0, https://oldschool.runescape.wiki):

  * the "money_making_guide" Bucket: one row per Money making guide page, with profit, skills, quests, inputs and
    outputs exactly as the wiki's own money making tables use them;
  * each guide page's wikitext, for its Location, Other requirements, recurring times and Details text;
  * each Location page's infobox map, for a spot to walk to, and a shop's owner, for an NPC to click;
  * prices.runescape.wiki's item mapping, to turn item names into ids so the plugin can update profit with live
    GE prices.

The plugin itself never contacts the wiki for this list; run this script again to refresh it.

Usage: python3 build_money_methods.py <cache dir> <out.tsv>
Downloads are cached in <cache dir>; delete it to fetch again.
"""
import json
import os
import re
import sys
import time
import urllib.parse
import urllib.request

API = "https://oldschool.runescape.wiki/api.php"
MAPPING = "https://prices.runescape.wiki/api/v1/osrs/mapping"
UA = {"User-Agent": "skilling-helper RuneLite plugin data build (github.com/Feelsrat/skilling-helper)"}
SKILLS = ["Attack", "Strength", "Defence", "Ranged", "Prayer", "Magic", "Runecraft", "Construction", "Hitpoints",
          "Agility", "Herblore", "Thieving", "Crafting", "Fletching", "Slayer", "Hunter", "Mining", "Smithing",
          "Fishing", "Cooking", "Firemaking", "Woodcutting", "Farming", "Sailing"]


def fetch(url):
    for i in range(5):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=60) as r:
                return json.load(r)
        except Exception as e:  # network hiccups: back off and retry
            print("retry", url[:120], e, file=sys.stderr)
            time.sleep(2 ** i)
    raise SystemExit("failed: " + url)


def api(params):
    return fetch(API + "?" + urllib.parse.urlencode(params))


def cached(cache, name, build):
    path = os.path.join(cache, name)
    if not os.path.exists(path):
        json.dump(build(), open(path, "w"))
    return json.load(open(path))


def page_texts(titles):
    """Wikitext of each title (following redirects), keyed by the title as asked."""
    out = {}
    titles = sorted(set(titles))
    for i in range(0, len(titles), 50):
        batch = titles[i:i + 50]
        d = api({"action": "query", "prop": "revisions", "rvprop": "content", "rvslots": "main",
                 "titles": "|".join(batch), "redirects": 1, "format": "json", "formatversion": 2})
        q = d["query"]
        alias = {}
        for kind in ("normalized", "redirects"):
            for n in q.get(kind, []):
                alias[n["from"]] = n["to"]
        text = {p["title"]: p["revisions"][0]["slots"]["main"]["content"] for p in q["pages"] if "revisions" in p}
        for t in batch:
            r, hops = t, 0
            while r in alias and hops < 5:
                r, hops = alias[r], hops + 1
            if r.split("#")[0] in text:
                out[t] = text[r.split("#")[0]]
        time.sleep(0.5)
    return out


def field(text, name):
    """A template parameter's raw value, up to the next top-level parameter."""
    m = re.search(r"^\|\s*" + re.escape(name) + r"\s*=(.*?)(?=^\||^\}\}|\Z)", text, re.M | re.S)
    return m.group(1).strip() if m else ""


def links(s):
    return [l.strip() for l in re.findall(r"\[\[([^\]|#]+)", s)]


def plain(s):
    """Wikitext to plain text, keeping the words of links and the common inline templates."""
    s = re.sub(r"<ref[^>]*/>|<ref.*?</ref>", "", s, flags=re.S)
    s = re.sub(r"<!--.*?-->", "", s, flags=re.S)
    for _ in range(4):
        s = re.sub(r"\{\{\s*(?:plink|plinkt|plinkp|Wp|Skill clickpic)\s*\|([^|{}]*)[^{}]*\}\}", r"\1", s, flags=re.I)
        s = re.sub(r"\{\{\s*SCP\s*\|([^|{}]*)\|([^|{}]*)[^{}]*\}\}", r"\2 \1", s, flags=re.I)
        s = re.sub(r"\{\{\s*Coins\s*\|([0-9,]+)[^{}]*\}\}", lambda m: "{:,} coins".format(int(m.group(1).replace(",", ""))), s, flags=re.I)
        s = re.sub(r"\{\{[^{}]*\}\}", "", s)
    s = re.sub(r"\[\[(?:File|Image):[^\]]*\]\]", "", s, flags=re.I)
    s = re.sub(r"\[\[[^\]|]*\|([^\]]*)\]\]", r"\1", s)
    s = re.sub(r"\[\[([^\]]*)\]\]", r"\1", s)
    s = re.sub(r"\[https?://\S+ ([^\]]*)\]", r"\1", s)
    s = re.sub(r"'{2,}", "", s)
    s = re.sub(r"<br\s*/?>", " ", s)
    s = re.sub(r"<[^>]+>", "", s)
    s = re.sub(r"^[*#:;]+\s*", "- ", s, flags=re.M)
    s = re.sub(r"\s*\n\s*", " ", s)
    s = re.sub(r"\s+([.,;:])", r"\1", s)
    s = re.sub(r"\(\s*\)", "", s)
    return re.sub(r"\s{2,}", " ", s).strip()


def clean(s):
    return (s or "").replace("\t", " ").replace("\n", " ").strip() or "-"


def skill_reqs(raw):
    """Hard skill levels from the wiki's skill list, plus the lines without a level (e.g. "High melee stats") as
    text. Lines marked optional are left out."""
    reqs, notes = {}, []
    for line in raw.split("\n"):
        if "optional" in line.lower():
            continue
        found = re.findall(r'data-skill="([A-Za-z]+)" data-level="(\d+)', line)
        for skill, level in found:
            if skill in SKILLS:
                reqs[skill] = max(reqs.get(skill, 0), int(level))
        text = plain(line).lstrip("- ").strip()
        if not found and text and text.lower().rstrip(".") != "none":
            notes.append(text)
    return reqs, notes


def map_spot(infobox):
    """(x, y, plane, radius) from an infobox's {{Map}}, or None for other maps (instances, interiors)."""
    m = re.search(r"\|\s*map\s*=\s*\{\{Map\|([^}]*)\}\}", infobox, re.I)
    if not m:
        return None
    args = m.group(1).split("|")
    kv = dict(a.split("=", 1) for a in args if "=" in a)
    if kv.get("mapID", "0").strip() not in ("0", "-1", ""):
        return None
    plane = int(kv.get("plane", "0").strip() or 0)
    if "x" in kv and "y" in kv:
        return int(kv["x"]), int(kv["y"]), plane, 10
    pts = [tuple(int(v) for v in a.split(",")[:2]) for a in args if re.fullmatch(r"\s*\d+,\d+(,\d+)?\s*", a)]
    if not pts:
        return None
    xs, ys = [p[0] for p in pts], [p[1] for p in pts]
    if len(pts) == 1:
        rx = int(kv.get("rectX", "10"))
        ry = int(kv.get("rectY", "10"))
        return xs[0], ys[0], plane, max(5, min(40, max(rx, ry)))
    radius = max(max(xs) - min(xs), max(ys) - min(ys)) // 2 + 2
    return (min(xs) + max(xs)) // 2, (min(ys) + max(ys)) // 2, plane, max(5, min(40, radius))


def hours(raw):
    """'0.5' hours, '30 minutes', '1440 minutes', '7 days' to hours."""
    m = re.search(r"([\d.]+)\s*(minute|min|hour|day|week)?", raw or "", re.I)
    if not m:
        return 0
    v = float(m.group(1))
    unit = (m.group(2) or "hour").lower()
    return v / 60 if unit.startswith("min") else v * 24 if unit == "day" else v * 168 if unit == "week" else v


MAX_ITEMS = 25


def items(entries, ids, kph, per_kill):
    """id:qtyPerHour:wikiValue:name per item, so the plugin can redo profit with live prices. Unknown names get id -1.
    Boss drop tables list hundreds of items, so only the ones worth the most per hour are kept: the rest barely move
    the total when prices change."""
    rows = []
    for it in entries:
        qty = float(it.get("qty") or 0)
        per_hour = qty if (it.get("isph") or not per_kill) else qty * kph
        rows.append((per_hour * float(it.get("value") or 0), per_hour, it))
    rows.sort(key=lambda r: -abs(r[0]))
    out = []
    for _, per_hour, it in rows[:MAX_ITEMS]:
        out.append("%d:%s:%s:%s" % (ids.get(it["name"].lower(), -1), round(per_hour, 3),
                                    round(float(it.get("value") or 0), 2), it["name"].replace(":", " ").replace(";", " ")))
    return ";".join(out) or "-"


def main(cache, out_path):
    os.makedirs(cache, exist_ok=True)
    rows = cached(cache, "mmg.json", lambda: api({"action": "bucket", "format": "json", "query":
        "bucket('money_making_guide').select('page_name','value','recurring','json').limit(5000).run()"})["bucket"])
    texts = cached(cache, "texts.json", lambda: page_texts([r["page_name"] for r in rows]))
    loc_names = set()
    for t in texts.values():
        loc_names.update(l[0].upper() + l[1:] for l in links(field(t, "Location"))[:1])
    loc_texts = cached(cache, "locations.json", lambda: page_texts(loc_names))
    mapping = cached(cache, "mapping.json", lambda: fetch(MAPPING))
    ids = {m["name"].lower(): m["id"] for m in mapping}

    lines = [
        "# Money making methods, generated by tools/money/build_money_methods.py on %s. Do not edit by hand."
        % time.strftime("%Y-%m-%d"),
        "# Source: OSRS Wiki money making guides (CC BY-NC-SA 3.0), https://oldschool.runescape.wiki/w/Money_making_guide",
        "# page activity category skillCategory members intensity recurringHours/everyHours wikiGpHr skills quests "
        "other location spot(x,y,plane,radius) npc inputs outputs details",
    ]
    for r in sorted(rows, key=lambda r: r["page_name"]):
        j = json.loads(r["json"])
        t = texts.get(r["page_name"], "")
        p = j.get("prices", {})
        per_kill = bool(j.get("isperkill"))
        kph = float(p.get("default_kph") or 0)
        recurring = "Mmgtable recurring" in t
        rec = "-"
        if recurring:
            rec = "%s/%s" % (round(hours(field(t, "Activity Time")), 3), round(hours(field(t, "Recurrence Time")), 2))
        loc_raw = field(t, "Location")
        loc_links = links(loc_raw)
        loc_name = plain(loc_raw)
        spot, npc = None, "-"
        if loc_links:
            lt = loc_texts.get(loc_links[0][0].upper() + loc_links[0][1:], "")
            spot = map_spot(lt)
            owner = links(field(lt, "owner"))
            if "Infobox Shop" in lt and owner:
                npc = owner[0]
        quests = [q for q in links(j.get("quest") or "") if q.lower() != "none"]
        skills, skill_notes = skill_reqs(j.get("skill") or "")
        other = plain(field(t, "Other"))
        other = "" if other.lower().rstrip(".") == "none" else other
        other = "; ".join([n for n in skill_notes] + ([other] if other else []))
        lines.append("\t".join([
            r["page_name"],
            clean(plain(j.get("activity", ""))),
            clean(j.get("category")),
            clean(j.get("skillcategory")),
            "1" if j.get("members") else "0",
            clean(j.get("intensity")),
            rec,
            str(int(round(float(r["value"] or 0)))),
            ",".join("%s:%d" % kv for kv in sorted(skills.items())) or "-",
            ";".join(quests) or "-",
            clean(other),
            clean(loc_name),
            "%d,%d,%d,%d" % spot if spot else "-",
            clean(npc),
            items(j.get("inputs", []), ids, kph, per_kill),
            items(j.get("outputs", []), ids, kph, per_kill),
            clean(plain(field(t, "Details"))[:1200]),
        ]))
    with open(out_path, "w") as f:
        f.write("\n".join(lines) + "\n")
    print(len(lines) - 3, "methods,", sum(1 for l in lines[3:] if l.split("\t")[12] != "-"), "with a map spot")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
