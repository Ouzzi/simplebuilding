package com.simplebuilding.entity.vehicle;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/**
 * Places a tiered cart on a rail, like vanilla's {@code MinecartItem}. The entity type comes from a supplier: the
 * items are registered before the entity types on NeoForge and Forge.
 */
public class TieredMinecartItem extends Item {
    private final Supplier<? extends EntityType<? extends AbstractMinecart>> type;

    public TieredMinecartItem(Properties properties, Supplier<? extends EntityType<? extends AbstractMinecart>> type) {
        super(properties);
        this.type = type;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(BlockTags.RAILS)) {
            return InteractionResult.FAIL;
        }
        ItemStack stack = context.getItemInHand();
        RailShape shape = state.getBlock() instanceof BaseRailBlock rail ? state.getValue(rail.getShapeProperty()) : RailShape.NORTH_SOUTH;
        Vec3 spawn = new Vec3(pos.getX() + 0.5, pos.getY() + 0.0625 + (shape.isSlope() ? 0.5 : 0.0), pos.getZ() + 0.5);
        AbstractMinecart cart = AbstractMinecart.createMinecart(level, spawn.x, spawn.y, spawn.z, this.type.get(),
                EntitySpawnReason.DISPENSER, stack, context.getPlayer());
        if (cart == null) {
            return InteractionResult.FAIL;
        }
        if (AbstractMinecart.useExperimentalMovement(level)) {
            for (Entity entity : level.getEntities(null, cart.getBoundingBox())) {
                if (entity instanceof AbstractMinecart) {
                    return InteractionResult.FAIL;
                }
            }
        }
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.addFreshEntity(cart);
            serverLevel.gameEvent(GameEvent.ENTITY_PLACE, pos, GameEvent.Context.of(context.getPlayer(), serverLevel.getBlockState(pos.below())));
        }
        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }
}
