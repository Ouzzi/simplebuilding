package com.simplebuilding.modules.simpletweaks.protect;

import java.util.ArrayList;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Health ESP protection: entity data about another player sent to {@code viewer} carries that player's maximum health
 * instead of the real value (0 stays 0 for the death animation). Own health, mounts and mobs are unchanged.
 */
public final class HideHealth {
    /** Set by the LivingEntity accessor mixin. */
    public static volatile EntityDataAccessor<Float> healthId;

    /** The packet {@code viewer} should get instead of {@code packet}, or {@code packet} itself. */
    public static ClientboundSetEntityDataPacket filter(ServerPlayer viewer, ClientboundSetEntityDataPacket packet) {
        var id = healthId;
        if (id == null || packet.id() == viewer.getId() || !Protect.on() || !Protect.config().hideHealth().enabled()) return packet;
        int slot = -1;
        var items = packet.packedItems();
        for (int i = 0; i < items.size(); i++) if (items.get(i).id() == id.id()) { slot = i; break; }
        if (slot < 0) return packet;
        if (!(viewer.level().getEntity(packet.id()) instanceof Player other)) return packet;
        if (!(items.get(slot).value() instanceof Float health) || health <= 0f) return packet;
        var copy = new ArrayList<>(items);
        copy.set(slot, SynchedEntityData.DataValue.create(id, other.getMaxHealth()));
        return new ClientboundSetEntityDataPacket(packet.id(), copy);
    }
    private HideHealth() {}
}
