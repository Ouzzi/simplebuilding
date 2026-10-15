package com.simplebuilding.modules.simplestats;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.entity.npc.villager.Villager;

/**
 * Stored names of mobs: {@code minecraft:cat} for the kind, {@code minecraft:cat#tabby} with variant parts (read from the
 * vanilla entity variant components, plus baby, charged creeper, slime size and villager profession). Mobs of other mods
 * count under their entity id; their variants only if they expose a vanilla variant component.
 */
public final class MobKeys {
    private static final List<DataComponentType<?>> VARIANTS = List.of(DataComponents.CAT_VARIANT, DataComponents.WOLF_VARIANT, DataComponents.FROG_VARIANT,
            DataComponents.AXOLOTL_VARIANT, DataComponents.HORSE_VARIANT, DataComponents.LLAMA_VARIANT, DataComponents.RABBIT_VARIANT,
            DataComponents.PARROT_VARIANT, DataComponents.FOX_VARIANT, DataComponents.MOOSHROOM_VARIANT, DataComponents.PIG_VARIANT,
            DataComponents.COW_VARIANT, DataComponents.CHICKEN_VARIANT, DataComponents.ZOMBIE_NAUTILUS_VARIANT, DataComponents.VILLAGER_VARIANT,
            DataComponents.SALMON_SIZE, DataComponents.SHEEP_COLOR, DataComponents.SHULKER_COLOR, DataComponents.TROPICAL_FISH_PATTERN,
            DataComponents.TROPICAL_FISH_BASE_COLOR);

    private MobKeys() {}

    /** The kind only: {@code minecraft:zombie}. */
    public static String kind(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
    }

    /** Kind plus variant parts, or just the kind when there are none. */
    public static String variant(Entity entity) {
        List<String> parts = new ArrayList<>();
        for (DataComponentType<?> type : VARIANTS) {
            Object value = entity.get(type);
            String name = value == null ? null : name(value);
            if (name != null && !name.isEmpty()) parts.add(name);
        }
        if (entity instanceof Villager villager) {
            String job = villager.getVillagerData().profession().unwrapKey().map(k -> k.identifier().getPath()).orElse("none");
            if (!job.equals("none")) parts.add(job);
        }
        if (entity instanceof AbstractCubeMob cube) parts.add("size_" + cube.getSize());
        if (entity instanceof Creeper creeper && creeper.isPowered()) parts.add("charged");
        if (entity instanceof LivingEntity living && living.isBaby()) parts.add("baby");
        return parts.isEmpty() ? kind(entity) : kind(entity) + "#" + String.join("+", parts);
    }

    static String name(Object value) {
        if (value instanceof Holder<?> holder) return holder.unwrapKey().map(k -> k.identifier().getPath()).orElse("");
        if (value instanceof StringRepresentable named) return named.getSerializedName();
        if (value instanceof Enum<?> e) return e.name().toLowerCase(java.util.Locale.ROOT);
        return value.toString().toLowerCase(java.util.Locale.ROOT);
    }
}
