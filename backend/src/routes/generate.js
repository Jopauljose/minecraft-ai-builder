import express from "express";
import { generateStructure } from "../services/gemini.js";
import { getFallbackStructure } from "../services/fallback.js";
import { sanitize } from "../utils/sanitize.js";
import { MAX_DIM, MAX_HEIGHT } from "../utils/constants.js";

const router = express.Router();

router.post("/", async (req, res) => {
  try {
    const prompt = req.body.prompt || "";
    const width = Math.min(Math.max(req.body.width || 16, 1), MAX_DIM);
    const depth = Math.min(Math.max(req.body.depth || 16, 1), MAX_DIM);
    const height = Math.min(Math.max(req.body.height || 16, 1), MAX_HEIGHT);

    if (!prompt) return res.status(400).json({ error: "prompt required" });

    let structure;

    if (process.env.GEMINI_API_KEY) {
      structure = await generateStructure(prompt, width, depth, height);
    } else {
      structure = getFallbackStructure();
    }

    const sanitizedStructure = sanitize(structure, width, depth, height);
    return res.json(sanitizedStructure);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

export default router;