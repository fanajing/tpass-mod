package org.fanajing.tpass;

import net.minecraft.server.level.ServerPlayer;
import org.fanajing.tpass.core.CoreFlightCardManager;

import java.util.UUID;

/**
 * 飞行卡管理器（包装层）
 * 委托给 CoreFlightCardManager 处理实际逻辑
 * 保持向后兼容的 API
 */
public class FlightCardManager {
    
    /**
     * 使用飞行卡
     * @param player 玩家
     * @return 是否成功使用
     */
    public static boolean useFlightCard(ServerPlayer player) {
        return CoreFlightCardManager.useFlightCard(player);
    }
    
    /**
     * 检查玩家是否在冷却中
     * @param playerId 玩家UUID
     * @return 是否在冷却中
     */
    public static boolean isOnCooldown(UUID playerId) {
        return CoreFlightCardManager.isOnCooldown(playerId);
    }
    
    /**
     * 获取冷却剩余时间（秒）
     * @param playerId 玩家UUID
     * @return 剩余秒数
     */
    public static long getCooldownRemaining(UUID playerId) {
        return CoreFlightCardManager.getCooldownRemaining(playerId);
    }
    
    /**
     * 更新飞行时间（每秒调用一次）
     * @param player 玩家
     */
    public static void updateFlightTime(ServerPlayer player) {
        CoreFlightCardManager.updateFlightTime(player);
    }
    
    /**
     * 禁用飞行
     * @param player 玩家
     */
    public static void disableFlight(ServerPlayer player) {
        CoreFlightCardManager.disableFlight(player);
    }
    
    /**
     * 禁用飞行
     * @param player 玩家
     * @param sendMessage 是否发送消息
     */
    public static void disableFlight(ServerPlayer player, boolean sendMessage) {
        CoreFlightCardManager.disableFlight(player, sendMessage);
    }
    
    /**
     * 检查玩家是否正在飞行中
     * @param playerId 玩家UUID
     * @return 是否在飞行中
     */
    public static boolean isFlying(UUID playerId) {
        return CoreFlightCardManager.isFlying(playerId);
    }
    
    /**
     * 获取飞行剩余时间（秒）
     * @param playerId 玩家UUID
     * @return 剩余秒数
     */
    public static int getFlightTimeRemaining(UUID playerId) {
        return CoreFlightCardManager.getFlightTimeRemaining(playerId);
    }
}
