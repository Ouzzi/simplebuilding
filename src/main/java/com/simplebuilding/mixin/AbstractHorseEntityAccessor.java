package com.simplebuilding.mixin;

import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.inventory.SimpleInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractHorseEntity.class)
public interface AbstractHorseEntityAccessor {
    // Greift auf das protected Feld 'items' in AbstractHorseEntity zu
    @Accessor("items")
    SimpleInventory getItems();
}