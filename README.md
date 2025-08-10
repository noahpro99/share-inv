# Share Inventory

A Minecraft Fabric mod that allows players to share inventories with each other in real-time.

## Features

- **Real-time inventory syncing** - When one player's inventory changes, all players in the same group see the changes instantly
- **Server-side only** - No client mod required, works with vanilla clients
- **Easy to use** - Simple command to join shared inventory groups
- **Comprehensive syncing** - Handles all inventory operations:
  - Manual slot changes
  - Picking up items
  - Dropping items (Q key)
  - Block placement
  - Crafting
  - Container interactions

## Installation

1. Download the latest release from the [Releases page](https://github.com/noahpro99/share-inv/releases)
2. Place the `.jar` file in your server's `mods` folder
3. Restart your server

## Usage

### Joining a Shared Inventory

Use the command to join or create a shared inventory group:

```
/joinSharedInventory <inventoryName>
```

**Examples:**

- `/joinSharedInventory team1` - Join or create a shared inventory called "team1"
- `/joinSharedInventory builders` - Join or create a shared inventory called "builders"

### How it Works

1. **First player** joins a group: Their current inventory becomes the shared inventory
2. **Subsequent players** join the same group: Their inventory is replaced with the shared inventory
3. **Any changes** made by any player in the group are instantly synced to all other players

## Requirements

- **Server:** Minecraft 1.21 with Fabric Loader 0.16.14+
- **Client:** Vanilla Minecraft 1.21 (no mods required)
- **Dependencies:** Fabric API

## Building from Source

1. Clone the repository:

   ```bash
   git clone https://github.com/noahpro99/share-inv.git
   cd share-inv
   ```

2. Build the mod:

   ```bash
   ./gradlew build
   ```

3. The built jar will be in `build/libs/`

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Issues

If you encounter any bugs or have feature requests, please [open an issue](https://github.com/noahpro99/share-inv/issues).

## Changelog

### v0.1.0

- Initial release
- Basic shared inventory functionality
- Support for all inventory operations
- Server-side only implementationExample Mod

## Setup

For setup instructions please see the [fabric documentation page](https://docs.fabricmc.net/develop/getting-started/setting-up-a-development-environment) that relates to the IDE that you are using.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
