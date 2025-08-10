# Share Inventory

A Minecraft Fabric mod that allows players to share inventories in real-time on servers.

## Features

- Real-time inventory syncing between players in the same group
- Server-side only - works with vanilla clients
- Simple command interface
- Supports all inventory operations (picking up, dropping, crafting, etc.)

## Installation

1. Download from [Releases](https://github.com/noahpro99/share-inv/releases)
2. Place the `.jar` file in your server's `mods` folder
3. Restart your server

## Usage

Join or create a shared inventory group:

```
/joinSharedInventory <inventoryName>
```

Examples:

- `/joinSharedInventory team1`
- `/joinSharedInventory builders`

The first player to join sets the shared inventory. Subsequent players receive a copy and all changes sync automatically.

## Requirements

- Minecraft 1.21 server with Fabric Loader 0.16.14+
- Fabric API
- Vanilla clients supported

## Building

```bash
git clone https://github.com/noahpro99/share-inv.git
cd share-inv
./gradlew build
```

Built jar will be in `build/libs/`
