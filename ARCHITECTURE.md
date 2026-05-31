# TPass 版本隔离架构说明

## 架构概述

本次重构采用了**版本隔离架构**,将核心业务逻辑与 Minecraft 版本特定的 API 分离,使得模组可以轻松适配多个 Minecraft 版本。

## 架构层次

```
┌─────────────────────────────────────┐
│   Tpass.java (主类/命令注册)          │
│   - 使用 VersionAdapter              │
│   - 注册所有命令                      │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│   包装层 (原 Manager 类)              │
│   - TeamManager                     │
│   - TeleportManager                 │
│   - FlightCardManager               │
│   - 保持向后兼容的 API                │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│   核心层 (core 包)                   │
│   - CoreTeamManager                 │
│   - CoreTeleportManager             │
│   - CoreFlightCardManager           │
│   - 包含所有版本无关的业务逻辑         │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│   版本适配器层 (adapter 包)           │
│   - VersionAdapter (接口)            │
│   - VersionAdapter_1_21 (实现)       │
│   - VersionAdapterFactory (工厂)     │
└─────────────────────────────────────┘
```

## 核心组件

### 1. VersionAdapter 接口
**位置**: `org.fanajing.tpass.adapter.VersionAdapter`

定义了所有与 Minecraft 版本相关的 API 抽象:
- 聊天组件创建和样式设置
- 玩家交互(消息发送、传送等)
- 队伍发光效果管理
- GUI 打开
- 飞行能力设置
- 实体相关操作

### 2. VersionAdapter_1_21 实现
**位置**: `org.fanajing.tpass.adapter.VersionAdapter_1_21`

针对 Minecraft 1.21 版本的具体实现,处理了:
- 1.21+ 的 MobEffectInstance 不需要 Holder 包装
- Scoreboard API 的方法名差异
- 其他 1.21 特有的 API 调用

### 3. VersionAdapterFactory 工厂
**位置**: `org.fanajing.tpass.adapter.VersionAdapterFactory`

自动检测 Minecraft 版本并返回对应的适配器实例:
```java
VersionAdapter adapter = VersionAdapterFactory.getAdapter();
```

### 4. 核心管理器 (Core Managers)
**位置**: `org.fanajing.tpass.core.*`

- **CoreTeamManager**: 队伍管理的核心逻辑
- **CoreTeleportManager**: 传送管理的核心逻辑  
- **CoreFlightCardManager**: 飞行卡管理的核心逻辑

这些类使用 VersionAdapter 来处理所有版本相关的操作,专注于业务逻辑。

### 5. 包装层 (Wrapper Layer)
**位置**: 原有的 Manager 类

- `TeamManager`
- `TeleportManager`
- `FlightCardManager`

这些类现在只是简单地委托给对应的 Core Manager,保持了向后兼容的 API,确保现有代码无需修改。

## 如何添加新版本支持

### 步骤 1: 创建新版本适配器

```java
package org.fanajing.tpass.adapter;

public class VersionAdapter_1_20 implements VersionAdapter {
    @Override
    public String getMinecraftVersion() {
        return "1.20";
    }
    
    // 实现所有接口方法,针对 1.20 版本的 API
    // ...
}
```

### 步骤 2: 更新工厂类

在 `VersionAdapterFactory.createAdapter()` 中添加版本判断:

```java
private static VersionAdapter createAdapter() {
    String mcVersion = getMinecraftVersion();
    
    if (mcVersion.startsWith("1.21")) {
        return new VersionAdapter_1_21();
    } else if (mcVersion.startsWith("1.20")) {
        return new VersionAdapter_1_20();  // 新增
    }
    
    throw new RuntimeException("不支持的 Minecraft 版本: " + mcVersion);
}
```

### 步骤 3: 完成!

所有核心业务逻辑会自动使用新版本的适配器,无需修改任何业务代码。

## 优势

1. **单一职责**: 每个类只负责一个层面的功能
2. **开闭原则**: 添加新版本无需修改现有代码
3. **依赖倒置**: 核心层依赖于抽象(接口),而非具体实现
4. **向后兼容**: 原有 API 保持不变,不影响现有代码
5. **易于测试**: 可以单独测试每个层级
6. **代码复用**: 核心业务逻辑在不同版本间完全共享

## 文件清单

### 新增文件
- `src/main/java/org/fanajing/tpass/adapter/VersionAdapter.java`
- `src/main/java/org/fanajing/tpass/adapter/VersionAdapter_1_21.java`
- `src/main/java/org/fanajing/tpass/adapter/VersionAdapterFactory.java`
- `src/main/java/org/fanajing/tpass/core/CoreTeamManager.java`
- `src/main/java/org/fanajing/tpass/core/CoreTeleportManager.java`
- `src/main/java/org/fanajing/tpass/core/CoreFlightCardManager.java`

### 修改文件
- `src/main/java/org/fanajing/tpass/Tpass.java` - 使用适配器
- `src/main/java/org/fanajing/tpass/team/TeamManager.java` - 委托给 CoreTeamManager
- `src/main/java/org/fanajing/tpass/teleport/TeleportManager.java` - 委托给 CoreTeleportManager
- `src/main/java/org/fanajing/tpass/FlightCardManager.java` - 委托给 CoreFlightCardManager

### 保持不变
- `src/main/java/org/fanajing/tpass/team/TeamData.java`
- `src/main/java/org/fanajing/tpass/team/TeamStorage.java`
- `src/main/java/org/fanajing/tpass/teleport/TeleportRequest.java`
- `src/main/java/org/fanajing/tpass/teleport/SavedLocation.java`

## 编译和运行

编译方式保持不变:
```bash
# Windows
gradlew build

# Linux/Mac
./gradlew build
```

## 下一步

1. 为其他版本(如 1.20, 1.19)创建适配器
2. 考虑将配置文件外部化,支持动态版本选择
3. 添加单元测试验证各版本适配器的正确性

---

**重构完成时间**: 2026-05-31  
**当前支持版本**: Minecraft 1.21  
**架构设计**: 版本隔离模式 (Version Isolation Pattern)
