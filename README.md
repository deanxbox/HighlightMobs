# HighlightMobs

A client-side Fabric mod for Minecraft 1.21.2-1.21.4 that outlines selected entity types.

## Features

- Toggle all configured highlights with a keybind.
- Open settings with a keybind or through Mod Menu.
- Search every registered entity type, select multiple types, clear the search, or clear every selection.
- Persist settings in `config/highlightmobs.json`.

## Build

Requires JDK 21.

```shell
./gradlew build
```

The remapped mod jar is written to `build/libs`.

## Requirements

- Minecraft 1.21.2-1.21.4
- Fabric Loader 0.19.3 or newer
- Fabric API
- Mod Menu matching your Minecraft version (optional, for access through the Mods screen)

## Default controls

- `H`: enable or disable highlighting
- `O`: open HighlightMobs settings

Both controls can be changed in Minecraft's key bindings screen.
