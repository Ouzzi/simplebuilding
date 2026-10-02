package com.simplebuilding.dummy;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractSkullBlock;
import org.jspecify.annotations.Nullable;

/**
 * Welche Mob-Art eine Trainingspuppe darstellt (Besitzer 2026-10-02): die ihres Kopfes. Jeder Kopf-Block
 * (Vanilla, SimpleBuilding, simplefun und jede andere Mod) traegt einen {@code SkullBlock.Type}; dessen Name
 * ({@code zombie}, {@code simplebuilding:spider}, {@code simplefun:pig}) fuehrt ueber den Pfad zur Entity-Art,
 * zuerst unter {@code minecraft:}, sonst im Namensraum des Kopfes. So koppeln die Module nur ueber Registry-Ids.
 * Der Drachenkopf heisst {@code dragon}; Spielerkopf und Kuerbis stellen keine Art dar.
 */
public final class DummyTargets {
    private DummyTargets() {
    }

    /** Die dargestellte Art, oder {@code null} (kein Kopf, Kuerbis, Spielerkopf, unbekannter Kopf). */
    public static @Nullable EntityType<?> typeForHead(ItemStack head) {
        if (head.isEmpty() || !(head.getItem() instanceof BlockItem blockItem)
                || !(blockItem.getBlock() instanceof AbstractSkullBlock skull)) {
            return null;
        }
        String name = skull.getType().getSerializedName();
        if (name.equals("dragon")) {
            return net.minecraft.world.entity.EntityTypes.ENDER_DRAGON;
        }
        if (name.equals("player")) {
            return null;
        }
        Identifier id = Identifier.tryParse(name);
        if (id == null) {
            return null;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.withDefaultNamespace(id.getPath()))
                .orElseGet(() -> BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null));
        return type == null || type == net.minecraft.world.entity.EntityTypes.PLAYER ? null : type;
    }

    /** Die Art, gegen die Treffer auf {@code entity} zaehlen: bei einer Trainingspuppe die ihres Kopfes. */
    public static EntityType<?> effectiveType(Entity entity) {
        if (entity instanceof TrainingDummy dummy && dummy.isTrainingDummy()) {
            EntityType<?> type = typeForHead(dummy.getItemBySlot(EquipmentSlot.HEAD));
            if (type != null) {
                return type;
            }
        }
        return entity.getType();
    }

    /**
     * Opfer fuer Verzauberungs-Bedingungen: bei einer Trainingspuppe mit Mob-Kopf ein nie gespawnter
     * Stellvertreter dieser Art an ihrer Stelle, sonst {@code entity} selbst.
     */
    public static Entity victim(Entity entity) {
        if (entity instanceof TrainingDummy dummy) {
            Entity proxy = dummy.proxy();
            if (proxy != null) {
                return proxy;
            }
        }
        return entity;
    }
}
