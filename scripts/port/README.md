# 3.12.1 移植用的工具脚本

这三个脚本是用来做**机械化清理**的：把合并过程中丢掉的 import 补回来、把指向已搬迁类的 import 改过去、
把指向不存在类的未使用 import 删掉。它们每一轮都靠编译日志驱动，**改完必须重新编译验证**。

```bash
# 1. 编译 + 拿到完整错误日志（.port/errors.txt）
scripts/port/errors.sh

# 2. （可选但强烈建议）建立"类名声表"，用来判断某个 import 到底存不存在
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
./gradlew -I scripts/port/printcp.gradle printCompileCp
python3 scripts/port/jarindex.py

# 3. 先看它想改什么（默认 dry run），确认没问题再加 --apply
python3 scripts/port/portfix.py
python3 scripts/port/portfix.py --apply
```

注意：

- `portfix.py` 会去 `../mr-mantle-fabric` 找 Mantle 的源码（Mantle 的类来自 jar 依赖，不在这棵树里）。
- 它**故意不做**的事：不碰 `net.minecraftforge.*` / `mezz.jei.api.forge.*`（那些名字在 Fabric 上根本不存在，
  硬补只会在别处制造新错误）；不猜同名类（JEI/REI 各有一个同名助手时会跳过）；
  不允许跨包乱改（候选必须段数相同、前 3 段相同）。
- 这几把刷子刷干净之后（日志不再产生提案），剩下的就必须逐文件改 API 了，见
  [docs/HANDOFF.md](../../docs/HANDOFF.md) 的"剩余工作"。
