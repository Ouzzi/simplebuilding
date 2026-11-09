package com.simplebuilding.modules.simpleend;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HangingEntityItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/** The End Painting: Vanilla's painting placing, but the picture comes from the End variants ({@link EndPaintings#TAG}). */
public class EndPaintingItem extends HangingEntityItem {
    public EndPaintingItem(Item.Properties properties) {
        super(EntityTypes.PAINTING, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Direction face = context.getClickedFace();
        BlockPos at = context.getClickedPos().relative(face);
        Player player = context.getPlayer();
        ItemStack inHand = context.getItemInHand();
        if (player != null && !mayPlace(player, face, inHand, at)) return InteractionResult.FAIL;
        Level level = context.getLevel();
        Optional<Painting> painting = EndPaintings.create(level, at, face);
        if (painting.isEmpty()) return InteractionResult.CONSUME;
        Painting entity = painting.get();
        EntityType.<HangingEntity>createDefaultStackConfig(level, inHand, player).apply(entity);
        if (!entity.survives()) return InteractionResult.CONSUME;
        if (!level.isClientSide()) {
            entity.playPlacementSound();
            level.gameEvent(player, GameEvent.ENTITY_PLACE, entity.position());
            level.addFreshEntity(entity);
        }
        inHand.shrink(1);
        return InteractionResult.SUCCESS;
    }
}
