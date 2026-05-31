package org.fanajing.tpass.core;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.fanajing.tpass.adapter.VersionAdapter;
import org.fanajing.tpass.adapter.VersionAdapterFactory;
import org.fanajing.tpass.team.TeamData;
import org.fanajing.tpass.teleport.SavedLocation;
import org.fanajing.tpass.teleport.TeleportRequest;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 核心传送管理器
 * 包含所有与版本无关的传送管理逻辑
 */
public class CoreTeleportManager {
    private static final Map<ServerPlayer, TeleportRequest> pendingRequests = new ConcurrentHashMap<>();
    private static final Map<UUID, SavedLocation> lastLocations = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, SavedLocation>> homes = new ConcurrentHashMap<>();
    private static final int MAX_HOMES = 5;
    
    private static final VersionAdapter adapter = VersionAdapterFactory.getAdapter();

    public static void sendRequest(ServerPlayer requester, ServerPlayer target) {
        if (pendingRequests.containsKey(target)) {
            adapter.sendSystemMessage(requester, adapter.createTextComponent("该玩家有待处理的传送请求，请稍后再试"));
            return;
        }

        TeleportRequest request = new TeleportRequest(requester, target);
        pendingRequests.put(target, request);
        adapter.sendSystemMessage(requester, adapter.createTextComponent("已向 " + adapter.getPlayerName(target).getString() + " 发送传送请求"));

        Component acceptBtn = adapter.styleComponent(
            adapter.createTextComponent("[接受]"),
            ChatFormatting.GREEN,
            new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept")
        );
        Component denyBtn = adapter.styleComponent(
            adapter.createTextComponent("[拒绝]"),
            ChatFormatting.RED,
            new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny")
        );

        adapter.sendSystemMessage(target, adapter.appendComponents(
            adapter.createTextComponent(adapter.getPlayerName(requester).getString() + " 请求传送到你身边 "),
            acceptBtn,
            adapter.createTextComponent(" "),
            denyBtn
        ));
    }

    public static void acceptRequest(ServerPlayer target) {
        TeleportRequest request = pendingRequests.remove(target);
        if (request == null) {
            adapter.sendSystemMessage(target, adapter.createTextComponent("你没有待处理的传送请求"));
            return;
        }

        ServerPlayer requester = request.requester;
        if (requester == null || !adapter.isAlive(requester)) {
            adapter.sendSystemMessage(target, adapter.createTextComponent("请求者已离线"));
            return;
        }

        ServerLevel requesterLevel = adapter.getPlayerLevel(requester);
        lastLocations.put(adapter.getPlayerUUID(requester), new SavedLocation(
            requesterLevel, 
            requester.getX(), 
            requester.getY(), 
            requester.getZ(), 
            requester.getYRot(), 
            requester.getXRot()
        ));
        
        ServerLevel targetLevel = adapter.getPlayerLevel(target);
        adapter.teleportPlayer(requester, targetLevel, target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
        
        adapter.sendSystemMessage(requester, adapter.createTextComponent(adapter.getPlayerName(target).getString() + " 接受了你的传送请求"));
        adapter.sendSystemMessage(target, adapter.createTextComponent("已接受 " + adapter.getPlayerName(requester).getString() + " 的传送请求"));
    }

    public static void denyRequest(ServerPlayer target) {
        TeleportRequest request = pendingRequests.remove(target);
        if (request == null) {
            adapter.sendSystemMessage(target, adapter.createTextComponent("你没有待处理的传送请求"));
            return;
        }

        ServerPlayer requester = request.requester;
        if (requester != null && adapter.isAlive(requester)) {
            adapter.sendSystemMessage(requester, adapter.createTextComponent(adapter.getPlayerName(target).getString() + " 拒绝了你的传送请求"));
        }
        adapter.sendSystemMessage(target, adapter.createTextComponent("已拒绝传送请求"));
    }

    public static void saveLocation(ServerPlayer player) {
        ServerLevel level = adapter.getPlayerLevel(player);
        lastLocations.put(adapter.getPlayerUUID(player), new SavedLocation(
            level, 
            player.getX(), 
            player.getY(), 
            player.getZ(), 
            player.getYRot(), 
            player.getXRot()
        ));
    }

    public static void back(ServerPlayer player) {
        SavedLocation loc = lastLocations.remove(adapter.getPlayerUUID(player));
        if (loc == null) {
            adapter.sendSystemMessage(player, adapter.createTextComponent("没有可返回的位置"));
            return;
        }

        ServerLevel level = adapter.getLevel(player.getServer(), loc.dimension);
        if (level == null) {
            adapter.sendSystemMessage(player, adapter.createTextComponent("无法返回：目标维度不存在"));
            return;
        }

        adapter.teleportPlayer(player, level, loc.x, loc.y, loc.z, loc.yRot, loc.xRot);
        adapter.sendSystemMessage(player, adapter.createTextComponent("已返回上一个地点"));
    }

    public static void setHome(ServerPlayer player, String name) {
        UUID playerId = adapter.getPlayerUUID(player);
        Map<String, SavedLocation> playerHomes = homes.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());

        if (playerHomes.containsKey(name)) {
            ServerLevel level = adapter.getPlayerLevel(player);
            playerHomes.put(name, new SavedLocation(level, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()));
            adapter.sendSystemMessage(player, adapter.createTextComponent("传送点 [" + name + "] 已更新"));
            return;
        }

        if (playerHomes.size() >= MAX_HOMES) {
            adapter.sendSystemMessage(player, adapter.createTextComponent("传送点数量已达上限 (" + MAX_HOMES + "个)，请先删除不需要的传送点"));
            return;
        }

        ServerLevel level = adapter.getPlayerLevel(player);
        playerHomes.put(name, new SavedLocation(level, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()));
        adapter.sendSystemMessage(player, adapter.createTextComponent("传送点 [" + name + "] 已创建"));
    }

