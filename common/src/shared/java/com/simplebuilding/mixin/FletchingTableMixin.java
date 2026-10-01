package com.simplebuilding.mixin;

import com.simplebuilding.fletching.FletchingMenu;
import com.simplebuilding.version.McVersion;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Der Vanilla-Befiederungstisch ist ein einfacher Block ohne eigenes {@code useWithoutItem}; der Rechtsklick landet
 * deshalb hier in {@link BlockBehaviour} und oeffnet das {@link FletchingMenu} (B14, nur Hauptlinie).
 */
@Mixin(BlockBehaviour.class)
public abstract class FletchingTableMixin {
    private static final Component SIMPLEBUILDING$TITLE = Component.translatable("container.simplebuilding.fletching");

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$openFletching(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit,
                                              CallbackInfoReturnable<InteractionResult> cir) {
        if (!McVersion.FLETCHING || !state.is(Blocks.FLETCHING_TABLE)) {
            return;
        }
        if (!level.isClientSide()) {
            player.openMenu(new SimpleMenuProvider((containerId, inventory, p) ->
                    new FletchingMenu(containerId, inventory, ContainerLevelAccess.create(level, pos)), SIMPLEBUILDING$TITLE));
        }
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
