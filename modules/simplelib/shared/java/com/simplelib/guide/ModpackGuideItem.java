package com.simplelib.guide;

import com.simplelib.api.ModpackGuide;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * The Simple Modpack guide ({@link ModpackGuide}). Using it opens the book screen on the client (the loader's client entry
 * sets the opener) and lets the mods refresh their own bookmarks on the server ({@link ModpackGuide#onUse}). The tooltip
 * says one line only - which chapters are open shows in the book (owner 03.11.).
 */
public class ModpackGuideItem extends Item {
    private static BiConsumer<ItemStack, InteractionHand> clientOpener = (stack, hand) -> {
    };

    public ModpackGuideItem(Properties properties) {
        super(properties);
    }

    /** The client entry hands in the screen; a dedicated server leaves it empty. */
    public static void setClientOpener(BiConsumer<ItemStack, InteractionHand> opener) {
        clientOpener = opener != null ? opener : (stack, hand) -> {
        };
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) clientOpener.accept(player.getItemInHand(hand).copy(), hand);
        if (player instanceof ServerPlayer serverPlayer) ModpackGuide.onUse(serverPlayer);
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.simplelib.modpack_guide").withStyle(ChatFormatting.GRAY));
    }
}
