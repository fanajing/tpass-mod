package org.fanajing.tpass.team;

import org.fanajing.tpass.teleport.SavedLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TeamData {
    public final String name;
    public final UUID leader;
    private final Set<UUID> members = new HashSet<>();
    private final Set<UUID> pendingInvites = new HashSet<>();
    private boolean pvpEnabled = false;
    private final Map<String, SavedLocation> warps = new HashMap<>();

    public TeamData(String name, UUID leader) {
        this.name = name;
        this.leader = leader;
        this.members.add(leader);
    }

    public boolean isPvpEnabled() {
        return pvpEnabled;
    }

    public void setPvpEnabled(boolean pvpEnabled) {
        this.pvpEnabled = pvpEnabled;
    }

    public void addMember(UUID player) {
        members.add(player);
    }

    public void removeMember(UUID player) {
        members.remove(player);
    }

    public boolean isMember(UUID player) {
        return members.contains(player);
    }

    public Set<UUID> getMembers() {
        return new HashSet<>(members);
    }

    public void addPendingInvite(UUID player) {
        pendingInvites.add(player);
    }

    public void removePendingInvite(UUID player) {
        pendingInvites.remove(player);
    }

    public boolean isInvited(UUID player) {
        return pendingInvites.contains(player);
    }

    public Set<UUID> getPendingInvitesInternal() {
        return new HashSet<>(pendingInvites);
    }

    public int getSize() {
        return members.size();
    }

    public void setWarp(String name, SavedLocation location) {
        warps.put(name, location);
    }

    public SavedLocation getWarp(String name) {
        return warps.get(name);
    }

    public void removeWarp(String name) {
        warps.remove(name);
    }

    public Map<String, SavedLocation> getWarps() {
        return new HashMap<>(warps);
    }
}
