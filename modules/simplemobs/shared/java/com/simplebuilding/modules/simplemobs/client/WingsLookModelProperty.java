package com.simplebuilding.modules.simplemobs.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * {@code simplemobs:wings_look_v2} (ABN30-9): true while the Dev variants panel has the wings look "2" chosen (WA24-2 variant 2, item
 * textures of Wither, Nether and End Wings; the built-in look is variant 1). Client side, read-only, no gameplay effect. Fabric registers
 * it on the {@code ID_MAPPER} ({@code FabricClientExample}).
 */
public record WingsLookModelProperty() implements ConditionalItemModelProperty {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("simplemobs", "wings_look_v2");
    public static final String VARIANT = "simplemobs:wings_look";
    public static final MapCodec<WingsLookModelProperty> CODEC = MapCodec.unit(new WingsLookModelProperty());

    /** Dev variants panel entry: wings look 1 (built in, default) / 2. */
    public static void registerVariants() {
        com.simplelib.api.DevVariants.register(VARIANT, "Simple Mobs", "Schwingen-Look (Item)", "v1",
                new com.simplelib.api.DevVariants.Option("v1", "1"), new com.simplelib.api.DevVariants.Option("v2", "2"));
    }

    @Override
    public boolean get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed, ItemDisplayContext context) {
        return com.simplelib.api.DevVariants.is(VARIANT, "v2");
    }

    @Override
    public MapCodec<WingsLookModelProperty> type() {
        return CODEC;
    }
}
