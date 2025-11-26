package com.simplebuilding.util;

import net.minecraft.component.type.ItemEnchantmentsComponent;

public interface IEnchantableShulkerBox {
    void simplebuilding$setEnchantments(ItemEnchantmentsComponent enchants);
    ItemEnchantmentsComponent simplebuilding$getEnchantments();
}