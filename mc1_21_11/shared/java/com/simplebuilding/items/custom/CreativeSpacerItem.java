package com.simplebuilding.items.custom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Unsichtbarer Platzhalter im Kreativinventar ({@code simplebuilding:creative_spacer}), siehe
 * {@link com.simplebuilding.items.CreativeTabLayout}.
 *
 * <p>Nicht erhaeltlich: kein Rezept, kein Loot, nicht im Suchtab, per
 * {@code c:hidden_from_recipe_viewers} in JEI/REI/EMI versteckt, leeres Item-Modell. Ein Klick auf
 * einen Platzhalter im Kreativinventar tut nichts, weil {@code SlotMixin} einen Platz mit Platzhalter
 * als inaktiv meldet (kein Hover, kein Tooltip, kein Klick). Gelangt trotzdem einer in ein Inventar
 * (etwa per {@code /give}), loescht er sich beim naechsten Inventar-Tick selbst.
 */
public class CreativeSpacerItem extends Item {
    public CreativeSpacerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot) {
        stack.setCount(0);
    }
}
