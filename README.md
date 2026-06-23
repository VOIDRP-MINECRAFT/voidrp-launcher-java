# VoidRP Launcher (Java)

Standalone Java desktop launcher for the VoidRP Minecraft server.

**Stack:** JavaFX 21 · OkHttp · Gradle Shadow Plugin

## Features

- Automatic mod pack download and update
- Play-ticket authentication flow (backend API integration)
- Mod pack manifest verification (SHA-256 checksums)
- Embedded JVM runtime bootstrap

## Build

```bash
./gradlew shadowJar
# Output: build/libs/voidrp-launcher-*.jar  (~56 MB fat JAR)
```

## Run

```bash
java -jar build/libs/voidrp-launcher-*.jar
```

## Requirements

- Java 21+
- Internet access to `https://void-rp.ru`
