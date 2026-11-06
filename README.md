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
- **Shops when cheaper**: with "Buy from shops when cheaper" on (the default, in all three guide plugins), a buy step
  sends you to an NPC shop instead of the GE when it sells what you need for less: farming tools at Sarah's, a chisel
  at the nearest general store, raw sharks at Frankie's in Piscarilius. The guide knows each shop's stock and how its
  price climbs as the stock runs down, so it says how many to buy per world and only picks a shop that stays cheaper
  within 3 worlds. A detour has to save at least 1,000 gp (more for far-away shops), unless the shop has everything
  and is closer than the GE. Only shops anyone can enter are used (no quest, guild or Wilderness shops). The shop list
  comes from the OSRS Wiki (`tools/shops/build_shops.py`); the plugin doesn't go online for it.
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
- **Slayer tasks**: once you have a task, the Slayer guide reads it from the game (monster, kills left, Konar's
  area, your points) and paths you to a place the monsters spawn that your quests and levels let you into, then
  highlights them. Krystilia's tasks stay in the Wilderness and other masters' tasks stay out of it. Cancelling is
  only suggested when you have the 30 points for it. Task locations come from the OSRS Wiki via
  [slayer-sensei](https://github.com/rRexhepi/slayer-sensei)'s BSD-licensed data (see `tools/slayer/`).
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
| Slayer | Turael to Duradel (with combat level checks); with a task, leads you to its monsters and highlights them |
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

## PvM Progression (second plugin)

The same jar also contains **PvM Progression**, a separate plugin you can switch on or off by itself. It works out
which combat content to work towards next from your levels, quests, kill counts and what you own, from the first
bosses (Obor, Scurrius, Giant Mole) through Pest Control, Barrows, Barbarian Assault and the Fight Cave, to God Wars,
the raids, the Colosseum and the Inferno.

- **Dropdown sorted by level**: every entry is listed from the lowest recommended level to the highest, coloured
  by whether you're ready, almost ready, need to train, or are locked out by a quest. It starts on the lowest one
  you haven't done yet.
- **Checklist** for the one you pick: quests, levels (green when you have them, with a **Train** button that opens
  the Skilling Helper guide for that skill and level), what to bring (ticked off from your inventory, gear and
  bank), how to get there (with a **Show path** button that uses Shortest Path), and a link to the wiki strategy
  guide.
- **Done automatically**: your first kill (read from the game's "kill count" messages) marks it done. Minigames
  without a kill count can be marked done by hand.

Recommended levels are the comfortable levels commonly suggested in wiki guides, not hard limits. The plugin only
shows information before you go in: it has no in-fight helpers (no attack, prayer or mechanic indicators), which
RuneLite doesn't allow for bosses.

## Money Making (third plugin)

The jar also contains **Money Making**, its own plugin and panel (coin icon), all about gold rather than xp. It lists
every method in the [OSRS Wiki money making guide](https://oldschool.runescape.wiki/w/Money_making_guide) (skilling,
combat, collecting, processing and recurring ones like herb runs and daily shop stock), keeps only the ones your
levels, quests and world allow, and sorts them by gp/hr at today's GE prices.

- **Filters**: category, effort (the wiki's intensity), a search box (name, skill or item made), and a switch to also
  show the ones you can't do yet with what's missing.
- **Details** for each method: requirements ticked off against your account (diaries and other unlocks the plugin
  can't read are shown as "?"), where it's done, what to bring and what it makes per hour, and the wiki's own
  instructions.
- **Start guide**: written for players new to the game. If your bank doesn't have the supplies it walks you to the
  Grand Exchange and explains how to buy them; then it has you open a bank and take out exact amounts (outlined in
  the bank), walks you to the spot (hint arrow and Shortest Path line), and shows the wiki's instructions with the
  shopkeeper or the items to use outlined. When your inventory fills up, or the main input runs out, it takes you
  back to the bank and round again.
- **Profit at today's prices**: supplies are costed at what they cost to buy and products at what they sell for,
  minus the 2% GE tax, with the breakdown in each method's details. Methods that lose money right now are hidden
  (a switch shows them), and so are ones you can't afford a trip's supplies for, counting what you already own
  (checked once you've opened your bank).
- **Will it sell?** With "Live OSRS Wiki prices" on (off by default, it contacts the wiki's price API), each method's
  main products are checked against how many traded on the GE in the last day: ones that barely trade are hidden as
  "hard to sell" and slow ones get a warning. Inputs used faster than the GE's 4-hour buy limit get a warning too.
- **Bank button**: a coin button at the top of the bank shows only the running guide's supplies. It's on by
  default; click it to show the whole bank again.
- **Full guides**: when Skilling Helper or PvM Progression has a click-by-click guide for the method (e.g. Barrows,
  Zulrah, Vorkath, cleaning herbs), Start opens that one instead. Turn this off with "Use full guides" in the settings.

The method list is bundled with the plugin (built from the wiki by `tools/money/build_money_methods.py`). Prices come
from RuneLite's own GE guide unless you turn on live wiki prices.

## Quest Order Guide

Also in the jar: **Quest Order Guide** (blue quest icon). It follows the OSRS Wiki's
[optimal quest guide](https://oldschool.runescape.wiki/w/Optimal_quest_guide) (main or Ironman route, picked by
account type) and shows what's done, what you can do now, and what's missing for the rest (quests, levels, Quest
points, members). The next quest is handed over to the [Quest Helper](https://runelite.net/plugin-hub/show/quest-helper)
plugin for the click-by-click steps: the guide draws the way to the start with Shortest Path, outlines the NPC who
starts it and the quest's line in the Quest list, and tells you to right-click it and choose "Start Quest Helper".
Without Quest Helper it warns once in the chat and in the panel. Diary rows send you to Achievement Diary Guides.
Data is rebuilt with `python3 tools/quests/build_quests.py src/main/resources/com/skillinghelper/quests`.

## Collection Log Guide

Also in the jar: **Collection Log Guide** (red book icon). It shows the collection log slots you don't have yet that
most players have (the wiki's completion figures, so the quickest ones come first), the pages you're closest to
finishing, every pet with where it comes from, and the whole log by tab. Pick one for beginner steps: where a full
guide exists elsewhere in the jar (PvM Progression for bosses and raids, Minigame Guides for Wintertodt, Tempoross,
Guardians of the Rift, Castle Wars and other minigames, Skilling Helper for rooftops and skilling pets, Clue Scroll
Guide for the clue pages) "Open full guide" starts it, and boss pages also open their Combat Achievement Guides
tasks; otherwise "Show the way" draws
the route with Shortest Path and the boss is outlined. Your log is read from the log pages you open in game and from
the "New item added to your collection log" chat message, and saved per account; items in your bank count until the
log page says otherwise. Wilderness pages follow the shared Wilderness setting. No network requests. Data is rebuilt
with `python3 tools/collection/build_collection_log.py <cache dir> src/main/resources/com/skillinghelper/collection/collection-log.tsv`.

## Clue Scroll Guide

A panel for Treasure Trails that works alongside RuneLite's built-in Clue Scroll plugin (which solves each step; the
panel offers to turn it on if it's off):

- **Your clues**: every clue scroll, scroll box, clue bottle/nest/geode and reward casket you carry or have in the
  bank, per tier, with a warning when you're near your scroll box limit and new drops would be lost.
- **Guide**: opens caskets and scroll boxes you already have, takes your clue and a spade out of the bank, tells you how
  to start the clue, and when you have none of a tier, walks you through getting one step by step (goblins, dark
  wizards, H.A.M. members, wealthy citizens, Falador guards, or impling jars bought on the GE). Master: collects one
  easy, medium, hard and elite clue and takes you to Watson.
- **Rewards**: what each tier's caskets can give, the average casket value at today's GE prices, its most valuable
  uniques, master clue chances and milestone rewards.
- **STASH units**: the Construction level and planks per tier, and all 119 units with the items they hold (ticked
  when you own them all). Click one to be guided there.
- **Drop sources**: the best clue drop rates of each tier from the wiki. Wilderness units and sources only show when
  Wilderness is allowed.

The data (rewards, STASH units, drop sources) is built from the OSRS Wiki by `tools/clue/build_clue_data.py`; the
plugin doesn't go online for it.

## Account Unlocks

Also in the jar: **Account Unlocks** (padlock icon). A checklist of the unlocks that make the game smoother, in a
suggested order: graceful, a house, the Dramen staff, fairy rings, spirit trees, gnome gliders, the easy diary
rewards (Ardougne cloak, Explorer's ring, Karamja gloves, Varrock armour), coal and gem bags, the Ectophial, Ava's
accumulator, the Slayer helmet unlock, the rune pouch, Barrows gloves, Ancient Magicks, the Lunar spellbook and
staffless fairy rings. Each row says what it gives, how it's got and what's still missing; done ones are ticked (read
from quests, varbits, claimed diary rewards and the items in your bank). Start the guide, or click a row, and it
takes you through the next part: quests go to Quest Helper (warns in the chat and panel if it's missing), diary
rewards open Achievement Diary Guides, points to earn (marks of grace, golden nuggets, Slayer points) open Skilling
Helper, and the last part (buying it from Grace, Percy, a Slayer master or the Culinaromancer's chest) is guided step
by step with the bank, the route and what to click. No network requests.

## Network use

Skilling Helper makes no network requests unless you turn on **Live OSRS Wiki prices** (off by default). With it on, it
fetches the wiki's single bulk price list (`prices.runescape.wiki/api/v1/osrs/latest`) at most once every 5 minutes
while you're logged in, with a User-Agent that identifies the plugin, as the wiki asks. Money Making's own **Live OSRS
Wiki prices** setting (also off by default) fetches the same price list plus the wiki's daily trade volumes
(`/volumes`, every 30 minutes). PvM Progression makes no network requests.

## Windows

Every release includes `skilling-helper-windows.ps1`, which installs and starts the RuneLite dev client with all the
plugins built in. The first time, open PowerShell (Start menu, type `PowerShell`, Enter) and paste:

```
irm https://github.com/Feelsrat/skilling-helper/releases/latest/download/skilling-helper-windows.ps1 | iex
```

(or download the file, right-click it and pick **Run with PowerShell**). It downloads a private Java 21 and the client
into `%LOCALAPPDATA%\SkillingHelper`, adds a **Skilling Helper** icon to the Desktop and Start menu, and opens RuneLite.
From then on, start the game with that icon: it first downloads the newest plugins (and a newer launcher) if there's
a new release, then opens RuneLite. With no internet it just starts the version you have. What it did is written to
`%LOCALAPPDATA%\SkillingHelper\launcher.log`.

Logging in with a Jagex account needs your saved login in `%USERPROFILE%\.runelite\credentials.properties`. The
launcher takes it from `skilling-helper-phone.zip` (the Steam Deck "phone kit") if that's in your Downloads folder.
Otherwise save it once: Start menu > **RuneLite (configure)**, add `--insecure-write-credentials` to Client arguments,
start RuneLite from the Jagex Launcher once, then use the Skilling Helper icon (see
[Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts)).

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

To update every time you start the game, even from Game Mode, say yes when `--install` (or the Update icon) asks.
It puts `"$HOME/.local/bin/skilling-helper-update" --auto %command%` at the start of the shortcut's launch options
for you (Steam closes for a few seconds while it saves that; `--enable-auto` does it without asking). Each start shows a small "Checking for Skilling Helper updates..." window and then whether it updated, was already up to date, or failed, with a 3-second countdown before it closes (put `SKILLING_HELPER_QUIET=1 ` before the launch options to hide it). If there's no internet
or GitHub is slow, the game just starts with the jar it has; what happened is logged to
`~/.cache/skilling-helper-update.log`. Other options: `--list` shows your shortcuts, `--shortcut NAME` picks one,
`--force` reinstalls.

## Android (GameNative)

[GameNative](https://github.com/utkarshdalal/GameNative) runs Windows programs on Android, so it can run the normal
Windows RuneLite dev client with these plugins built in. Every release includes `skilling-helper-android.sh`,
which sets it up from [Termux](https://f-droid.org/packages/com.termux/):

1. Your Jagex login: on the Steam Deck double-click **Skilling Helper phone kit** on the Desktop (added by the Deck
   updater). It makes `skilling-helper-phone.zip` with your saved login (see
   [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts)). Copy it into the phone's
   Download folder.
2. In Termux run
   `curl -fLo sh-android.sh https://github.com/Feelsrat/skilling-helper/releases/latest/download/skilling-helper-android.sh && bash sh-android.sh`.
   If GameNative isn't installed it downloads it and opens the installer; open GameNative once, then run
   `skilling-helper-android`. It builds `Download/SkillingHelper`: `SkillingHelper.exe`, a Windows Java 21, the
   client and your login.
3. In GameNative: **Add Custom Game > Continue >** pick `Download/SkillingHelper`. GameNative copies it into its own
   storage (it isn't allowed to read other apps' files, so the game has to carry everything).
4. Press Play. GameNative picks `SkillingHelper.exe` (the only `.exe`, a tiny launcher built from
   `android/launcher/`) by itself. Turn on **Touchscreen Mode** in GameNative's quick menu once.

Updates are automatic: on every start the launcher compares the newest release's `android-version.txt` with its own
and downloads `android-client.jar` if it changed. Only a new Jagex login needs steps 1 to 3 again.

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
