#!/usr/bin/env python3
"""Builds src/main/resources/com/skillinghelper/ca/ca-tasks.tsv from the OSRS Wiki.

Every Combat Achievement task comes from the wiki's `combat_achievement` bucket (id, name, monster, task, tier,
type). The id is the game's task id: bit id % 32 of varp CA_TASK_COMPLETED_<id / 32> is set once it's done. Each
task's own wiki page adds whether it's members only, the skill levels and quests its intro says it needs, and its
"Strategy" section as plain sentences.

Columns (tab separated): id, tier, type, monster, name, task, members (1/0), levels ("Slayer 60;Prayer 37"),
quests ("Dragon Slayer II;..."), strategy (sentences joined with " | ").

    python3 tools/ca/build_ca_tasks.py src/main/resources/com/skillinghelper/ca/ca-tasks.tsv

It also writes ca-monsters.tsv (each boss's "getting there" sentences) and ca-rewards.tsv next to it: tier, points needed, then each reward line.
"""
import json
import re
import sys
import time
import urllib.parse
import urllib.request

API = "https://oldschool.runescape.wiki/api.php"
UA = {"User-Agent": "skilling-helper combat achievement data builder"}
SKILLS = ["Attack", "Strength", "Defence", "Ranged", "Prayer", "Magic", "Runecraft", "Construction", "Hitpoints",
          "Agility", "Herblore", "Thieving", "Crafting", "Fletching", "Slayer", "Hunter", "Mining", "Smithing",
          "Fishing", "Cooking", "Firemaking", "Woodcutting", "Farming", "Sailing"]


def get(params):
    q = urllib.parse.urlencode(params)
    for attempt in range(5):
        try:
            with urllib.request.urlopen(urllib.request.Request(API + "?" + q, headers=UA)) as r:
                return json.load(r)
        except Exception:
            time.sleep(2 ** attempt)
    raise RuntimeError("wiki request failed: " + q)


def tasks():
    query = "bucket('combat_achievement').select('id','name','monster','task','tier','type').limit(5000).run()"
    return get({"action": "bucket", "format": "json", "query": query})["bucket"]


def pages(titles):
    out = {}
    for i in range(0, len(titles), 40):
        batch = titles[i:i + 40]
        d = get({"action": "query", "prop": "revisions", "rvprop": "content", "rvslots": "main", "format": "json",
                 "redirects": 1, "titles": "|".join(batch)})
        q = d["query"]
        alias = {}
        for n in q.get("normalized", []) + q.get("redirects", []):
            alias[n["to"]] = alias.get(n["from"], n["from"])
        for p in q["pages"].values():
            if "revisions" not in p:
                continue
            text = p["revisions"][0]["slots"]["main"]["*"]
            title = p["title"]
            out[title] = text
            src = title
            while src in alias:
                src = alias[src]
                out[src] = text
    return out


def inline_templates(s):
    """Turns the templates that carry words (skill levels, item links, coins) into those words."""
    s = re.sub(r"\{\{(?:SCP|Skill clickpic)\|([^|}]+)\|?([^|}]*)[^}]*\}\}",
               lambda m: (m.group(2) + " " + m.group(1)).strip(), s)
    s = re.sub(r"\{\{(?:plink|plinkp|plinkt)\|([^|}]+)[^}]*\}\}", r"\1", s)
    s = re.sub(r"\{\{Coins\|([^}]+)\}\}", r"\1 coins", s)
    s = re.sub(r"\{\{[Tt]ick\|([^}]+)\}\}", r"\1 ticks", s)
    return re.sub(r"\{\{[Ff]airycode\|([^}|]+)[^}]*\}\}", r"\1", s)


