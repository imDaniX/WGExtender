# WGExtender X
### [Releases](https://github.com/imDaniX/WGExtender/releases/latest) | [Dev Builds](https://github.com/imDaniX/WGExtender/actions?query=branch%3Amaster)

[WGExtender](https://github.com/Shevchik/WorldGuardExtender) is a plugin that extends WorldGuard's capabilities:
- Extended region protection for liquids, fire, and explosions
- Claim command enhancements, such as automatic vertical selection expansion
- Per-group region sizes
- Global command restrictions inside regions

WGExtender **X** is a fork that is maintained for modern Paper versions and includes additional features:
- Experimental Folia support
- Configurable minimum region claim size limits (volume, horizontal, vertical)
- Configurable messages in `messages.yml`

## Requirements

- Java 21
- Paper 1.21.4 or newer
- WorldGuard 7
- WorldEdit 7
- Vault 1.7

## Configuration

> [!NOTE]
> For per-group options to work, LuckPerms users might need to set `vault-group-use-displaynames` to `false`.

You can find [the default configuration](/resources/config.yml) in this repository in the `resources` folder.

### `claim`
- `vertexpand`: Auto-expand claim selection from world top to bottom.
- `blocklimits.enabled`: Enforce block limits per region.
- `blocklimits.limits`: Max region block count per group. Do note that the limit in WorldGuard's `config.yml` should be higher than specified here. Example:
```yaml
limits:
  default: 50000
  vip: 200000
```
- `blocklimits.minimal.volume`: Minimum volume (width\*height\*length).
- `blocklimits.minimal.horizontal`: Minimum width and length.
- `blocklimits.minimal.vertical`: Minimum height.

### `regionprotect`
- `flow.lava`, `flow.water`, `flow.other`: Block liquid flow across region borders.
- `fire.spread.toregion`: Block fire spreading into a region from outside.
- `fire.spread.inregion`: Block fire spread within a region.
- `fire.burn`: Prevent fire from destroying blocks.
- `explosion.block`: Prevent explosions from destroying blocks.
- `explosion.entity`: Prevent explosion damage to entities.

### `autoflags`
- `enabled`: Auto-apply defined flags to new regions.
- `show-messages`: Show vanilla WorldGuard messages when setting flags.
- `flags`: WorldGuard flag names and values. Example:
```yaml
flags:
  pvp: deny
```

### `restrictcommands`
- `enabled`: Block certain commands and their aliases inside any WorldGuard region for players who do not have access to that region.
- `commands`: Commands to block. Example:
```yaml
commands:
  - 'home'
  - 'tpa'
```
- `recheck-ticks`: How often to recheck command aliases, in ticks. Requires server restart for the changes to take effect.

### `extendedwewand`
Allows renaming of the WorldEdit wand given by the `//wand` command via `messages.yml`. This wand is deleted when dropped.

### `misc`
> [!WARNING]
> Both PvP options are deprecated. While they still work, there may be issues per release, as they require manual mimicking and copying of WorldGuard logic. They also require a server restart for the changes to take effect.
- `pvpmode`: Default PvP behavior for regions without an explicit flag: `allow`, `deny`, or `default` (WorldGuard behavior).
- `old-pvp-flags`: Use legacy 1.8 PvP flag handling.

### `messages`
- `serializer`: Format parser. Possible values:
  - `LEGACY` (`&c`)
  - `LEGACY_SECTION` (`§c`)
  - `MINIMESSAGE` (`<red>`)
- Messages are loaded from the `messages.yml` file.

## Building

Unlike the original WGExtender, WGExtender X uses Maven instead of Gradle to simplify the build configuration.
```bash
git clone https://github.com/imDaniX/WGExtender.git
cd WGExtender
mvn clean package
```

## License

This project retains the same **GNU Affero General Public License v3.0 [(AGPL-3.0)](https://www.gnu.org/licenses/agpl-3.0.html)** as the original WGExtender. It can also be found in the repository.
