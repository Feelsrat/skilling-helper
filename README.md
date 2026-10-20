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
- **Skilling outfits**: if you own Lumberjack, Prospector, Angler, Pyromancer or cooking gauntlets, the guide
  tells you to wear them and highlights them.
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
| Woodcutting | Trees, oaks, Draynor willows, Seers' maples, Edgeville yews, Woodcutting Guild yews & magics |
| Mining | Copper/tin, iron (Varrock SE, Al Kharid), Motherlode Mine |
| Fishing | Shrimp, fly fishing, lobsters, barbarian fishing, sharks |
| Cooking | Shrimp to sharks on ranges near banks, wines, karambwans |
| Firemaking | Logs to magic logs at the Grand Exchange, Wintertodt |
| Fletching | Cutting short/longbows, stringing longbows |

Every other skill shows a link to its wiki training guide until it gets a built-in guide.

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
