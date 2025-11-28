import express from "express";
import { generateStructure } from "../services/gemini.js";
import { getFallbackStructure } from "../services/fallback.js";
import { sanitize } from "../utils/sanitize.js";
import { MAX_DIM } from "../utils/constants.js";

const router = express.Router();

router.post("/", async (req, res) => {
  try {
    const prompt = req.body.prompt || "";
    const maxDim = Math.min(req.body.max_dim || MAX_DIM, MAX_DIM);

    if (!prompt) return res.status(400).json({ error: "prompt required" });

    let structure;

    if (process.env.GEMINI_API_KEY) {
      structure = await generateStructure(prompt, maxDim);
    } else {
      structure = getFallbackStructure();
    }

    const sanitizedStructure = sanitize(structure, maxDim);
    return res.json(sanitizedStructure);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

export default router;