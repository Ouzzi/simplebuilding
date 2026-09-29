package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.items.ModItems;
import java.util.function.BiConsumer;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

/**
 * Where the enchantment Range does not lengthen the block reach (owner 2026-09-29): on the Attractor
 * it widens the pull radius ({@code MagnetItem#pullRange}), on the Gauge the altimeter
 * ({@code VelocityGaugeItem#altimeterRange}). {@code RangeReachMixin} filters the reach modifier.
 */
public final class RangeReach {
    private RangeReach() {
    }

    /** Id of the reach modifier the enchantment Range adds ({@code ModEnchantments}). */
    public static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "enchantment.range");

    /** Whether Range on this stack keeps the reach modifier (false for the Attractor and the Gauge). */
    public static boolean keepsReach(ItemStack stack) {
        return !(stack.is(ModItems.MAGNET) || stack.is(ModItems.VELOCITY_GAUGE));
    }

    /** {@code consumer} as is, or - for the Attractor and the Gauge - without Range's reach modifier. */
    public static BiConsumer<Holder<Attribute>, AttributeModifier> filter(ItemStack stack,
                                                                         BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
        if (stack == null || stack.isEmpty() || keepsReach(stack)) {
            return consumer;
        }
        return (attribute, modifier) -> {
            if (!isRangeModifier(modifier.id())) {
                consumer.accept(attribute, modifier);
            }
        };
    }

    /** Range's modifier, with or without the slot suffix vanilla appends ({@code enchantment.range/mainhand}). */
    public static boolean isRangeModifier(Identifier id) {
        return id.getNamespace().equals(MODIFIER_ID.getNamespace())
                && (id.getPath().equals(MODIFIER_ID.getPath()) || id.getPath().startsWith(MODIFIER_ID.getPath() + "/"));
    }
}
