package org.fanajing.tpass.teleport;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.fanajing.tpass.team.TeamData;
import org.fanajing.tpass.team.TeamManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportManager {
    private static final Map<ServerPlayer, TeleportRequest> pendingRequests = new ConcurrentHashMap<>();
    private static final Map<UUID, SavedLocation> lastLocations = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, SavedLocation>> homes = new ConcurrentHashMap<>();
    private static final int MAX_HOMES = 5;

    public static void sendRequest(ServerPlayer requester, ServerPlayer target) {
        if (pendingRequests.containsKey(target)) {
            requester.sendSystemMessage(Component.literal("该玩家有待处理的传送请求，请稍后再试"));
            return;
        }

        TeleportRequest request = new TeleportRequest(requester, target);
        pendingRequests.put(target, request);
        requester.sendSystemMessage(Component.literal("已向 " + target.getName().getString() + " 发送传送请求"));

        Component acceptBtn = Component.literal("[接受]")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept")));
        Component denyBtn = Component.literal("[拒绝]")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny")));

        target.sendSystemMessage(Component.literal(requester.getName().getString() + " 请求传送到你身边 ")
                .append(acceptBtn)
                .append(" ")
                .append(denyBtn));
    }

    public static void acceptRequest(ServerPlayer target) {
        TeleportRequest request = pendingRequests.remove(target);
        if (request == null) {
            target.sendSystemMessage(Component.literal("你没有待处理的传送请求"));
            return;
        }

        ServerPlayer requester = request.requester;
        if (requester == null || !requester.isAlive()) {
            target.sendSystemMessage(Component.literal("请求者已离线"));
            return;
        }

        lastLocations.put(requester.getUUID(), new SavedLocation(requester.serverLevel(), requester.getX(), requester.getY(), requester.getZ(), requester.getYRot(), requester.getXRot()));
        requester.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
        requester.sendSystemMessage(Component.literal(target.getName().getString() + " 接受了你的传送请求"));
        target.sendSystemMessage(Component.literal("已接受 " + requester.getName().getString() + " 的传送请求"));
    }

    public static void denyRequest(ServerPlayer target) {
        TeleportRequest request = pendingRequests.remove(target);
        if (request == null) {
            target.sendSystemMessage(Component.literal("你没有待处理的传送请求"));
            return;
        }

        ServerPlayer requester = request.requester;
        if (requester != null && requester.isAlive()) {
            requester.sendSystemMessage(Component.literal(target.getName().getString() + " 拒绝了你的传送请求"));
        }
        target.sendSystemMessage(Component.literal("已拒绝传送请求"));
    }

    public static void saveLocation(ServerPlayer player) {
        lastLocations.put(player.getUUID(), new SavedLocation(player.serverLevel(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()));
    }

    public static void back(ServerPlayer player) {
        SavedLocation loc = lastLocations.remove(player.getUUID());
        if (loc == null) {
            player.sendSystemMessage(Component.literal("没有可返回的位置"));
            return;
        }

        ServerLevel level = player.getServer().getLevel(loc.dimension);
        if (level == null) {
            player.sendSystemMessage(Component.literal("无法返回：目标维度不存在"));
            return;
        }

        player.teleportTo(level, loc.x, loc.y, loc.z, loc.yRot, loc.xRot);
        player.sendSystemMessage(Component.literal("已返回上一个地点"));
    }

    public static void setHome(ServerPlayer player, String name) {
        Map<String, SavedLocation> playerHomes = homes.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>());

        if (playerHomes.containsKey(name)) {
            playerHomes.put(name, new SavedLocation(player.serverLevel(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()));
            player.sendSystemMessage(Component.literal("传送点 [" + name + "] 已更新"));
            return;
        }

        if (playerHomes.size() >= MAX_HOMES) {
            player.sendSystemMessage(Component.literal("传送点数量已达上限 (" + MAX_HOMES + "个)，请先删除不需要的传送点"));
            return;
        }

        playerHomes.put(name, new SavedLocation(player.serverLevel(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()));
        player.sendSystemMessage(Component.literal("传送点 [" + name + "] 已创建"));
    }

    public static void goHome(ServerPlayer player, String name) {
        Map<String, SavedLocation> playerHomes = homes.get(player.getUUID());
        if (playerHomes == null || !playerHomes.containsKey(name)) {
            player.sendSystemMessage(Component.literal("找不到传送点 [" + name + "]"));
            return;
        }

        SavedLocation loc = playerHomes.get(name);
        ServerLevel level = player.getServer().getLevel(loc.dimension);
        if (level == null) {
            player.sendSystemMessage(Component.literal("无法传送：目标维度不存在"));
            return;
        }

        player.teleportTo(level, loc.x, loc.y, loc.z, loc.yRot, loc.xRot);
        player.sendSystemMessage(Component.literal("已传送到 [" + name + "]"));
    }

    public static void removeHome(ServerPlayer player, String name) {
        Map<String, SavedLocation> playerHomes = homes.get(player.getUUID());
        if (playerHomes == null || !playerHomes.containsKey(name)) {
            player.sendSystemMessage(Component.literal("找不到传送点 [" + name + "]"));
            return;
        }

        playerHomes.remove(name);
        player.sendSystemMessage(Component.literal("传送点 [" + name + "] 已删除"));
    }

    public static void setTeamWarp(ServerPlayer player, String name) {
        TeamData team = TeamManager.getPlayerTeam(player);
        if (team == null) {
            player.sendSystemMessage(Component.literal("你不在任何队伍中"));
            return;
        }
        SavedLocation loc = new SavedLocation(player.serverLevel(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        team.setWarp(name, loc);
        player.sendSystemMessage(Component.literal("队伍传送点 [" + name + "] 已设置"));
    }

    public static void goTeamWarp(ServerPlayer player, String name) {
        TeamData team = TeamManager.getPlayerTeam(player);
        if (team == null) {
            player.sendSystemMessage(Component.literal("你不在任何队伍中"));
            return;
        }
        SavedLocation loc = team.getWarp(name);
        if (loc == null) {
            player.sendSystemMessage(Component.literal("找不到队伍传送点 [" + name + "]"));
            return;
        }
        ServerLevel level = player.getServer().getLevel(loc.dimension);
        if (level == null) {
            player.sendSystemMessage(Component.literal("无法传送：目标维度不存在"));
            return;
        }
        player.teleportTo(level, loc.x, loc.y, loc.z, loc.yRot, loc.xRot);
        player.sendSystemMessage(Component.literal("已传送到队伍传送点 [" + name + "]"));
    }

    public static void listTeamWarps(ServerPlayer player) {
        TeamData team = TeamManager.getPlayerTeam(player);
        if (team == null) {
            player.sendSystemMessage(Component.literal("你不在任何队伍中"));
            return;
        }
        Map<String, SavedLocation> warps = team.getWarps();
        if (warps.isEmpty()) {
            player.sendSystemMessage(Component.literal("当前队伍没有设置任何传送点"));
            return;
        }
        player.sendSystemMessage(Component.literal("=== warps ==="));
        for (String warpName : warps.keySet()) {
            Component warpBtn = Component.literal("[ " + warpName + " ]")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/warp " + warpName)));
            player.sendSystemMessage(warpBtn);
        }
    }
}
