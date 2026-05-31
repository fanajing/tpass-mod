package org.fanajing.tpass.team;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import org.fanajing.tpass.core.CoreTeamManager;

import java.util.Map;
import java.util.UUID;

/**
 * 队伍管理器（包装层）
 * 委托给 CoreTeamManager 处理实际逻辑
 * 保持向后兼容的 API
 */
public class TeamManager {

    public static boolean createTeam(String name, ServerPlayer leader) {
        return CoreTeamManager.createTeam(name, leader);
    }

    public static boolean invitePlayer(String teamName, UUID target) {
        return CoreTeamManager.invitePlayer(teamName, target);
    }

    public static boolean denyInvite(String teamName, UUID player) {
        return CoreTeamManager.denyInvite(teamName, player);
    }

    public static boolean joinTeam(String teamName, ServerPlayer player) {
        return CoreTeamManager.joinTeam(teamName, player);
    }

    public static boolean leaveTeam(ServerPlayer player) {
        return CoreTeamManager.leaveTeam(player);
    }

    public static boolean kickPlayer(String teamName, UUID target, ServerPlayer kicker) {
        return CoreTeamManager.kickPlayer(teamName, target, kicker);
    }

    public static boolean disbandTeam(String teamName, ServerPlayer leader) {
        return CoreTeamManager.disbandTeam(teamName, leader);
    }

    public static TeamData getPlayerTeam(ServerPlayer player) {
        return CoreTeamManager.getPlayerTeam(player);
    }

    public static TeamData getTeam(String name) {
        return CoreTeamManager.getTeam(name);
    }

    public static boolean isInTeam(ServerPlayer player) {
        return CoreTeamManager.isInTeam(player);
    }

    public static String getTeamName(ServerPlayer player) {
        return CoreTeamManager.getTeamName(player);
    }

    public static void setGlowColor(UUID player, ChatFormatting color) {
        CoreTeamManager.setGlowColor(player, color);
    }

    public static ChatFormatting getGlowColor(UUID player) {
        return CoreTeamManager.getGlowColor(player);
    }

    public static void removeGlowColor(UUID player) {
        CoreTeamManager.removeGlowColor(player);
    }

    public static Map<String, TeamData> getAllTeams() {
        return CoreTeamManager.getAllTeams();
    }

    public static void clearAll() {
        CoreTeamManager.clearAll();
    }

    public static void loadTeam(TeamData team) {
        CoreTeamManager.loadTeam(team);
    }

    public static Map<UUID, ChatFormatting> getAllGlowColors() {
        return CoreTeamManager.getAllGlowColors();
    }

    public static boolean setPvpEnabled(String teamName, boolean enabled) {
        return CoreTeamManager.setPvpEnabled(teamName, enabled);
    }

    public static net.minecraft.world.SimpleContainer getTeamChest(String teamName) {
        return CoreTeamManager.getTeamChest(teamName);
    }

    public static void removeTeamChest(String teamName) {
        CoreTeamManager.removeTeamChest(teamName);
    }
}
