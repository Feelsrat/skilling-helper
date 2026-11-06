#!/usr/bin/env python3
"""Builds the Quest Order Guide data from the OSRS Wiki.

Writes two files into the given directory (normally src/main/resources/com/skillinghelper/quests):

* order.tsv  - the wiki's Optimal quest guide rows, main and ironman routes: route, kind, ref, label, note
* quests.tsv - every quest and miniquest those routes name, from its page's {{Quest details}}, {{Infobox Quest}}
               and {{Quest rewards}}: name, members, difficulty, length, start, startmap,
               startNpc, skills, quests,
               questsStarted, qp, combat, items, kills, rewards

Run from anywhere:

    python3 tools/quests/build_quests.py src/main/resources/com/skillinghelper/quests
"""
import json
import os
import re
import sys
import time
import urllib.parse
import urllib.request

API = "https://oldschool.runescape.wiki/api.php"
UA = {"User-Agent": "skilling-helper quest data builder"}
ROUTES = {"MAIN": "Optimal quest guide", "IRONMAN": "Optimal quest guide/Ironman"}
DIARY_REGIONS = {
    "Ardougne": "ARDOUGNE", "Desert": "DESERT", "Falador": "FALADOR", "Fremennik": "FREMENNIK",
    "Kandarin": "KANDARIN", "Karamja": "KARAMJA", "Kourend & Kebos": "KOUREND", "Lumbridge & Draynor": "LUMBRIDGE",
    "Morytania": "MORYTANIA", "Varrock": "VARROCK", "Western Provinces": "WESTERN", "Wilderness": "WILDERNESS",
}
SKILLS = ["Attack", "Strength", "Defence", "Ranged", "Prayer", "Magic", "Runecraft", "Construction", "Hitpoints",
          "Agility", "Herblore", "Thieving", "Crafting", "Fletching", "Slayer", "Hunter", "Mining", "Smithing",
          "Fishing", "Cooking", "Firemaking", "Woodcutting", "Farming", "Sailing"]


def get(params):
    q = urllib.parse.urlencode(dict(params, format="json", formatversion="2"))
    for attempt in range(5):
        try:
            with urllib.request.urlopen(urllib.request.Request(API + "?" + q, headers=UA)) as r:
                return json.load(r)
        except Exception:
            if attempt == 4:
                raise
            time.sleep(2 ** attempt)


def wikitext(page):
    return get({"action": "parse", "page": page, "prop": "wikitext"})["parse"]["wikitext"]


def pages(titles):
    """Wikitext of many pages, following redirects. Returns {asked title: text}."""
    out = {}
    titles = list(dict.fromkeys(titles))
    for i in range(0, len(titles), 40):
        chunk = titles[i:i + 40]
        d = get({"action": "query", "prop": "revisions", "rvprop": "content", "rvslots": "main",
                 "titles": "|".join(chunk), "redirects": "1"})["query"]
        back = {}
        for m in d.get("normalized", []):
            back[m["to"]] = m["from"]
        for m in d.get("redirects", []):
            back[m["to"]] = back.get(m["from"], m["from"])
        for p in d["pages"]:
            if "revisions" not in p:
                continue
            asked = back.get(p["title"], p["title"])
            out[asked] = p["revisions"][0]["slots"]["main"]["content"]
    return out


def clean(s):
    s = re.sub(r"<ref[^>]*/>|<ref[^>]*>.*?</ref>", "", s, flags=re.S)
    s = re.sub(r"<!--.*?-->", "", s, flags=re.S)
    s = re.sub(r"\{\{(?:SCP|Skill clickpic)\|([^|}]+)\|?([^|}]*)[^}]*\}\}",
               lambda m: ("" if "=" in m.group(2) else m.group(2) + " " + m.group(1)).strip()
               or m.group(1), s)
    s = re.sub(r"\{\{Coins\|([^}|]+)[^}]*\}\}", r"\1 coins", s)
    s = re.sub(r"\{\{FloorNumber\|uk=(\d)\}\}", lambda m: ["ground floor", "first floor", "second floor",
                                                          "third floor"][min(3, int(m.group(1)))], s)
    s = re.sub(r"\{\{fairycode\|([a-z]+)\}\}", lambda m: m.group(1).upper(), s)
    s = re.sub(r"\{\{(?:plink|plinkp|Plink)\|([^|}]+)[^}]*\}\}", r"\1", s)
    s = re.sub(r"\{\{Boostable\|yes\}\}", "(boostable)", s, flags=re.I)
    s = re.sub(r"\{\{Boostable\|no\}\}", "(not boostable)", s, flags=re.I)
    while re.search(r"\{\{[^{}]*\}\}", s):
        s = re.sub(r"\{\{[^{}]*\}\}", "", s)
    s = re.sub(r"\[\[File:[^\]]*\]\]", "", s)
    s = re.sub(r"\[\[[^|\]]*\|([^\]]+)\]\]", r"\1", s)
    s = re.sub(r"\[\[([^\]]+)\]\]", r"\1", s)
    s = re.sub(r"\[https?://\S+ ([^\]]+)\]", r"\1", s)
    s = re.sub(r"'''?", "", s)
    s = re.sub(r"<[^>]+>", "", s)
    s = s.replace("\t", " ").replace("&nbsp;", " ")
    s = re.sub(r"\b(" + "|".join(SKILLS + ["Combat"]) + r") \1\b", r"\1", s)
    return re.sub(r"\s+", " ", s).strip()


