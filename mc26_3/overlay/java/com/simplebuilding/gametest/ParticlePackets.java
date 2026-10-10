package com.simplebuilding.gametest;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.world.phys.Vec3;

/** Where a particle packet spawns and whether it is a music note, MC 26.3+ side (record accessors; twin in mc26_2). */
final class ParticlePackets {

    private ParticlePackets() {
    }

    static boolean isNote(ClientboundLevelParticlesPacket packet) {
        return packet.particle().getType() == ParticleTypes.NOTE;
    }

    static Vec3 position(ClientboundLevelParticlesPacket packet) {
        return new Vec3(packet.x(), packet.y(), packet.z());
    }
}
