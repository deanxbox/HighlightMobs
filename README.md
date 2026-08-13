# HighlightMobs

A client-side Fabric mod for Minecraft 26.2 that outlines selected entity types.

## Features

- Toggle all configured highlights with a keybind.
- Open settings with a keybind or through Mod Menu.
- Search every registered entity type, select multiple types, clear the search, or clear every selection.
- Persist settings in `config/highlightmobs.json`.

## Build

Requires JDK 25.

```shell
./gradlew build
```

The remapped mod jar is written to `build/libs`.

## Requirements

- Minecraft 26.2
- Fabric Loader 0.19.3 or newer
- Fabric API
- Mod Menu 20.x (optional, for access through the Mods screen)

## Default controls

- `H`: enable or disable highlighting
- `O`: open HighlightMobs settings

Both controls can be changed in Minecraft's key bindings screen.
