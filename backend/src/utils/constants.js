export const MAX_DIM = 32;
export const MAX_HEIGHT = 128;
export const GEMINI_KEY = process.env.GEMINI_API_KEY || null;

export const ALLOWED_BLOCKS = [
  // Air
  "minecraft:air",
  
  // Stone variants
  "minecraft:stone",
  "minecraft:cobblestone",
  "minecraft:stone_bricks",
  "minecraft:mossy_stone_bricks",
  "minecraft:cracked_stone_bricks",
  "minecraft:smooth_stone",
  "minecraft:granite",
  "minecraft:polished_granite",
  "minecraft:diorite",
  "minecraft:polished_diorite",
  "minecraft:andesite",
  "minecraft:polished_andesite",
  "minecraft:deepslate",
  "minecraft:deepslate_bricks",
  "minecraft:cobbled_deepslate",
  
  // Wood planks
  "minecraft:oak_planks",
  "minecraft:spruce_planks",
  "minecraft:birch_planks",
  "minecraft:jungle_planks",
  "minecraft:acacia_planks",
  "minecraft:dark_oak_planks",
  "minecraft:mangrove_planks",
  "minecraft:cherry_planks",
  
  // Wood logs
  "minecraft:oak_log",
  "minecraft:spruce_log",
  "minecraft:birch_log",
  "minecraft:jungle_log",
  "minecraft:acacia_log",
  "minecraft:dark_oak_log",
  "minecraft:stripped_oak_log",
  "minecraft:stripped_spruce_log",
  
  // Bricks and terracotta
  "minecraft:bricks",
  "minecraft:terracotta",
  "minecraft:white_terracotta",
  "minecraft:orange_terracotta",
  "minecraft:red_terracotta",
  "minecraft:yellow_terracotta",
  "minecraft:brown_terracotta",
  "minecraft:cyan_terracotta",
  "minecraft:light_blue_terracotta",
  
  // Concrete
  "minecraft:white_concrete",
  "minecraft:orange_concrete",
  "minecraft:red_concrete",
  "minecraft:yellow_concrete",
  "minecraft:blue_concrete",
  "minecraft:black_concrete",
  "minecraft:gray_concrete",
  "minecraft:light_gray_concrete",
  
  // Glass
  "minecraft:glass",
  "minecraft:white_stained_glass",
  "minecraft:orange_stained_glass",
  "minecraft:light_blue_stained_glass",
  "minecraft:yellow_stained_glass",
  "minecraft:glass_pane",
  "minecraft:white_stained_glass_pane",
  
  // Sandstone (desert theme)
  "minecraft:sand",
  "minecraft:sandstone",
  "minecraft:smooth_sandstone",
  "minecraft:cut_sandstone",
  "minecraft:chiseled_sandstone",
  "minecraft:red_sand",
  "minecraft:red_sandstone",
  "minecraft:smooth_red_sandstone",
  "minecraft:cut_red_sandstone",
  
  // Nether blocks
  "minecraft:nether_bricks",
  "minecraft:red_nether_bricks",
  "minecraft:blackstone",
  "minecraft:polished_blackstone",
  "minecraft:polished_blackstone_bricks",
  
  // Quartz
  "minecraft:quartz_block",
  "minecraft:smooth_quartz",
  "minecraft:quartz_bricks",
  "minecraft:quartz_pillar",
  "minecraft:chiseled_quartz_block",
  
  // Prismarine
  "minecraft:prismarine",
  "minecraft:prismarine_bricks",
  "minecraft:dark_prismarine",
  "minecraft:sea_lantern",
  
  // Copper
  "minecraft:copper_block",
  "minecraft:cut_copper",
  "minecraft:exposed_copper",
  "minecraft:weathered_copper",
  "minecraft:oxidized_copper",
  
  // Wool
  "minecraft:white_wool",
  "minecraft:red_wool",
  "minecraft:blue_wool",
  "minecraft:yellow_wool",
  "minecraft:green_wool",
  "minecraft:black_wool",
  "minecraft:brown_wool",
  "minecraft:orange_wool",
  
  // Decoration blocks
  "minecraft:bookshelf",
  "minecraft:hay_block",
  "minecraft:melon",
  "minecraft:pumpkin",
  "minecraft:jack_o_lantern",
  "minecraft:glowstone",
  "minecraft:shroomlight",
  "minecraft:lantern",
  "minecraft:soul_lantern",
  
  // Fences and walls
  "minecraft:oak_fence",
  "minecraft:spruce_fence",
  "minecraft:birch_fence",
  "minecraft:cobblestone_wall",
  "minecraft:stone_brick_wall",
  "minecraft:brick_wall",
  
  // Stairs
  "minecraft:oak_stairs",
  "minecraft:cobblestone_stairs",
  "minecraft:stone_brick_stairs",
  "minecraft:brick_stairs",
  "minecraft:sandstone_stairs",
  "minecraft:quartz_stairs",
  
  // Slabs
  "minecraft:oak_slab",
  "minecraft:stone_slab",
  "minecraft:cobblestone_slab",
  "minecraft:stone_brick_slab",
  "minecraft:brick_slab",
  "minecraft:sandstone_slab",
  "minecraft:quartz_slab",
  
  // Utility
  "minecraft:ladder",
  "minecraft:torch",
  "minecraft:crafting_table",
  "minecraft:furnace",
  "minecraft:chest",
  "minecraft:barrel",
  
  // Doors and trapdoors
  "minecraft:oak_door",
  "minecraft:spruce_door",
  "minecraft:oak_trapdoor",
  "minecraft:iron_door",
  
  // Misc
  "minecraft:obsidian",
  "minecraft:crying_obsidian",
  "minecraft:iron_block",
  "minecraft:gold_block",
  "minecraft:diamond_block",
  "minecraft:emerald_block",
  "minecraft:lapis_block",
  "minecraft:redstone_block",
  "minecraft:coal_block",
  "minecraft:dirt",
  "minecraft:grass_block",
  "minecraft:moss_block",
  "minecraft:clay",
  "minecraft:mud_bricks"
];