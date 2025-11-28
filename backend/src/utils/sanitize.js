export function sanitize(obj, width, depth, height) {
  if (!obj.size || !obj.palette || !obj.layers) {
    throw new Error("Structure missing one of size/palette/layers");
  }

  const [sx, sy, sz] = obj.size;
  
  // Just log dimensions for debugging - no strict validation
  console.log(`Structure dimensions: ${sx}x${sy}x${sz} (requested ~${width}x${height}x${depth})`);
  console.log(`Palette has ${Object.keys(obj.palette).length} block types`);
  console.log(`Structure has ${Object.keys(obj.layers).length} layers`);

  // Ensure "_" maps to air if not specified
  if (!obj.palette["_"]) {
    obj.palette["_"] = "minecraft:air";
  }

  return obj;
}