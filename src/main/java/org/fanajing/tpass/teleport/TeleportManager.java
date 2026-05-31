package org.fanajing.tpass.teleport;

import net.minecraft.server.level.ServerPlayer;
import org.fanajing.tpass.core.CoreTeleportManager;

/**
 * 传送管理器（包装层）
 * 委托给 CoreTeleportManager 处理实际逻辑
 * 保持向后兼容的 API
 */
public class TeleportManager {

    public static void sendRequest(ServerPlayer requester, ServerPlayer target) {
        CoreTeleportManager.sendRequest(requester, target);
    }

    public static void acceptRequest(ServerPlayer target) {
        CoreTeleportManager.acceptRequest(target);
    }

    public static void denyRequest(ServerPlayer target) {
        CoreTeleportManager.denyRequest(target);
    }

    public static void saveLocation(ServerPlayer player) {
        CoreTeleportManager.saveLocation(player);
    }

    public static void back(ServerPlayer player) {
        CoreTeleportManager.back(player);
    }

    public static void setHome(ServerPlayer player, String name) {
        CoreTeleportManager.setHome(player, name);
    }

    public static void goHome(ServerPlayer player, String name) {
        CoreTeleportManager.goHome(player, name);
    }

    public static void removeHome(ServerPlayer player, String name) {
        CoreTeleportManager.removeHome(player, name);
    }

    public static void setTeamWarp(ServerPlayer player, String name) {
        CoreTeleportManager.setTeamWarp(player, name);
    }

    public static void goTeamWarp(ServerPlayer player, String name) {
        CoreTeleportManager.goTeamWarp(player, name);
    }

    public static void listTeamWarps(ServerPlayer player) {
        CoreTeleportManager.listTeamWarps(player);
    }
}
