package org.fanajing.tpass.teleport;

import net.minecraft.server.level.ServerPlayer;

public class TeleportRequest {
    public final ServerPlayer requester;
    public final ServerPlayer target;
    public final long timestamp;

    public TeleportRequest(ServerPlayer requester, ServerPlayer target) {
        this.requester = requester;
        this.target = target;
        this.timestamp = System.currentTimeMillis();
    }
}
