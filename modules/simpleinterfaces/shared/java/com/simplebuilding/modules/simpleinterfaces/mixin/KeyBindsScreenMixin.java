package com.simplebuilding.modules.simpleinterfaces.mixin;

import com.simplebuilding.modules.simpleinterfaces.client.KeysScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The Vanilla controls page stays as it is; this adds one button to the key binding page of Simple Interfaces (WP-25). */
@Mixin(KeyBindsScreen.class)
public abstract class KeyBindsScreenMixin extends Screen {
    private KeyBindsScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void simpleinterfaces$keysButton(CallbackInfo ci) {
        this.addRenderableWidget(Button.builder(Component.translatable("simpleinterfaces.keys.open"),
                button -> this.minecraft.setScreenAndShow(new KeysScreen(this))).bounds(6, 6, 96, 20).build());
    }
}
