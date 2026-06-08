<div align="center">
  <img src=".github/assets/logo.png" alt="WGExtender X Logo" height="200">
  <h1>WGExtender X</h1>

[![Modrinth](https://img.shields.io/modrinth/v/JFMgRt9t?logo=modrinth&color=00AF5C&label=Modrinth)](https://modrinth.com/plugin/wgextender-x)
[![Snapshot](https://img.shields.io/github/actions/workflow/status/imDaniX/WGExtender/maven.yml?logo=github&label=Snapshot)](https://github.com/imDaniX/WGExtender/actions?query=branch%3Amaster)
</div>

[WGExtender](https://github.com/Shevchik/WorldGuardExtender) is a plugin that extends WorldGuard's capabilities:
- Extended region protection for liquids, fire, and explosions
- Claim command enhancements, such as automatic vertical selection expansion
- Per-group region sizes
- Global command restrictions inside regions

WGExtender **X** is a fork that is maintained for modern Paper versions and includes additional features:
- Experimental Folia support
- Configurable minimum region claim size limits (volume, horizontal, vertical)
- Configurable messages in the `messages/` folder
- PlaceholderAPI and LuckPerms integration
- New WG flags

Head to [the wiki pages](https://github.com/imDaniX/WGExtender/wiki/) for more info.

## Requirements

- Java 21
- Paper 1.21.11 or newer
- WorldGuard 7
- WorldEdit 7

## Building

Unlike the original WGExtender, which uses Gradle, WGExtender X uses Maven to simplify the build configuration.
```bash
git clone https://github.com/imDaniX/WGExtender.git
cd WGExtender
mvn clean package
```

## License

This project retains the same **GNU Affero General Public License v3.0 [(AGPL-3.0)](https://www.gnu.org/licenses/agpl-3.0.html)** as the original WGExtender. It can also be found in the repository.
