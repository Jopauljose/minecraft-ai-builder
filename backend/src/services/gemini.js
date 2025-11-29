import { GoogleGenerativeAI } from "@google/generative-ai";
import { GEMINI_KEY } from "../utils/constants.js";

let model = null;

if (GEMINI_KEY) {
  const genAI = new GoogleGenerativeAI(GEMINI_KEY);
  model = genAI.getGenerativeModel({
    model: "gemini-2.5-flash",
    generationConfig: {
      responseMimeType: "application/json"
    }
  });
}

/**
 * Extract JSON from a string that might be wrapped in markdown code blocks
 * @param {string} text - The text to extract JSON from
 * @returns {string} - The extracted JSON string
 */
function extractJson(text) {
  // Try to extract JSON from markdown code blocks
  const jsonMatch = text.match(/```(?:json)?\s*([\s\S]*?)```/);
  if (jsonMatch) {
    return jsonMatch[1].trim();
  }
  return text.trim();
}

export async function generateStructure(prompt, width, depth, height) {
  if (!model) {
    throw new Error("Gemini model not initialized");
  }

  const template = `
You are an expert Minecraft architect. Create FUNCTIONAL, ENTERABLE buildings with HOLLOW INTERIORS.

Return ONLY a valid JSON object with this exact format:
{
 "size": [x,y,z],
 "palette": { "A": "minecraft:block_name", "B": "minecraft:block_name[property=value]", ... },
 "layers": {
    "0": [[ "A","A","B" ], [ ... ], ... ],
    "1": ...
 }
}

CRITICAL RULES:
- Approximate dimensions: width(x)~${width}, height(y)~${height}, depth(z)~${depth} (can vary slightly)
- Use palette keys: A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, _ (as needed)
- "_" = minecraft:air (VERY IMPORTANT - use for interior spaces and doorways!)
- Layer "0" is ground floor, higher numbers go up
- Each layer's 2D array: outer array = Z rows, inner array = X columns

*** BLOCK ORIENTATION/PROPERTIES ***
For directional blocks, include properties in brackets:
- Stairs: "minecraft:oak_stairs[facing=north,half=bottom]" (facing: north/south/east/west, half: top/bottom)
- Logs: "minecraft:oak_log[axis=y]" (axis: x/y/z)
- Ladders: "minecraft:ladder[facing=north]" (facing: north/south/east/west)
- Slabs: "minecraft:oak_slab[type=bottom]" (type: top/bottom/double)
- Doors: "minecraft:oak_door[facing=north,half=lower]" (half: lower/upper)

*** MOST IMPORTANT - HOLLOW INTERIORS ***
1. Buildings MUST be hollow inside - walls on the outside, air ("_") on the inside
2. MUST have at least one doorway (2 blocks high, 1-2 blocks wide) using air "_" in the wall
3. Interior floor can be different from exterior (use planks, carpet blocks, etc.)
4. Windows should be glass with air behind them (interior space)

EXAMPLE of a simple 5x4x5 hollow room (layer 0 = floor, layers 1-2 = walls with door, layer 3 = roof):
Layer 0 (floor): [[A,A,A,A,A],[A,B,B,B,A],[A,B,B,B,A],[A,B,B,B,A],[A,A,A,A,A]] - A=stone, B=planks floor
Layer 1 (walls): [[A,A,_,A,A],[A,_,_,_,A],[A,_,_,_,A],[A,_,_,_,A],[A,A,A,A,A]] - "_" = air inside + doorway
Layer 2 (walls): [[A,A,_,A,A],[A,_,_,_,A],[A,_,_,_,A],[A,_,_,_,A],[A,A,A,A,A]] - door continues up
Layer 3 (roof):  [[A,A,A,A,A],[A,A,A,A,A],[A,A,A,A,A],[A,A,A,A,A],[A,A,A,A,A]] - solid roof

DESIGN GUIDELINES:
1. Use 5-10 different block types for visual variety
2. Theme-appropriate blocks:
   - Desert: sandstone, smooth_sandstone, cut_sandstone, terracotta, sand
   - Medieval: stone_bricks, cobblestone, oak_planks, oak_log, spruce_planks
   - Modern: concrete, quartz_block, glass, smooth_stone, iron_block
   - Fantasy: prismarine, sea_lantern, end_stone_bricks
   - Rustic: oak_log, cobblestone, bricks, hay_block
3. Add windows using glass blocks
4. Add architectural details: pillars, trim, patterns
5. Make the interior walkable (at least 2 blocks high ceiling)
6. For stairs, always specify facing direction based on position (e.g., roof edges face outward)

ALLOWED BLOCKS (use minecraft: prefix):
STONE: stone, cobblestone, stone_bricks, mossy_stone_bricks, smooth_stone, granite, polished_granite, diorite, andesite, deepslate_bricks
WOOD: oak_planks, spruce_planks, birch_planks, jungle_planks, acacia_planks, dark_oak_planks, cherry_planks, oak_log[axis=y], spruce_log, birch_log, stripped_oak_log
BRICKS: bricks, nether_bricks, red_nether_bricks, mud_bricks
SANDSTONE: sandstone, smooth_sandstone, cut_sandstone, chiseled_sandstone, red_sandstone, smooth_red_sandstone
TERRACOTTA: terracotta, white_terracotta, orange_terracotta, red_terracotta, yellow_terracotta, brown_terracotta, cyan_terracotta
CONCRETE: white_concrete, orange_concrete, red_concrete, yellow_concrete, blue_concrete, black_concrete, gray_concrete
QUARTZ: quartz_block, smooth_quartz, quartz_bricks, quartz_pillar, chiseled_quartz_block
GLASS: glass, white_stained_glass, orange_stained_glass, light_blue_stained_glass, glass_pane
PRISMARINE: prismarine, prismarine_bricks, dark_prismarine, sea_lantern
METAL: iron_block, gold_block, copper_block, cut_copper
DECOR: bookshelf, hay_block, glowstone, lantern, jack_o_lantern, barrel, chest, ladder[facing=direction]
FENCES: oak_fence, spruce_fence, cobblestone_wall, stone_brick_wall, brick_wall
STAIRS: oak_stairs[facing=north], cobblestone_stairs, stone_brick_stairs, brick_stairs, sandstone_stairs, quartz_stairs
SLABS: oak_slab[type=bottom], stone_slab, cobblestone_slab, stone_brick_slab, brick_slab, sandstone_slab, quartz_slab
WOOL: white_wool, red_wool, blue_wool, yellow_wool, green_wool, black_wool, orange_wool
OTHER: dirt, grass_block, sand, clay, obsidian, air, torch, carpet

User request: ${prompt}

Create a detailed, visually interesting structure. Use at least 5-8 different block types. Make it look good!
`;

  const result = await model.generateContent(template);
  const text = result.response.text();

  let json;
  try {
    const jsonStr = extractJson(text);
    json = JSON.parse(jsonStr);
  } catch (e) {
    console.error("Failed to parse Gemini response:", text);
    throw new Error("Gemini returned non-JSON output");
  }

  return json;
}