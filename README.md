# Skilling Helper

A RuneLite plugin that works like Quest Helper, but for skilling. Pick a skill and a goal, and it tells you
what to do step by step, draws the way there, and highlights what to click.

It only draws overlays and shows information. It never clicks, types or moves anything for you, so it stays within
RuneLite's and Jagex's third-party client rules. The one thing it changes is the order of the bank's own
Withdraw/Deposit options (like Menu Entry Swapper), and that can be turned off with "Left-click the amount needed".

## Features

- **Step panel**: the current step (bank, buy, wear gear, walk, train, drop) updates every tick from your
  inventory, bank, position and levels.
- **Goals**: the default goal is your current level + 10 (configurable). Change it any time in the panel.
- **Methods with options**: Recommended, Fastest, AFK, Cheapest and Profit. The planner splits the goal into
  legs and switches method when a better one unlocks. You can also pick a method yourself.
- **Knows what you own**: the bank is remembered per account once you open it, and your GP (coins + platinum
  tokens, carried and banked) is used to say whether you can afford a method and what it costs.
- **Best tool**: recommends the best axe/pickaxe/harpoon your level allows and highlights the best one you own.
- **GE prices**: material costs and product values come from the Grand Exchange (RuneLite's GE guide prices, or
  live instant-buy/sell prices from the OSRS Wiki if you turn that on). Product values have the 2% GE tax taken off.
  When buying, each item shows quantity × price, and if you can't afford the whole goal the guide says how many
  actions your gold covers and whether selling the products pays for the next batch.
- **Supplies**: every guide lists what to bring and withdraw first (e.g. logs, clockworks and seeds for birdhouse
  runs, enough traps for hunting, gardener payments for tree runs), plus optional extras like food, stamina
  potions and compost with their GE price.
- **Runs**: bird house, herb, tree and allotment runs lead you from one spot or patch to the next, with the fastest
  teleport for each, and skip the ones still growing (read from the game's own patch and bird house state). Bird
  houses you crafted in advance count instead of their logs and clockworks, and any herb or allotment seed you own
  works. When a run is done the panel says how long until the next one, countdown infoboxes show when bird houses,
  herbs, trees and allotments are ready, and you get a notification when it's time for a run (all can be turned
  off under Run timers in the plugin settings).
- **Making things first**: when an input is cheaper to make, or you already have what it's made from, the guide
  has you make it: baskets of fruit for gardeners, unfinished potions from herbs and vials, ground secondaries,
  gold bars and molten glass at the furnace, leather at the tanner and planks at the sawmill (with the route
  there). Items the GE doesn't sell, like the barbarian rod or an enchanted gem, send you to where you get them.
- **Bank filter stays on**: once you switch on "show only items for this guide" in the bank, it stays on between
  bank visits until you switch it off.
- **Combat gear**: combat and Slayer guides suggest the best weapon and armour you can wear for melee or ranged:
  wear it if it's in your bank, or buy the upgrade (price shown). Always optional.
- **Time to goal**: the panel shows the estimated time to your target level for the whole plan, and for each method.
- **Skilling outfits**: if you own Lumberjack, Prospector, Angler, Pyromancer, Farmer's, Carpenter's, Raiments of
  the Eye, Smiths' uniform, Rogue or cooking gauntlets, the guide tells you to wear them and highlights them. If
  you don't, it says where to earn them and how much time the full set would save; the method that earns it is
  marked with a gold ★ in the method list so you can choose to chase it.
- **Optional GE boosts**: tradeable helpers like the dodgy necklace, ring of forging and amulet of chemistry are
  suggested with their current price, and highlighted to wear once you own them.
- **Highlights**: trees, rocks, fishing spots, ranges and banks are outlined (or clickbox/tile). Items to use,
  wear, drop, deposit or withdraw are outlined in the inventory and bank.
