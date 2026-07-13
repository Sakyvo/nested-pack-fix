# NestedPackFix

NestedPackFix is a client-side Forge mod for malformed resource pack archives.
The Forge 1.7.10 integration is implemented; Forge 1.8.9 remains planned.

The shared archive layer recognizes ordinary root ZIP packs, ZIP packs wrapped
in exactly one top-level folder, and archives that cannot be used as resource
packs. RAR and 7z files are classified without opening their contents. No
archive is extracted, rewritten, or cached on disk.

## Supported Build Baselines

- Minecraft 1.7.10 / Forge 10.13.4.1614 / stable_12 mappings
- Minecraft 1.8.9 / Forge 11.15.1.2318 / stable_20 mappings
- Java 8

The two runtime integrations are separate deliverables. Forge 1.7.10 includes
the archive hook, warning rows, filters, tooltips, and file location workflow.
Forge 1.8.9 currently contains its build skeleton and shared archive tests.

## Build

A full JDK 8 is required. Set `JAVA8_HOME` or `JAVA_HOME`, then run:

```powershell
./build.ps1 -Clean
```

Legacy ForgeGradle is also supported through optional portable tools under the
ignored `.toolchains` directory. To copy verified build outputs into `dist`,
run `./build.ps1 -Command release` after the version integrations are present.

To build only the completed Forge 1.7.10 JAR:

```powershell
Set-Location forge-1.7.10
./gradlew.bat clean test build
```
