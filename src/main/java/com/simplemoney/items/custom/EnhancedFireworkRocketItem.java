package com.simplemoney.items.custom;

import com.simplemoney.entity.custom.EnhancedFireworkRocket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class EnhancedFireworkRocketItem extends FireworkRocketItem {

    public EnhancedFireworkRocketItem(Item.Settings settings) {
        super(settings);
    }

    // --- Korrigierte Signatur mit ActionResult ---
    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);

        if (!world.isClient()) {
            // Spawn-Logik
            EnhancedFireworkRocket enhancedRocket = new EnhancedFireworkRocket(
                    world,
                    itemStack.copy(),
                    user
            );
            world.spawnEntity(enhancedRocket);

            // Sound abspielen (optional)
            world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.AMBIENT, 2.5F, 1.5F);

            if (!user.getAbilities().creativeMode) {
                // Bei Erfolg ein Item entfernen (Client-Seite macht dies automatisch)
                itemStack.decrement(1);
            }
        }

        // Die Rückgabe muss ActionResult sein und verwendet das success/fail/pass Enum.
        // Der ItemStack wird direkt im Inventar des Spielers manipuliert (itemStack.decrement(1)).
        return ActionResult.SUCCESS;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        return ActionResult.PASS;
    }
}