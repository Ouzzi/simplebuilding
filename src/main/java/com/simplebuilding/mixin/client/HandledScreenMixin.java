package com.simplebuilding.mixin.client;

import com.simplebuilding.client.gui.tooltip.ReinforcedBundleTooltipSubmenuHandler;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.TooltipSubmenuHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin extends Screen {

    protected HandledScreenMixin(Text title) {
        super(title);
    }

    // Zugriff auf die geschÃ¼tzte Methode in HandledScreen, um Handler hinzuzufÃ¼gen
    @Shadow
    protected abstract void addTooltipSubmenuHandler(TooltipSubmenuHandler handler);

    /**
     * Wir injizieren uns in die 'init' Methode.
     * Dort fÃ¼gt Vanilla seinen Bundle-Handler hinzu. Wir fÃ¼gen einfach unseren dazu.
     * Minecraft geht dann beim Scrollen die Liste durch und prÃ¼ft 'isApplicableTo'.
     */
    @Inject(method = "init", at = @At("TAIL"))
    private void simplebuilding$addCustomBundleHandler(CallbackInfo ci) {
        if (this.client != null) {
            this.addTooltipSubmenuHandler(new ReinforcedBundleTooltipSubmenuHandler(this.client));
        }
    }
}