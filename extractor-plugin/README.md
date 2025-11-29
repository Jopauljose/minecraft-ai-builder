# Structure Extractor Plugin

A Minecraft Paper plugin for extracting structures from worlds to JSON format, designed for generating AI training datasets.

## Features

- **Auto-Detection**: Flood-fill algorithm to automatically detect structure boundaries
- **Multiple Capture Modes**: Auto, block targeting, manual selection, WorldEdit integration
- **Async Processing**: Non-blocking world scanning and file export
- **Clean JSON Output**: Ready for AI training with relative coordinates and metadata

## Requirements

- Minecraft Server: Paper 1.21.1+
- Java: 21+
- Optional: WorldEdit 7.3.0+ (for selection mode)

## Installation

1. Download `structure-extractor-plugin-1.0-SNAPSHOT.jar` from `target/`
2. Place in your server's `plugins/` folder
3. Restart the server
4. Configure via `plugins/StructureExtractor/config.yml`

## Commands

### Scanning Commands (`/scan`)
| Command | Permission | Description |
|---------|------------|-------------|
| `/scan start [world]` | `extractor.scan` | Start auto-scanning a world for structures |
| `/scan stop` | `extractor.scan` | Stop the current scan |
| `/scan status` | `extractor.scan` | Check scan progress |

### Capture Commands (`/capture`)
| Command | Permission | Description |
|---------|------------|-------------|
| `/capture auto [name]` | `extractor.capture` | Auto-detect nearest structure from player position |
| `/capture block [name]` | `extractor.capture` | Detect structure from the block you're looking at |
| `/capture selection [name]` | `extractor.capture` | Export a manually selected region |
| `/capture pos1` | `extractor.capture` | Set selection corner 1 (at player position) |
| `/capture pos2` | `extractor.capture` | Set selection corner 2 (at player position) |

### Admin Commands (`/extractor`)
| Command | Permission | Description |
|---------|------------|-------------|
| `/extractor help` | `extractor.admin` | Show help for all commands |
| `/extractor stats` | `extractor.admin` | Show export statistics |
| `/extractor config` | `extractor.admin` | Show current configuration |
| `/extractor reload` | `extractor.admin` | Reload configuration |

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `extractor.scan` | op | Access to scanning commands |
| `extractor.capture` | op | Access to capture commands |
| `extractor.admin` | op | Access to admin commands |

## Configuration

```yaml
# Export settings
export:
  directory: "exports"          # Output directory (relative to plugin folder)
  pretty-json: true             # Format JSON with indentation
  include-air: false            # Include air blocks in export

# World scanner settings
scanner:
  chunks-per-tick: 2            # Chunks to process per tick
  min-structure-size: 10        # Minimum blocks to consider a structure
  max-structure-size: 50000     # Maximum blocks for a structure
  max-dimensions:
    x: 100
    y: 100
    z: 100

# Detection settings
detection:
  search-radius: 15             # Search radius for auto-detect
  max-flood-iterations: 50000   # Max iterations for flood-fill
```

## JSON Output Format

```json
{
  "name": "village_house",
  "size": {
    "x": 9,
    "y": 6,
    "z": 9
  },
  "blocks": [
    {
      "x": 0,
      "y": 0,
      "z": 0,
      "block": "minecraft:cobblestone",
      "data": null
    },
    {
      "x": 1,
      "y": 0,
      "z": 0,
      "block": "minecraft:oak_stairs",
      "data": "facing=east,half=bottom,shape=straight"
    }
  ],
  "metadata": {
    "capturedAt": 1701234567890,
    "capturedBy": "PlayerName",
    "captureMode": "auto",
    "worldName": "world",
    "totalBlocks": 150,
    "origin": {
      "x": 100,
      "y": 64,
      "z": -200
    }
  }
}
```

## How It Works

### Structure Detection
The plugin uses a flood-fill algorithm starting from a structure block (like crafting tables, doors, beds, etc.):

1. Starts from a structure indicator block
2. Expands to connected non-natural blocks
3. Includes enclosed air spaces (rooms)
4. Respects dimension limits
5. Returns detected bounding box

### Natural vs Structure Blocks
- **Natural blocks** (ignored during detection): stone, dirt, grass, ores, water, lava, etc.
- **Structure indicator blocks** (trigger detection): crafting tables, furnaces, chests, beds, doors, etc.

### WorldEdit Integration
If WorldEdit is installed, `/capture selection` will first try to use your WorldEdit selection (made with `//wand`). If no WorldEdit selection exists, it falls back to manual selection with `/capture pos1` and `/capture pos2`.

## Use Cases

1. **AI Training Data**: Generate datasets for training structure-generation AI models
2. **Structure Documentation**: Export structures for analysis or sharing
3. **Backup**: Create JSON backups of important builds
4. **World Scanning**: Find and catalog structures across your world

## Building from Source

```bash
cd extractor-plugin
mvn clean package
```

The built JAR will be in `target/structure-extractor-plugin-1.0-SNAPSHOT.jar`

## License

MIT License
