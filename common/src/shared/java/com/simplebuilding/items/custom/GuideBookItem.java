package com.simplebuilding.items.custom;

import com.simplebuilding.guide.GuideBooks;
import com.simplebuilding.guide.GuideContent;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
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
 * Einsteiger-Handbuch und Themenbuecher (Seiten: {@link GuideBooks}, Bildschirm-Inhalt:
 * {@link GuideContent}).
 *
 * <p>Benutzen oeffnet auf dem Client den eigenen Buchbildschirm
 * ({@code com.simplebuilding.client.guide.GuideBookScreen}, von jedem Loader ueber
 * {@link #setClientOpener} eingehaengt) beim Abschnitt dieses Buchs; der Server zaehlt nur die
 * Benutzung und schickt kein Buch-oeffnen-Paket mehr. Die Seiten bleiben trotzdem als
 * Standardkomponente {@code WRITTEN_BOOK_CONTENT} am Item: das Lesepult zeigt sie (Vanillas
 * Lesepult-Bildschirm, mit Seitensignal fuer Redstone) - dort sieht man die schlichte Textfassung.
 *
 * <p>Tooltip nach dem Vorbild von Eidolons Codex: Name in der Farbe des Buchs, darunter eine
 * kursive Unterzeile und der Mod-Name.
 */
public class GuideBookItem extends Item {

    private static Consumer<ItemStack> clientOpener = book -> {
    };

    private final GuideBooks.Book book;

    public GuideBookItem(Properties settings, GuideBooks.Book book) {
        super(settings);
        this.book = book;
    }

    public GuideBooks.Book book() {
        return book;
    }

    /** Jeder Loader setzt hier beim Client-Start den Bildschirm ein (Server: bleibt leer). */
    public static void setClientOpener(Consumer<ItemStack> opener) {
        clientOpener = opener != null ? opener : b -> {
        };
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            clientOpener.accept(player.getItemInHand(hand).copy());
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResult.SUCCESS;
    }

    @Override
    public java.util.Optional<net.minecraft.world.inventory.tooltip.TooltipComponent> getTooltipImage(ItemStack stack) {
        if (!com.simplebuilding.version.McVersion.MEGA_GUIDES) return java.util.Optional.empty();
        return java.util.Optional.of(new com.simplebuilding.items.tooltip.GuideTooltipData(
                book.shelf().books().stream().filter(b -> GuideBooks.inserted(stack, b)).toList()));
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(s -> s.withColor(TextColor.fromRgb(GuideContent.style(book).colour())));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(GuideContent.taglineKey(book)).withStyle(s -> s.withColor(TextColor.fromRgb(GuideContent.secondaryColour(book))).withItalic(true)));
        tooltip.accept(Component.translatable(GuideContent.MOD_NAME_KEY).withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
    }
}
