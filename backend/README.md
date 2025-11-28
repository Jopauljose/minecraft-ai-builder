# Minecraft AI Build Backend

## Overview
This backend service powers the AI-driven procedural building engine for Minecraft. It utilizes the Google Gemini API to generate complex structures based on natural language prompts provided by players in-game.

## Features
- **AI Structure Generation**: Leverages Google Gemini for generating Minecraft structures from user prompts.
- **Deterministic Fallback**: Provides predefined structures for users without an API key.
- **JSON Responses**: Ensures all outputs are valid JSON, adhering to strict schema requirements.
- **Dimensional Constraints**: Enforces maximum dimensions for generated structures to maintain game integrity.
- **Palette Normalization**: Utilizes a defined palette of allowed Minecraft blocks for structure generation.

## Setup Instructions

### Prerequisites
- Node.js (version 14 or higher)
- npm (Node Package Manager)
- A Google Gemini API key (optional, for AI generation)

### Installation
1. Clone the repository:
   ```
   git clone <repository-url>
   cd minecraft-ai-build/backend
   ```

2. Install dependencies:
   ```
   npm install
   ```

3. Configure environment variables:
   - Copy `.env.example` to `.env` and set your `GEMINI_API_KEY` if you have one.

### Running the Server
To start the backend server, run:
```
npm start
```
The server will listen on the specified port (default is 3000).

### API Endpoints
- **POST /generate**: Accepts a JSON payload with a prompt and optional max dimensions. Returns a JSON object representing the generated structure.

## Contributing
Contributions are welcome! Please submit a pull request or open an issue for any enhancements or bug fixes.

## License
This project is licensed under the MIT License. See the LICENSE file for details.