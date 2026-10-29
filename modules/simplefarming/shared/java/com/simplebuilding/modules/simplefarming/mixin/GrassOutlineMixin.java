package com.simplebuilding.modules.simplefarming.mixin;

import com.simplebuilding.modules.simplefarming.SimpleFarming;
import com.simplebuilding.modules.simplefarming.VegetationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// WICHTIG: Wir greifen jetzt in den BlockState ein, nicht mehr in den Block direkt.
// Das fängt alle Blöcke ab, auch die, die eigene Formen definieren.
@Mixin(BlockBehaviour.BlockStateBase.class)
public class GrassOutlineMixin {

    @Inject(method = "getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;", at = @At("HEAD"), cancellable = true)
    private void removeOutlineForSharpness(BlockGetter world, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {


        if (!(context instanceof EntityCollisionContext entityContext) || !(entityContext.getEntity() instanceof Player player)) {
            return;
        }

        if (!SimpleFarming.active(player.level()) || !SimpleFarming.configFor(player.level()).sharpnessCutsGrass) return;
        if (player.isShiftKeyDown()) {
            return;
        }

        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.isEmpty()) return;

        if (!(mainHand.is(ItemTags.SWORDS) || mainHand.is(ItemTags.AXES))) {
            return;
        }

        // getShape is a hot path (outlines, every ray clip): test the state before the enchantment lookup (REV2-A).
        if (!VegetationUtil.isCuttable((BlockState) (Object) this)) return;

        var registryManager = player.level().registryAccess();
        var enchantmentRegistry = registryManager.lookup(Registries.ENCHANTMENT);

        if (enchantmentRegistry.isPresent()) {
            var sharpnessEntry = enchantmentRegistry.get().get(Enchantments.SHARPNESS);

            if (sharpnessEntry.isPresent()) {
                int level = EnchantmentHelper.getItemEnchantmentLevel(sharpnessEntry.get(), mainHand);

                if (VegetationUtil.passes(VegetationUtil.chancePercent(level, SimpleFarming.configFor(player.level())),
                        pos.asLong(), player.getId(), player.level().getGameTime())) {
                    cir.setReturnValue(Shapes.empty());
                }
            }
        }
    }
}
