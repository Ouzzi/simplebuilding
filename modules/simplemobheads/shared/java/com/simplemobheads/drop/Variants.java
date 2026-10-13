package com.simplemobheads.drop;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.simplemobheads.data.HeadDef;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.monster.Strider;

/**
 * The variant of a mob as a head variant id (concept §3.2 B). Most mobs name it in an entity data component
 * ({@code minecraft:cat/variant} = {@code minecraft:siamese} -> {@code siamese}), read through the component's own codec,
 * so registry variants, enums and dye colors all work the same way; three mobs keep it elsewhere (panda genes, copper
 * golem oxidation, strider out of lava). Unknown values give the base look.
 */
public final class Variants {
    private Variants() {
    }

    public static String of(HeadDef head, Entity entity) {
        String raw = switch (head.variantSource()) {
            case "panda" -> entity instanceof Panda panda ? panda.getVariant().getSerializedName() : "";
            case "copper_golem" -> entity instanceof CopperGolem golem ? golem.getWeatherState().getSerializedName() : "";
            case "strider" -> entity instanceof Strider strider ? (strider.isSuffocating() ? "cold" : "warm") : "";
            default -> head.variantComponent().isEmpty() ? "" : component(entity, head.variantComponent());
        };
        if (raw.startsWith("minecraft:")) {
            raw = raw.substring("minecraft:".length());
        }
        return head.variantIds().contains(raw) ? raw : "";
    }

    @SuppressWarnings("unchecked")
    private static String component(Entity entity, String id) {
        DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.parse(id));
        if (type == null) {
            return "";
        }
        Object value = entity.get(type);
        if (value == null) {
            return "";
        }
        Codec<Object> codec = (Codec<Object>) type.codecOrThrow();
        JsonElement json = codec.encodeStart(entity.registryAccess().createSerializationContext(JsonOps.INSTANCE), value).result().orElse(null);
        return json != null && json.isJsonPrimitive() ? json.getAsString() : "";
    }
}