def template(text, name):
    """Body of the first {{name ...}} template, braces balanced."""
    i = text.find("{{" + name)
    if i < 0:
        return ""
    depth = 0
    j = i
    while j < len(text) - 1:
        if text.startswith("{{", j):
            depth += 1
            j += 2
            continue
        if text.startswith("}}", j):
            depth -= 1
            j += 2
            if depth == 0:
                return text[i + 2 + len(name):j - 2]
            continue
        j += 1
    return text[i + 2 + len(name):]


def fields(body):
    """|key = value pairs at the top level of a template body."""
    out = {}
    depth = 0
    cur = None
    buf = []
    i = 0
    while i < len(body):
        two = body[i:i + 2]
        if two in ("{{", "[["):
            depth += 1
            buf.append(two)
            i += 2
            continue
        if two in ("}}", "]]"):
            depth -= 1
            buf.append(two)
            i += 2
            continue
        c = body[i]
        if c == "|" and depth == 0:
            if cur is not None:
                out[cur] = "".join(buf).strip()
            buf = []
            rest = body[i + 1:]
            m = re.match(r"\s*([A-Za-z0-9_ ]+?)\s*=", rest)
            if m:
                cur = m.group(1).strip().lower()
                i += 1 + m.end()
            else:
                cur = None
                i += 1
            continue
        buf.append(c)
        i += 1
    if cur is not None:
        out[cur] = "".join(buf).strip()
    return out


def bullets(value, top_only=True):
    out = []
    for line in value.split("\n"):
        m = re.match(r"^(\*+)\s*(.*)$", line.strip())
        if not m:
            continue
        if top_only and len(m.group(1)) > 1:
            continue
        t = clean(m.group(2))
        if t:
            out.append(t)
    return out


def route_rows(route, text):
    """(rowid, label, note lines) per row. The route's "Train X to N" header lines between rows become a
    "First:" note on the row after them."""
    rows = []
    pending = []
    for m in re.finditer(r'\|-\s*data-rowid="([^"\n]+)"?(.*?)(?=\|-\s*data-rowid=|\n\|\}|\Z)', text, re.S):
        rowid = m.group(1).split("\n")[0].strip()
        body = m.group(2)
        cut = re.search(r"\n\|-", body)
        after = body[cut.start():] if cut else ""
        body = body[:cut.start()] if cut else body
        cells = [c.strip() for c in re.split(r"\n\|(?!\|)", "\n" + body.strip("\n"))]
        if cells and cells[0] == "":
            cells = cells[1:]
        first = re.sub(r"^data-sort-value=\"[^\"]*\"\s*\|", "", cells[0]) if cells else rowid
        note = ""
        if len(cells) >= 6:
            note = cells[-2]
        note = re.sub(r"\{\{Checklist\|", "", note)
        note_lines = ["First: " + p for p in pending]
        pending = []
        for line in note.split("\n"):
            line = line.strip()
            depth = len(line) - len(line.lstrip("*"))
            t = clean(line.lstrip("*"))
            if t and t not in ("}}",):
                note_lines.append(("  " * max(0, depth - 1)) + t)
        rows.append((rowid, clean(first), note_lines))
        for h in re.findall(r"^!(?:[^|\n]*\|)?(.+)$", after, re.M):
            t = clean(h)
            if t:
                pending.append(t)
    return rows


def classify(rowid, label):
    """kind, ref for an order row."""
    m = re.match(r"^(.+) Diary#(Easy|Medium|Hard|Elite)$", rowid)
    if m and m.group(1) in DIARY_REGIONS:
        return "DIARY", DIARY_REGIONS[m.group(1)] + "." + m.group(2).upper()
    m = re.match(r"^(Easy|Medium|Hard|Elite) (.+) Diary$", rowid)
    if m and m.group(2) in DIARY_REGIONS:
        return "DIARY", DIARY_REGIONS[m.group(2)] + "." + m.group(1).upper()
    m = re.match(r"^All (Easy|Medium|Hard|Elite) Achievement Diaries$", rowid)
    if m:
        return "DIARY", "ALL." + m.group(1).upper()
    m = re.match(r"^(?:Partial completion of|Partially complete|Start) (.+)$", rowid)
    if m:
        name = {"Barcrawl": "Alfred Grimhand's Barcrawl"}.get(m.group(1), m.group(1))
        return "PARTIAL", name
    if rowid.startswith("Recipe for Disaster#"):
        return "QUEST", "Recipe for Disaster/" + rowid.split("#", 1)[1]
    return "QUEST", rowid


