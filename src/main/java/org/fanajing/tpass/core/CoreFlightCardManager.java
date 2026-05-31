package org.fanajing.tpass.core;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.fanajing.tpass.adapter.VersionAdapter;
import org.fanajing.tpass.adapter.VersionAdapterFactory;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 核心飞行卡管理器
 * 包含所有与版本无关的飞行卡管理逻辑
 */
public class CoreFlightCardManager {
    // 玩家冷却时间（上次使用时间戳）
    private static final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    // 玩家飞行剩余时间（秒）
    private static final Map<UUID, Integer> flightTimeRemaining = new ConcurrentHashMap<>();
    // 玩家是否暂停
    private static final Set<UUID> pausedPlayers = ConcurrentHashMap.newKeySet();
    
    // 飞行持续时间：5分钟 = 300秒
    private static final int FLIGHT_DURATION = 300;
    // 冷却时间：20分钟 = 1200秒
    private static final int COOLDOWN_DURATION = 1200;
    
    private static final VersionAdapter adapter = VersionAdapterFactory.getAdapter();
    
    /**
     * 使用飞行卡
     * @param player 玩家
     * @return 是否成功使用
     */
    public static boolean useFlightCard(ServerPlayer player) {
        UUID playerId = adapter.getPlayerUUID(player);
        
        // 如果已在飞行中或已暂停，切换暂停/继续状态
        if (flightTimeRemaining.containsKey(playerId)) {
            if (pausedPlayers.contains(playerId)) {
                // 继续飞行
                pausedPlayers.remove(playerId);
                adapter.setFlightAbility(player, true, true);
                int remaining = flightTimeRemaining.get(playerId);
                int minutes = remaining / 60;
                int seconds = remaining % 60;
                adapter.sendSystemMessage(player, adapter.styleComponent(
                        adapter.createTextComponent("已继续飞行！剩余时间: " + minutes + "分" + seconds + "秒"),
                        ChatFormatting.GREEN,
                        null));
            } else {
                // 暂停飞行
                pausedPlayers.add(playerId);
                adapter.setFlightAbility(player, false, false);
                int remaining = flightTimeRemaining.get(playerId);
                int minutes = remaining / 60;
                int seconds = remaining % 60;
                adapter.sendSystemMessage(player, adapter.styleComponent(
                        adapter.createTextComponent("已暂停飞行！剩余时间: " + minutes + "分" + seconds + "秒，再次输入可继续"),
                        ChatFormatting.YELLOW,
                        null));
            }
            return true;
        }
        
        // 检查冷却
        if (isOnCooldown(playerId)) {
            long remaining = getCooldownRemaining(playerId);
            int minutes = (int) (remaining / 60);
            int seconds = (int) (remaining % 60);
            adapter.sendSystemMessage(player, adapter.styleComponent(
                    adapter.createTextComponent("飞行卡冷却中，剩余时间: " + minutes + "分" + seconds + "秒"),
                    ChatFormatting.RED,
                    null));
            return false;
        }
        
        // 激活飞行
        adapter.setFlightAbility(player, true, true);
        
        // 设置飞行时间
        flightTimeRemaining.put(playerId, FLIGHT_DURATION);
        pausedPlayers.remove(playerId);
        
        adapter.sendSystemMessage(player, adapter.styleComponent(
                adapter.createTextComponent("已激活创造飞行体验卡！飞行时间：5分钟"),
                ChatFormatting.GREEN,
                null));
        adapter.sendSystemMessage(player, adapter.styleComponent(
                adapter.createTextComponent("再次输入 /flightcard 可暂停/继续，冷却20分钟（飞行结束后开始）"),
                ChatFormatting.GRAY,
                null));
        
        return true;
    }
    
    /**
     * 检查玩家是否在冷却中
     * @param playerId 玩家UUID
     * @return 是否在冷却中
     */
    public static boolean isOnCooldown(UUID playerId) {
        Long lastUse = cooldowns.get(playerId);
        if (lastUse == null) return false;
        
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastUse;
        return elapsed < COOLDOWN_DURATION * 1000L;
    }
    
    /**
     * 获取冷却剩余时间（秒）
     * @param playerId 玩家UUID
     * @return 剩余秒数
     */
    public static long getCooldownRemaining(UUID playerId) {
        Long lastUse = cooldowns.get(playerId);
        if (lastUse == null) return 0;
        
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastUse;
        long remaining = COOLDOWN_DURATION * 1000L - elapsed;
        return Math.max(0, remaining / 1000);
    }
    
    /**
     * 更新飞行时间（每秒调用一次）
     * @param player 玩家
     */
    public static void updateFlightTime(ServerPlayer player) {
        UUID playerId = adapter.getPlayerUUID(player);
        Integer remaining = flightTimeRemaining.get(playerId);
        
        if (remaining == null) return;
        
        if (remaining <= 0) {
            // 飞行时间结束
            disableFlight(player);
            return;
        }
        
        // 暂停时不显示不计时
        if (pausedPlayers.contains(playerId)) {
            return;
        }
        
        // 减少剩余时间
        flightTimeRemaining.put(playerId, remaining - 1);
        
        // 在 Action Bar 持续显示倒计时
        int minutes = (remaining - 1) / 60;
        int seconds = (remaining - 1) % 60;
        String timeStr = String.format("%d:%02d", minutes, seconds);
        adapter.displayClientMessage(player, 
            adapter.styleComponent(
                adapter.createTextComponent("✈ 飞行中 | 剩余时间: " + timeStr),
                ChatFormatting.GREEN,
                null),
            true);
    }
    
    /**
     * 禁用飞行
     * @param player 玩家
     */
    public static void disableFlight(ServerPlayer player) {
        disableFlight(player, true);
    }
    
    /**
     * 禁用飞行
     * @param player 玩家
     * @param sendMessage 是否发送消息
     */
    public static void disableFlight(ServerPlayer player, boolean sendMessage) {
        UUID playerId = adapter.getPlayerUUID(player);
        
        // 只有在飞行中才禁用
        if (!flightTimeRemaining.containsKey(playerId)) return;
        
        adapter.setFlightAbility(player, false, false);
        
        flightTimeRemaining.remove(playerId);
        pausedPlayers.remove(playerId);
        
        // 设置冷却时间（从飞行结束开始计算）
        cooldowns.put(playerId, System.currentTimeMillis());
        
        if (sendMessage) {
            adapter.sendSystemMessage(player, adapter.styleComponent(
                    adapter.createTextComponent("飞行体验卡时间结束！"),
                    ChatFormatting.RED,
                    null));
            adapter.sendSystemMessage(player, adapter.styleComponent(
                    adapter.createTextComponent("冷却时间：20分钟"),
                    ChatFormatting.GRAY,
                    null));
        }
    }
    
    /**
     * 检查玩家是否正在飞行中
     * @param playerId 玩家UUID
     * @return 是否在飞行中
     */
    public static boolean isFlying(UUID playerId) {
        return flightTimeRemaining.containsKey(playerId);
    }
    
    /**
     * 获取飞行剩余时间（秒）
     * @param playerId 玩家UUID
     * @return 剩余秒数
     */
    public static int getFlightTimeRemaining(UUID playerId) {
        return flightTimeRemaining.getOrDefault(playerId, 0);
    }
}
