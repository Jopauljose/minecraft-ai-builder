import "dotenv/config";
import express from "express";
import bodyParser from "body-parser";
import generateRoute from "./routes/generate.js";

const app = express();
app.use(bodyParser.json());

const PORT = process.env.PORT || 3000;

app.use("/generate", generateRoute);

app.listen(PORT, () =>
  console.log(`AI-driven building engine backend running on port ${PORT}`)
);