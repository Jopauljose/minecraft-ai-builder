# AI-Driven Procedural Building Engine for Minecraft

## Overview
This project implements an AI-driven procedural building engine for Minecraft, allowing players to generate complex structures using natural language prompts. The system consists of two main components: a Node.js backend that interacts with the Google Gemini API for structure generation and a Paper plugin that integrates with the Minecraft server to build the structures in-game.

## Architecture
- **Backend**: A Node.js server that handles incoming requests, processes prompts, and communicates with the Google Gemini API. It also provides deterministic fallback structures for users without an API key.
- **Plugin**: A custom Paper plugin that exposes commands to players, captures their input, and sends requests to the backend. It also manages the placement of structures in the Minecraft world and provides an undo functionality.

## Project Structure
```
minecraft-ai-build
├── backend
│   ├── src
│   │   ├── server.js
│   │   ├── routes
│   │   │   └── generate.js
│   │   ├── services
│   │   │   ├── gemini.js
│   │   │   └── fallback.js
│   │   ├── utils
│   │   │   ├── sanitize.js
│   │   │   └── constants.js
│   │   └── types
│   │       └── structure.js
│   ├── .env.example
│   ├── package.json
│   └── README.md
├── plugin
│   ├── src
│   │   └── main
│   │       ├── java
│   │       │   └── com
│   │       │       └── aibuild
│   │       │           ├── AIBuildPlugin.java
│   │       │           ├── commands
│   │       │           │   ├── AIBuildCommand.java
│   │       │           │   └── AIUndoCommand.java
│   │       │           ├── services
│   │       │           │   ├── BackendClient.java
│   │       │           │   └── StructureBuilder.java
│   │       │           ├── models
│   │       │           │   ├── Structure.java
│   │       │           │   └── UndoBuffer.java
│   │       │           └── utils
│   │       │               ├── BlockValidator.java
│   │       │               └── JsonParser.java
│   │       └── resources
│   │           └── plugin.yml
│   ├── pom.xml
│   └── README.md
└── README.md
```

## Setup Instructions

### Backend
1. Navigate to the `backend` directory.
2. Install dependencies using npm:
   ```
   npm install
   ```
3. Create a `.env` file based on the `.env.example` template and add your Gemini API key.
4. Start the server:
   ```
   npm start
   ```

### Plugin
1. Navigate to the `plugin` directory.
2. Build the plugin using Maven:
   ```
   mvn clean package
   ```
3. Place the generated JAR file in the `plugins` folder of your Paper server.
4. Start your Minecraft server.

## Usage
- In-game, players can use the `/aibuild <prompt>` command to generate structures based on their input.
- Players can revert their last action using the `/aiundo` command.

## Contribution
Contributions are welcome! Please submit a pull request or open an issue for any enhancements or bug fixes.

## License
This project is licensed under the MIT License. See the LICENSE file for more details.