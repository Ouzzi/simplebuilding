package com.simplemusic.disc;

import com.simplemusic.registry.MusicComponents;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A pressed disc (disc press, owner 21.10.): carries either a {@link TrackRef} (hash, title, length, uploader) of an
 * uploaded track or the composition of a sheet ({@code simplemusic:sheet}). Both play - owner decision U1 b - in the
 * Vanilla jukebox; the Pocket Jukebox takes only track discs. A disc whose track the server
 * deleted is a "scratched disc" and stays silent.
 */
public class CustomDiscItem extends Item {
    public CustomDiscItem(Properties properties) {
        super(properties);
    }

    public static TrackRef track(ItemStack stack) {
        return stack.get(MusicComponents.TRACK);
    }

    /** The pressed composition, or null. */
    public static com.simplemusic.sheet.SheetContent composition(ItemStack stack) {
        com.simplemusic.sheet.SheetContent c = stack.get(MusicComponents.SHEET);
        return c == null || c.code().isBlank() ? null : c;
    }

    /** A disc with music on it (track or composition). */
    public static boolean hasMusic(ItemStack stack) {
        return stack.getItem() instanceof CustomDiscItem && (track(stack) != null || composition(stack) != null);
    }

    @Override
    public Component getName(ItemStack stack) {
        TrackRef track = track(stack);
        if (track != null && !track.title().isBlank()) return Component.translatable("item.simplemusic.custom_disc.named", track.title());
        com.simplemusic.sheet.SheetContent sheet = composition(stack);
        if (sheet != null && !sheet.title().isBlank()) return Component.translatable("item.simplemusic.custom_disc.named", sheet.title());
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        TrackRef track = track(stack);
        if (track != null) {
            lines.accept(Component.translatable("item.simplemusic.custom_disc.length", track.seconds() / 60,
                    String.format(java.util.Locale.ROOT, "%02d", track.seconds() % 60), track.uploader()).withStyle(ChatFormatting.GRAY));
        } else {
            com.simplemusic.sheet.SheetContent sheet = composition(stack);
            if (sheet != null && !sheet.author().isBlank()) {
                lines.accept(Component.translatable("book.byAuthor", sheet.author()).withStyle(ChatFormatting.GRAY));
            }
        }
        super.appendHoverText(stack, context, display, lines, flag);
    }
}
