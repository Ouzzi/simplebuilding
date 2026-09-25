package com.simplebuilding.tweaks.mixin;

import com.simplebuilding.tweaks.xp.StackLimits;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Raketen-Stapelgroesse (Simple Tweaks: ItemStackMixin, Config rocketStackSize) fuer Fabric und
 * Forge: auf 26.x ist getMaxStackSize dort eine Default-Methode von ItemInstance, dieses Mixin legt
 * sie in ItemStack an. NeoForge deklariert die Methode selbst in ItemStack und nimmt stattdessen
 * {@code TweaksNeoForgeItemStackMixin} (eigene Mixin-Config), weil ein zweites Anlegen dort kollidiert.
 */
@Mixin(ItemStack.class)
public abstract class TweaksItemStackMixin {

    public int getMaxStackSize() {
        ItemStack self = (ItemStack) (Object) this;
        return StackLimits.limit(self, self.getOrDefault(DataComponents.MAX_STACK_SIZE, 1));
    }
}
