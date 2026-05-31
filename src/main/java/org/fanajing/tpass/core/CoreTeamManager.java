package org.fanajing.tpass.core;

import net.minecraft.ChatFormatting;
import org.fanajing.tpass.adapter.VersionAdapter;
import org.fanajing.tpass.adapter.VersionAdapterFactory;
import org.fanajing.tpass.team.TeamData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 核心队伍管理器
 * 包含所有与版本无关的队伍管理逻辑
 */
public class CoreTeamManager {
    private static final Map<String, TeamData> teams = new ConcurrentHashMap<>();
    private static final Map<UUID, String> playerTeamMap = new ConcurrentHashMap<>();
    private static final Map<UUID, ChatFormatting> glowColors = new ConcurrentHashMap<>();
    private static final Map<String, net.minecraft.world.SimpleContainer> teamChests = new ConcurrentHashMap<>();
    
    private static final VersionAdapter adapter = VersionAdapterFactory.getAdapter();

    public static boolean createTeam(String name, Object player) {
        if (teams.containsKey(name)) {
            return false;
        }
        UUID playerId = adapter.getPlayerUUID((net.minecraft.server.level.ServerPlayer) player);
        if (playerTeamMap.containsKey(playerId)) {
            return false;
        }
        TeamData team = new TeamData(name, playerId);
        teams.put(name, team);
        playerTeamMap.put(playerId, name);
        return true;
    }

    public static boolean invitePlayer(String teamName, UUID target) {
        TeamData team = teams.get(teamName);
        if (team == null) return false;
        team.addPendingInvite(target);
        return true;
    }

    public static boolean denyInvite(String teamName, UUID player) {
        TeamData team = teams.get(teamName);
        if (team == null) return false;
        if (!team.isInvited(player)) return false;
        team.removePendingInvite(player);
        return true;
    }

    public static boolean joinTeam(String teamName, Object player) {
        TeamData team = teams.get(teamName);
        if (team == null) return false;
        UUID playerId = adapter.getPlayerUUID((net.minecraft.server.level.ServerPlayer) player);
        if (!team.isInvited(playerId)) return false;
        if (playerTeamMap.containsKey(playerId)) return false;

        team.addMember(playerId);
        team.removePendingInvite(playerId);
        playerTeamMap.put(playerId, teamName);
        return true;
    }

    public static boolean leaveTeam(Object player) {
        UUID playerId = adapter.getPlayerUUID((net.minecraft.server.level.ServerPlayer) player);
        String teamName = playerTeamMap.get(playerId);
        if (teamName == null) return false;
        TeamData team = teams.get(teamName);
        if (team == null) return false;

        team.removeMember(playerId);
        playerTeamMap.remove(playerId);

        if (team.leader.equals(playerId)) {
            for (UUID member : team.getMembers()) {
                playerTeamMap.remove(member);
            }
            teams.remove(teamName);
        }

        return true;
    }

    public static boolean kickPlayer(String teamName, UUID target, Object kicker) {
        TeamData team = teams.get(teamName);
        if (team == null) return false;
        UUID kickerId = adapter.getPlayerUUID((net.minecraft.server.level.ServerPlayer) kicker);
        if (!team.leader.equals(kickerId)) return false;

        team.removeMember(target);
        playerTeamMap.remove(target);
        return true;
    }

    public static boolean disbandTeam(String teamName, Object leader) {
        TeamData team = teams.get(teamName);
        if (team == null) return false;
        UUID leaderId = adapter.getPlayerUUID((net.minecraft.server.level.ServerPlayer) leader);
        if (!team.leader.equals(leaderId)) return false;

        for (UUID member : team.getMembers()) {
            playerTeamMap.remove(member);
        }
        teams.remove(teamName);
        return true;
    }

    public static TeamData getPlayerTeam(Object player) {
        UUID playerId = adapter.getPlayerUUID((net.minecraft.server.level.ServerPlayer) player);
        String teamName = playerTeamMap.get(playerId);
        return teamName != null ? teams.get(teamName) : null;
    }

    public static TeamData getTeam(String name) {
        return teams.get(name);
    }

    public static boolean isInTeam(Object player) {
        UUID playerId = adapter.getPlayerUUID((net.minecraft.server.level.ServerPlayer) player);
        return playerTeamMap.containsKey(playerId);
    }

    public static String getTeamName(Object player) {
        UUID playerId = adapter.getPlayerUUID((net.minecraft.server.level.ServerPlayer) player);
        return playerTeamMap.get(playerId);
    }

    public static void setGlowColor(UUID player, ChatFormatting color) {
        glowColors.put(player, color);
    }

    public static ChatFormatting getGlowColor(UUID player) {
        return glowColors.getOrDefault(player, ChatFormatting.WHITE);
    }

    public static void removeGlowColor(UUID player) {
        glowColors.remove(player);
    }

    public static Map<String, TeamData> getAllTeams() {
        return new java.util.HashMap<>(teams);
    }

    public static void clearAll() {
        teams.clear();
        playerTeamMap.clear();
    }

    public static void loadTeam(TeamData team) {
        teams.put(team.name, team);
        for (UUID member : team.getMembers()) {
            playerTeamMap.put(member, team.name);
        }
    }

    public static Map<UUID, ChatFormatting> getAllGlowColors() {
        return new java.util.HashMap<>(glowColors);
    }

    public static boolean setPvpEnabled(String teamName, boolean enabled) {
        TeamData team = teams.get(teamName);
        if (team == null) return false;
        team.setPvpEnabled(enabled);
        return true;
    }

    public static net.minecraft.world.SimpleContainer getTeamChest(String teamName) {
        return teamChests.computeIfAbsent(teamName, k -> new net.minecraft.world.SimpleContainer(27));
    }

    public static void removeTeamChest(String teamName) {
        teamChests.remove(teamName);
    }
}
