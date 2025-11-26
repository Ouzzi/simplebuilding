package com.simplebuilding.mixin.client;

import com.simplebuilding.util.IEnchantableShulkerBox;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShulkerBoxBlockEntity.class)
public class ShulkerBoxTickMixin {

    @Inject(
            method = "tick",
            at = @At("TAIL")
    )
    private static void spawnMagicParticles(World world, BlockPos pos, BlockState state, ShulkerBoxBlockEntity blockEntity, CallbackInfo ci) {

        if (world.isClient()) {

            if (world.random.nextInt(5) == 0) {

                if (blockEntity instanceof IEnchantableShulkerBox enchantableBox) {

                    var enchants = enchantableBox.simplebuilding$getEnchantments();
                    if (enchants != null && !enchants.isEmpty()) {
                        for (int i = 0; i < 6; i++) {
                            double x = pos.getX() + 0.5 + (world.random.nextDouble() - 0.5) * 1.1;
                            double y = pos.getY() + 0.5 + (world.random.nextDouble() - 0.5) * 1.1;
                            double z = pos.getZ() + 0.5 + (world.random.nextDouble() - 0.5) * 1.1;

                            world.addParticleClient(
                                    ParticleTypes.ENCHANT,
                                    x, y, z,
                                    0.0, 0.05, 0.0
                            );
                        }
                        if (world.getTime() % 70 == 0) System.out.println("Client: Shulker Enchants: " + enchants);
                    } else {
                        if (world.getTime() % 70 == 0) System.out.println("Client: Shulker has no enchants!");
                    }
                }

            }
        }
    }
}