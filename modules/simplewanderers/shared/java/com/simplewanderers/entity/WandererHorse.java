package com.simplewanderers.entity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The wanderer's horse: a normal Horse (Vanilla coat variants and renderer) that hangs on the wanderer's leash. Owner answer
 * W4: it can be captured. While it hangs on the wanderer nobody rides it; sneaking and right-clicking with an empty main hand
 * moves the leash to the player (shears or a lead work as in Vanilla). From then on it is a wild horse like any other.
 */
public class WandererHorse extends Horse {
    public WandererHorse(EntityType<? extends Horse> type, Level level) {
        super(type, level);
    }

    public boolean onWanderer() {
        return getLeashHolder() instanceof VillageWanderer;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!onWanderer()) return super.mobInteract(player, hand);
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        if (hand == InteractionHand.MAIN_HAND && player.isSecondaryUseActive() && player.getMainHandItem().isEmpty()) {
            capture(player);
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.CONSUME;
    }

    /** The leash moves from the wanderer to the player; the wanderer notices on his next upkeep and starts the replacement timer. */
    public void capture(Player player) {
        setLeashedTo(player, true);
        playSound(net.minecraft.sounds.SoundEvents.LEAD_TIED);
    }

    @Override
    protected void doPlayerRide(Player player) {
        if (onWanderer()) return;
        super.doPlayerRide(player);
    }

    @Override
    public double leashSnapDistance() {
        return onWanderer() ? 20.0 : super.leashSnapDistance();
    }

    @Override
    public void leashTooFarBehaviour() {
        if (getLeashHolder() instanceof VillageWanderer holder) {
            // Never lose the horse because the wanderer walked on or a chunk lagged: it follows him.
            teleportTo(holder.getX() + 1.5, holder.getY(), holder.getZ() + 1.5);
            return;
        }
        super.leashTooFarBehaviour();
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }
}
