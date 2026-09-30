package com.simplequalityoflife.mixin;

import com.simplequalityoflife.Simplequalityoflife;
import com.simplequalityoflife.util.VegetationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class SharpnessGrassCutMixin {

    @Inject(method = "attack", at = @At("HEAD"))
    private void onAttack(Entity target, CallbackInfo ci) {
        if (!Simplequalityoflife.getConfig().qOL.sharpnessCutsGrass) return;

        Player player = (Player) (Object) this;
        // attack() runs on both sides; breaking blocks on the client desyncs and spawns ghost drops.
        if (player.level().isClientSide()) return;
        ItemStack mainHand = player.getMainHandItem();
        boolean isWeapon = mainHand.is(ItemTags.SWORDS) || mainHand.is(ItemTags.AXES);

        if (isWeapon) {
            var registry = player.level().registryAccess().lookup(Registries.ENCHANTMENT);
            if (registry.isPresent()) {
                var sharpness = registry.get().get(Enchantments.SHARPNESS);

                if (sharpness.isPresent()) {
                    int level = EnchantmentHelper.getItemEnchantmentLevel(sharpness.get(), mainHand);
                    if (level >= 3) {
                        cutGrassAround(player, target);
                    }
                }
            }
        }
    }

    @Unique
    private void cutGrassAround(Player player, Entity target) {
        if (player.isShiftKeyDown() || player.distanceToSqr(target) > 4.5 * 4.5) return;
        if (!com.simplequalityoflife.event.InteractionGuard.action(player)) return;
        AABB box = target.getBoundingBox().intersect(player.getBoundingBox().inflate(2));
        BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
        BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);

        int checked = 0;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (++checked > 27) break;
            if (!com.simplequalityoflife.event.InteractionGuard.mayChange(player, pos)) continue;
            BlockState state = player.level().getBlockState(pos);

            if (VegetationUtil.isCuttable(state)) {
                // Block abbauen und Drops fallen lassen (true)
                player.level().destroyBlock(pos, true, player, 512);
            }
        }
    }
}
