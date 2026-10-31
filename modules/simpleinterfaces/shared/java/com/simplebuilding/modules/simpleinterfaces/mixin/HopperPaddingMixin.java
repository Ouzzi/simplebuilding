package com.simplebuilding.modules.simpleinterfaces.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.simplebuilding.modules.simpleinterfaces.client.StyledScreens;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * IG31-30 (owner 31.10.): the Vanilla hopper in the Simple style gets the room under its slot row that SimpleBuilding's mod
 * hoppers have (owner 20.10. "zu wenig padding unten", {@code ModHopperScreenHandler.INVENTORY_SHIFT}): the player's slots of
 * this screen's (client) menu sit {@link #SHIFT} px lower and the image grows by as much, so the container box keeps 6 px
 * under the five slots instead of touching its bottom frame. Only when the hopper style is on as the screen opens; with the
 * style switched off while it is open, hopper.png is drawn stretched by the shift (the plain rows under the slots repeated),
 * as the mod hoppers do. Slot indices stay; only the client's positions move.
 */
@Mixin(HopperScreen.class)
public abstract class HopperPaddingMixin extends AbstractContainerScreen<HopperMenu> {
    @Unique
    private static final int SHIFT = 6;
    @Unique
    private boolean simpleinterfaces$shifted;

    private HopperPaddingMixin(HopperMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    /** Vanilla's image height 133 (final field, set by the super constructor): 6 px more while the hopper style is on. */
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 133))
    private static int simpleinterfaces$height(int height) {
        return StyledScreens.hopperStyleOn() ? height + SHIFT : height;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void simpleinterfaces$shift(HopperMenu menu, Inventory inventory, Component title, CallbackInfo ci) {
        if (this.imageHeight != 133 + SHIFT) return;
        for (int index = HopperMenu.CONTAINER_SIZE; index < menu.slots.size(); index++) {
            Slot vanilla = menu.slots.get(index);
            Slot moved = new Slot(vanilla.container, vanilla.getContainerSlot(), vanilla.x, vanilla.y + SHIFT);
            moved.index = vanilla.index;
            menu.slots.set(index, moved);
        }
        this.simpleinterfaces$shifted = true;
    }

    @WrapWithCondition(method = "extractBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
    private boolean simpleinterfaces$stretched(GuiGraphicsExtractor g, RenderPipeline pipeline, Identifier texture,
            int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        if (!this.simpleinterfaces$shifted || StyledScreens.isStyled(this)) return true; // styled: StorageBackgroundMixin skips it
        g.blit(pipeline, texture, x, y, 0, 0, width, 38, textureWidth, textureHeight);
        g.blit(pipeline, texture, x, y + 38, 0, 38, width, SHIFT, textureWidth, textureHeight);
        g.blit(pipeline, texture, x, y + 38 + SHIFT, 0, 38, width, 133 - 38, textureWidth, textureHeight);
        return false;
    }
}
