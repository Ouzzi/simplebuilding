package com.simplequalityoflife.event;

import com.simplequalityoflife.Simplequalityoflife;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

public class FurnaceLavaFillHandler {

    // Öfen/Schmelzöfen/Räucheröfen nutzen Slot 1 als Brennstoff-Slot.
    // AbstractFurnaceBlockEntity.SLOT_FUEL ist 'protected', daher hier als Konstante gespiegelt.
    private static final int FUEL_SLOT = 1;



    public static InteractionResult onRightClickBlock(Player player, InteractionHand hand, BlockPos pos, Direction face) {
        if (!Simplequalityoflife.getConfig().qOL.enableFurnaceLavaFill) return InteractionResult.PASS;

        // Item der genutzten Hand prüfen (funktioniert für Haupt- und Nebenhand).
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() != Items.LAVA_BUCKET) return InteractionResult.PASS;

        // Schleichen lässt das normale Item-Verhalten zu (z.B. Lava bewusst neben dem Ofen platzieren).
        if (player.isShiftKeyDown()) return InteractionResult.PASS;

        Level world = player.level();
        if (!world.isClientSide() && !InteractionGuard.allow(player, pos)) return InteractionResult.PASS;
        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof AbstractFurnaceBlockEntity furnace)) return InteractionResult.PASS;

        // Verschlossene Öfen (Lock-Component) nicht umgehen -> normales Verhalten (Vanilla prüft den Schlüssel).
        if (furnace.isLocked()) return InteractionResult.PASS;

        // Behandelt werden zwei Fälle: der Brennstoff-Slot ist leer ODER er enthält genau einen
        // übrig gebliebenen leeren Eimer (Rest vom letzten Lava-Verbrennen). Dann wird getauscht.
        // Alles andere (Kohle, bereits Lava, gestapelte Eimer) -> normales Verhalten (GUI öffnen).
        ItemStack fuel = furnace.getItem(FUEL_SLOT);
        boolean emptySlot = fuel.isEmpty();
        boolean leftoverBucket = fuel.getItem() == Items.BUCKET && fuel.getCount() == 1;
        if (!emptySlot && !leftoverBucket) return InteractionResult.PASS;

        // Server-autoritativ: Auf dem Client nur die Interaktion beanspruchen,
        // damit sich die Ofen-GUI nicht öffnet. Die eigentliche Logik läuft serverseitig.
        if (world.isClientSide()) return InteractionResult.SUCCESS;

        if (!InteractionGuard.action(player) || !InteractionGuard.mayChange(player, pos)) return InteractionResult.PASS;

        // Vorherigen Inhalt sichern (leer oder genau 1 leerer Eimer), dann den Lava-Eimer einsetzen.
        ItemStack previousFuel = fuel.copy();
        furnace.setItem(FUEL_SLOT, new ItemStack(Items.LAVA_BUCKET));
        furnace.setChanged();

        if (!player.isCreative()) {
            // Gehaltenen Lava-Eimer verbrauchen ...
            stack.shrink(1);
            // ... und einen evtl. im Ofen liegenden leeren Eimer zurückgeben (Swap, kein Dupe).
            // Bei leerem Slot kommt nichts zurück – der leere Eimer erscheint nach dem Verbrennen im Ofen.
            if (!previousFuel.isEmpty()) {
                if (stack.isEmpty()) {
                    player.setItemInHand(hand, previousFuel);
                } else if (!player.getInventory().add(previousFuel)) {
                    player.drop(previousFuel, false, net.minecraft.util.Prediction.SERVER_ONLY);
                }
            }
        }

        world.playSound(null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 1.0f, 1.0f);
        player.swing(hand, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);

        return InteractionResult.SUCCESS;
    }
}
