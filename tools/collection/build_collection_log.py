#!/usr/bin/env python3
"""Builds src/main/resources/com/skillinghelper/collection/collection-log.tsv, the Collection Log Guide's slot list.

Everything comes from the OSRS Wiki (CC BY-NC-SA 3.0, https://oldschool.runescape.wiki):

  * the "Collection log" page: every tab (Bosses, Raids, Clues, Minigames, Other), every entry in in-game order,
    the items of each entry in in-game order, and the page each entry links to;
  * Module:Collection log/data.json: each slot's item id (matched by name within its entry);
  * Module:Collection log/completion.json: the share of tracked players who have each slot, which the plugin uses
    as "how quick is this slot" (common slots come first);
  * each linked page's wikitext: its {{Map}} (overworld only) for a place to walk to, its Slayer level, and its
    opening sentence as a one-line "what is this".

Rows (tab separated):
  E  tab  entry name  wiki page  x y plane (or -)  slayer level  intro sentence
  I  item id  item name  % of players who have it (-1 if unknown)
Item rows belong to the entry row above them.

The plugin itself never contacts the wiki for this list; run this script again to refresh it.

Usage: python3 tools/collection/build_collection_log.py <cache dir> <out.tsv>
"""
import json
import os
import re
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "money"))
from build_money_methods import api, cached, page_texts, plain, clean  # noqa: E402

TABS = ["Bosses", "Raids", "Clues", "Minigames", "Other"]


def module(name):
    d = api({"action": "query", "prop": "revisions", "rvprop": "content", "rvslots": "main",
             "titles": "Module:Collection log/" + name, "format": "json", "formatversion": 2})
    return json.loads(d["query"]["pages"][0]["revisions"][0]["slots"]["main"]["content"])


def clog_page():
    d = api({"action": "parse", "page": "Collection log", "prop": "wikitext", "format": "json", "formatversion": 2})
    return d["parse"]["wikitext"]


def entries(text):
    """[(tab, entry, linked page, [item names])] in page order."""
    out, tab, cur = [], None, None
    for line in text.split("\n"):
        m = re.match(r"^==([^=].*?)==\s*$", line)
        if m:
            tab = m.group(1).strip() if m.group(1).strip() in TABS else None
            cur = None
            continue
        if tab is None:
            continue
        m = re.match(r"^===([^=].*?)===\s*$", line)
        if m:
            cur = [tab, m.group(1).strip(), None, []]
            out.append(cur)
            continue
        if cur is None:
            continue
        if cur[2] is None and re.match(r"^There (are|is) ", line):
            link = re.search(r"\[\[([^\]|#]+)", line)
            cur[2] = link.group(1).strip() if link else ""
        for name in re.findall(r"\{\{\s*plink\s*\|([^|}]+)", line, re.I):
            cur[3].append(name.strip())
    return out


def real(point):
    """Instances (Cerberus' rooms, raids inside) use made-up coordinates far south of the real map: drop them."""
    return point if point and point[1] >= 2400 else None


def map_point(text):
    """(x, y, plane) of the page's first {{Map}} or monster location line on the real map, or None."""
    return real(infobox_map(text)) or real(loc_line(text))


def loc_line(text):
    m = re.search(r"\{\{\s*LocLine(.*?)\}\}", text, re.S | re.I)
    if not m:
        return None
    xy = re.search(r"\|\s*x:(\d+),\s*y:(\d+)", m.group(1))
    plane = re.search(r"\|\s*plane\s*=\s*(\d+)", m.group(1))
    return (int(xy.group(1)), int(xy.group(2)), int(plane.group(1)) if plane else 0) if xy else None


def infobox_map(text):
    for m in re.finditer(r"\{\{\s*Map\s*\|([^{}]*)\}\}", text, re.I):
        args = [a.strip() for a in m.group(1).split("|")]
        kv = dict(a.split("=", 1) for a in args if "=" in a)
        kv = {k.strip(): v.strip() for k, v in kv.items()}
        if kv.get("mapID", "0") not in ("0", "-1", ""):
            continue
        plane = int(kv.get("plane", "0") or 0) if kv.get("plane", "0").isdigit() else 0
        if kv.get("x", "").isdigit() and kv.get("y", "").isdigit():
            return int(kv["x"]), int(kv["y"]), plane
        for a in args:
            p = re.fullmatch(r"(\d+),(\d+)(?:,(\d+))?", a)
            if p:
                return int(p.group(1)), int(p.group(2)), int(p.group(3) or plane)
    return None


