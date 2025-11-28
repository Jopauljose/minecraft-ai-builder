import { ALLOWED_BLOCKS } from "./constants.js";

export function sanitize(obj, maxDim) {
  if (!obj.size || !obj.palette || !obj.layers) {
    throw new Error("Structure missing one of size/palette/layers");
  }

  const [sx, sy, sz] = obj.size;
  if (sx > maxDim || sz > maxDim) throw new Error("Size exceeds maxDim");
  if (sy > 128) throw new Error("Height > 128");

  // Validate all palette blocks are allowed
  for (const [key, blockId] of Object.entries(obj.palette)) {
    if (!ALLOWED_BLOCKS.includes(blockId)) {
      throw new Error(`Block ${blockId} is not in the allowed blocks list`);
    }
  }

  // Validate layer dimensions match size
  const layerCount = Object.keys(obj.layers).length;
  if (layerCount !== sy) {
    console.warn(`Layer count (${layerCount}) doesn't match height (${sy}), adjusting...`);
  }

  return obj;
}