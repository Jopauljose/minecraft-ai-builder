/**
 * Returns a fallback structure when Gemini API is not available.
 * This is a simple 8x4x8 house structure for testing purposes.
 */
export function getFallbackStructure() {
  const palette = {
    A: "minecraft:cobblestone",
    B: "minecraft:oak_planks",
    C: "minecraft:oak_fence",
    D: "minecraft:glass",
    E: "minecraft:air"
  };

  // Layer 0: Floor (8x8)
  const layer0 = [
    ["A", "A", "A", "A", "A", "A", "A", "A"],
    ["A", "B", "B", "B", "B", "B", "B", "A"],
    ["A", "B", "B", "B", "B", "B", "B", "A"],
    ["A", "B", "B", "B", "B", "B", "B", "A"],
    ["A", "B", "B", "B", "B", "B", "B", "A"],
    ["A", "B", "B", "B", "B", "B", "B", "A"],
    ["A", "B", "B", "B", "B", "B", "B", "A"],
    ["A", "A", "A", "A", "A", "A", "A", "A"]
  ];

  // Layer 1: Walls with door opening
  const layer1 = [
    ["A", "A", "A", "E", "E", "A", "A", "A"],
    ["A", "E", "E", "E", "E", "E", "E", "A"],
    ["A", "E", "E", "E", "E", "E", "E", "A"],
    ["A", "E", "E", "E", "E", "E", "E", "A"],
    ["A", "E", "E", "E", "E", "E", "E", "A"],
    ["A", "E", "E", "E", "E", "E", "E", "A"],
    ["A", "E", "E", "E", "E", "E", "E", "A"],
    ["A", "A", "A", "A", "A", "A", "A", "A"]
  ];

  // Layer 2: Walls with windows
  const layer2 = [
    ["A", "A", "D", "E", "E", "D", "A", "A"],
    ["A", "E", "E", "E", "E", "E", "E", "A"],
    ["D", "E", "E", "E", "E", "E", "E", "D"],
    ["A", "E", "E", "E", "E", "E", "E", "A"],
    ["A", "E", "E", "E", "E", "E", "E", "A"],
    ["D", "E", "E", "E", "E", "E", "E", "D"],
    ["A", "E", "E", "E", "E", "E", "E", "A"],
    ["A", "A", "D", "A", "A", "D", "A", "A"]
  ];

  // Layer 3: Roof
  const layer3 = [
    ["B", "B", "B", "B", "B", "B", "B", "B"],
    ["B", "B", "B", "B", "B", "B", "B", "B"],
    ["B", "B", "B", "B", "B", "B", "B", "B"],
    ["B", "B", "B", "B", "B", "B", "B", "B"],
    ["B", "B", "B", "B", "B", "B", "B", "B"],
    ["B", "B", "B", "B", "B", "B", "B", "B"],
    ["B", "B", "B", "B", "B", "B", "B", "B"],
    ["B", "B", "B", "B", "B", "B", "B", "B"]
  ];

  return {
    name: "fallback_house",
    size: [8, 4, 8],
    palette,
    layers: {
      0: layer0,
      1: layer1,
      2: layer2,
      3: layer3
    }
  };
}