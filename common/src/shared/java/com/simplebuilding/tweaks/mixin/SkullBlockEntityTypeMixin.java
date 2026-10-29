package com.simplebuilding.tweaks.mixin;

import com.simplebuilding.tweaks.block.BlazeHeadType;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Der Lohenkopf nutzt Vanillas {@code SkullBlockEntity} (Notenblock-Klang, Renderer, Tragen auf dem
 * Kopf), deren Typ {@code minecraft:skull} aber nur die Vanilla-Koepfe als gueltige Bloecke kennt -
 * ohne diesen Eintrag verweigert {@code BlockEntity#validateBlockState} das Setzen. Ein Mixin statt
 * Loader-API (Fabric {@code addSupportedBlock}, NeoForge {@code BlockEntityTypeAddBlocksEvent}),
 * damit alle Loader denselben Weg gehen.
 */
@Mixin(BlockEntityType.class)
public abstract class SkullBlockEntityTypeMixin {

    @Inject(method = "isValid", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$acceptModSkulls(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this == BlockEntityTypes.SKULL && state.getBlock() instanceof AbstractSkullBlock skull
                && skull.getType() instanceof BlazeHeadType) {
            cir.setReturnValue(true);
        } else if ((Object) this == BlockEntityTypes.CAMPFIRE && state.getBlock() == com.simplebuilding.blocks.custom.SulfurLights.CAMPFIRE) {
            cir.setReturnValue(true); // the sulfur campfire (N58) uses Vanilla's campfire block entity
        } else if ((Object) this == BlockEntityTypes.CHISELED_BOOKSHELF && com.simplebuilding.version.McVersion.ASTRAL_ENCHANTING
                && com.simplebuilding.blocks.ModBlocks.CHISELED_BLAZEWOOD_BOOKSHELVES.contains(state.getBlock())) {
            cir.setReturnValue(true); // the chiseled blazewood bookshelves (BS25-1) use Vanilla's chiseled bookshelf block entity
        }
    }
}
