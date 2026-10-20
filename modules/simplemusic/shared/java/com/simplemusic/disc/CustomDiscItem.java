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
 * A burned disc: carries a {@link TrackRef} (hash, title, length, uploader) and plays in the Orchestrion and - owner
 * decision U1 b - in the Vanilla jukebox. A disc whose track the server deleted is a "scratched disc" and stays silent.
 */
public class CustomDiscItem extends Item {
    public CustomDiscItem(Properties properties) {
        super(properties);
    }

    public static TrackRef track(ItemStack stack) {
        return stack.get(MusicComponents.TRACK);
    }

    @Override
    public Component getName(ItemStack stack) {
        TrackRef track = track(stack);
        if (track != null && !track.title().isBlank()) return Component.translatable("item.simplemusic.custom_disc.named", track.title());
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        TrackRef track = track(stack);
        if (track != null) {
            lines.accept(Component.translatable("item.simplemusic.custom_disc.length", track.seconds() / 60,
                    String.format(java.util.Locale.ROOT, "%02d", track.seconds() % 60), track.uploader()).withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, display, lines, flag);
    }
}
