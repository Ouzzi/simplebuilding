package com.simplebuilding.items.custom;

import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Einsteiger-Handbuch und Themenbuecher (Seiten: {@link com.simplebuilding.guide.GuideBooks}).
 *
 * <p>Verhaelt sich beim Benutzen wie Vanillas {@code WrittenBookItem}: der Server schickt das
 * Buch-oeffnen-Paket, der Client liest die Seiten aus der Komponente {@code WRITTEN_BOOK_CONTENT}
 * des Stapels in der Hand - die hat jedes Buch-Item als Standardkomponente, also auf beiden Seiten
 * ohne Synchronisation. {@code openItemGui} ist auf dem Client-Spieler leer.
 */
public class GuideBookItem extends Item {

    public GuideBookItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.openItemGui(stack, hand);
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResult.SUCCESS;
    }
}
