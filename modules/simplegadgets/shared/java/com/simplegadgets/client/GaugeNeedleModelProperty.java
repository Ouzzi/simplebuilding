package com.simplegadgets.client;

import com.mojang.serialization.MapCodec;
import com.simplegadgets.SimpleGadgets;
import com.simplegadgets.gauge.VelocityGaugeItem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * {@code simplegadgets:gauge_needle} (2026-09-29): the needle of the Gauge's item texture, like the
 * compass needle - 0 at rest (lower left), 1 at full scale (lower right), on the same square-root
 * scale as the HUD dial ({@link VelocityGaugeItem#needleFraction}). The speed is that of whoever
 * holds it (their mount, if riding; vertical speed ignored on the ground, as in the HUD); lying in
 * an item frame or on the ground the needle rests. The item model picks one of the needle frames
 * with {@code minecraft:range_dispatch} ({@code assets/simplegadgets/items/velocity_gauge.json}).
 *
 * <p>Fabric registers it directly on the {@code ID_MAPPER} ({@code GadgetsFabricClient}).
 */
public record GaugeNeedleModelProperty() implements RangeSelectItemModelProperty {

    public static final Identifier ID = SimpleGadgets.id("gauge_needle");

    public static final MapCodec<GaugeNeedleModelProperty> CODEC = MapCodec.unit(new GaugeNeedleModelProperty());

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        LivingEntity holder = owner == null ? null : owner.asLivingEntity();
        if (holder == null) {
            return 0.0f;
        }
        Entity moving = holder.getVehicle() != null ? holder.getVehicle() : holder;
        Vec3 velocity = moving.getDeltaMovement();
        double vy = moving.onGround() ? 0.0 : velocity.y;
        double bps = Math.sqrt(velocity.x * velocity.x + vy * vy + velocity.z * velocity.z) * 20.0;
        return (float) VelocityGaugeItem.needleFraction(bps < 0.05 ? 0.0 : bps);
    }

    @Override
    public MapCodec<GaugeNeedleModelProperty> type() {
        return CODEC;
    }
}
