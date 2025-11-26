package com.simplebuilding.items.custom;

import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.particle.ModParticles;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;

import java.util.Map;
import java.util.function.Consumer;

// TODO
// Entchantments:
// - Fast Chiseling I, II: Reduziert den Cooldown
// - Constructor's Touch: Fügt weitere Block-Transformationen hinzu
// - Range I, II: Erlaubt das Chiseln von weiter entfernten Blöcken
// - Unbreaking I, II, III: Reduziert die Abnutzung
// - Mending: Repariert den Chisel mit gesammelten XP

public class ChiselItem extends Item {

    private Map<Block, Block> transformationMap;
    private SoundEvent chiselSound = SoundEvents.UI_STONECUTTER_TAKE_RESULT;
    private int cooldownTicks = 100;

    private static final Map<Block, Block> DEFAULT_CHISEL_MAP = Map.of(
        Blocks.STONE, Blocks.STONE_BRICKS,
        Blocks.STONE_BRICKS, Blocks.CHISELED_STONE_BRICKS,
        Blocks.CHISELED_STONE_BRICKS, Blocks.COBBLESTONE
    );

    public ChiselItem(Settings settings) {
        super(settings);
        this.transformationMap = DEFAULT_CHISEL_MAP;
    }

    /**
     * Setzt die Transformation Map für diese Chisel/Spatula Instanz.
     */
    public void setTransformationMap(Map<Block, Block> map) {
        this.transformationMap = map;
    }

    /**
     * Setzt den Cooldown in Ticks für diese Item-Instanz.
     */
    public void setCooldownTicks(int ticks) {
        this.cooldownTicks = ticks;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        Block clickedBlock = world.getBlockState(context.getBlockPos()).getBlock();
        PlayerEntity player = context.getPlayer();

        // Wir holen den Stack, der gerade benutzt wird
        ItemStack usedStack = context.getStack();

        // 1. Cooldown-Prüfung (Muss mit dem Stack erfolgen, da Item nicht erkannt wird)
        if (player != null && player.getItemCooldownManager().isCoolingDown(new ItemStack(this))) {
            return ActionResult.PASS;
        }

        if(this.transformationMap.containsKey(clickedBlock)) {
            if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
                RegistryWrapper.WrapperLookup registryManager = context.getWorld().getRegistryManager();
                RegistryEntry<Enchantment> fastChiselingEntry = registryManager
                        .getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT)
                        .getOrThrow(com.simplebuilding.enchantment.ModEnchantments.FAST_CHISELING);

                int fastChiselingLevel = EnchantmentHelper.getLevel(fastChiselingEntry, context.getStack());
                int finalCooldownTicks = this.cooldownTicks;

                if (fastChiselingLevel > 0) {
                    // Beispiel: Reduziert Cooldown um 15% pro Level
                    finalCooldownTicks = Math.max(1, finalCooldownTicks - (finalCooldownTicks * fastChiselingLevel * 15 / 100));
                }

                // Cooldown setzen (mit neu berechnetem Wert)
                if (!player.getAbilities().creativeMode && finalCooldownTicks > 0) {
                    player.getItemCooldownManager().set(new ItemStack(this), finalCooldownTicks);
                }

                context.getStack().damage(1, ((ServerWorld) world), ((ServerPlayerEntity) context.getPlayer()),
                        item -> context.getPlayer().sendEquipmentBreakStatus(item, EquipmentSlot.MAINHAND));

                world.playSound(null, context.getBlockPos(), chiselSound, SoundCategory.BLOCKS, 0.5f, 1.5f);

                ((ServerWorld) world).spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, clickedBlock.getDefaultState()),
                        context.getBlockPos().getX() + 0.5, context.getBlockPos().getY() + 1.0,
                        context.getBlockPos().getZ() + 0.5, 5, 0, 0, 0, 1);

                ((ServerWorld) world).spawnParticles(ParticleTypes.FLAME,
                        context.getBlockPos().getX() + 0.5, context.getBlockPos().getY() + 1.5,
                        context.getBlockPos().getZ() + 0.5, 10, 0, 0, 0, 3);

                ((ServerWorld) world).spawnParticles(ModParticles.PINK_GARNET_PARTICLE,
                        context.getBlockPos().getX() + 0.5, context.getBlockPos().getY() + 1.0,
                        context.getBlockPos().getZ() + 0.5, 8, 0, 0, 0, 2);

                context.getStack().set(ModDataComponentTypes.COORDINATES, context.getBlockPos());
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        //todo: make it look better
        if(stack.get(ModDataComponentTypes.COORDINATES) != null) {
            textConsumer.accept(
                    Text.literal(stack.get(ModDataComponentTypes.COORDINATES).getX() + ", "
                    + stack.get(ModDataComponentTypes.COORDINATES).getY() + ", "
                    + stack.get(ModDataComponentTypes.COORDINATES).getZ())
            );
        }

        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }

    public void setChiselSound(SoundEvent chiselSound) {
        this.chiselSound = chiselSound;
    }


}
