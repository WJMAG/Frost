# Frost - A Minestom-Based Minecraft Server
Frost is a Minecraft server software built on [Minestom](https://github.com/Minestom/Minestom), originally created for MineStudio. While designed with MineStudio in mind, Frost can be used for any Minecraft server purpose as long as the license terms are followed.

## Features

- **Lightweight**: Built on Minestom for high performance
- **Modular Shard System**: Extend functionality with shards
- **Modern Tooling**: Built with Gradle for easy compilation
- **Flexible**: Suitable for various server types and use cases

## Getting Started

### Prerequisites

- Java 17 or higher
- Gradle (included in wrapper)

### Building

To build Frost, run:

```bash
./gradlew build
```

The output will be in `build/libs/`.

### Running

After building, you can run the server with:

```bash
java -jar frost-[version].jar
```

## Shard System

Frost uses a modular "shard" system to add functionality. Shards can:
- Add new content
- Modify server behavior
- Extend core functionality

Example shard: [https://github.com/MineStudio-Host/example-shard](https://github.com/MineStudio-Host/example-shard)

*Note: Comprehensive shard documentation is planned but not yet available.*

## Contributing
You're welcome to contribute, however there is no guarantee that your contributions will be accepted or merged.

## License

Frost is licensed under the GNU Affero General Public License v3.0. Please see the LICENSE file for details.

## Original Authors

- Cam M ([@expxx](https://github.com/expxx) on GitHub, @cammyzed on Discord)

## Disclaimer

This software is provided as-is. While originally created for MineStudio, it may be used for other purposes as long as the license terms are followed. Future development plans (including documentation) are subject to change.

---

*Frost is not affiliated with Mojang or Microsoft. Minecraft is a trademark of Mojang Studios.*