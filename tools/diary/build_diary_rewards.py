#!/usr/bin/env python3
"""Builds src/main/resources/com/skillinghelper/diary/diary-rewards.tsv from the OSRS Wiki diary pages.

One row per region and tier: region key, tier, reward item, then each perk (tab separated). Perks are the
wiki's own reward bullet points with the wiki markup stripped. Run from anywhere:

    python3 tools/diary/build_diary_rewards.py <out.tsv>
"""
import json
import re
import sys
import urllib.parse
import urllib.request

PAGES = {
    "ARDOUGNE": "Ardougne Diary",
    "DESERT": "Desert Diary",
    "FALADOR": "Falador Diary",
    "FREMENNIK": "Fremennik Diary",
    "KANDARIN": "Kandarin Diary",
    "KARAMJA": "Karamja Diary",
    "KOUREND": "Kourend & Kebos Diary",
    "LUMBRIDGE": "Lumbridge & Draynor Diary",
    "MORYTANIA": "Morytania Diary",
    "VARROCK": "Varrock Diary",
    "WESTERN": "Western Provinces Diary",
    "WILDERNESS": "Wilderness Diary",
}
TIERS = ["Easy", "Medium", "Hard", "Elite"]
API = "https://oldschool.runescape.wiki/api.php"


def wikitext(page):
    q = urllib.parse.urlencode({"action": "parse", "page": page, "prop": "wikitext", "format": "json"})
    req = urllib.request.Request(API + "?" + q, headers={"User-Agent": "skilling-helper diary data builder"})
    with urllib.request.urlopen(req) as r:
        return json.load(r)["parse"]["wikitext"]["*"]


def clean(s):
    s = re.sub(r"<ref[^>]*/>|<ref[^>]*>.*?</ref>", "", s)
    s = re.sub(r"\{\{efn\|.*?\}\}", "", s)
    s = re.sub(r"\{\{(?:SCP|Skill clickpic)\|([^|}]+)\|?([^|}]*)[^}]*\}\}", lambda m: (m.group(2) + " " + m.group(1)).strip(), s)
    s = re.sub(r"\{\{Coins\|([^}]+)\}\}", r"\1 coins", s)
    while re.search(r"\{\{[^{}]*\}\}", s):
        s = re.sub(r"\{\{[^{}]*\}\}", "", s)
    s = re.sub(r"\[\[[^|\]]*\|([^\]]+)\]\]", r"\1", s)
    s = re.sub(r"\[\[([^\]]+)\]\]", r"\1", s)
    s = re.sub(r"\[https?://\S+ ([^\]]+)\]", r"\1", s)
    s = re.sub(r"'''?", "", s)
    s = re.sub(r"<[^>]+>", "", s)
    s = s.replace("\t", " ")
    s = re.sub(r"\s+", " ", s).strip().rstrip(":")
    # Coin values the wiki works out live from GE prices ("worth a total of {{NoCoins|...}} coins") come out empty
    # here, so the whole aside goes
    return re.sub(r",? (?:resellable on the Grand Exchange )?(?:worth (?:a total |an )?(?:average )?of|for a profit of)"
                  r" coins(?: on the Grand Exchange)?(?: \(after tax\))?", "", s)


def rewards(text, tier):
    sec = re.search(r"\n==\s*" + tier + r"\s*==\n(.*?)(?=\n==[^=])", text, re.S)
    if not sec:
        return None
    rew = re.search(r"===\s*Rewards?\s*===[ \t]*\n(.*?)(?=\n==|\Z)", sec.group(1), re.S)
    if not rew:
        return None
    item, perks = None, []
    for line in rew.group(1).splitlines():
        m = re.match(r"^(\*+)\s*(.*)$", line)
        if not m:
            continue
        depth, body = len(m.group(1)), clean(m.group(2))
        if not body:
            continue
        if item is None and depth == 1:
            item = body
        elif depth >= 2 or item is not None:
            perks.append(body)
    return item, perks


def main(out):
    rows = []
    for key, page in PAGES.items():
        text = wikitext(page)
        for tier in TIERS:
            r = rewards(text, tier)
            if r is None:
                print("no rewards for", page, tier, file=sys.stderr)
                continue
            item, perks = r
            rows.append("\t".join([key, tier.upper(), item] + perks))
    with open(out, "w", encoding="utf-8") as f:
        f.write("# region\ttier\treward\tperks... (built by tools/diary/build_diary_rewards.py from the OSRS Wiki)\n")
        f.write("\n".join(rows) + "\n")
    print(len(rows), "rows")


if __name__ == "__main__":
    main(sys.argv[1])
