<div align="center">
  <img src=".github/assets/logo.png" alt="WGExtender X Logo" height="200">

# WGExtender X
[![Modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/modrinth_vector.svg)](https://modrinth.com/plugin/wgextender-x)
[![Snapshot](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/github_vector.svg)](https://github.com/imDaniX/WGExtender/actions?query=branch%3Amaster)
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
- Paper 1.21.3 or newer
- WorldGuard 7
- WorldEdit 7

## Building

Unlike the original WGExtender, which uses Gradle, WGExtender X uses Maven to simplify the build configuration.
```bash
git clone https://github.com/imDaniX/WGExtender.git
cd WGExtender
mvn clean package
```

## License and Credits

This project retains the same **GNU Affero General Public License v3.0 [(AGPL-3.0)](https://www.gnu.org/licenses/agpl-3.0.html)** as the original WGExtender. It can also be found in the repository.

Using **[Devin's badges](https://intergrav.github.io/devins-badges-docs/)** for README and other pages.
