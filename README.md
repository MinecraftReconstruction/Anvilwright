> [!IMPORTANT]
> **本仓库是非官方、由 AI 大幅生成（largely vibed）的试验性移植工程，与原作者无任何隶属或背书关系。**
> 代码来源与署名请见 [ATTRIBUTION.md](ATTRIBUTION.md)；当前进度与下一步见 [docs/STATUS.md](docs/STATUS.md) 与 [docs/PLAN.md](docs/PLAN.md)。
> 想要稳定可用的版本，请使用 [官方 Tinkers' Construct](https://github.com/SlimeKnights/TinkersConstruct) 或 [Hephaestus](https://github.com/Alpha-s-Stuff/TinkersConstruct)。

## About this fork / 关于本仓库

`MinecraftReconstruction/TinkersConstruct` is a fork of [Alpha-s-Stuff/TinkersConstruct](https://github.com/Alpha-s-Stuff/TinkersConstruct)
(*Hephaestus*), the Fabric port of [Tinkers' Construct](https://github.com/SlimeKnights/TinkersConstruct).

**Goal:** bring the Fabric port from Tinkers' Construct **3.6.4** up to **3.12.1** (current upstream) and keep it in sync.
The port's own Fabric adaptation layer — everything in the tables below — is AlphaMode's work, not ours; see [ATTRIBUTION.md](ATTRIBUTION.md).

| | |
|---|---|
| Base | Tinkers' Construct 3.6.4 + 328 port commits (port's last commit: 2026-01-08) |
| Target | Tinkers' Construct 3.12.1, Minecraft 1.20.1, Fabric |
| Upstream gap | **2469 commits** since the merge base; 1924 changed files, +119,995 / −49,844 lines (src/main/java) |
| Merge experiment | `git merge v3.12.1.231` → **4048 conflicts** = 3343 generated JSON (regenerate via datagen) + **696 Java** + 9 build files |
| Hard blocker | upstream 3.12.1 requires **Mantle `[1.11.113,)`**, but Fabric Mantle is only published up to `1.20.1-1.9.296` → see [Mantle-Fabric](https://github.com/MinecraftReconstruction/Mantle-Fabric) |
| Status / plan | [docs/STATUS.md](docs/STATUS.md) · [docs/PLAN.md](docs/PLAN.md) |
| For AI agents | [AGENTS.md](AGENTS.md) |

**Nothing here is playable yet.** The branch `1.20.1` currently corresponds to Tinkers' Construct 3.6.4 on Fabric;
the upstream sync has not been started. Contributions and corrections welcome — the whole point of this repo is to
see how far an AI-driven reconstruction can get, in the open.

# [Hephaestus](https://modrinth.com/mod/hephaestus)

Modify all the things, then do it again!   
Melt down any metals you find. 	 
Power the world with spinning wind!

## Issue reporting
Please include the following:

* Minecraft version
* Hephaestus version
* Fabric Loader and api version/build
* Versions of any mods potentially related to the issue 
* Any relevant screenshots are greatly appreciated.
* For crashes:
	* Steps to reproduce
	* ForgeModLoader-client-0.log (the FML log) from the root folder of the client

## Licenses
Code, Textures and binaries are licensed under the [MIT License](https://tldrlegal.com/license/mit-license).

You are allowed to use the mod in your modpack.
Any modpack which uses Hephaestus takes **full** responsibility for user support queries. For anyone else, we only support official builds from the main CI server, not custom built jars. We also do not take bug reports for outdated builds of Minecraft.

Any alternate licenses are noted where appropriate.
