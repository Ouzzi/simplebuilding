package com.simplebuilding.mixin;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Setzt den Handwerksrest eines Items nach seiner Erzeugung. Gebraucht fuer das Einsteiger-Handbuch,
 * das sein eigener Rest ist (Handbuch + Schluesselitem = Themenbuch, das Handbuch bleibt liegen):
 * {@code Item.Properties#craftRemainder} braucht das fertige Item, das es beim Bauen der
 * Eigenschaften noch nicht gibt. Siehe {@link com.simplebuilding.guide.GuideBooks#makeSelfRemainder}.
 */
@Mixin(Item.class)
public interface ItemCraftRemainderAccessor {

    @Mutable
    @Accessor("craftingRemainingItem")
    void simplebuilding$setCraftingRemainingItem(ItemStackTemplate remainder);
}