    public static void goHome(ServerPlayer player, String name) {
        UUID playerId = adapter.getPlayerUUID(player);
        Map<String, SavedLocation> playerHomes = homes.get(playerId);
        if (playerHomes == null || !playerHomes.containsKey(name)) {
            adapter.sendSystemMessage(player, adapter.createTextComponent("找不到传送点 [" + name + "]"));
            return;
        }

        SavedLocation loc = playerHomes.get(name);
        ServerLevel level = adapter.getLevel(player.getServer(), loc.dimension);
        if (level == null) {
            adapter.sendSystemMessage(player, adapter.createTextComponent("无法传送：目标维度不存在"));
            return;
        }

        adapter.teleportPlayer(player, level, loc.x, loc.y, loc.z, loc.yRot, loc.xRot);
        adapter.sendSystemMessage(player, adapter.createTextComponent("已传送到 [" + name + "]"));
    }

    public static void removeHome(ServerPlayer player, String name) {
        UUID playerId = adapter.getPlayerUUID(player);
        Map<String, SavedLocation> playerHomes = homes.get(playerId);
        if (playerHomes == null || !playerHomes.containsKey(name)) {
            adapter.sendSystemMessage(player, adapter.createTextComponent("找不到传送点 [" + name + "]"));
            return;
        }

        playerHomes.remove(name);
        adapter.sendSystemMessage(player, adapter.createTextComponent("传送点 [" + name + "] 已删除"));
    }

    public static void setTeamWarp(ServerPlayer player, String name) {
        TeamData team = CoreTeamManager.getPlayerTeam(player);
        if (team == null) {
            adapter.sendSystemMessage(player, adapter.createTextComponent("你不在任何队伍中"));
            return;
        }
        ServerLevel level = adapter.getPlayerLevel(player);
        SavedLocation loc = new SavedLocation(level, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        team.setWarp(name, loc);
        adapter.sendSystemMessage(player, adapter.createTextComponent("队伍传送点 [" + name + "] 已设置"));
    }

    public static void goTeamWarp(ServerPlayer player, String name) {
        TeamData team = CoreTeamManager.getPlayerTeam(player);
        if (team == null) {
            adapter.sendSystemMessage(player, adapter.createTextComponent("你不在任何队伍中"));
            return;
        }
        SavedLocation loc = team.getWarp(name);
        if (loc == null) {
            adapter.sendSystemMessage(player, adapter.createTextComponent("找不到队伍传送点 [" + name + "]"));
            return;
        }
        ServerLevel level = adapter.getLevel(player.getServer(), loc.dimension);
        if (level == null) {
            adapter.sendSystemMessage(player, adapter.createTextComponent("无法传送：目标维度不存在"));
            return;
        }
        adapter.teleportPlayer(player, level, loc.x, loc.y, loc.z, loc.yRot, loc.xRot);
        adapter.sendSystemMessage(player, adapter.createTextComponent("已传送到队伍传送点 [" + name + "]"));
    }

    public static void listTeamWarps(ServerPlayer player) {
        TeamData team = CoreTeamManager.getPlayerTeam(player);
        if (team == null) {
            adapter.sendSystemMessage(player, adapter.createTextComponent("你不在任何队伍中"));
            return;
        }
        Map<String, SavedLocation> warps = team.getWarps();
        if (warps.isEmpty()) {
            adapter.sendSystemMessage(player, adapter.createTextComponent("当前队伍没有设置任何传送点"));
            return;
        }
        adapter.sendSystemMessage(player, adapter.createTextComponent("=== warps ==="));
        for (String warpName : warps.keySet()) {
            Component warpBtn = adapter.styleComponent(
                adapter.createTextComponent("[ " + warpName + " ]"),
                ChatFormatting.AQUA,
                new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/warp " + warpName)
            );
            adapter.sendSystemMessage(player, warpBtn);
        }
    }
    
    public static void listHomes(ServerPlayer player) {
        UUID playerId = adapter.getPlayerUUID(player);
        Map<String, SavedLocation> playerHomes = homes.get(playerId);
        if (playerHomes == null || playerHomes.isEmpty()) {
            adapter.sendSystemMessage(player, adapter.createTextComponent("你没有设置任何传送点"));
            return;
        }
        adapter.sendSystemMessage(player, adapter.createTextComponent("=== 我的传送点 ==="));
        for (String homeName : playerHomes.keySet()) {
            Component homeBtn = adapter.createButtonComponent("[ " + homeName + " ]", ChatFormatting.AQUA, "/home " + homeName);
            adapter.sendSystemMessage(player, homeBtn);
        }
        adapter.sendSystemMessage(player, adapter.createTextComponent("=================="));
    }
}