- **Pathing**: minimap arrow, world map marker, hint arrow and destination tile. If the
  [Shortest Path](https://runelite.net/plugin-hub/show/shortest-path) plugin is installed, the full
  route is drawn with it. The route ends anywhere in the training area (not on one exact tile), boats, ships,
  shortcuts and other unlocked transports are allowed (turn off with "Allow all transports in path"), and if
  there's still no full route the step says so. For Sailing, it sends you to the dock where you left your boat,
  then sails to the nearest wreck; while sailing, a land step docks you near it first. Training spots are chosen
  near banks.
- **Bank filter button**: a button in the bank (next to the `?`) shows only the items the guide needs, like Quest
  Helper's. There's also a button for it in the panel.

## Guides

Built-in step-by-step guides (based on the OSRS wiki training guides):

| Skill | Methods |
| --- | --- |
| Attack / Strength / Defence / Hitpoints | Chickens, cows, Al Kharid warriors, hill giants, sand crabs, Nightmare Zone |
| Ranged | The same monsters with arrows |
| Prayer | Burying bones, gilded altar, Chaos Temple |
| Magic | Strike spells, teleports, Superheat, High Alchemy, enchanting rings |
| Runecraft | Air, earth and fire runes, Guardians of the Rift, Arceuus blood runes |
| Crafting | Leather, gold jewellery, gem cutting, glassblowing, battlestaves, d'hide bodies |
| Smithing | Smelting bronze to gold, platebodies at Varrock, Blast Furnace, Giants' Foundry |
| Herblore | Druidic Ritual, attack potions to Saradomin brews |
| Agility | Gnome course and every rooftop course from Draynor to Ardougne |
| Thieving | Men, tea and cake stalls, Pyramid Plunder, Rogues' Den, master farmers, Ardougne knights |
| Slayer | Turael to Duradel (with combat level checks) |
| Hunter | Bird snaring, swamp lizards, birdhouse runs, red chinchompas |
| Farming | Allotments, Tithe Farm, tree runs, herb runs |
| Construction | Chairs and bookcases to mahogany tables, Mahogany Homes |
| Woodcutting | Trees, oaks, Draynor willows, Seers' maples, Edgeville yews, Woodcutting Guild yews & magics |
| Mining | Copper/tin, iron (Varrock SE, Al Kharid), Motherlode Mine |
| Fishing | Shrimp, fly fishing, lobsters, barbarian fishing, sharks, Fishing Trawler |
| Cooking | Shrimp to sharks on ranges near banks, wines, karambwans |
| Firemaking | Logs to magic logs at the Grand Exchange, Wintertodt |
| Fletching | Cutting short/longbows, stringing longbows |
| Sailing | Pandemonium and port tasks, salvaging shipwrecks |

Xp rates are rough estimates for a typical setup. After about 10 minutes of training a method, the plugin uses the
xp rate you actually get instead.

## Network use

The plugin makes no network requests unless you turn on **Live OSRS Wiki prices** (off by default). With it on, it
fetches the wiki's single bulk price list (`prices.runescape.wiki/api/v1/osrs/latest`) at most once every 5 minutes
while you're logged in, with a User-Agent that identifies the plugin, as the wiki asks.

## Updating on Steam Deck

Every release includes `skilling-helper-update.sh`. It reads Steam's list of non-Steam shortcuts, works out which
Skilling Helper jar your RuneLite shortcut runs (a `java -jar skilling-helper-…-all.jar` shortcut, a launch script
that runs one, or RuneLite with the plugin in `~/.runelite/sideloaded-plugins`), and swaps in the newest release.
The old jar is kept next to it as `.bak`, and the jar keeps its file name so the shortcut still works.

In Desktop Mode, download the script from the latest release, then in a terminal:

```
bash ~/Downloads/skilling-helper-update.sh --install
```

That puts an "Update Skilling Helper" launcher on the Desktop and in the app menu; run it whenever there's a new
release. While the repo is private, the first run asks for a GitHub token (a fine-grained token with read-only
Contents access to this repo) and saves it in `~/.config/skilling-helper-updater/token`.

To update every time you start the game, even from Game Mode, put
`"$HOME/.local/bin/skilling-helper-update" --auto %command%` (with your home folder written out, usually
`/home/deck`) at the start of the shortcut's launch options, before what's already there. If there's no internet
or GitHub is slow, the game just starts with the jar it has; what happened is logged to
`~/.cache/skilling-helper-update.log`. Other options: `--list` shows your shortcuts, `--shortcut NAME` picks one,
`--force` reinstalls.

## Adding a method

Methods are plain Java data in `src/main/java/com/skillinghelper/data/guides/`. Each one lists its level, xp rates,
location and nearest bank, tools, inputs, products and what to click (matched by name and menu action, so every
variant of e.g. "Willow tree" is covered). The planner and step engine handle the rest.

## Development

```
./gradlew build   # compile + unit tests
./gradlew run     # start RuneLite with the plugin loaded (developer mode)
```

`GuideSnapshotTest` records what the guide shows (checklist, next step, highlights, bank amounts and the planner's
ranking) for every method in about 20 situations each, so a refactor can't change the guide by accident. When a
change is meant to alter the guide, run the tests, check the changed cases written to `build/guide-snapshots-actual/`,
then update the snapshots with `UPDATE_SNAPSHOTS=true ./gradlew test`.

Code layout:

- `data/`: the guides themselves (methods, locations, items, outfits), as plain Java data.
- `engine/`: pure logic with no client access, so it's unit tested: `MethodPlanner` ranks methods and plans the
  goal, `StepResolver` picks the next step (with `RequirementChecker`, `BankTrip`, `Recipes` and `Runs`).
- The plugin package reads the game (`PlayerSnapshot`, trackers), saves choices (`GuideSettings`), and shows the
  guide (`ui/`, `overlay/`, `bank/`, `PathDisplay`).

To log in to the development client with a Jagex account, follow
[Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).
