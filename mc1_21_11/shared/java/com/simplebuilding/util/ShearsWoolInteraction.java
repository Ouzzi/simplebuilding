package com.simplebuilding.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Schere (Rechtsklick) auf einem platzierten Wollblock: der Block verschwindet und laesst
 * {@value #STRING_PER_WOOL} Faeden fallen, die Schere verliert {@value #SHEARS_DAMAGE} Haltbarkeit.
 *
 * <p>Laeuft ueber {@code ShearsItemMixin} am Kopf von {@code ShearsItem#useOn} und ist damit auf
 * Fabric, NeoForge und Forge und auf beiden MC-Linien derselbe Code. Das Wiki liest die Zahlen
 * ueber {@code InWorldTransformations#shearWool}.
 */
public final class ShearsWoolInteraction {

    /** Faeden je geschorenem Wollblock - so viele, wie ein Wollblock im Rezept kostet. */
    public static final int STRING_PER_WOOL = 4;
    /** Haltbarkeit je geschorenem Wollblock. */
    public static final int SHEARS_DAMAGE = 1;

    private ShearsWoolInteraction() {
    }

    /**
     * @return {@code null}, wenn der angeklickte Block keine Wolle ist (dann bleibt es bei
     *         Vanillas {@code useOn}), sonst {@link InteractionResult#SUCCESS}.
     */
    public static @Nullable InteractionResult tryShearWool(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(BlockTags.WOOL)) {
            return null;
        }

        if (level instanceof ServerLevel serverLevel) {
            Player player = context.getPlayer();
            ItemStack shears = context.getItemInHand();

            level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
            serverLevel.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, state),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    20, 0.25, 0.25, 0.25, 0.05);
            level.removeBlock(pos, false);
            level.gameEvent(player, GameEvent.SHEAR, pos);
            Block.popResource(level, pos, new ItemStack(Items.STRING, STRING_PER_WOOL));

            if (player != null) {
                shears.hurtAndBreak(SHEARS_DAMAGE, player, context.getHand().asEquipmentSlot());
            }
        }
        return InteractionResult.SUCCESS;
    }
}
