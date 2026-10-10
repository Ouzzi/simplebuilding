package com.simplebuilding.gametest;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.world.phys.Vec3;

/** Where a particle packet spawns and whether it is a music note, MC 26.2 side (getters; twins in mc26_3/mc26_4 overlays). */
final class ParticlePackets {

    private ParticlePackets() {
    }

    static boolean isNote(ClientboundLevelParticlesPacket packet) {
        return packet.getParticle().getType() == ParticleTypes.NOTE;
    }

    static Vec3 position(ClientboundLevelParticlesPacket packet) {
        return new Vec3(packet.getX(), packet.getY(), packet.getZ());
    }
}
