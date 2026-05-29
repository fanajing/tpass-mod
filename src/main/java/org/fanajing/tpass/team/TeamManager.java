package org.fanajing.tpass.team;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeamManager {
    private static final Map<String, TeamData> teams = new ConcurrentHashMap<>();
    private static final Map<UUID, String> playerTeamMap = new ConcurrentHashMap<>();
    private static final Map<UUID, ChatFormatting> glowColors = new ConcurrentHashMap<>();
    private static final Map<String, net.minecraft.world.SimpleContainer> teamChests = new ConcurrentHashMap<>();

    public static boolean createTeam(String name, ServerPlayer leader) {
        if (teams.containsKey(name)) {
            return false;
        }
        if (playerTeamMap.containsKey(leader.getUUID())) {
            return false;
        }
        TeamData team = new TeamData(name, leader.getUUID());
        teams.put(name, team);
        playerTeamMap.put(leader.getUUID(), name);
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

    public static boolean joinTeam(String teamName, ServerPlayer player) {
        TeamData team = teams.get(teamName);
        if (team == null) return false;
        if (!team.isInvited(player.getUUID())) return false;
        if (playerTeamMap.containsKey(player.getUUID())) return false;

        team.addMember(player.getUUID());
        team.removePendingInvite(player.getUUID());
        playerTeamMap.put(player.getUUID(), teamName);
        return true;
    }

    public static boolean leaveTeam(ServerPlayer player) {
        String teamName = playerTeamMap.get(player.getUUID());
        if (teamName == null) return false;
        TeamData team = teams.get(teamName);
        if (team == null) return false;

        team.removeMember(player.getUUID());
        playerTeamMap.remove(player.getUUID());

        if (team.leader.equals(player.getUUID())) {
            for (UUID member : team.getMembers()) {
                playerTeamMap.remove(member);
            }
            teams.remove(teamName);
        }

        return true;
    }

    public static boolean kickPlayer(String teamName, UUID target, ServerPlayer kicker) {
        TeamData team = teams.get(teamName);
        if (team == null) return false;
        if (!team.leader.equals(kicker.getUUID())) return false;

        team.removeMember(target);
        playerTeamMap.remove(target);
        return true;
    }

    public static boolean disbandTeam(String teamName, ServerPlayer leader) {
        TeamData team = teams.get(teamName);
        if (team == null) return false;
        if (!team.leader.equals(leader.getUUID())) return false;

        for (UUID member : team.getMembers()) {
            playerTeamMap.remove(member);
        }
        teams.remove(teamName);
        return true;
    }

    public static TeamData getPlayerTeam(ServerPlayer player) {
        String teamName = playerTeamMap.get(player.getUUID());
        return teamName != null ? teams.get(teamName) : null;
    }

    public static TeamData getTeam(String name) {
        return teams.get(name);
    }

    public static boolean isInTeam(ServerPlayer player) {
        return playerTeamMap.containsKey(player.getUUID());
    }

    public static String getTeamName(ServerPlayer player) {
        return playerTeamMap.get(player.getUUID());
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
