package com.simplearrows.fletching;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractSkullBlock;

/**
 * Tips against a mob group also count on SimpleBuilding's Training Dummy as the mob of its head (owner 2026-10-02). Only ids
 * and Vanilla classes - no SimpleBuilding import (module independence): the dummy is the entity type
 * {@code simplebuilding:training_dummy}, its head's skull type names the mob, first under {@code minecraft:}, else in the
 * head's namespace (same rule as SimpleBuilding's {@code DummyTargets}); dragon head = Ender Dragon, player head = none.
 */
public final class DummyHeads {
    static final Identifier TRAINING_DUMMY = Identifier.fromNamespaceAndPath("simplebuilding", "training_dummy");

    private DummyHeads() {}

    /** The type hits on {@code entity} count against: a training dummy's head mob, else the entity's own type. */
    public static EntityType<?> effectiveType(Entity entity) {
        if (entity instanceof LivingEntity living && TRAINING_DUMMY.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()))) {
            EntityType<?> head = typeForHead(living.getItemBySlot(EquipmentSlot.HEAD));
            if (head != null) return head;
        }
        return entity.getType();
    }

    static EntityType<?> typeForHead(ItemStack head) {
        if (head.isEmpty() || !(head.getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof AbstractSkullBlock skull)) {
            return null;
        }
        String name = skull.getType().getSerializedName();
        if (name.equals("dragon")) return net.minecraft.world.entity.EntityTypes.ENDER_DRAGON;
        if (name.equals("player")) return null;
        Identifier id = Identifier.tryParse(name);
        if (id == null) return null;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.withDefaultNamespace(id.getPath()))
                .orElseGet(() -> BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null));
        return type == null || type == net.minecraft.world.entity.EntityTypes.PLAYER ? null : type;
    }
}
