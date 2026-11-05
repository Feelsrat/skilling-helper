#!/usr/bin/env python3
"""Builds the Clue Scroll Guide data files from the OSRS Wiki.

Writes three files into the given folder (normally src/main/resources/com/skillinghelper/clue):

  clue-rewards.tsv  tier, section, item name, item id (0 = none found), quantity, rarity ("1/117"), chance per roll.
                    Lines starting "#" hold each tier's casket summary: "#tier<TAB>min<TAB>max<TAB>summary".
                    From the "Reward casket (<tier>)" pages ({{DropsLineReward}} rows, {{#expr}} rarities worked out).
  clue-stash.tsv    tier, x, y, plane, short name, where, items ("|"-separated), item ids ("|"-separated, 0 = none).
                    From the "STASH unit" page ({{StashLocation}} rows).
  clue-drops.tsv    tier, source, rarity, chance, level, type, wilderness (1 when the source is only in the Wilderness).
                    From the wiki's "dropsline" Bucket, every monster/activity/reward that gives the clue.

Item ids come from the "infobox_item" Bucket. Run from anywhere:

    python3 tools/clue/build_clue_data.py <out-folder>
"""
import json
import os
import re
import sys
import urllib.parse
import urllib.request
from fractions import Fraction

API = "https://oldschool.runescape.wiki/api.php"
UA = {"User-Agent": "skilling-helper clue data builder"}
TIERS = ["beginner", "easy", "medium", "hard", "elite", "master"]


def fetch(params):
    req = urllib.request.Request(API + "?" + urllib.parse.urlencode(params), headers=UA)
    with urllib.request.urlopen(req) as r:
        return json.load(r)


def wikitext(page):
    return fetch({"action": "parse", "page": page, "prop": "wikitext", "format": "json",
                  "redirects": 1})["parse"]["wikitext"]["*"]


def bucket(name, fields, where=""):
    rows, offset = [], 0
    while True:
        q = "bucket('%s').select(%s)%s.limit(5000).offset(%d).run()" % (
            name, ",".join(repr(f) for f in fields), where, offset)
        r = fetch({"action": "bucket", "format": "json", "query": q})
        if "error" in r:
            raise SystemExit(r["error"])
        rows += r["bucket"]
        if len(r["bucket"]) < 5000:
            return rows
        offset += 5000


def item_ids():
    ids = {}
    for it in bucket("infobox_item", ["page_name", "item_name", "item_id"]):
        raw = it.get("item_id") or []
        if raw and str(raw[0]).isdigit():
            for key in ("item_name", "page_name"):
                if it.get(key):
                    ids.setdefault(it[key].lower(), int(raw[0]))
    return ids


def expr(s):
    """Works out {{#expr: ... round N}} the way the wiki does, for rarities."""
    def one(m):
        body = re.sub(r"\s+round\s+\d+\s*$", "", m.group(1))
        if not re.fullmatch(r"[0-9.\s*/()+-]+", body):
            raise ValueError(body)
        return str(round(eval(body), 1))  # digits and arithmetic only, checked above
    while "{{#expr:" in s:
        s = re.sub(r"\{\{#expr:([^{}]*)\}\}", one, s)
    return s


def chance(rarity):
    try:
        num, den = rarity.replace(",", "").split("/")
        return float(num) / float(den)
    except ValueError:
        return 0.0


def template_args(body):
    """Splits a template's arguments at top-level pipes into a dict."""
    out, depth, cur = [], 0, ""
    for i, ch in enumerate(body):
        if body.startswith("{{", i):
            depth += 1
        elif body.startswith("}}", i) and depth > 0:
            depth -= 1
        if ch == "|" and depth == 0:
            out.append(cur)
            cur = ""
        else:
            cur += ch
    out.append(cur)
    args = {}
    for a in out[1:]:
        if "=" in a:
            k, v = a.split("=", 1)
            args[k.strip()] = v.strip()
    return args


def templates(text, name):
    """Yields the full body of every {{name ...}} in the text, nested templates included."""
    start = 0
    while True:
        i = text.find("{{" + name, start)
        if i < 0:
            return
        depth, j = 0, i
        while j < len(text):
            if text.startswith("{{", j):
                depth += 1
                j += 2
            elif text.startswith("}}", j):
                depth -= 1
                j += 2
                if depth == 0:
                    break
            else:
                j += 1
        yield text[i + 2:j - 2]
        start = j


