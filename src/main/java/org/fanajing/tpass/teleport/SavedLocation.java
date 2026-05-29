package org.fanajing.tpass.teleport;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class SavedLocation {
    public final ResourceKey<Level> dimension;
    public final double x;
    public final double y;
    public final double z;
    public final float yRot;
    public final float xRot;

    public SavedLocation(ServerLevel level, double x, double y, double z, float yRot, float xRot) {
        this.dimension = level.dimension();
        this.x = x;
        this.y = y;
        this.z = z;
        this.yRot = yRot;
        this.xRot = xRot;
    }

    public SavedLocation(ResourceKey<Level> dimension, double x, double y, double z, float yRot, float xRot) {
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yRot = yRot;
        this.xRot = xRot;
    }
}
