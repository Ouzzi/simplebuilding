package com.simplebuilding.modules.simplemobs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.biome.Biome;

/**
 * Biome Brush (concept N26 B): brush in the main hand, a biome material in the off hand, right-click a block - the
 * biome around it becomes the material's biome ({@link BiomeBrush}). One material and one durability point per stroke
 * (none in creative), holding the button keeps painting (one stroke per {@link #STROKE_TICKS} ticks). Durability 512,
 * repaired with Netherite Ingots, takes Unbreaking and Mending. No text: a refused stroke puffs smoke.
 */
public class BiomeBrushItem extends Item {
    public static final int DURABILITY = 512;
    public static final int STROKE_TICKS = 5;

    public BiomeBrushItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<net.minecraft.network.chat.Component> builder, net.minecraft.world.item.TooltipFlag flag) {
        builder.accept(net.minecraft.network.chat.Component.translatable("item.simplemobs.biome_brush.tooltip").withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getHand() != InteractionHand.MAIN_HAND || context.getPlayer() == null) return InteractionResult.PASS;
        ItemStack material = context.getPlayer().getOffhandItem();
        if (material.isEmpty()) return InteractionResult.PASS;
        if (!(context.getLevel() instanceof ServerLevel level) || !(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.SUCCESS;
        return stroke(level, player, context.getClickedPos(), context.getItemInHand(), material) == BiomeBrush.Result.PAINTED
                ? InteractionResult.SUCCESS_SERVER : InteractionResult.CONSUME;
    }

    /** One stroke at {@code target} (server). Public for the GameTests. */
    public static BiomeBrush.Result stroke(ServerLevel level, ServerPlayer player, BlockPos target, ItemStack brush, ItemStack material) {
        if (!MobsRegistry.on() || !MobsConfig.biomeBrushEnabled) return fizzle(level, target, BiomeBrush.Result.DISABLED);
        if (MobsConfig.biomeBrushOpsOnly && !net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS).test(player.createCommandSourceStack()))
            return fizzle(level, target, BiomeBrush.Result.DISABLED);
        if (player.getCooldowns().isOnCooldown(brush)) return BiomeBrush.Result.UNCHANGED;
        var entry = BiomeBrush.material(level.getServer(), material);
        if (entry.isEmpty()) return fizzle(level, target, BiomeBrush.Result.NO_MATERIAL);
        Holder<Biome> biome = BiomeBrush.biome(level, entry.get().biome());
        if (biome == null || !BiomeBrush.fitsDimension(level, biome)) return fizzle(level, target, BiomeBrush.Result.WRONG_DIMENSION);
        if (!BiomeBrush.centreAllowed(level, player, target)) return fizzle(level, target, BiomeBrush.Result.DENIED);
        player.getCooldowns().addCooldown(brush, STROKE_TICKS);
        int changed = BiomeBrush.paint(level, player, target, biome, MobsConfig.biomeBrushRadius);
        if (changed <= 0) return BiomeBrush.Result.UNCHANGED;
        if (!player.hasInfiniteMaterials()) {
            material.shrink(1);
            brush.hurtAndBreak(1, player, InteractionHand.MAIN_HAND);
        }
        level.playSound(null, target, SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, 1.0F, 0.8F + level.getRandom().nextFloat() * 0.3F);
        level.sendParticles(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF000000 | entry.get().color()),
                target.getX() + 0.5, target.getY() + 1.1, target.getZ() + 0.5, 24, 2.0, 0.6, 2.0, 0.0);
        return BiomeBrush.Result.PAINTED;
    }

    private static BiomeBrush.Result fizzle(ServerLevel level, BlockPos target, BiomeBrush.Result why) {
        level.sendParticles(ParticleTypes.SMOKE, target.getX() + 0.5, target.getY() + 1.1, target.getZ() + 0.5, 6, 0.2, 0.1, 0.2, 0.01);
        level.playSound(null, target, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.3F, 1.6F);
        return why;
    }
}
