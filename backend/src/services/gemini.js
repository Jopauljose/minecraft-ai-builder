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

export async function generateStructure(prompt, maxDim) {
  if (!model) {
    throw new Error("Gemini model not initialized");
  }

  const template = `
You are a Minecraft structure generator.

Return ONLY a valid JSON object with this exact format:
{
 "size": [x,y,z],
 "palette": { "A": "minecraft:block_name", "B": "minecraft:block_name", ... },
 "layers": {
    "0": [[ "A","A","B" ], [ ... ], ... ],
    "1": ...
 }
}

Rules:
- x and z ≤ ${maxDim}, y ≤ 128
- Use ONLY palette keys A,B,C,D...
- Use only allowed blocks:
  minecraft:cobblestone,
  minecraft:oak_planks,
  minecraft:oak_fence,
  minecraft:ladder,
  minecraft:glass,
  minecraft:stone_bricks,
  minecraft:brick,
  minecraft:air
- Each layer is a 2D array where the outer array represents Z rows and inner arrays represent X columns
- Layer "0" is the bottom layer, higher numbers are higher in the world
- No explanation, no comments. Raw JSON only.

User prompt: ${prompt}
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