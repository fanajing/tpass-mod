package org.fanajing.tpass.team;

import com.google.gson.*;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.fanajing.tpass.teleport.SavedLocation;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class TeamStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "tpass_teams.json";

    public static void save(Path worldPath) {
        Path filePath = worldPath.resolve(FILE_NAME);
        TeamDataContainer container = new TeamDataContainer();

        for (Map.Entry<String, TeamData> entry : TeamManager.getAllTeams().entrySet()) {
            TeamData team = entry.getValue();
            TeamDataEntry teamEntry = new TeamDataEntry();
            teamEntry.name = team.name;
            teamEntry.leader = team.leader.toString();
            for (UUID member : team.getMembers()) {
                teamEntry.members.add(member.toString());
            }
            for (UUID invite : team.getPendingInvitesInternal()) {
                teamEntry.pendingInvites.add(invite.toString());
            }
            teamEntry.pvpEnabled = team.isPvpEnabled();
            for (Map.Entry<String, SavedLocation> warpEntry : team.getWarps().entrySet()) {
                teamEntry.warps.put(warpEntry.getKey(), SavedLocationEntry.from(warpEntry.getValue()));
            }
            container.teams.add(teamEntry);
        }

        for (Map.Entry<UUID, ChatFormatting> entry : TeamManager.getAllGlowColors().entrySet()) {
            container.glowColors.put(entry.getKey().toString(), entry.getValue().name());
        }

        try (Writer writer = Files.newBufferedWriter(filePath)) {
            GSON.toJson(container, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void load(Path worldPath) {
        Path filePath = worldPath.resolve(FILE_NAME);
        if (!Files.exists(filePath)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(filePath)) {
            TeamDataContainer container = GSON.fromJson(reader, TeamDataContainer.class);
            if (container == null || container.teams == null) return;

            TeamManager.clearAll();

            for (TeamDataEntry entry : container.teams) {
                if (entry.name == null || entry.leader == null) continue;
                UUID leader = UUID.fromString(entry.leader);
                TeamData team = new TeamData(entry.name, leader);
                team.setPvpEnabled(entry.pvpEnabled);

                for (String member : entry.members) {
                    team.addMember(UUID.fromString(member));
                }
                for (String invite : entry.pendingInvites) {
                    team.addPendingInvite(UUID.fromString(invite));
                }
                for (Map.Entry<String, SavedLocationEntry> warpEntry : entry.warps.entrySet()) {
                    SavedLocationEntry sle = warpEntry.getValue();
                    ResourceKey<Level> dim = ResourceKey.create(
                            net.minecraft.core.registries.Registries.DIMENSION,
                            net.minecraft.resources.ResourceLocation.parse(sle.dimension)
                    );
                    SavedLocation loc = new SavedLocation(dim, sle.x, sle.y, sle.z, sle.yRot, sle.xRot);
                    team.setWarp(warpEntry.getKey(), loc);
                }

                TeamManager.loadTeam(team);
            }

            for (Map.Entry<String, String> entry : container.glowColors.entrySet()) {
                UUID playerId = UUID.fromString(entry.getKey());
                ChatFormatting color = ChatFormatting.valueOf(entry.getValue());
                TeamManager.setGlowColor(playerId, color);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class TeamDataContainer {
        List<TeamDataEntry> teams = new ArrayList<>();
        Map<String, String> glowColors = new HashMap<>();
    }

    private static class TeamDataEntry {
        String name;
        String leader;
        List<String> members = new ArrayList<>();
        List<String> pendingInvites = new ArrayList<>();
        boolean pvpEnabled = false;
        Map<String, SavedLocationEntry> warps = new HashMap<>();
    }

    private static class SavedLocationEntry {
        String dimension;
        double x, y, z;
        float yRot, xRot;

        static SavedLocationEntry from(SavedLocation loc) {
            SavedLocationEntry entry = new SavedLocationEntry();
            entry.dimension = loc.dimension.location().toString();
            entry.x = loc.x;
            entry.y = loc.y;
            entry.z = loc.z;
            entry.yRot = loc.yRot;
            entry.xRot = loc.xRot;
            return entry;
        }
    }
}
