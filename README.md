# NestedPackFix

NestedPackFix is a client-side Forge mod for malformed resource pack archives.
It is planned for Minecraft 1.7.10 and 1.8.9.

The shared archive layer recognizes ordinary root ZIP packs, ZIP packs wrapped
in exactly one top-level folder, and archives that cannot be used as resource
packs. RAR and 7z files are classified without opening their contents. No
archive is extracted, rewritten, or cached on disk.

## Supported Build Baselines

- Minecraft 1.7.10 / Forge 10.13.4.1614 / stable_12 mappings
- Minecraft 1.8.9 / Forge 11.15.1.2318 / stable_20 mappings
- Java 8

The two runtime integrations are implemented as separate deliverables. The
current foundation contains their build skeleton and shared archive tests.

## Build

A full JDK 8 is required. Set `JAVA8_HOME` or `JAVA_HOME`, then run:

```powershell
./build.ps1 -Clean
```

Legacy ForgeGradle is also supported through optional portable tools under the
ignored `.toolchains` directory. To copy verified build outputs into `dist`,
run `./build.ps1 -Command release` after the version integrations are present.
