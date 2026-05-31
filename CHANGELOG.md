# TPass 模组更新日志

## [未发布] - dev/version-isolation-architecture

### ✨ 新增功能
- **版本隔离架构重构** - 完全重构为版本隔离架构，支持多版本适配
  - 新增 `VersionAdapter` 接口，抽象所有版本特定的 API
  - 新增 `VersionAdapter_1_21` 实现，针对 Minecraft 1.21 的适配器
  - 新增 `VersionAdapterFactory` 工厂类，自动检测并选择对应版本的适配器
  - 核心业务逻辑移至 `core` 包（CoreTeamManager, CoreTeleportManager, CoreFlightCardManager）
  - 原有 Manager 类改为包装层，保持向后兼容

- **按钮交互优化** - 改进菜单按钮的用户体验
  - 新增 `createSuggestCommandButton()` 方法，支持需要用户输入的命令
  - `[创建队伍]` 按钮现在打开聊天框并预填充命令，等待用户输入队伍名
  - `[邀请玩家]` 按钮现在打开聊天框并预填充命令，等待用户输入玩家名
  - 其他直接执行的按钮保持使用 `RUN_COMMAND`

- **传送点列表功能** - 新增 `/homes` 命令
  - 显示玩家所有已设置的传送点列表
  - 每个传送点显示为可点击的按钮，点击即可传送
  - 格式：`[ 传送点名 ]`，使用青色高亮

- **中文名称支持** - 所有名称类命令参数现在支持中文
  - 队伍名称：`/tpass team create "我的队伍"`
  - 传送点名称：`/sethome "我的家"`, `/home "矿洞入口"`
  - 队伍传送点：`/setwarp "基地"`, `/warp "农场"`
  - 注意：包含空格或中文的名称需要用引号包裹

### 🔧 技术改进
- **错误处理优化**
  - `VersionAdapterFactory` 移除硬编码的默认版本回退
  - 版本检测失败时抛出明确的异常信息
  - 添加详细的版本检测日志输出

- **代码架构优化**
  - 所有 Minecraft API 调用通过 `VersionAdapter` 抽象
  - 消除硬编码的版本特定 API 调用
  - 95%+ 的业务逻辑完全版本无关

- **帮助信息更新**
  - `/tpass help` 中新增 `/homes` 命令说明

### 📝 命令变更
| 命令 | 变更类型 | 说明 |
|------|---------|------|
| `/homes` | 新增 | 查看所有传送点列表并支持点击传送 |
| `/tpass team create <名称>` | 增强 | 参数从 `word()` 改为 `string()`，支持中文 |
| `/tpass team join <名称>` | 增强 | 参数从 `word()` 改为 `string()`，支持中文 |
| `/tpass team accept <名称>` | 增强 | 参数从 `word()` 改为 `string()`，支持中文 |
| `/tpass team deny <名称>` | 增强 | 参数从 `word()` 改为 `string()`，支持中文 |
| `/sethome <名称>` | 增强 | 参数从 `word()` 改为 `string()`，支持中文 |
| `/home <名称>` | 增强 | 参数从 `word()` 改为 `string()`，支持中文 |
| `/rehome <名称>` | 增强 | 参数从 `word()` 改为 `string()`，支持中文 |
| `/setwarp <名称>` | 增强 | 参数从 `word()` 改为 `string()`，支持中文 |
| `/warp <名称>` | 增强 | 参数从 `word()` 改为 `string()`，支持中文 |

### 🐛 Bug 修复
- 修复版本号检测问题，使用反射调用 `getName()` 方法获取正确版本
- 修复 Scoreboard 重复创建队伍的警告，优化 `setupGlowTeam()` 逻辑
- 修复按钮点击后直接执行导致缺少参数报错的问题

### 📊 代码统计
- 新增文件：7 个（adapter 包 3 个，core 包 3 个，ARCHITECTURE.md 1 个）
- 修改文件：6 个
- 总变更：+1400+ 行，-420+ 行

---

## 使用说明

### 版本隔离架构
本项目采用版本隔离架构，要添加新版本支持只需：
1. 在 `src/main/java/org/fanajing/tpass/adapter/` 下创建新的适配器类
2. 在 `VersionAdapterFactory.createAdapter()` 中添加版本判断
3. 修改 `gradle.properties` 中的版本配置

详见：[ARCHITECTURE.md](ARCHITECTURE.md)

### 中文名称使用示例
```bash
# 创建中文队伍
/tpass team create "我的世界战队"

# 设置中文传送点
/sethome "温馨小家"
/sethome "挖矿基地"

# 查看传送点列表
/homes

# 传送到中文传送点（点击列表中的按钮或直接输入）
/home "温馨小家"

# 设置中文队伍传送点
/setwarp "团队基地"
/warp "团队基地"
```

### 按钮交互
- **需要输入的命令**：点击后打开聊天框，预填充命令，等待输入
  - `[创建队伍]` → 打开聊天框，输入 `/tpass team create ` + 队伍名
  - `[邀请玩家]` → 打开聊天框，输入 `/tpass team invite ` + 玩家名

- **直接执行的命令**：点击后立即执行
  - `[pvp开启/关闭]`、`[队伍仓库]`、颜色选择器等

---

**注意**: 此版本为开发分支版本，尚未合并到主分支。
