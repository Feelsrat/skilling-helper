# Skilling Helper

A RuneLite plugin that works like Quest Helper, but for skilling. Pick a skill and a goal, and it tells you
what to do step by step, draws the way there, and highlights what to click.

It only draws overlays and shows information. It never clicks, types or moves anything for you, so it stays within
RuneLite's and Jagex's third-party client rules.

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
  [Shortest Path](https://runelite.net/plugin-hub/show/shortest-path) plugin is installed, the full walking
  route is drawn with it. Training spots are chosen near banks.
- **Bank filter button**: a button in the bank (next to the `?`) shows only the items the guide needs, like Quest
  Helper's. There's also a button for it in the panel.

## Guides

Built-in step-by-step guides (data from the OSRS wiki training guides):

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

Xp rates are rough averages from the wiki for a typical setup; your own rates will vary.

## Adding a method

Methods are plain Java data in `src/main/java/com/skillinghelper/data/guides/`. Each one lists its level, xp rates,
location and nearest bank, tools, inputs, products and what to click (matched by name and menu action, so every
variant of e.g. "Willow tree" is covered). The planner and step engine handle the rest.

## Development

```
./gradlew build   # compile + unit tests
./gradlew run     # start RuneLite with the plugin loaded (developer mode)
```

To log in to the development client with a Jagex account, follow
[Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).
