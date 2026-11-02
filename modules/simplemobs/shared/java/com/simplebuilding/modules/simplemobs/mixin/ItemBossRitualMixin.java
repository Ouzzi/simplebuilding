package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.BossRituals;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Boss summoning (WP-19, {@link BossRituals}): Dragon's Breath on the top of a purpur column wakes the Void Runner, a
 * Nether Star on the middle of an obsidian platform the Brimguard. A wrong figure: smoke, the item stays in hand.
 */
@Mixin(Item.class)
public abstract class ItemBossRitualMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void simplemobs$bossRitual(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        boolean runner = stack.is(Items.DRAGON_BREATH) && (state.is(Blocks.PURPUR_BLOCK) || state.is(Blocks.PURPUR_PILLAR));
        boolean brim = stack.is(Items.NETHER_STAR) && (state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN));
        if (!runner && !brim) return;
        if (!(runner ? BossRituals.runnerMatches(level, pos) : BossRituals.brimMatches(level, pos))) {
            BossRituals.fizzle(level, pos);
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }
        boolean done = !level.isClientSide() && (runner ? BossRituals.trySummonRunner(level, pos) : BossRituals.trySummonBrimguard(level, pos));
        if (done && context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
            stack.shrink(1);
            if (runner) {
                ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                if (!context.getPlayer().getInventory().add(bottle) && level instanceof net.minecraft.server.level.ServerLevel sl) context.getPlayer().spawnAtLocation(sl, bottle);
            }
        }
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
