package org.fanajing.tpass.adapter;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.UUID;

/**
 * Minecraft 1.21 版本适配器实现
 */
public class VersionAdapter_1_21 implements VersionAdapter {
    
    @Override
    public String getMinecraftVersion() {
        return "1.21";
    }
    
    // ==================== 聊天组件相关 ====================
    
    @Override
    public Component createTextComponent(String text) {
        return Component.literal(text);
    }
    
    @Override
    public Component styleComponent(Component component, ChatFormatting color, ClickEvent clickEvent) {
        if (component instanceof net.minecraft.network.chat.MutableComponent mutable) {
            Style style = Style.EMPTY.withColor(color);
            if (clickEvent != null) {
                style = style.withClickEvent(clickEvent);
            }
            return mutable.withStyle(style);
        }
        // 如果不是 MutableComponent，创建一个新的
        return Component.literal(component.getString()).withStyle(Style.EMPTY.withColor(color).withClickEvent(clickEvent));
    }
    
    @Override
    public Component appendComponents(Component... components) {
        if (components.length == 0) {
            return Component.empty();
        }
        Component result = components[0];
        for (int i = 1; i < components.length; i++) {
            result = result.copy().append(components[i]);
        }
        return result;
    }
    
    // ==================== 玩家交互相关 ====================
    
    @Override
    public void sendSystemMessage(ServerPlayer player, Component message) {
        player.sendSystemMessage(message);
    }
    
    @Override
    public void displayClientMessage(ServerPlayer player, Component message, boolean overlay) {
        player.displayClientMessage(message, overlay);
    }
    
    // ==================== 传送相关 ====================
    
    @Override
    public void teleportPlayer(ServerPlayer player, ServerLevel level, double x, double y, double z, float yRot, float xRot) {
        player.teleportTo(level, x, y, z, yRot, xRot);
    }
    
    @Override
    public ServerLevel getPlayerLevel(ServerPlayer player) {
        return player.serverLevel();
    }
    
    @Override
    public ServerLevel getLevel(MinecraftServer server, ResourceKey<Level> dimension) {
        return server.getLevel(dimension);
    }
    
    // ==================== 队伍发光效果相关 ====================
    
    @Override
    public void addGlowingEffect(ServerPlayer player, int duration) {
        // 1.21+ 不需要 Holder 包装，直接使用 MobEffects
        player.addEffect(new MobEffectInstance(
            MobEffects.GLOWING, duration, 0, false, false, false));
    }
    
    @Override
    public PlayerTeam setupGlowTeam(Scoreboard scoreboard, ServerPlayer player, String teamName, ChatFormatting color) {
        String playerName = player.getName().getString();
        
        // 检查是否已在正确的队伍中
        PlayerTeam glowTeam = scoreboard.getPlayerTeam(playerName);
        if (glowTeam != null && glowTeam.getName().equals(teamName)) {
            // 已经在正确的队伍中，只需更新颜色
            glowTeam.setColor(color);
            return glowTeam;
        }
        
        // 如果在其他队伍中，先移除
        if (glowTeam != null) {
            scoreboard.removePlayerFromTeam(playerName, glowTeam);
        }
        
        // 创建或获取队伍
        glowTeam = scoreboard.getPlayerTeam(teamName);
        if (glowTeam == null) {
            glowTeam = scoreboard.addPlayerTeam(teamName);
        }
        
        // 设置颜色并添加玩家
        glowTeam.setColor(color);
        scoreboard.addPlayerToTeam(playerName, glowTeam);
        
        return glowTeam;
    }
    
    @Override
    public void removePlayerFromGlowTeam(Scoreboard scoreboard, String playerName, String teamName) {
        PlayerTeam glowTeam = scoreboard.getPlayerTeam(playerName);
        if (glowTeam != null && glowTeam.getName().equals(teamName)) {
            scoreboard.removePlayerFromTeam(playerName, glowTeam);
        }
    }
    
    // ==================== GUI 相关 ====================
    
    @Override
    public void openChestGui(ServerPlayer player, Component title, net.minecraft.world.Container inventory, int rows) {
        player.openMenu(new SimpleMenuProvider((id, inv, p) ->
            ChestMenu.threeRows(id, inv, inventory),
            title));
    }
    
    // ==================== 飞行能力相关 ====================
    
    @Override
    public void setFlightAbility(ServerPlayer player, boolean canFly, boolean isFlying) {
        player.getAbilities().mayfly = canFly;
        player.getAbilities().flying = isFlying;
        player.onUpdateAbilities();
    }
    
    // ==================== 实体相关 ====================
    
    @Override
    public boolean isAlive(ServerPlayer player) {
        return player.isAlive();
    }
    
    @Override
    public UUID getPlayerUUID(ServerPlayer player) {
        return player.getUUID();
    }
    
    @Override
    public Component getPlayerName(ServerPlayer player) {
        return player.getName();
    }
    
    @Override
    public boolean isSamePlayer(ServerPlayer player1, ServerPlayer player2) {
        return player1.equals(player2);
    }
}
