# NestedPackFix

[English](#english) | [中文](#中文)

## English

NestedPackFix is a client-side Forge mod that makes common malformed resource
pack archives usable and clearly identifies archives that Minecraft cannot
load. It supports Minecraft 1.7.10 and 1.8.9 with no configuration required.

[Download release 1.0.0](https://github.com/Sakyvo/nested-pack-fix/releases/tag/1.0.0)

### Behavior

| Pack layout | Result |
| --- | --- |
| Folder or ZIP with root `assets/` and `pack.mcmeta` | Loaded normally |
| ZIP containing exactly one top-level pack folder | Loaded with a yellow `[!]` warning |
| ZIP-in-ZIP, malformed ZIP, ambiguous folders, missing metadata | Shown as a red, non-selectable illegal row |
| RAR or 7z archive | Shown as a red, non-selectable illegal row |

Nested ZIP packs keep the outer archive filename when saved. Hovering a warning
for 700 ms explains the problem; clicking the warning after the tooltip appears
locates the outer archive in the resource-packs folder. The `Nested`, `Illegal`,
and `All` buttons filter only the Available list.

NestedPackFix never extracts, rewrites, or caches archive contents. It does not
decode RAR or 7z files and does not read an archive nested inside another ZIP.

### Compatibility

- Minecraft 1.7.10 with Forge 10.13.4.1614
- Minecraft 1.8.9 with Forge 11.15.1.2318
- Java 8
- Patcher and PolyPatcher on 1.8.9, with resource-pack fast scanning enabled or disabled

The mod is client-side only. The 1.7.10 and 1.8.9 JARs are separate; install
only the file matching your Minecraft version.

### Installation

1. Install the supported Forge version for your Minecraft version.
2. Download the matching JAR from the GitHub release.
3. Place the JAR in the instance's `mods` folder and restart Minecraft.

### Building

A full JDK 8 is required. Set `JAVA8_HOME` or `JAVA_HOME`, then run:

```powershell
./build.ps1 -Command release -Clean
```

Verified JARs and SHA-256 files are written to `dist/`. To build one version:

```powershell
Set-Location forge-1.7.10
./gradlew.bat clean test build

Set-Location ../forge-1.8.9
./gradlew.bat clean test build
```

## 中文

NestedPackFix 是一个客户端 Forge Mod，用于读取常见的错误嵌套材质包，
并明确标出 Minecraft 无法加载的非法归档。支持 Minecraft 1.7.10 和
1.8.9，无需配置。

[下载 1.0.0 版本](https://github.com/Sakyvo/nested-pack-fix/releases/tag/1.0.0)

### 处理方式

| 材质包结构 | 处理结果 |
| --- | --- |
| 根目录含 `assets/` 和 `pack.mcmeta` 的文件夹或 ZIP | 正常读取 |
| ZIP 内仅嵌套一层材质包文件夹 | 正常读取，并显示黄色 `[!]` 警告 |
| ZIP 套 ZIP、损坏 ZIP、多候选文件夹、缺少元数据 | 显示红色且不可启用的非法行 |
| RAR 或 7z 归档 | 显示红色且不可启用的非法行 |

嵌套 ZIP 在保存时仍使用外层压缩包文件名。警告图标悬停 700 ms 后会
显示问题说明；tooltip 出现后点击警告图标，可在材质包目录中定位外层
归档。`Nested`、`Illegal` 和 `All` 按钮只过滤左侧可用材质包列表。

NestedPackFix 不会解压、改写或缓存归档内容，不会解码 RAR/7z，也不会
读取 ZIP 内部再次嵌套的压缩文件。

### 兼容性

- Minecraft 1.7.10 + Forge 10.13.4.1614
- Minecraft 1.8.9 + Forge 11.15.1.2318
- Java 8
- 1.8.9 的 Patcher 与 PolyPatcher，材质包快速扫描开启或关闭均支持

本 Mod 仅在客户端运行。1.7.10 与 1.8.9 使用不同 JAR，只安装与当前
Minecraft 版本对应的文件。

### 安装

1. 为当前 Minecraft 版本安装上述 Forge 版本。
2. 从 GitHub Release 下载对应版本的 JAR。
3. 将 JAR 放入实例的 `mods` 文件夹并重启 Minecraft。

### 构建

需要完整 JDK 8。设置 `JAVA8_HOME` 或 `JAVA_HOME` 后运行：

```powershell
./build.ps1 -Command release -Clean
```

验证后的 JAR 和 SHA-256 文件会输出到 `dist/`。单独构建某一版本：

```powershell
Set-Location forge-1.7.10
./gradlew.bat clean test build

Set-Location ../forge-1.8.9
./gradlew.bat clean test build
```

## License / 许可证

[MIT License](LICENSE)