def clean(s):
    s = re.sub(r"<ref[^>]*/>|<ref[^>]*>.*?</ref>", "", s, flags=re.S)
    s = re.sub(r"<!--.*?-->", "", s, flags=re.S)
    s = inline_templates(s)
    while re.search(r"\{\{[^{}]*\}\}", s):
        s = re.sub(r"\{\{[^{}]*\}\}", "", s)
    s = re.sub(r"\[\[File:[^\]]*\]\]", "", s)
    s = re.sub(r"\[\[[^|\]]*\|([^\]]+)\]\]", r"\1", s)
    s = re.sub(r"\[\[([^\]]+)\]\]", r"\1", s)
    s = re.sub(r"\[https?://\S+ ([^\]]+)\]", r"\1", s)
    s = re.sub(r"'''?", "", s)
    s = re.sub(r"<[^>]+>", "", s)
    s = s.replace("\t", " ").replace("|", "/")
    return re.sub(r"\s+", " ", s).strip()


def section(text, name):
    m = re.search(r"^(==+)\s*" + name + r"\s*\1\s*$(.*?)(?=^==[^=]|\Z)", text, flags=re.M | re.S)
    if m:
        return m.group(2)
    m = re.search(r"^==\s*" + name + r"\s*==\s*$(.*?)(?=^==[^=]|\Z)", text, flags=re.M | re.S)
    return m.group(1) if m else ""


def sentences(block):
    out = []
    block = re.sub(r"<ref[^>]*/>|<ref[^>]*>.*?</ref>", "", block, flags=re.S)
    block = inline_templates(block)
    while re.search(r"\{\{[^{}]*\}\}", block):
        block = re.sub(r"\{\{[^{}]*\}\}", "", block, flags=re.S)
    block = re.sub(r"\[\[File:[^\]]*\]\]", "", block)
    for raw in block.split("\n"):
        raw = raw.strip()
        if not raw or raw.startswith("{|") or raw.startswith("|") or raw.startswith("!") or raw.startswith("="):
            continue
        line = clean(raw.lstrip("*#:; "))
        if not line or line.startswith("File:") or "thumb/" in line or re.match(r"^[,\d:\s]+$", line):
            continue
        for s in re.split(r"(?<=[.!?])\s+(?=[A-Z0-9])", line):
            s = s.strip()
            if len(s) > 3:
                out.append(s)
    return out


def intro(text):
    body = re.sub(r"^\{\{Infobox.*?^\}\}", "", text, flags=re.S | re.M)
    m = re.search(r"^==", body, flags=re.M)
    return body[:m.start()] if m else body


def levels(text):
    found = {}
    for s in sentences(intro(text)):
        if not re.search(r"requir|need", s, re.I):
            continue
        for m in re.finditer(r"(?:level of |level )?(\d{1,2}) (" + "|".join(SKILLS) + r")\b", s):
            found[m.group(2)] = max(found.get(m.group(2), 0), int(m.group(1)))
        for m in re.finditer(r"(" + "|".join(SKILLS) + r") level of (\d{1,2})", s):
            found[m.group(1)] = max(found.get(m.group(1), 0), int(m.group(2)))
    return ";".join(k + " " + str(v) for k, v in sorted(found.items()))


def quests(text):
    found = []
    for s in re.split(r"(?<=[.!?])\s+", intro(text)):
        if not re.search(r"quest|complet|requir", s, re.I):
            continue
        for m in re.finditer(r"\[\[([^\]|#]+)(?:\|[^\]]*)?\]\]", s):
            name = m.group(1).strip()
            if name not in found:
                found.append(name)
    return ";".join(found)


TIERS = ["Easy", "Medium", "Hard", "Elite", "Master", "Grandmaster"]
GLOBALS = ["easy", "medium", "hard", "elite", "master", "gm"]


def rewards(out_path):
    """One row per tier: tier, points needed (when built), then each reward line ("- " marks a sub-point)."""
    text = get({"action": "parse", "page": "Combat Achievements", "prop": "wikitext", "format": "json"})
    text = text["parse"]["wikitext"]["*"]
    expand = " ".join("{{Globals|ca %s points}}" % g for g in GLOBALS)
    points = get({"action": "expandtemplates", "format": "json", "prop": "wikitext", "text": expand})
    points = points["expandtemplates"]["wikitext"].split()
    block = section(text, "Rewards")
    table = block[block.index("Required points"):]
    rows = re.split(r"^\|-\s*$", table, flags=re.M)[1:]
    with open(out_path, "w", encoding="utf-8") as f:
        f.write("# Generated by tools/ca/build_ca_tasks.py from the OSRS Wiki (CC BY-NC-SA 3.0). Do not edit.\n")
        for tier, pts, row in zip(TIERS, points, rows):
            perks = []
            for line in row.split("\n"):
                m = re.match(r"^(\*+)\s*(.*)$", line.strip())
                if m:
                    perks.append(("- " if len(m.group(1)) > 1 else "") + clean(m.group(2)))
            f.write("\t".join([tier, pts] + perks) + "\n")
    print("wrote rewards to", out_path)


