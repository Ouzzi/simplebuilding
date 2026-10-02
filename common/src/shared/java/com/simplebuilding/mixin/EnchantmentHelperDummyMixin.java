package com.simplebuilding.mixin;

import com.simplebuilding.dummy.DummyTargets;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Trainingspuppe (2026-10-02): Schadens-Verzauberungen sehen statt der Puppe einen Stellvertreter-Mob ihres Kopfes.
 * So wirken Bann, Nemesis der Gliederfuesser, Aufspiessen und jede datengetriebene Bedingung ueber Entity-Type-Tags
 * (auch anderer Mods) genau wie gegen den echten Mob - im Nahkampf wie bei Pfeilen und Dreizacken.
 */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperDummyMixin {
    @ModifyVariable(method = {"modifyDamage", "modifyFallBasedDamage"}, at = @At("HEAD"), argsOnly = true)
    private static Entity simplebuilding$dummyVictim(Entity victim) {
        return DummyTargets.victim(victim);
    }
}