def skills_of(value):
    out = []
    for m in re.finditer(r"\{\{SCP\|([A-Za-z ]+)\|(\d+)[^}]*\}\}([^\n]*)", value):
        skill = m.group(1).strip()
        rest = m.group(3)
        boost = "b" if re.search(r"Boostable\|yes", rest, re.I) else ""
        if skill == "Quest":
            continue
        if skill in SKILLS or skill == "Combat":
            out.append(f"{skill} {m.group(2)}{(' ' + boost) if boost else ''}")
    return list(dict.fromkeys(out))


def qp_of(value):
    m = re.search(r"\{\{SCP\|Quest\|(\d+)", value)
    if m:
        return m.group(1)
    m = re.search(r"(\d+)\s*\[\[Quest points?(?:\|[^\]]*)?\]\]", value)
    return m.group(1) if m else ""


def quests_of(value, known):
    full, started = [], []
    for line in value.split("\n"):
        links = [l.split("|")[0].strip() for l in re.findall(r"\[\[([^\]]+)\]\]", line)]
        links = [l.split("#")[0] if not l.startswith("Recipe for Disaster/") else l for l in links]
        hits = [l for l in links if l in known]
        if not hits:
            continue
        lead = clean(line.lstrip("*: ")).lower()
        target = started if re.match(r"(?:started|start|partial|partly|partially)\b", lead) else full
        target.extend(hits)
    full = list(dict.fromkeys(full))
    started = [s for s in dict.fromkeys(started) if s not in full]
    return full, started


def start_npc(value):
    """Display text of the first link in the start field: usually the NPC to talk to."""
    m = re.search(r"\[\[([^\]|]+)(?:\|([^\]]+))?\]\]", value)
    if not m:
        return ""
    return clean(m.group(2) or m.group(1))


def rewards_of(text):
    body = template(text, "Quest rewards")
    f = fields(body)
    out = []
    if f.get("qp"):
        out.append(clean(f["qp"]) + " Quest point" + ("" if clean(f["qp"]) == "1" else "s"))
    out.extend(bullets(f.get("rewards", ""), top_only=True))
    return out


def main(out_dir):
    os.makedirs(out_dir, exist_ok=True)
    order = []
    for route, page in ROUTES.items():
        for rowid, label, note in route_rows(route, wikitext(page)):
            kind, ref = classify(rowid, label)
            order.append((route, kind, ref, label, note))
    names = sorted({ref for _, kind, ref, _, _ in order if kind in ("QUEST", "PARTIAL")})
    texts = pages(names)
    known = set(texts)
    # Prerequisites can name quests the routes don't list (none should, but keep them)
    rows = []
    for name in names:
        text = texts.get(name)
        if text is None or "{{Quest details" not in text:
            # Not a quest page (balloon routes, museum quiz, Knight Waves...): an activity row
            continue
        det = fields(template(text, "Quest details"))
        info = fields(template(text, "Infobox Quest"))
        req = det.get("requirements", "")
        full, started = quests_of(req, known)
        start_map = re.sub(r"[^0-9,]", "", det.get("startmap", "").split("|")[0])
        rows.append([
            name,
            "Y" if info.get("members", "").lower().startswith("yes") else "N",
            clean(det.get("difficulty", "")),
            clean(det.get("length", "")),
            clean(det.get("start", "")),
            start_map,
            start_npc(det.get("start", "")),
            ";".join(skills_of(req)),
            ";".join(q for q in full if q != name),
            ";".join(q for q in started if q != name),
            qp_of(req),
            ";".join(s for s in skills_of(det.get("recommended", "")) if s.startswith("Combat")),
            ";".join(bullets(det.get("items", ""))),
            ";".join(bullets(det.get("kills", ""))),
            ";".join(rewards_of(text)),
        ])
    quest_names = {r[0] for r in rows}
    with open(os.path.join(out_dir, "order.tsv"), "w") as f:
        f.write("# Built by tools/quests/build_quests.py from the OSRS Wiki Optimal quest guide pages\n")
        f.write("# route\tkind\tref\tlabel\tnote lines (|)\n")
        for route, kind, ref, label, note in order:
            if kind in ("QUEST", "PARTIAL") and ref not in quest_names:
                kind = "ACTIVITY"
            f.write("\t".join([route, kind, ref, label, "|".join(n.replace("|", "/") for n in note)]) + "\n")
    with open(os.path.join(out_dir, "quests.tsv"), "w") as f:
        f.write("# Built by tools/quests/build_quests.py from the OSRS Wiki quest pages\n")
        f.write("# name\tmembers\tdifficulty\tlength\tstart\tstartmap\tstartNpc\tskills\tquests\tquestsStarted\tqp\tcombat"
                "\titems\tkills\trewards\n")
        for r in rows:
            f.write("\t".join(c.replace("\t", " ").replace("\n", " ") for c in r) + "\n")
    print(len(order), "order rows,", len(rows), "quests")


if __name__ == "__main__":
    main(sys.argv[1])
