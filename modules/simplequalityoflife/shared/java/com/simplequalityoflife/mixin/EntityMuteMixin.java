package com.simplequalityoflife.mixin;

import com.simplequalityoflife.Simplequalityoflife;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Entity.class)
public abstract class EntityMuteMixin {

    @Shadow public abstract boolean hasCustomName();
    @Shadow public abstract Component getCustomName();
    @Shadow public abstract EntityType<?> getType();

    @Inject(method = "isSilent", at = @At("HEAD"), cancellable = true)
    private void checkMuted(CallbackInfoReturnable<Boolean> cir) {
        // --- SCHRITT 1: Check per Entity-Typ (z.B. "minecraft:cow") ---
        // Das ist die "alte" Funktionalität, die du behalten wolltest.
        List<String> mutedEntities = Simplequalityoflife.getConfig().qOL.mutedEntities;

        if (mutedEntities != null && !mutedEntities.isEmpty()) {
            // EntityType zu String ist relativ schnell (intern gecached)
            String entityId = EntityType.getKey(this.getType()).toString();
            if (mutedEntities.contains(entityId)) {
                cir.setReturnValue(true);
                return; // Wenn hier gemutet, sind wir fertig!
            }
        }

        // --- SCHRITT 2: Check per Namens-Suffix (z.B. "Name [MUTE]") ---
        // Das ist die "neue" Funktionalität.

        // Performance: Sofort abbrechen, wenn kein Custom Name da ist.
        // Das spart uns das teure Laden der Suffix-Liste und String-Konvertierung.
        if (!this.hasCustomName()) return;

        List<String> suffixes = Simplequalityoflife.getConfig().qOL.nametagMuteSuffixes;

        // Performance: Wenn die Liste in der Config leer ist, sofort raus.
        if (suffixes == null || suffixes.isEmpty()) return;

        // Erst JETZT machen wir die teure String-Konvertierung
        String name = this.getCustomName().getString();

        for (String suffix : suffixes) {
            // Check auf null/empty um NullPointerExceptions zu vermeiden
            if (suffix != null && !suffix.isEmpty() && name.endsWith(suffix)) {
                cir.setReturnValue(true);
                return;
            }
        }
    }
}
