package org.fanajing.tpass.adapter;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * 版本适配器接口
 * 用于隔离不同 Minecraft 版本的 API 差异
 */
public interface VersionAdapter {
    
    /**
     * 获取 Minecraft 版本号
     * @return 版本号字符串，如 "1.21"
     */
    String getMinecraftVersion();
    
    // ==================== 聊天组件相关 ====================
    
    /**
     * 创建文本组件
     * @param text 文本内容
     * @return Component 实例
     */
    Component createTextComponent(String text);
    
    /**
     * 为组件设置样式（颜色和点击事件）
     * @param component 组件
     * @param color 颜色
     * @param clickEvent 点击事件
     * @return 带样式的组件
     */
    Component styleComponent(Component component, ChatFormatting color, ClickEvent clickEvent);
    
    /**
     * 拼接多个组件
     * @param components 组件数组
     * @return 拼接后的组件
     */
    Component appendComponents(Component... components);
    
    // ==================== 玩家交互相关 ====================
    
    /**
     * 向玩家发送系统消息
     * @param player 玩家
     * @param message 消息组件
     */
    void sendSystemMessage(ServerPlayer player, Component message);
    
    /**
     * 在 Action Bar 显示消息
     * @param player 玩家
     * @param message 消息组件
     * @param overlay 是否覆盖显示
     */
    void displayClientMessage(ServerPlayer player, Component message, boolean overlay);
    
    // ==================== 传送相关 ====================
    
    /**
     * 传送玩家到指定位置
     * @param player 玩家
     * @param level 目标维度
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标
     * @param yRot Y轴旋转
     * @param xRot X轴旋转
     */
    void teleportPlayer(ServerPlayer player, ServerLevel level, double x, double y, double z, float yRot, float xRot);
    
    /**
     * 获取玩家当前所在维度
     * @param player 玩家
     * @return 维度实例
     */
    ServerLevel getPlayerLevel(ServerPlayer player);
    
    /**
     * 从服务器获取指定维度
     * @param server 服务器
     * @param dimension 维度键
     * @return 维度实例，可能为 null
     */
    ServerLevel getLevel(net.minecraft.server.MinecraftServer server, ResourceKey<Level> dimension);
    
    // ==================== 队伍发光效果相关 ====================
    
    /**
     * 为玩家添加发光效果
     * @param player 玩家
     * @param duration 持续时间（tick）
     */
    void addGlowingEffect(ServerPlayer player, int duration);
    
    /**
     * 设置玩家的发光颜色（通过 scoreboard team）
     * @param scoreboard 记分板
     * @param player 玩家
     * @param teamName 队伍名称
     * @param color 颜色
     * @return PlayerTeam 实例
     */
    PlayerTeam setupGlowTeam(Scoreboard scoreboard, ServerPlayer player, String teamName, ChatFormatting color);
    
    /**
     * 从记分板移除玩家的发光队伍
     * @param scoreboard 记分板
     * @param playerName 玩家名称
     * @param teamName 队伍名称
     */
    void removePlayerFromGlowTeam(Scoreboard scoreboard, String playerName, String teamName);
    
    // ==================== GUI 相关 ====================
    
    /**
     * 打开箱子 GUI
     * @param player 玩家
     * @param title 标题
     * @param inventory 容器
     * @param rows 行数（3 = 27格）
     */
    void openChestGui(ServerPlayer player, Component title, net.minecraft.world.Container inventory, int rows);
    
    // ==================== 飞行能力相关 ====================
    
    /**
     * 设置玩家飞行能力
     * @param player 玩家
     * @param canFly 是否可以飞行
     * @param isFlying 是否正在飞行
     */
    void setFlightAbility(ServerPlayer player, boolean canFly, boolean isFlying);
    
    // ==================== 实体相关 ====================
    
    /**
     * 检查实体是否存活
     * @param player 玩家
     * @return 是否存活
     */
    boolean isAlive(ServerPlayer player);
    
    /**
     * 获取玩家 UUID
     * @param player 玩家
     * @return UUID
     */
    UUID getPlayerUUID(ServerPlayer player);
    
    /**
     * 获取玩家名称组件
     * @param player 玩家
     * @return 名称组件
     */
    Component getPlayerName(ServerPlayer player);
    
    /**
     * 比较两个玩家是否为同一人
     * @param player1 玩家1
     * @param player2 玩家2
     * @return 是否相同
     */
    boolean isSamePlayer(ServerPlayer player1, ServerPlayer player2);
}