def plain(s):
    s = re.sub(r"<ref[^>]*/>|<ref[^>]*>.*?</ref>", "", s, flags=re.S)
    s = re.sub(r"\{\{(?:Efn|efn)\|.*?\}\}", "", s)
    s = re.sub(r"\{\{[Cc]oins\|([^}]*)\}\}", "", s)
    while re.search(r"\{\{[^{}]*\}\}", s):
        s = re.sub(r"\{\{[^{}]*\}\}", "", s)
    s = re.sub(r"\[\[[^|\]]*\|([^\]]+)\]\]", r"\1", s)
    s = re.sub(r"\[\[([^\]]+)\]\]", r"\1", s)
    s = re.sub(r"'''?", "", s)
    s = re.sub(r"<[^>]+>", "", s)
    return re.sub(r"\s+", " ", s).replace("\t", " ").strip()


def rewards(ids):
    rows, heads = [], []
    for tier in TIERS:
        text = wikitext("Reward casket (%s)" % tier)
        body = text.split("==Rewards==", 1)[1]
        body = re.split(r"\n==(?:Rewards on Entrana|Changes|Gallery|Trivia|Notes|References)", body)[0]
        first = plain(body.split("\n", 2)[1])
        m = re.search(r"minimum of (\d+) rewards? and a maximum of (\d+)", first)
        summary = re.split(r"(?<=\.) ", first)
        keep = [s for s in summary if "chance" in s or "average" in s.lower()]
        unique = re.search(r"probability to receive a clue scroll unique item from an? \w+ casket is .*?(?:%|\d)\.(?=\s|$)", plain(body))
        if unique:
            keep.append(unique.group(0).replace("probability to receive", "Chance of"))
        heads.append("#%s\t%s\t%s\t%s" % (tier, m.group(1), m.group(2), " ".join(keep)))
        section = ""
        for line in body.split("\n"):
            h = re.match(r"^(={3,4})\s*(.*?)\s*\1\s*$", line)
            if h:
                if len(h.group(1)) == 3:
                    section = h.group(2)
                    sub = ""
                else:
                    sub = h.group(2)
                continue
            for t in templates(line, "DropsLineReward"):
                a = template_args(t)
                name = plain(a.get("name", ""))
                if not name:
                    continue
                rarity = expr(a.get("rarity", "")).strip()
                where = section + (" / " + sub if sub and "uniques" not in section.lower() else "")
                if "uniques" in section.lower() and sub:
                    where = section + " / " + sub
                rows.append("\t".join([tier, where, name, str(ids.get(name.lower(), 0)),
                                       plain(a.get("quantity", "1")), rarity, "%.8f" % chance(rarity)]))
    return heads + rows


def stash(ids):
    text = wikitext("STASH unit")
    rows = []
    for tier in TIERS:
        part = re.split(r"\n==%s==" % tier.capitalize(), text, maxsplit=1)[1]
        part = re.split(r"\n==[A-Z]", part, maxsplit=1)[0]
        for row in part.split("\n|-")[1:]:
            loc = next(templates(row, "StashLocation"), None)
            if loc is None:
                continue
            a = template_args(loc)
            cells = row.split("\n|")
            where = ""
            items = []
            after = row[row.find("}}", row.find("StashLocation")) + 2:]
            parts = [c.strip() for c in after.split("\n|") if c.strip()]
            if parts:
                where = plain(parts[0])
            for p in parts[1:]:
                found = [re.sub(r"#(.*)", lambda m: " (" + m.group(1).lower() + ")", f).strip()
                         for f in re.findall(r"\{\{plink\|([^|}]+)", p)]
                if found:
                    items = found
                    break
            name = plain(a.get("text", "").strip('"'))
            rows.append("\t".join([tier, a.get("x", "0"), a.get("y", "0"), a.get("plane", "0"), name, where,
                                   "|".join(items), "|".join(str(ids.get(i.lower(), 0)) for i in items)]))
    return rows


def drops():
    rows = []
    for tier in TIERS[:-1]:
        name = "Clue scroll (%s)" % tier
        for r in bucket("dropsline", ["page_name", "drop_json"], ".where('item_name','%s')" % name):
            d = json.loads(r["drop_json"])
            source = re.sub(r"#(.*)", r" (\1)", d.get("Dropped from") or r["page_name"])
            rarity = d.get("Rarity", "")
            region = (d.get("League region") or "").lower()
            wild = "1" if region == "wilderness" or "wilderness" in source.lower() else "0"
            rows.append("\t".join([tier, plain(source), rarity, "%.8f" % chance(rarity), d.get("Drop level", ""),
                                   d.get("Drop type", ""), wild]))
    return rows


def main(out):
    os.makedirs(out, exist_ok=True)
    ids = item_ids()
    for fname, lines in (("clue-rewards.tsv", rewards(ids)), ("clue-stash.tsv", stash(ids)),
                         ("clue-drops.tsv", drops())):
        with open(os.path.join(out, fname), "w", encoding="utf-8") as f:
            f.write("\n".join(lines) + "\n")
        print(fname, len(lines))


if __name__ == "__main__":
    main(sys.argv[1])
