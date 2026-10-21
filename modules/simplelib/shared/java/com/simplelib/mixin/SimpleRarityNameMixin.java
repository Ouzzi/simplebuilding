package com.simplelib.mixin;

import com.simplelib.api.SimpleRarity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Names of {@link SimpleRarity} items (owner 21.10.). Tooltip, hotbar name and chat item all wrap {@code getHoverName()}
 * in the Vanilla rarity colour; the tier style sits on the child here and wins over it. The tooltip
 * ({@code getStyledHoverName}) additionally animates the mythic gradient on the client.
 */
@Mixin(ItemStack.class)
public abstract class SimpleRarityNameMixin {
    @Inject(method = "getHoverName", at = @At("RETURN"), cancellable = true)
    private void simplelib$rarityName(CallbackInfoReturnable<Component> cir) {
        SimpleRarity rarity = SimpleRarity.of((ItemStack) (Object) this);
        if (rarity != null) cir.setReturnValue(rarity.name(cir.getReturnValue(), false));
    }

    @Inject(method = "getStyledHoverName", at = @At("RETURN"), cancellable = true)
    private void simplelib$rarityTooltip(CallbackInfoReturnable<Component> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (SimpleRarity.of(self) != SimpleRarity.MYTHIC || !SimpleRarity.onClient()) return;
        Component custom = self.getCustomName();
        Component name = custom != null ? custom : self.getItemName();
        var animated = SimpleRarity.gradient(name.getString(), System.currentTimeMillis(), true);
        if (self.has(DataComponents.CUSTOM_NAME)) animated.withStyle(s -> s.withItalic(true));
        cir.setReturnValue(animated);
    }
}
