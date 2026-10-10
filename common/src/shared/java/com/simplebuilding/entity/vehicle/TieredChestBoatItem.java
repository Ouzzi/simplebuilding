package com.simplebuilding.entity.vehicle;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Places a tiered chest boat of the stack's wood ({@link BoatWoods}), like vanilla's {@code BoatItem}. The tooltip
 * names the wood by vanilla's chest boat of that wood.
 */
public class TieredChestBoatItem extends Item {
    private final Supplier<? extends EntityType<? extends TieredChestBoat>> type;

    public TieredChestBoatItem(Properties properties, Supplier<? extends EntityType<? extends TieredChestBoat>> type) {
        super(properties);
        this.type = type;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() == HitResult.Type.MISS) {
            return InteractionResult.PASS;
        }
        Vec3 view = player.getViewVector(1.0F);
        List<Entity> entities = level.getEntities(player, player.getBoundingBox().expandTowards(view.scale(5.0)).inflate(1.0),
                EntitySelector.CAN_BE_PICKED);
        Vec3 eye = player.getEyePosition();
        for (Entity entity : entities) {
            AABB box = entity.getBoundingBox().inflate(entity.getPickRadius());
            if (box.contains(eye)) {
                return InteractionResult.PASS;
            }
        }
        if (!(hit instanceof BlockHitResult)) {
            return InteractionResult.PASS;
        }
        TieredChestBoat boat = this.type.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (boat == null) {
            return InteractionResult.FAIL;
        }
        Vec3 location = hit.getLocation();
        boat.setInitialPos(location.x, location.y, location.z);
        if (level instanceof ServerLevel serverLevel) {
            EntityType.<TieredChestBoat>createDefaultStackConfig(serverLevel, stack, player).apply(boat);
        }
        boat.setWood(BoatWoods.of(stack));
        boat.setYRot(player.getYRot());
        if (!level.noCollision(boat, boat.getBoundingBox())) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            level.addFreshEntity(boat);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, location);
            stack.consume(1, player);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("tooltip.simplebuilding.tiered_chest_boat.wood",
                BoatWoods.chestBoat(BoatWoods.of(stack)).getName()).withStyle(ChatFormatting.GRAY));
    }
}
