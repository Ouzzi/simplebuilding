package com.simplebuilding.modules.simplemobmanners.mixin;

import com.simplebuilding.modules.simplemobmanners.SimpleMobManners;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

@Mixin(Entity.class)
public abstract class EntityMuteMixin {

    @Shadow public abstract boolean hasCustomName();
    @Shadow public abstract Component getCustomName();
    @Shadow public abstract EntityType<?> getType();

    /** REV2-B-2: the id strings resolved to types once; rebuilt only when the config list changes. */
    private static List<String> simplemobmanners$source;
    private static int simplemobmanners$hash;
    private static Set<EntityType<?>> simplemobmanners$types = Set.of();

    private static synchronized Set<EntityType<?>> mutedTypes(List<String> ids) {
        int hash = ids.hashCode();
        if (ids != simplemobmanners$source || hash != simplemobmanners$hash) {
            Set<EntityType<?>> out = new HashSet<>();
            for (String id : ids) {
                Identifier key = id == null ? null : Identifier.tryParse(id);
                if (key != null) BuiltInRegistries.ENTITY_TYPE.getOptional(key).ifPresent(out::add);
            }
            simplemobmanners$types = out;
            simplemobmanners$source = ids;
            simplemobmanners$hash = hash;
        }
        return simplemobmanners$types;
    }

    @Inject(method = "isSilent", at = @At("HEAD"), cancellable = true)
    private void checkMuted(CallbackInfoReturnable<Boolean> cir) {
        if (!SimpleMobManners.active()) return;
        // --- SCHRITT 1: Check per Entity-Typ (z.B. "minecraft:cow") ---
        // Das ist die "alte" Funktionalität, die du behalten wolltest.
        List<String> mutedEntities = SimpleMobManners.config().mutedEntities;

        if (mutedEntities != null && !mutedEntities.isEmpty()) {
            if (mutedTypes(mutedEntities).contains(this.getType())) {
                cir.setReturnValue(true);
                return; // Wenn hier gemutet, sind wir fertig!
            }
        }

        // --- SCHRITT 2: Check per Namens-Suffix (z.B. "Name [MUTE]") ---
        // Das ist die "neue" Funktionalität.

        // Performance: Sofort abbrechen, wenn kein Custom Name da ist.
        // Das spart uns das teure Laden der Suffix-Liste und String-Konvertierung.
        if (!this.hasCustomName()) return;

        List<String> suffixes = SimpleMobManners.config().nametagMuteSuffixes;

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
