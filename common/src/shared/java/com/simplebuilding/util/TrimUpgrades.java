package com.simplebuilding.util;

import com.simplebuilding.items.ModItems;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * Die drei Besatz-Aufwertungen der Mod am Schmiedetisch, an einer Stelle: Glowing (Leuchttinte, eine
 * Stufe), Emitting (Glowstonestaub, Strahlkraft bis 5) und Pulsating (Echoscherbe, einmalig).
 * {@code SmithingScreenHandlerMixin} legt das Ergebnis in den Ausgabe-Slot, die JEI-Anzeige zeigt
 * dasselbe Ergebnis (Ruestung mit Wirkung statt der Vorlage, die das Platzhalter-Rezept nennt).
 */
public final class TrimUpgrades {
    /** Hoechste (und einzige) Glowing-Stufe - Glowing II gibt es nicht mehr (Besitzer 2026-09-29). */
    public static final int MAX_GLOW_LEVEL = GlowingTrimUtils.MAX_GLOW_LEVEL;
    /** Hoechste Strahlkraft (Emitting). */
    public static final int MAX_EMISSION_LEVEL = 5;

    /** Vorlage, Material und die Ruestung, die JEI als Beispiel zeigt - in Anzeige-Reihenfolge. */
    public record Upgrade(Item template, Item material) {
    }

    public static final List<Upgrade> ALL = List.of(
            new Upgrade(ModItems.GLOWING_TRIM_TEMPLATE, Items.GLOW_INK_SAC),
            new Upgrade(ModItems.EMITTING_TRIM_TEMPLATE, Items.GLOWSTONE_DUST),
            new Upgrade(ModItems.PULSATING_TRIM_TEMPLATE, Items.ECHO_SHARD));

    private TrimUpgrades() {
    }

    /** Nimmt die Aufwertung dieses Teil an? Im Tag {@code #minecraft:trimmable_armor} oder ausruestbar. */
    public static boolean isValidArmor(ItemStack armorStack) {
        return !armorStack.isEmpty() && (armorStack.is(ItemTags.TRIMMABLE_ARMOR) || armorStack.get(DataComponents.EQUIPPABLE) != null);
    }

    /**
     * Das Ergebnis der Aufwertung, {@link ItemStack#EMPTY}, wenn die Kombination der Mod gehoert, aber
     * nichts mehr geht (Obergrenze erreicht), und {@code null}, wenn sie nicht der Mod gehoert (falsche
     * Vorlage, falsches Material, kein Ruestungsteil) - dann entscheidet Vanilla.
     */
    public static @Nullable ItemStack result(ItemStack template, ItemStack armor, ItemStack material) {
        if (!isValidArmor(armor)) {
            return null;
        }
        if (template.is(ModItems.GLOWING_TRIM_TEMPLATE) && material.is(Items.GLOW_INK_SAC)) {
            int level = GlowingTrimUtils.getGlowLevel(armor);
            if (level >= MAX_GLOW_LEVEL) {
                return ItemStack.EMPTY;
            }
            ItemStack out = armor.copyWithCount(1);
            GlowingTrimUtils.setGlowLevel(out, level + 1);
            return out;
        }
        if (template.is(ModItems.EMITTING_TRIM_TEMPLATE) && material.is(Items.GLOWSTONE_DUST)) {
            if (GlowingTrimUtils.getEmissionLevel(armor) >= MAX_EMISSION_LEVEL) {
                return ItemStack.EMPTY;
            }
            ItemStack out = armor.copyWithCount(1);
            GlowingTrimUtils.incrementEmissionLevel(out);
            return out;
        }
        if (template.is(ModItems.PULSATING_TRIM_TEMPLATE) && material.is(Items.ECHO_SHARD)) {
            if (GlowingTrimUtils.isPulsating(armor)) {
                return ItemStack.EMPTY;
            }
            ItemStack out = armor.copyWithCount(1);
            GlowingTrimUtils.setPulsating(out, true);
            return out;
        }
        return null;
    }
}