def intro(text):
    """The page's first sentence of prose, plain."""
    body = re.sub(r"\{\{(?:[^{}]|\{\{(?:[^{}]|\{\{[^{}]*\}\})*\}\})*\}\}", "", text)
    for para in body.split("\n"):
        para = para.strip()
        if not para or para[0] in "|{}[=*#:!<_" and not para.startswith("[["):
            continue
        s = plain(para)
        if len(s) < 25:
            continue
        m = re.match(r"(.+?[.!?])(\s|$)", s)
        return (m.group(1) if m else s)[:300]
    return ""


def slayer_level(text):
    m = re.search(r"^\|\s*slaylvl\d*\s*=\s*(\d+)", text, re.M)
    return int(m.group(1)) if m else 0


def tab_key(entry, by_tab):
    key = entry.lower()
    if key not in by_tab:
        key = key.replace(" treasure trail rewards (rare)", " treasure trails (rare)")
    return key


def infobox_id(text, allowed):
    """The first id in the item page's infobox (|id = or |id1 = ...) that is one of the entry's ids."""
    for m in re.finditer(r"^\|\s*id\d*\s*=\s*([\d, ]+)", text, re.M):
        for part in m.group(1).split(","):
            if part.strip().isdigit() and int(part) in allowed:
                return int(part)
    return None


def main(cache, out_path):
    os.makedirs(cache, exist_ok=True)
    data = cached(cache, "data.json", lambda: module("data.json"))
    completion = cached(cache, "completion.json", lambda: module("completion.json"))
    text = cached(cache, "clog.json", clog_page)
    rows = entries(text)

    by_tab = {}
    for item in data:
        for t in item["tabs"]:
            by_tab.setdefault(t.lower(), {}).setdefault(item["name"].lower(), []).append(item["id"])

    pages = sorted({r[2] for r in rows if r[2]})
    texts = cached(cache, "pages.json", lambda: page_texts(pages))

    # Names the module spells differently (variants like "Decorative helm (red)"): the item page's infobox id,
    # kept only if that id belongs to the entry
    unmatched = sorted({n for tab, entry, page, names in rows for n in names
                        if n.lower() not in by_tab.get(tab_key(entry, by_tab), {})})
    item_pages = cached(cache, "items.json", lambda: page_texts(unmatched))
    in_tab = {}
    for item in data:
        for t in item["tabs"]:
            in_tab.setdefault(t.lower(), set()).add(item["id"])

    out = ["# Collection log slots from the OSRS Wiki (CC BY-NC-SA 3.0), built by tools/collection/"
           "build_collection_log.py. E = entry, I = item of the entry above."]
    missing = []
    for tab, entry, page, names in rows:
        # The wiki's section titles and the module's tab names differ a little ("... Rewards (Rare)")
        key = tab_key(entry, by_tab)
        ids = by_tab.get(key, {})
        page_text = texts.get(page, "") if page else ""
        point = map_point(page_text) if page_text else None
        out.append("\t".join(["E", tab, entry, clean((page or "Collection_log#" + entry).replace(" ", "_")),
                              " ".join(map(str, point)) if point else "-", str(slayer_level(page_text)),
                              clean(intro(page_text))]))
        seen = set()
        for name in names:
            # Same-named slots (Mysterious page 1-5, Ancient page 1-26): the next id of that name not used yet
            item_id = next((i for i in ids.get(name.lower(), []) if i not in seen), None)
            if item_id is None:
                item_id = infobox_id(item_pages.get(name, ""), in_tab.get(key, set()) - seen)
            if item_id is None:
                missing.append(entry + ": " + name)
                continue
            if item_id in seen:
                continue
            seen.add(item_id)
            pct = completion.get(str(item_id), -1)
            out.append("\t".join(["I", str(item_id), clean(name), str(pct)]))
    with open(out_path, "w") as f:
        f.write("\n".join(out) + "\n")
    print(len(rows), "entries,", sum(1 for l in out if l.startswith("I")), "items", file=sys.stderr)
    for m in missing:
        print("no id:", m, file=sys.stderr)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
