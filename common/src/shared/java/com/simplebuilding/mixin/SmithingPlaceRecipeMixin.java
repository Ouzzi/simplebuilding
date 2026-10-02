package com.simplebuilding.mixin;

import com.simplebuilding.recipe.SmithingPlacement;
import com.simplebuilding.version.McVersion;
import net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SmithingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Rezeptbuch am Schmiedetisch, Serverseite: Vanilla platziert nur in {@link RecipeBookMenu}s. Ist das offene Menue ein
 * {@link SmithingMenu}, gelten dieselben Pruefungen wie bei Vanilla (Container-ID, Zuschauer, gueltiges Menue, Rezept dem
 * Spieler bekannt, platzierbar) und {@link SmithingPlacement} legt ein. Laeuft erst nach dem Thread-Wechsel.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class SmithingPlaceRecipeMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handlePlaceRecipe", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V", shift = At.Shift.AFTER), cancellable = true)
    private void simplebuilding$placeSmithingRecipe(ServerboundPlaceRecipePacket packet, CallbackInfo ci) {
        if (!McVersion.SMITHING_RECIPE_BOOK || !(this.player.containerMenu instanceof SmithingMenu menu)) {
            return;
        }
        ci.cancel();
        if (this.player.isSpectator() || menu.containerId != packet.containerId() || !menu.stillValid(this.player)) {
            return;
        }
        RecipeManager.ServerDisplayInfo info = this.player.level().getServer().getRecipeManager().getRecipeFromDisplay(packet.recipe());
        if (info == null) {
            return;
        }
        RecipeHolder<?> recipe = info.parent();
        if (!this.player.getRecipeBook().contains(recipe.id()) || !(recipe.value() instanceof SmithingRecipe smithing)
                || recipe.value().placementInfo().isImpossibleToPlace()) {
            return;
        }
        if (SmithingPlacement.place(menu, smithing, this.player.getInventory(), packet.useMaxItems()) == RecipeBookMenu.PostPlaceAction.PLACE_GHOST_RECIPE) {
            this.player.connection.send(new ClientboundPlaceGhostRecipePacket(menu.containerId, info.display().display()));
        }
        menu.broadcastChanges();
    }
}
