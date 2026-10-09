package com.simplebuilding.client.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.items.custom.ColorBrushItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * {@code simplebuilding:brush_ink}: which ink the colour brush takes next, read on the client from the inventory of the
 * player holding it ({@link ColorBrushItem#inkKey}: {@code none}, a dye colour name or {@code palette}); the model tints
 * the bristle tip with it. Registered like {@link BlueprintStateModelProperty}: Fabric at the {@code ID_MAPPER} (also
 * for the datagen), NeoForge through {@code RegisterSelectItemModelPropertyEvent}, Forge by mixin. Loading this class
 * also hands the brush tooltip the client player.
 */
public record BrushInkModelProperty() implements SelectItemModelProperty<String> {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "brush_ink");

    public static final MapCodec<BrushInkModelProperty> CODEC = MapCodec.unit(new BrushInkModelProperty());

    public static final SelectItemModelProperty.Type<BrushInkModelProperty, String> PROPERTY_TYPE =
            SelectItemModelProperty.Type.create(CODEC, Codec.STRING);

    static {
        ColorBrushItem.clientPlayer = () -> Minecraft.getInstance().player;
    }

    @Override
    public Type<? extends SelectItemModelProperty<String>, String> type() {
        return PROPERTY_TYPE;
    }

    @Override
    public String get(ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity entity, int seed, ItemDisplayContext context) {
        return ColorBrushItem.inkKey(entity instanceof Player player ? player : null);
    }

    @Override
    public Codec<String> valueCodec() {
        return Codec.STRING;
    }
}
