# Yami Client

A high-performance Fabric client mod for Minecraft 1.21.1.

## Requirements

- **Java Development Kit (JDK) 21** or newer
- Fabric Loader for Minecraft 1.21.1

## Building

Clone the repository and build with Gradle:

### Standard Build

```bash
# Linux / macOS
./gradlew build

# Windows
.\gradlew.bat build
```

### Obfuscated Production Build

```bash
# Windows
.\gradlew.bat obfuscateJar build
```

The compiled mod JAR will be generated in:
- `build/libs/`

## Development

To run the client in a development environment:

```bash
.\gradlew.bat runClient
```

### IDE Setup (IntelliJ IDEA)

1. Open the project folder in IntelliJ IDEA as a Gradle project.
2. Ensure the Gradle JVM is configured to **JDK 21** or newer.
3. Allow Loom to finish importing and generating the Minecraft run configurations.
4. Launch via the generated **Minecraft Client** run configuration.
