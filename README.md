<p align="center">
  <img src="docs/fags.png" alt="Fags Logo" width="220">
</p>

# Yami Client

A high-performance Fabric client mod for Minecraft 1.21.1 (HvH / 2B2T / Grim).

[![Discord](https://img.shields.io/badge/Discord-Join%20Us-5865F2?logo=discord&logoColor=white)](https://discord.gg/E3ay6BAQTZ)

## Community

Join our official Discord: [https://discord.gg/E3ay6BAQTZ](https://discord.gg/E3ay6BAQTZ)

## Requirements

- **Java Development Kit (JDK) 21** or newer
- Fabric Loader for Minecraft 1.21.1

## Building

Clone the repository and build with Gradle:

```bash
./gradlew build
```

The compiled mod JAR will be generated in:
- `build/libs/`

## Development

To run the client in a development environment:

```bash
./gradlew runClient
```

### IDE Setup (IntelliJ IDEA)

1. Open the project folder in IntelliJ IDEA as a Gradle project.
2. Ensure the Gradle JVM is configured to **JDK 21** or newer.
3. Allow Loom to finish importing and generating the Minecraft run configurations.
4. Launch via the generated **Minecraft Client** run configuration.
