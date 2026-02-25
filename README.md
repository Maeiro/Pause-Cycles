# Pause Cycles (Forge 1.20.1)

Simple server-side utility mod that pauses world cycles when no players are online.

## Behavior

- If **no players are online**:
  - applies the configured **offline** value for each gamerule
- If **at least one player is online**:
  - applies the configured **online** value for each gamerule

The mod checks player presence continuously and only updates gamerules when state changes.

## Compatibility

- Minecraft: `1.20.1`
- Forge: `47.3.0+`
- Java: `17+`
- Server-side mod (clients do not need it)

## Install

1. Build (already validated in this project):
   - `.\gradlew.bat build`
2. Copy the jar from `build/libs/` to your server `mods/` folder.
3. Restart the server.

## Notes

- The server config file is created at:
  - `config/pausecycles-server.toml`
- You can configure any boolean gamerule with this format:
  - `gamerule|onlineValue|offlineValue`
- Default rules:
  - `doDaylightCycle|true|false`
  - `doSeasonCycle|true|false`
- If a gamerule does not exist, Pause Cycles logs a warning and keeps running normally.

## Config Example

```toml
[pausecycles]
# 20 ticks = 1 second
checkIntervalTicks = 20

rules = [
  "doDaylightCycle|true|false",
  "doSeasonCycle|true|false",
  "doWeatherCycle|true|false"
]
```