def main(out_path):
    rows = sorted(tasks(), key=lambda t: t["id"])
    texts = pages([t["name"] for t in rows])
    missing = [t["name"] for t in rows if t["name"] not in texts]
    if missing:
        print("no page for:", missing, file=sys.stderr)
    with open(out_path, "w", encoding="utf-8") as f:
        f.write("# Generated by tools/ca/build_ca_tasks.py from the OSRS Wiki (CC BY-NC-SA 3.0). Do not edit.\n")
        for t in rows:
            text = texts.get(t["name"], "")
            members = "0" if re.search(r"\|members\s*=\s*No", text) else "1"
            strategy = sentences(section(text, "Strategy")) or []
            fields = [str(t["id"]), t["tier"], t["type"], t.get("monster") or "None", clean(t["name"]),
                      clean(t["task"]), members, levels(text), quests(text), " | ".join(strategy)]
            f.write("\t".join(x.replace("\t", " ").replace("\n", " ") for x in fields) + "\n")
    print("wrote", len(rows), "tasks to", out_path)


# Monsters whose own page isn't about where they are, mapped to a page that is
MONSTER_PAGES = {"Giants": "Giants' Den", "Galvek": "Dragon Slayer II", "Glough": "Monkey Madness II",
                 "Fragment of Seren": "Song of the Elves", "Corrupted Hunllef": "The Gauntlet",
                 "Crystalline Hunllef": "The Gauntlet", "TzTok-Jad": "TzHaar Fight Cave", "TzKal-Zuk": "Inferno",
                 "Tombs of Amascut: Entry Mode": "Tombs of Amascut", "Tombs of Amascut: Expert Mode": "Tombs of Amascut",
                 "Theatre of Blood: Entry Mode": "Theatre of Blood", "Theatre of Blood: Hard Mode": "Theatre of Blood",
                 "Chambers of Xeric: Challenge Mode": "Chambers of Xeric", "Phosani's Nightmare": "The Nightmare",
                 "Dagannoth Prime": "Dagannoth Kings", "Dagannoth Rex": "Dagannoth Kings",
                 "Dagannoth Supreme": "Dagannoth Kings"}


def monsters(tasks_path):
    """One row per monster: name, then how to get there (sentences from its wiki page's location sections)."""
    names = sorted({l.split("\t")[3] for l in open(tasks_path, encoding="utf-8") if not l.startswith("#")})
    names = [n for n in names if n != "None"]
    texts = pages(sorted({MONSTER_PAGES.get(n, n) for n in names}))
    out_path = tasks_path.replace("ca-tasks.tsv", "ca-monsters.tsv")
    with open(out_path, "w", encoding="utf-8") as f:
        f.write("# Generated by tools/ca/build_ca_tasks.py from the OSRS Wiki (CC BY-NC-SA 3.0). Do not edit.\n")
        for n in names:
            text = texts.get(MONSTER_PAGES.get(n, n), "")
            found = []
            for name in re.findall(r"^==+\s*([^=]+?)\s*==+\s*$", text, flags=re.M):
                if re.search(r"getting there|transport|^location|access", name, re.I):
                    found += sentences(section(text, re.escape(name)))
            f.write("\t".join([n] + found[:8]) + "\n")
    print("wrote monsters to", out_path)


if __name__ == "__main__":
    main(sys.argv[1])
    monsters(sys.argv[1])
    rewards(sys.argv[1].replace("ca-tasks.tsv", "ca-rewards.tsv"))
