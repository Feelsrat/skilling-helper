"""Builds src/main/resources/com/skillinghelper/data/shops.tsv: NPC shops that sell items for coins, so the guides can
send the player to a shop instead of the Grand Exchange when that's cheaper.

Everything comes from the OSRS Wiki (CC BY-NC-SA 3.0, https://oldschool.runescape.wiki):

  * the "storeline" Bucket: every item every shop sells, with its price at full stock, how many the shop holds and
    how fast the price climbs as the stock runs down (delta, per mille of the item's value per item bought);
  * the "infobox_shop" Bucket and each shop page's infobox map, for the owner to click and where the shop is;
  * the "infobox_item" Bucket, to turn item names into ids;
  * Module:GEPrices/data.json, to leave out items a shop sells for more than the GE does today.

Only the shops in SHOPS are used: ones anyone can walk into without a quest, a guild level, minigame points or the
Wilderness. Add a shop there after checking its wiki page. The plugin never contacts the wiki for this list; run this
script again to refresh it.

Usage: python3 build_shops.py <cache dir> <out.tsv>
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
UA = {"User-Agent": "skilling-helper RuneLite plugin data build (github.com/Feelsrat/skilling-helper)"}

# Shops with no entry requirement. Checked by hand against each wiki page (2026-11-03).
SHOPS = {
    "Aemad's Adventuring Supplies", "Al Kharid General Store", "Aleck's Hunter Emporium", "Arhein's General Goods",
    "Ardougne Baker's Stall", "Ardougne Spice Stall", "Aubury's Rune Shop", "Bedabin Village Bartering",
    "Betty's Magic Emporium", "Blue Moon Inn", "Bolkoy's Village Shop", "Briget's Weapons", "Burthorpe Supplies",
    "Construction supplies", "Dancing Donkey Inn", "Daryl's Ranging Surplus", "Diango's Toy Store",
    "Dommik's Crafting Store", "Dragon Inn", "Draynor Seed Market", "Drogo's Mining Emporium",
    "Dwarven shopping store", "Edgeville General Store", "Falador General Store", "Falador Party Room",
    "Fancy Clothes Store", "Flying Horse Inn", "Food Store", "Forester's Arms", "Frankie's Fishing Emporium",
    "Frenita's Cookery Shop", "Frincos' Fabulous Herb Store", "Funch's Fine Groceries", "Gem Trader",
    "Gerrant's Fishy Business", "Grand Tree Groceries", "Harry's Fishing Shop", "Herquin's Gems",
    "Hickton's Archery Emporium", "Jatix's Herblore Shop", "Jennifer's General Supplies", "Jiminua's Jungle Store",
    "Jolly Boar Inn", "Karamja General Store", "Karamja Wines, Spirits, and Beers", "Kenelme's Wares",
    "Khazard General Store", "Kourend Castle Baker's Stall", "Leenz's General Supplies", "Little Munty's Little Shop",
    "Little Shop of Horace", "Logava Gricoller's Cooking Supplies", "Lovecraft's Tackle", "Lowe's Archery Emporium",
    "Lumbridge General Store", "Nardah General Store", "Nardah Hunter Shop", "Ned's Handmade Rope (100% Wool)",
    "Perry's Chop-chop Shop", "Pollnivneach general store", "Rasolo the Wandering Merchant", "Regath's Wares",
    "Richard's Farming shop", "Rimmington General Store", "Rommik's Crafty Supplies", "Rusty Anchor Inn",
    "Sarah's Farming shop", "Shantay Pass Shop", "Shayzien Styles", "Silk trader", "The Asp & Snake Bar",
    "The Shrimp and Parrot", "Thessalia's Fine Clothes", "Thyria's Wares", "Toothy's Pickaxes",
    "Trader Stan's Trading Post", "Tynan's Fishing Supplies", "Vanessa's Farming shop", "Varrock General Store",
    "Void Knight Archery Store", "Void Knight General Store", "Void Knight Magic Store", "Wine Shop",
    "Zaff's Superior Staffs!", "Zeke's Superior Scimitars",
}

# Where the owner field doesn't name the NPC to click
OWNERS = {
    "Silk trader": ["Silk trader"],
    "Construction supplies": ["Sawmill operator"],
    "Trader Stan's Trading Post": ["Trader Crewmember"],
}


def fetch(url, cache, name):
    path = os.path.join(cache, name)
    if os.path.exists(path):
        with open(path) as f:
            return json.load(f)
    for i in range(5):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=60) as r:
                data = json.load(r)
            with open(path, "w") as f:
                json.dump(data, f)
            return data
        except Exception as e:  # network hiccups: back off and retry
            print("retry", url[:120], e, file=sys.stderr)
            time.sleep(2 ** i)
    raise SystemExit("failed: " + url)


def bucket(cache, name, fields, where=""):
    rows = []
    offset = 0
    while True:
        query = "bucket('%s').select(%s)%s.limit(5000).offset(%d).run()" % (
            name, ",".join(repr(f) for f in fields), where, offset)
        r = fetch(API + "?" + urllib.parse.urlencode({"action": "bucket", "format": "json", "query": query}),
                  cache, "%s-%d.json" % (name, offset))
        if "error" in r:
            raise SystemExit(r["error"])
        rows += r["bucket"]
        if len(r["bucket"]) < 5000:
            return rows
        offset += 5000


def wikitext(cache, titles):
    out = {}
    titles = sorted(titles)
    for i in range(0, len(titles), 50):
        r = fetch(API + "?" + urllib.parse.urlencode({
            "action": "query", "format": "json", "prop": "revisions", "rvprop": "content", "rvslots": "main",
            "titles": "|".join(titles[i:i + 50])}), cache, "pages-%d.json" % i)
        for p in r["query"]["pages"].values():
            if "revisions" in p:
                out[p["title"]] = p["revisions"][0]["slots"]["main"]["*"]
    return out


def map_point(text):
    """Middle of the shop's infobox map: a polygon's corners or a single x,y pin."""
    m = re.search(r"\{\{Map\|([^}]*)\}\}", text)
    if not m:
        return None
    points, plane, x = [], 0, None
    for part in (p.strip() for p in m.group(1).split("|")):
        mm = re.fullmatch(r"(?:x:)?(\d{3,5})\s*,\s*(?:y:)?(\d{3,5})", part)
        if mm:
            points.append((int(mm.group(1)), int(mm.group(2))))
        mm = re.fullmatch(r"x\s*=\s*(\d+)", part)
        if mm:
            x = int(mm.group(1))
        mm = re.fullmatch(r"y\s*=\s*(\d+)", part)
        if mm and x is not None:
            points.append((x, int(mm.group(1))))
        mm = re.fullmatch(r"plane\s*=\s*(\d)", part)
        if mm:
            plane = int(mm.group(1))
    if not points:
        return None
    return (round(sum(p[0] for p in points) / len(points)), round(sum(p[1] for p in points) / len(points)), plane)


def owners(shop, owner):
    if shop in OWNERS:
        return OWNERS[shop]
    names = []
    for link in re.findall(r"\[\[([^\]|]+)(?:\|([^\]]+))?\]\]", owner or ""):
        names.append(link[1] or re.sub(r"\s*\(.*\)$", "", link[0]))
    if not names:
        names = [re.sub(r"\s*\(.*\)$", "", n.strip()) for n in re.split(r"[,/]", owner or "") if n.strip()]
    return names


def main(cache, out_path):
    os.makedirs(cache, exist_ok=True)
    lines = bucket(cache, "storeline", ["page_name", "sold_item", "store_stock", "store_sell_price",
                                        "store_delta", "store_sell_multiplier", "sold_item_json"],
                   ".where('store_currency','Coins')")
    shops = {s["page_name"]: s for s in bucket(cache, "infobox_shop", ["page_name", "owner", "is_members_only"])}
    items = bucket(cache, "infobox_item", ["page_name", "item_name", "item_id", "tradeable"])
    ge = json.loads(fetch(API + "?action=parse&format=json&prop=wikitext&page=Module:GEPrices/data.json", cache,
                          "geprices.json")["parse"]["wikitext"]["*"])

    ids = {}
    for key in ("item_name", "page_name"):
        for it in items:
            # Bucket returns booleans as "" for true and leaves the field out for false
            if it.get("tradeable") == "" and it.get("item_id") and it["item_id"][0].isdigit():
                ids.setdefault(it[key], int(it["item_id"][0]))

    pages = wikitext(cache, {l["page_name"] for l in lines if l["page_name"].rstrip(".") in SHOPS})
    rows = {}
    for l in lines:
        shop = l["page_name"].rstrip(".")
        name = l["sold_item"]
        if shop not in SHOPS or name not in ids:
            continue
        point = map_point(pages.get(l["page_name"], ""))
        try:
            price = int(float(l["store_sell_price"]))
        except ValueError:
            continue
        stock = l["store_stock"]
        stock = int(stock) if stock.isdigit() else -1  # -1: never runs out
        today = ge.get(name)
        if not point or price <= 0 or stock == 0 or not isinstance(today, (int, float)) or price >= today:
            continue
        info = shops.get(l["page_name"], {})
        members = "is_members_only" in info if info else \
            str(json.loads(l["sold_item_json"]).get("Members", "")).lower().startswith("y")
        row = (shop, ",".join(owners(shop, info.get("owner"))), "%d,%d,%d" % point, "1" if members else "0",
               ids[name], name, price, stock, int(float(l["store_delta"] or 0)),
               int(float(l["store_sell_multiplier"] or 1000)))
        # A shop listed twice for an item (versions of the shop, haggling): keep the dearer price to be safe
        key = (shop, ids[name])
        if key not in rows or rows[key][6] < price:
            rows[key] = row
    rows = list(rows.values())

    with open(out_path, "w") as f:
        f.write("# NPC shops, generated by tools/shops/build_shops.py on %s. Do not edit by hand.\n"
                % time.strftime("%Y-%m-%d"))
        f.write("# Source: OSRS Wiki shop pages (CC BY-NC-SA 3.0), https://oldschool.runescape.wiki/w/Shop\n")
        f.write("# shop\tnpcs\tx,y,plane\tmembers\titemId\titem\tprice\tstock(-1=unlimited)\tdelta\tsellMultiplier\n")
        for r in sorted(rows, key=lambda r: (r[0], r[5], r[6])):
            f.write("\t".join(str(c) for c in r) + "\n")
    print(len(rows), "offers from", len({r[0] for r in rows}), "shops")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
