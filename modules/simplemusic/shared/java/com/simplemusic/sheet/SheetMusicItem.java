package com.simplemusic.sheet;

import com.simplemusic.SimpleMusic;
import com.simplemusic.registry.MusicComponents;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Sheet of music (concept §3): right-click opens the note code editor (like the blueprint), signing makes it read-only
 * with title and author. The code lives in the {@code simplemusic:sheet} component; the Orchestrion plays it.
 */
public class SheetMusicItem extends Item {
    public SheetMusicItem(Properties properties) {
        super(properties);
    }

    public static SheetContent content(ItemStack stack) {
        SheetContent c = stack.get(MusicComponents.SHEET);
        return c == null ? SheetContent.EMPTY : c;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!SimpleMusic.on()) return InteractionResult.PASS;
        if (level.isClientSide()) SimpleMusic.openSheet.accept(hand == InteractionHand.MAIN_HAND ? 0 : 1);
        return InteractionResult.SUCCESS;
    }

    @Override
    public Component getName(ItemStack stack) {
        SheetContent content = content(stack);
        if (content.signed() && !content.title().isBlank()) return Component.literal(content.title());
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        SheetContent content = content(stack);
        if (content.signed() && !content.author().isBlank()) {
            lines.accept(Component.translatable("book.byAuthor", content.author()).withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, display, lines, flag);
    }
}
