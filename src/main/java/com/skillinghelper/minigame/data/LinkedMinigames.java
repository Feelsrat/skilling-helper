package com.skillinghelper.minigame.data;

import java.util.List;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

/**
 * Minigames that already have a full click-by-click guide in Skilling Helper or PvM Progression. Here they get what
 * those guides don't show: every requirement in one place, which rewards are worth chasing, and whether you already
 * own them. "Start guide" hands over to the other plugin.
 *
 * Reward costs are from the OSRS Wiki shop pages (Bucket API storeline rows, 2026-11-05) where they're listed, else
 * from the wiki's reward sections.
 */
final class LinkedMinigames
{
	private LinkedMinigames()
	{
	}

	static List<Minigame> all()
	{
		return List.of(
			// ---- Skilling ----
			Minigame.builder()
				.id("wintertodt")
				.name("Wintertodt")
				.category(MinigameCategory.SKILLING)
				.trains(Skill.FIREMAKING)
				.summary("Keep braziers lit against a giant ice boss with a big group: chop bruma roots, fletch "
					+ "them into kindling and feed the braziers. Each round gives a supply crate.")
				.wikiPage("Wintertodt")
				.team("Group boss (anyone on the world can join; solo works on quiet worlds)")
				.skill(Skill.FIREMAKING, 50)
				.note("Warm clothing protects you from the cold (4 pieces, e.g. the Pyromancer outfit or any hat/boots/"
					+ "gloves listed as warm)")
				.skillingMethod("fm_wintertodt")
				.reward(Reward.item("Pyromancer hood", "+2.5% Firemaking xp with the full outfit, and counts as warm "
					+ "clothing", null, 0, ItemID.PYROMANCER_HOOD))
				.reward(Reward.item("Pyromancer garb", "Part of the Pyromancer outfit", null, 0, ItemID.PYROMANCER_TOP))
				.reward(Reward.item("Pyromancer robe", "Part of the Pyromancer outfit", null, 0, ItemID.PYROMANCER_BOTTOM))
				.reward(Reward.item("Pyromancer boots", "Part of the Pyromancer outfit", null, 0, ItemID.PYROMANCER_BOOTS))
				.reward(Reward.item("Warm gloves", "Warm clothing, and needed to light braziers faster", null, 0,
					ItemID.PYROMANCER_GLOVES))
				.reward(Reward.item("Bruma torch", "A light source that also works as a tinderbox", null, 0,
					ItemID.WINT_TORCH))
				.reward(Reward.other("Supply crates", "Logs, ores, herbs, seeds and gems: good early money and banked xp"))
				.reward(Reward.also("Tome of fire", "Magic offhand, rare", ItemID.TOME_OF_FIRE, ItemID.TOME_OF_FIRE_UNCHARGED))
				.reward(Reward.also("Dragon axe", "Rare drop from the crates", ItemID.DRAGON_AXE))
				.reward(Reward.also("Phoenix", "Skilling pet", ItemID.PHOENIXPET_BLUE))
				.build(),

			Minigame.builder()
				.id("tempoross")
				.name("Tempoross")
				.category(MinigameCategory.SKILLING)
				.trains(Skill.FISHING)
				.summary("Fish harpoonfish, load them into cannons and fight off a storm spirit on a boat. Each "
					+ "round gives reward permits for the reward pool.")
				.wikiPage("Tempoross")
				.team("Group boss (solo works, but slower)")
				.skill(Skill.FISHING, 35)
				.skillingMethod("fish_tempoross")
				.points(Points.varbit("reward permits", VarbitID.TEMPOROSS_REWARDPERMITS))
				.reward(Reward.item("Fish barrel", "Holds 28 extra raw fish: almost doubles every fishing trip",
					"reward pool", 0, ItemID.FISH_BARREL_CLOSED, ItemID.FISH_BARREL_OPEN))
				.reward(Reward.item("Angler hat", "+2.5% Fishing xp with the full Angler outfit", "reward pool", 0,
					ItemID.TRAWLER_REWARD_HAT))
				.reward(Reward.item("Angler top", "Part of the Angler outfit", "reward pool", 0, ItemID.TRAWLER_REWARD_TOP))
				.reward(Reward.item("Angler waders", "Part of the Angler outfit", "reward pool", 0,
					ItemID.TRAWLER_REWARD_LEGS))
				.reward(Reward.item("Angler boots", "Part of the Angler outfit", "reward pool", 0,
					ItemID.TRAWLER_REWARD_BOOTS))
				.reward(Reward.item("Tackle box", "Stores your fishing gear in one slot", "reward pool", 0,
					ItemID.TACKLE_BOX))
				.reward(Reward.also("Spirit flakes", "Chance of an extra fish each catch", ItemID.SPIRIT_FLAKES))
				.reward(Reward.also("Tome of water", "Magic offhand, rare", ItemID.TOME_OF_WATER,
					ItemID.TOME_OF_WATER_UNCHARGED))
				.reward(Reward.also("Tiny tempor", "Skilling pet", ItemID.TEMPOROSSPET))
				.build(),

			Minigame.builder()
				.id("gotr")
				.name("Guardians of the Rift")
				.category(MinigameCategory.SKILLING)
				.trains(Skill.RUNECRAFT)
				.summary("Mine fragments, craft them into guardian essence and charge the altars through portals "
					+ "with a group. Gives Runecraft xp, runes and abyssal pearls to spend.")
				.wikiPage("Guardians_of_the_Rift")
				.team("Group game (everyone on the world plays together)")
				.skill(Skill.RUNECRAFT, 27)
				.quest(Quest.TEMPLE_OF_THE_EYE)
				.skillingMethod("rc_gotr")
				.points(Points.item("abyssal pearls", ItemID.ABYSSAL_PEARL))
				.reward(Reward.item("Abyssal needle", "Combine the essence pouches into the colossal pouch (85 "
					+ "Runecraft to use fully)", "750 pearls", 750, ItemID.ABYSSAL_NEEDLE, ItemID.RCU_POUCH_COLOSSAL,
					ItemID.RCU_POUCH_COLOSSAL_DEGRADE))
				.reward(Reward.item("Hat of the Eye", "Raiments of the Eye: up to +60% extra runes when "
					+ "runecrafting with the full set", "400 pearls", 400, ItemID.HAT_OF_THE_EYE,
					ItemID.HAT_OF_THE_EYE_RED, ItemID.HAT_OF_THE_EYE_GREEN, ItemID.HAT_OF_THE_EYE_BLUE))
				.reward(Reward.item("Robe top of the Eye", "Part of the Raiments of the Eye", "350 pearls", 350,
					ItemID.ROBE_TOP_OF_THE_EYE, ItemID.ROBE_TOP_OF_THE_EYE_RED, ItemID.ROBE_TOP_OF_THE_EYE_GREEN,
					ItemID.ROBE_TOP_OF_THE_EYE_BLUE))
				.reward(Reward.item("Robe bottoms of the Eye", "Part of the Raiments of the Eye", "350 pearls", 350,
					ItemID.ROBE_BOTTOM_OF_THE_EYE, ItemID.ROBE_BOTTOM_OF_THE_EYE_RED,
					ItemID.ROBE_BOTTOM_OF_THE_EYE_GREEN, ItemID.ROBE_BOTTOM_OF_THE_EYE_BLUE))
				.reward(Reward.item("Boots of the Eye", "Part of the Raiments of the Eye", "250 pearls", 250,
					ItemID.BOOTS_OF_THE_EYE))
				.reward(Reward.item("Abyssal lantern", "A light source that also burns logs for extra Firemaking xp",
					"1,500 pearls", 1500, ItemID.ABYSSAL_LANTERN))
				.reward(Reward.also("Catalytic talisman", "Rare drop from the reward guardian", ItemID.CATALYTIC_TALISMAN))
				.reward(Reward.also("Abyssal protector", "Pet", ItemID.ABYSSALPET))
				.build(),

			Minigame.builder()
				.id("mahogany_homes")
				.name("Mahogany Homes")
				.category(MinigameCategory.SKILLING)
				.trains(Skill.CONSTRUCTION)
				.summary("Take building contracts from Amy and fix furniture in houses around the map. Cheaper "
					+ "Construction xp than the POH, plus Carpenter points.")
				.wikiPage("Mahogany_Homes")
				.skill(Skill.CONSTRUCTION, 1)
				.skillingMethod("con_mahogany_homes")
				.reward(Reward.item("Plank sack", "Holds 28 extra planks: fewer bank trips for Construction",
					"350 points", 0, ItemID.PLANK_SACK))
				.reward(Reward.item("Carpenter's helmet", "+2.5% Construction xp with the full Carpenter's outfit",
					null, 0, ItemID.CONSTRUCTION_HAT))
				.reward(Reward.item("Carpenter's shirt", "Part of the Carpenter's outfit", null, 0,
					ItemID.CONSTRUCTION_SHIRT))
				.reward(Reward.item("Carpenter's trousers", "Part of the Carpenter's outfit", null, 0,
					ItemID.CONSTRUCTION_TROUSERS))
				.reward(Reward.item("Carpenter's boots", "Part of the Carpenter's outfit", null, 0,
					ItemID.CONSTRUCTION_BOOTS))
				.reward(Reward.also("Amy's saw", "A saw you can wear in the weapon slot", ItemID.WEARABLE_SAW))
				.reward(Reward.also("Hosidius blueprints", "Unlocks Hosidius-style furniture", ItemID.HOSIDIUS_BLUEPRINTS))
				.build(),

			Minigame.builder()
				.id("tithe_farm")
				.name("Tithe Farm")
				.category(MinigameCategory.SKILLING)
				.trains(Skill.FARMING)
				.summary("Plant, water and harvest special seeds in a timed farm in Hosidius, then hand the fruit in "
					+ "for Farming xp and points.")
				.wikiPage("Tithe_Farm")
				.skill(Skill.FARMING, 34)
				.skillingMethod("farm_tithe")
				.points(Points.varbit("Tithe Farm points", VarbitID.HOSIDIUS_TITHE_REWARDPOINTS))
				.reward(Reward.item("Herb sack", "Holds 30 of each grimy herb: makes herb runs and Slayer much easier",
					"250 points", 250, ItemID.SLAYER_HERB_SACK, ItemID.SLAYER_HERB_SACK_OPEN))
				.reward(Reward.item("Seed box", "Holds seeds in one slot", "250 points", 250, ItemID.SEED_BOX,
					ItemID.SEED_BOX_OPEN))
				.reward(Reward.item("Gricoller's can", "A watering can with 1,000 uses", "200 points", 200,
					ItemID.ZEAH_WATERINGCAN))
				.reward(Reward.item("Farmer's strawhat", "+2.5% Farming xp with the full Farmer's outfit", "75 points",
					75, ItemID.TITHE_REWARD_HAT_MALE, ItemID.TITHE_REWARD_HAT_FEMALE))
				.reward(Reward.item("Farmer's jacket", "Part of the Farmer's outfit", "150 points", 150,
					ItemID.TITHE_REWARD_TORSO_MALE, ItemID.TITHE_REWARD_TORSO_FEMALE))
				.reward(Reward.item("Farmer's boro trousers", "Part of the Farmer's outfit", "125 points", 125,
					ItemID.TITHE_REWARD_LEGS_MALE, ItemID.TITHE_REWARD_LEGS_FEMALE))
				.reward(Reward.item("Farmer's boots", "Part of the Farmer's outfit", "50 points", 50,
					ItemID.TITHE_REWARD_FEET_MALE, ItemID.TITHE_REWARD_FEET_FEMALE))
				.build(),

			Minigame.builder()
				.id("motherlode")
				.name("Motherlode Mine")
				.category(MinigameCategory.SKILLING)
				.trains(Skill.MINING)
				.summary("Mine pay-dirt veins under Falador, wash it in the water wheel and collect the ores from the "
					+ "sack. Very AFK, and the golden nuggets buy the Prospector kit.")
				.wikiPage("Motherlode_Mine")
				.skill(Skill.MINING, 30)
				.skillingMethod("mine_motherlode")
				.points(Points.item("golden nuggets", ItemID.MOTHERLODE_NUGGET))
				.reward(Reward.item("Coal bag", "Holds 27 coal (36 with the Smithing cape): halves Blast Furnace and "
					+ "superheat trips", "100 nuggets", 100, ItemID.COAL_BAG, ItemID.COAL_BAG_OPEN))
				.reward(Reward.item("Gem bag", "Holds 60 of each gem: no more full inventories of gems", "100 nuggets",
					100, ItemID.GEM_BAG, ItemID.GEM_BAG_OPEN))
				.reward(Reward.item("Prospector helmet", "+2.5% Mining xp with the full Prospector kit", "40 nuggets",
					40, ItemID.MOTHERLODE_REWARD_HAT, ItemID.MOTHERLODE_REWARD_HAT_GOLD))
				.reward(Reward.item("Prospector jacket", "Part of the Prospector kit", "60 nuggets", 60,
					ItemID.MOTHERLODE_REWARD_TOP, ItemID.MOTHERLODE_REWARD_TOP_GOLD))
				.reward(Reward.item("Prospector legs", "Part of the Prospector kit", "50 nuggets", 50,
					ItemID.MOTHERLODE_REWARD_LEGS, ItemID.MOTHERLODE_REWARD_LEGS_GOLD))
				.reward(Reward.item("Prospector boots", "Part of the Prospector kit", "30 nuggets", 30,
					ItemID.MOTHERLODE_REWARD_BOOTS, ItemID.MOTHERLODE_REWARD_BOOTS_GOLD))
				.reward(Reward.other("Bigger sack", "Pay Prospector Percy 200 nuggets to double the sack, so you "
					+ "bank half as often"))
				.build(),

			Minigame.builder()
				.id("giants_foundry")
				.name("Giants' Foundry")
				.category(MinigameCategory.SKILLING)
				.trains(Skill.SMITHING)
				.summary("Smelt bars into giant swords for Kovac: heat, hammer, grind and polish the blade to his "
					+ "order. Great Smithing xp without making items to sell.")
				.wikiPage("Giants'_Foundry")
				.skill(Skill.SMITHING, 15)
				.quest(Quest.SLEEPING_GIANTS)
				.skillingMethod("smith_giants_foundry")
				.points(Points.varp("reputation", VarPlayerID.GIANTS_FOUNDRY_REWARD_SHOP_POINTS))
				.reward(Reward.item("Double ammo mould", "Makes 8 cannonballs per bar instead of 4", "2,000 reputation",
					2000, ItemID.DOUBLE_AMMO_MOULD))
				.reward(Reward.item("Smiths tunic", "Smiths' uniform: chance to smith an item faster", "4,000 reputation",
					4000, ItemID.SMITHING_UNIFORM_TORSO))
				.reward(Reward.item("Smiths trousers", "Part of the Smiths' uniform", "4,000 reputation", 4000,
					ItemID.SMITHING_UNIFORM_LEGS))
				.reward(Reward.item("Smiths boots", "Part of the Smiths' uniform", "3,500 reputation", 3500,
					ItemID.SMITHING_UNIFORM_BOOTS))
				.reward(Reward.item("Smiths gloves", "Part of the Smiths' uniform", "3,500 reputation", 3500,
					ItemID.SMITHING_UNIFORM_GLOVES))
				.reward(Reward.also("Colossal blade", "A two-handed sword that hits harder against big monsters",
					ItemID.GIANTS_FOUNDRY_COLOSSAL_BLADE))
				.build(),

			Minigame.builder()
				.id("fishing_trawler")
				.name("Fishing Trawler")
				.category(MinigameCategory.SKILLING)
				.trains(Skill.FISHING)
				.summary("Keep a fishing boat afloat by bailing water and patching leaks; the net fills with fish "
					+ "while you do. The easiest way to the Angler outfit at low Fishing.")
				.wikiPage("Fishing_Trawler")
				.team("Team game (others on the boat help; solo is possible but hard)")
				.skill(Skill.FISHING, 15)
				.skillingMethod("fish_trawler")
				.reward(Reward.item("Angler hat", "+2.5% Fishing xp with the full Angler outfit", null, 0,
					ItemID.TRAWLER_REWARD_HAT))
				.reward(Reward.item("Angler top", "Part of the Angler outfit", null, 0, ItemID.TRAWLER_REWARD_TOP))
				.reward(Reward.item("Angler waders", "Part of the Angler outfit", null, 0, ItemID.TRAWLER_REWARD_LEGS))
				.reward(Reward.item("Angler boots", "Part of the Angler outfit", null, 0, ItemID.TRAWLER_REWARD_BOOTS))
				.build(),

			Minigame.builder()
				.id("brimhaven_arena")
				.name("Brimhaven Agility Arena")
				.category(MinigameCategory.SKILLING)
				.trains(Skill.AGILITY)
				.summary("Run between pillars in a trap-filled arena on Karamja, tagging the one with the arrow for "
					+ "tickets. Tickets turn into Agility xp or vouchers.")
				.wikiPage("Brimhaven_Agility_Arena")
				.skill(Skill.AGILITY, 1)
				.skillingMethod("agility_brimhaven")
				.points(Points.item("Brimhaven vouchers", ItemID.AGILITYARENA_VOUCHER))
				.reward(Reward.item("Pirate's hook", "Cosmetic hook, the arena's rare reward", "800 vouchers", 800,
					ItemID.PIRATEHOOK))
				.reward(Reward.other("Agility xp from tickets", "Hand tickets in for a lump of Agility xp"))
				.build(),

			Minigame.builder()
				.id("hallowed_sepulchre")
				.name("Hallowed Sepulchre")
				.category(MinigameCategory.SKILLING)
				.trains(Skill.AGILITY)
				.summary("Sneak through five floors of a vampyre crypt dodging arrows, swords and flames. The best "
					+ "Agility xp from 72, and coffins give loot and hallowed marks.")
				.wikiPage("Hallowed_Sepulchre")
				.skill(Skill.AGILITY, 52)
				.quest(Quest.SINS_OF_THE_FATHER)
				.skillingMethod("agility_sepulchre")
				.points(Points.item("hallowed marks", ItemID.HALLOWED_MARK))
				.reward(Reward.item("Ring of endurance", "Stamina potions last twice as long while you wear it (with "
					+ "500+ charges)", "coffin drop", 0, ItemID.RING_OF_ENDURANCE, ItemID.RING_OF_ENDURANCE_UNCHARGED))
				.reward(Reward.item("Strange old lockpick", "Opens the Grand Hallowed Coffin with fewer fails and "
					+ "some chests elsewhere", "coffin drop", 0, ItemID.STRANGE_OLD_LOCKPICK,
					ItemID.STRANGE_OLD_LOCKPICK_FULL))
				.reward(Reward.item("Hallowed ring", "Ring that helps with the floors' traps", "250 marks", 250,
					ItemID.HALLOWED_RING))
				.reward(Reward.also("Dark dye", "Recolours Graceful", ItemID.DARK_DYE))
				.build(),

			Minigame.builder()
				.id("pyramid_plunder")
				.name("Pyramid Plunder")
				.category(MinigameCategory.SKILLING)
				.trains(Skill.THIEVING)
				.summary("Loot urns, chests and sarcophagi in the Sophanem pyramid before time runs out. Fast "
					+ "Thieving xp and artefacts to sell.")
				.wikiPage("Pyramid_Plunder")
				.skill(Skill.THIEVING, 21)
				.questStarted(Quest.ICTHLARINS_LITTLE_HELPER)
				.skillingMethod("thieve_pyramid_plunder")
				.reward(Reward.item("Pharaoh's sceptre", "Teleports to the pyramids (Jalsavrah, Jaleustrophos, "
					+ "Jaldraocht); rare chest drop", "rare drop", 0, ItemID.PHARAOHS_SCEPTRE_CHARGED,
					ItemID.PHARAOHS_SCEPTRE_CHARGED_INITIAL))
				.reward(Reward.other("Artefacts", "Gold and stone artefacts sell for good money"))
				.build(),

			Minigame.builder()
				.id("rogues_den")
				.name("Rogues' Den")
				.category(MinigameCategory.SKILLING)
				.trains(Skill.THIEVING)
				.summary("Get through a maze of traps under Burthorpe for a chance at a piece of the Rogue outfit.")
				.wikiPage("Rogues'_Den")
				.skill(Skill.THIEVING, 50)
				.skill(Skill.AGILITY, 50)
				.skillingMethod("thieve_rogues_den")
				.reward(Reward.item("Rogue mask", "With the full Rogue outfit pickpocketing can give double or "
					+ "triple loot", null, 0, ItemID.ROGUESDEN_HELM))
				.reward(Reward.item("Rogue top", "Part of the Rogue outfit", null, 0, ItemID.ROGUESDEN_BODY))
				.reward(Reward.item("Rogue trousers", "Part of the Rogue outfit", null, 0, ItemID.ROGUESDEN_LEGS))
				.reward(Reward.item("Rogue gloves", "Part of the Rogue outfit", null, 0, ItemID.ROGUESDEN_GLOVES))
				.reward(Reward.item("Rogue boots", "Part of the Rogue outfit", null, 0, ItemID.ROGUESDEN_BOOTS))
				.build(),

			Minigame.builder()
				.id("sorceress_garden")
				.name("Sorceress's Garden")
				.category(MinigameCategory.SKILLING)
				.trains(Skill.THIEVING)
				.summary("Sneak past the elementals in the seasonal gardens and pick sq'irks; the juice gives "
					+ "Thieving xp (and Farming xp for the herbs).")
				.wikiPage("Sorceress's_Garden")
				.skill(Skill.THIEVING, 65)
				.quest(Quest.PRINCE_ALI_RESCUE)
				.skillingMethod("thieve_summer_garden")
				.reward(Reward.other("Sq'irk juice", "Give it to Osman in Al Kharid for Thieving xp"))
				.build(),

			Minigame.builder()
				.id("nightmare_zone")
				.name("Nightmare Zone")
				.category(MinigameCategory.COMBAT)
				.trains(Skill.STRENGTH)
				.summary("Fight quest bosses in a dream for very AFK combat xp. The points imbue rings and the "
					+ "Slayer helmet, which is the big reason to go.")
				.wikiPage("Nightmare_Zone")
				.note("5 of the Nightmare Zone quests done (Skilling Helper checks which)")
				.skillingMethod("strength_nmz")
				.points(Points.varp("Nightmare Zone points", VarPlayerID.NZONE_REWARDPOINTS))
				.reward(Reward.other("Imbued rings", "Berserker, archers', seers' and warrior rings get much "
					+ "stronger: 650,000 points each (Dominic Onion's reward chest)"))
				.reward(Reward.other("Imbued Slayer helmet / black mask", "+15% damage on task for every style "
					+ "instead of melee only: 1,250,000 points"))
				.reward(Reward.also("Herb boxes", "10 random herbs for 9,500 points (members, 15 a day)",
					ItemID.NZONE_HERBBOX))
				.build(),

			// ---- Combat (PvM Progression has the guides) ----
			Minigame.builder()
				.id("pest_control")
				.name("Pest Control")
				.category(MinigameCategory.COMBAT)
				.summary("Defend a Void Knight with a team by killing the pests and their portals. Commendation "
					+ "points buy the Void Knight set, a great early ranged and magic set.")
				.wikiPage("Pest_Control")
				.team("Team game (boats leave once enough players are on)")
				.combat(40)
				.note("For Void equipment: 42 Attack, Strength, Defence, Hitpoints, Ranged and Magic, and 22 Prayer")
				.pvmContent("pest_control")
				.reward(Reward.item("Void knight top", "Void set: +10% accuracy and damage with the matching helm",
					"250 points", 0, ItemID.PEST_VOID_KNIGHT_TOP, ItemID.ELITE_VOID_KNIGHT_TOP))
				.reward(Reward.item("Void knight robe", "Part of the Void set", "250 points", 0,
					ItemID.PEST_VOID_KNIGHT_ROBES, ItemID.ELITE_VOID_KNIGHT_ROBES))
				.reward(Reward.item("Void knight gloves", "Part of the Void set", "150 points", 0,
					ItemID.PEST_VOID_KNIGHT_GLOVES))
				.reward(Reward.item("Void ranger helm", "The helm picks the style: ranger is the most used",
					"200 points", 0, ItemID.GAME_PEST_ARCHER_HELM))
				.reward(Reward.item("Void mage helm", "Void helm for Magic", "200 points", 0, ItemID.GAME_PEST_MAGE_HELM))
				.reward(Reward.item("Void melee helm", "Void helm for melee", "200 points", 0, ItemID.GAME_PEST_MELEE_HELM))
				.reward(Reward.also("Elite void top and robe", "Upgrade after the hard Western Provinces diary (200 "
					+ "points each)"))
				.build(),

			Minigame.builder()
				.id("barbarian_assault")
				.name("Barbarian Assault")
				.category(MinigameCategory.COMBAT)
				.summary("A team of five, each with a role (attacker, collector, defender, healer), fights ten waves "
					+ "of Penance monsters. Honour points buy the Fighter torso, the best free melee body.")
				.wikiPage("Barbarian_Assault")
				.team("Needs a team of 5 (find one in the Barbarian Assault chat channel)")
				.pvmContent("barbarian_assault")
				.reward(Reward.item("Fighter torso", "Best-in-slot-for-free melee body (+4 Strength), no Defence "
					+ "level needed", "375 points of each role", 0, ItemID.BARBASSAULT_PENANCE_FIGHTER_TORSO))
				.reward(Reward.item("Penance skirt", "Ranged legs with no Defence needed", "375 points of each role",
					0, ItemID.BARBASSAULT_PENANCE_RANGER_LEGS))
				.reward(Reward.item("Healer hat", "Prayer and Magic hat", "275 points of each role", 0,
					ItemID.BARBASSAULT_PENANCE_HEALER_HAT))
				.reward(Reward.also("Fighter hat, runner hat, ranger hat, runner boots, Penance gloves",
					"Role gear; mostly cosmetic"))
				.build(),

			Minigame.builder()
				.id("warriors_guild")
				.name("Warriors' Guild")
				.category(MinigameCategory.COMBAT)
				.summary("Animate armour for tokens, then kill cyclopes on the top floor for defenders: bronze up to "
					+ "rune, and dragon in the basement at 60 Attack and Defence.")
				.wikiPage("Warriors'_Guild")
				.note("Attack + Strength adds up to 130, or 99 in either")
				.pvmContent("warriors_guild")
				.points(Points.item("warrior guild tokens", ItemID.WARGUILD_TOKENS))
				.reward(Reward.item("Rune defender", "The best offhand for melee until the dragon defender",
					"cyclops drop", 0, ItemID.RUNE_PARRYINGDAGGER, ItemID.DRAGON_PARRYINGDAGGER))
				.reward(Reward.item("Dragon defender", "Best common melee offhand (60 Defence)", "cyclops drop",
					0, ItemID.DRAGON_PARRYINGDAGGER))
				.build(),

			Minigame.builder()
				.id("fight_caves")
				.name("TzHaar Fight Cave")
				.category(MinigameCategory.COMBAT)
				.summary("Survive 63 waves of TzHaar monsters and beat TzTok-Jad for the fire cape.")
				.wikiPage("TzHaar_Fight_Cave")
				.note("Recommended: 70+ Ranged, 43+ Prayer, good ranged gear")
				.pvmContent("fight_caves")
				.reward(Reward.item("Fire cape", "Best melee cape until the infernal cape", "beat Jad", 0,
					ItemID.TZHAAR_CAPE_FIRE, ItemID.INFERNAL_CAPE))
				.build(),

			Minigame.builder()
				.id("gauntlet")
				.name("The Gauntlet")
				.category(MinigameCategory.COMBAT)
				.summary("Gather and craft your own gear inside a crystal dungeon in Prifddinas, then fight the "
					+ "Crystalline Hunllef. No gear risk: you bring nothing in.")
				.wikiPage("The_Gauntlet")
				.quest(Quest.SONG_OF_THE_ELVES)
				.pvmContent("gauntlet")
				.reward(Reward.other("Crystal armour seeds and the enhanced crystal weapon seed",
					"Bowfa and crystal armour, mostly from the Corrupted Gauntlet"))
				.build(),

			Minigame.builder()
				.id("fortis_colosseum")
				.name("Fortis Colosseum")
				.category(MinigameCategory.COMBAT)
				.summary("Twelve waves of gladiators with modifiers you pick, ending with Sol Heredit. Dangerous "
					+ "endgame content.")
				.wikiPage("Fortis_Colosseum")
				.dangerous(true)
				.quest(Quest.CHILDREN_OF_THE_SUN)
				.pvmContent("fortis_colosseum")
				.reward(Reward.item("Dizana's quiver", "Best ranged cape slot (beat wave 12)", "beat Sol Heredit", 0,
					ItemID.DIZANAS_QUIVER_CHARGED))
				.build(),

			Minigame.builder()
				.id("inferno")
				.name("Inferno")
				.category(MinigameCategory.COMBAT)
				.summary("69 waves ending in TzKal-Zuk. You sacrifice a fire cape to enter; the infernal cape is "
					+ "the best melee cape.")
				.wikiPage("Inferno")
				.note("A fire cape to sacrifice (or 1 earlier Inferno completion)")
				.pvmContent("inferno")
				.reward(Reward.item("Infernal cape", "Best melee cape", "beat Zuk", 0, ItemID.INFERNAL_CAPE))
				.build()
		);
	}
}
