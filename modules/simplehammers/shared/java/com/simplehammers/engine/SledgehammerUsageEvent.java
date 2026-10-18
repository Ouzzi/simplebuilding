package com.simplehammers.engine;

import com.simplebuilding.framework.api.Protection;
import com.simplehammers.SimpleHammers;
import com.simplehammers.item.SledgehammerItem;
import com.simplelib.api.HammerActions;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The area break: every loader calls {@link #handleBeforeBlockBreak} before a player breaks a block (Fabric
 * {@code PlayerBlockBreakEvents.BEFORE}, NeoForge/Forge {@code BlockEvent.BreakEvent}). Moved from SimpleBuilding
 * (split stage 7). While the extra blocks break, SimpleLib {@link HammerActions#areaBreaking()} is true.
 */
public class SledgehammerUsageEvent {
    private static final Set<BlockPos> HARVESTED_BLOCKS = new HashSet<>();

    protected SledgehammerUsageEvent() {
    }

    /** Returns false when the break must not happen at all (protected origin). */
    public static boolean handleBeforeBlockBreak(Level world, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        ItemStack mainHandItem = player.getMainHandItem();
        if (!(mainHandItem.getItem() instanceof SledgehammerItem) || !(player instanceof ServerPlayer serverPlayer)) return true;
        // A permission probe of another mod (no real break) or the module switched off: no area break.
        if (HammerActions.areaBreaking() || HammerActions.areaVetoed() || !SimpleHammers.on()) return true;
        if (!HammerProtection.mayAct(world, player, pos, Protection.Action.DESTROY)) return false;
        if (HARVESTED_BLOCKS.contains(pos)) {
            return true;
        }

        BlockState originState = world.getBlockState(pos);
        int overrideLevel = HammerEnchantments.level(mainHandItem, world, HammerEnchantments.OVERRIDE);
        for (BlockPos position : SledgehammerItem.getBlocksToBeDestroyed(1, pos, serverPlayer)) {
            if (pos.equals(position)) {
                continue;
            }

            BlockState targetState = world.getBlockState(position);

            if (!SledgehammerUtils.shouldBreak(world, position, originState, mainHandItem, overrideLevel)) {
                continue;
            }
            // Vanilla-Spawnschutz und Weltgrenze: destroyBlock prueft beides nicht.
            if (!world.mayInteract(serverPlayer, position) || !HammerProtection.mayAct(world, player, position, Protection.Action.DESTROY)) {
                continue;
            }

            HARVESTED_BLOCKS.add(position);
            HammerActions.areaBreaking(true);
            try {
                boolean wasBroken = serverPlayer.gameMode.destroyBlock(position);
                if (wasBroken) {
                    HammerActions.fire(HammerActions.AREA_BLOCK, serverPlayer);
                    // Die Grundabnutzung (SledgehammerItem#WEAR_PER_BLOCK) hat destroyBlock ueber
                    // mineBlock schon abgezogen; das falsche Werkzeug kostet einen Punkt mehr.
                    boolean isSuitable = mainHandItem.getItem().isCorrectToolForDrops(mainHandItem, targetState);
                    if (!isSuitable && !mainHandItem.isEmpty()) {
                        mainHandItem.hurtAndBreak(1, serverPlayer, EquipmentSlot.MAINHAND);
                    }
                    if (mainHandItem.isEmpty()) {
                        break;
                    }
                }
            } finally {
                HammerActions.areaBreaking(false);
                HARVESTED_BLOCKS.remove(position);
            }
        }
        return true;
    }
}
