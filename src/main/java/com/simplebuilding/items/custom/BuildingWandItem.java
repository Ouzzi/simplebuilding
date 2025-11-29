package com.simplebuilding.items.custom;

import com.simplebuilding.component.ModDataComponentTypes;
import net.minecraft.block.Block;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.function.Consumer;

// TODO
// Entchantments:
// - Color Palette: changes pickmode to random blocks from inventory (TODO)
// - Master Builder: links to shulker box or bundle to pick blocks from (requires master builder on item mabe offhand?) (TODO)
// - Range I, II: Erlaubt das messen von weiter entfernten Blöcken (TODO)
// - Unbreaking I, II, III: Reduziert die Abnutzung (done durch vanilla)
// - Mending: Repariert den Chisel mit gesammelten XP (done durch vanilla)

// Build mechanics:
// - on top/bottom of block -> place on top/bottom squareDiameter x squareDiameter (only free space, if blocked skip)
// - on side of block -> place on side squareDiameter x squareDiameter (only free space, if blocked skip)

public class BuildingWandItem extends Item {

    public static final int BUILDING_WAND_SQUARE_COPPER = 3;
    public static final int BUILDING_WAND_SQUARE_IRON = 5;
    public static final int BUILDING_WAND_SQUARE_GOLD = 7;
    public static final int BUILDING_WAND_SQUARE_DIAMOND = 7;
    public static final int BUILDING_WAND_SQUARE_NETHERITE = 9;

    public static final int DURABILITY_MULTIPLAYER_WAND = 8;

    private int wandSquareDiameter;
    private SoundEvent placeSound = SoundEvents.UI_STONECUTTER_TAKE_RESULT;
    private static final int DEFAULT_WAND_SQUARE_DAIAMETER = 1;

    public BuildingWandItem(Settings settings) {
        super(settings);
        this.wandSquareDiameter = DEFAULT_WAND_SQUARE_DAIAMETER;
    }

    public void setWandSquareDiameter(int wandSquareDiameter) {
        this.wandSquareDiameter = wandSquareDiameter;
    }

    @Override
    public net.minecraft.util.ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        Block clickedBlock = world.getBlockState(context.getBlockPos()).getBlock();
        PlayerEntity player = context.getPlayer();

        if (player != null && player.getItemCooldownManager().isCoolingDown(new ItemStack(this))) {
            return net.minecraft.util.ActionResult.PASS;
        }

        if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
            context.getStack().damage(1, ((ServerWorld) world), serverPlayer,
                    item -> serverPlayer.sendEquipmentBreakStatus(item, EquipmentSlot.MAINHAND));

            world.playSound(null, context.getBlockPos(), placeSound, SoundCategory.BLOCKS, 0.5f, 1.5f);

            ((ServerWorld) world).spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, clickedBlock.getDefaultState()),
                    context.getBlockPos().getX() + 0.5, context.getBlockPos().getY() + 1.0,
                    context.getBlockPos().getZ() + 0.5, 5, 0, 0, 0, 1);

            ((ServerWorld) world).spawnParticles(ParticleTypes.FLAME,
                    context.getBlockPos().getX() + 0.5, context.getBlockPos().getY() + 1.5,
                    context.getBlockPos().getZ() + 0.5, 10, 0, 0, 0, 3);

            context.getStack().set(ModDataComponentTypes.COORDINATES, context.getBlockPos());
        }
        return net.minecraft.util.ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        if(stack.get(ModDataComponentTypes.COORDINATES) != null) {
            textConsumer.accept(
                    Text.literal(stack.get(ModDataComponentTypes.COORDINATES).getX() + ", "
                            + stack.get(ModDataComponentTypes.COORDINATES).getY() + ", "
                            + stack.get(ModDataComponentTypes.COORDINATES).getZ())
            );
        }
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }

    public void setPlaceSound(SoundEvent placeSound) {
        this.placeSound = placeSound;
    }
}