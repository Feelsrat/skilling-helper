"""Builds src/main/resources/com/skillinghelper/data/travel.tsv: every teleport item (jewellery, tablets, scrolls,
capes, diary items, seed pods...) and every fairy ring, with where it lands and what it needs, so the guides can tell
the player which of their own travel items to bring.

The data comes from the Shortest Path plugin (BSD 2-Clause, https://github.com/Skretzo/shortest-path):
src/main/resources/transports/teleportation_items.tsv and fairy_rings.tsv. Using the same data as Shortest Path means
an item the guide suggests is one Shortest Path knows how to route with. Run this again to refresh it.

Usage: python3 build_travel.py <shortest-path checkout> <out.tsv>

Output columns (tab separated):
  kind        I = teleport item, F = fairy ring (needs a fairy ring staff, which the plugin adds)
  group       the item's name for the guide, e.g. "Skills necklace" (or "Fairy ring")
  place       where it lands, e.g. "Fishing Guild" (or the fairy ring code)
  point       x y plane of the landing tile
  items       item ids that can do it, '|' separated (empty for fairy rings)
  consumable  T when using it spends a charge, a tablet or a scroll
  quests      quest names that must be finished, ';' separated
  skills      Skill=level, ';' separated
  varbits     id(=|<|>)value, ';' separated, all must hold
  varps       same, for player variables
  origin      x y plane where a paid transport starts (C rows only)
  coins       what it costs (C rows only)

C rows are transports that cost coins (boats, ships, charter ships, magic carpets, minecarts, dungeon entrances
and the like). Shortest Path only uses them when the coins are in the inventory, so the guides tell the player to
take them along.
"""
import os
import re
import sys

SKILLS = {
    "attack", "defence", "strength", "hitpoints", "ranged", "prayer", "magic", "cooking", "woodcutting",
    "fletching", "fishing", "firemaking", "crafting", "smithing", "mining", "herblore", "agility", "thieving",
    "slayer", "farming", "runecraft", "hunter", "construction", "sailing",
}


def rows(path):
    with open(path, encoding="utf-8") as f:
        header = None
        for line in f:
            line = line.rstrip("\n")
            if line.startswith("# ") and header is None and "\t" in line:
                header = [h.strip() for h in line[2:].split("\t")]
                continue
            if not line.strip() or line.startswith("#"):
                continue
            cells = line.split("\t")
            cells += [""] * (len(header) - len(cells))
            yield dict(zip(header, cells))


def skills(text):
    out = []
    for part in filter(None, (p.strip() for p in text.split(";"))):
        m = re.fullmatch(r"(\d+) (\w+)", part)
        # "Max Total" and the like: only on capes you can't own without them, so leave them out
        if m and m.group(2).lower() in SKILLS:
            out.append(m.group(2).upper() + "=" + m.group(1))
    return ";".join(out)


def conditions(text):
    parts = [p.strip() for p in text.split(";") if p.strip()]
    for p in parts:
        if not re.fullmatch(r"\d+[=<>]\d+", p):
            raise ValueError("unexpected condition " + p)
    return ";".join(parts)


def split_display(display):
    """'Skills necklace: Fishing Guild' -> ('Skills necklace', 'Fishing Guild')."""
    if ":" in display:
        group, place = display.split(":", 1)
        # Diary cape and talisman menus number their options ("2. Jarr"): keep just the name
        return group.strip(), re.sub(r"^[0-9A-Z]\. ", "", place.strip())
    return display.strip(), display.strip()


# Paid transport files and what to call them in the guide when the row has no name of its own
FEE_FILES = {
    "transports.tsv": None,
    "boats.tsv": "boat",
    "ships.tsv": "boat",
    "charter_ships.tsv": "charter ship",
    "magic_carpets.tsv": "magic carpet",
    "minecarts.tsv": "minecart",
}


def fee_rows(base):
    out = []
    for name, noun in FEE_FILES.items():
        for r in rows(os.path.join(base, name)):
            items = r.get("Items", "").strip()
            m = re.fullmatch(r"COINS=(\d+)", items)
            # Only rows paid in coins alone: a pass, a token or a log basket can't be fetched as easily
            if not m or not r["Origin"].strip() or not r["Destination"].strip():
                continue
            display = re.sub(r"^[0-9A-Z]+: ", "", r.get("Display info", "").strip())
            menu = re.sub(r" \d+$", "", r.get("menuOption menuTarget objectID", "").strip())
            if noun is None or not display:
                label = menu
            else:
                label = noun + (" to " + display if display else "")
            out.append(["C", label, display or label, r["Destination"].strip(), "", "T", r.get("Quests", "").strip(),
                        skills(r.get("Skills", "")), conditions(r.get("Varbits", "")),
                        conditions(r.get("VarPlayers", "")), r["Origin"].strip(), m.group(1)])
    # Rows that only differ by the tile you click from: keep one per origin area and destination
    seen = set()
    unique = []
    for row in out:
        ox, oy, _ = row[10].split()
        key = (row[1], row[3], int(ox) // 8, int(oy) // 8, row[11], row[6], row[8])
        if key not in seen:
            seen.add(key)
            unique.append(row)
    return unique


def main():
    sp, out_path = sys.argv[1], sys.argv[2]
    base = os.path.join(sp, "src/main/resources/transports")
    out = []
    skipped = 0
    for r in rows(os.path.join(base, "teleportation_items.tsv")):
        items = r["Items"].strip()
        display = r["Display info"].strip()
        # Unlock flags (e.g. Xeric's honour) can't be read from the client: leave those out
        if not items or "&" in items or not display:
            skipped += 1
            continue
        ids = []
        for alt in items.split("||"):
            m = re.fullmatch(r"(\d+)=1", alt.strip())
            if not m:
                raise ValueError("unexpected items " + items)
            ids.append(m.group(1))
        group, place = split_display(display)
        out.append(["I", group, place, r["Destination"].strip(), "|".join(ids),
                    "T" if r["Consumable"].strip() == "T" else "F", r["Quests"].strip(),
                    skills(r["Skills"]), conditions(r["Varbits"]), conditions(r.get("VarPlayers", ""))])
    for r in rows(os.path.join(base, "fairy_rings.tsv")):
        if not r["Destination"].strip():
            continue
        code = r["Display info"].strip()
        out.append(["F", "Fairy ring", code, r["Destination"].strip(), "", "F", r["Quests"].strip(),
                    skills(r["Skills"]), conditions(r["Varbits"]), ""])
    fees = fee_rows(base)
    out += fees
    with open(out_path, "w", encoding="utf-8") as f:
        f.write("# Generated by tools/travel/build_travel.py from the Shortest Path plugin's transport data "
                "(BSD 2-Clause, github.com/Skretzo/shortest-path). Do not edit by hand.\n")
        f.write("# kind\tgroup\tplace\tpoint\titems\tconsumable\tquests\tskills\tvarbits\tvarps\torigin\tcoins\n")
        for row in out:
            f.write("\t".join(row + [""] * (12 - len(row))) + "\n")
    print("wrote", len(out), "rows (", len(fees), "paid transports ), skipped", skipped)


if __name__ == "__main__":
    main()
