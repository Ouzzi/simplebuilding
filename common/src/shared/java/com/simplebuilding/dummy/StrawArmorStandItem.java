package com.simplebuilding.dummy;

import com.simplebuilding.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PostSpawnProcessor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Stellt einen Stroh-Ruestungsstaender auf; Ablauf wie Vanillas {@code ArmorStandItem}. */
public class StrawArmorStandItem extends Item {
    public StrawArmorStandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getClickedFace() == Direction.DOWN) {
            return InteractionResult.FAIL;
        }
        Level level = context.getLevel();
        BlockPos pos = new BlockPlaceContext(context).getClickedPos();
        ItemStack stack = context.getItemInHand();
        Vec3 bottom = Vec3.atBottomCenterOf(pos);
        EntityType<TrainingDummy> type = ModEntities.STRAW_ARMOR_STAND;
        AABB box = type.getDimensions().makeBoundingBox(bottom.x(), bottom.y(), bottom.z());
        if (!level.noCollision(null, box) || !level.getEntities(null, box).isEmpty()) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel server) {
            PostSpawnProcessor<TrainingDummy> config = EntityType.createDefaultStackConfig(server, stack, context.getPlayer());
            TrainingDummy entity = type.create(server, config, pos, EntitySpawnReason.SPAWN_ITEM_USE, true, true);
            if (entity == null) {
                return InteractionResult.FAIL;
            }
            float yRot = Mth.floor((Mth.wrapDegrees(context.getRotation() - 180.0F) + 22.5F) / 45.0F) * 45.0F;
            entity.snapTo(entity.getX(), entity.getY(), entity.getZ(), yRot, 0.0F);
            server.addFreshEntityWithPassengers(entity);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 0.75F, 0.8F);
            entity.gameEvent(GameEvent.ENTITY_PLACE, context.getPlayer());
        }
        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
            java.util.function.Consumer<net.minecraft.network.chat.Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        lines.accept(net.minecraft.network.chat.Component.translatable("tooltip.simplebuilding.straw_armor_stand")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
