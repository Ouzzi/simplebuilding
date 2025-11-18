package com.simplemoney.mixin;

import com.simplemoney.Simplemoney;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Mixin, um die Eigenschaften des Vanilla-Items FireworkRocketItem zu modifizieren.
 * Ziel dieser Modifikation ist es, die maximale Stapelgröße des Items von 64 auf 16 zu reduzieren.
 */
@Mixin(FireworkRocketItem.class)
public abstract class FireworkRocketItemMixin extends Item {

    /**
     * Erforderlicher Konstruktor, da diese Klasse von Item erbt.
     * @param settings Die Einstellungen für das Item.
     */
    public FireworkRocketItemMixin(Settings settings) {
        super(settings);
    }

    /**
     * Modifiziert das {@code Item.Settings}-Objekt, das an den Konstruktor der übergeordneten Klasse
     * ({@code Item}) übergeben wird, um die maximale Stapelgröße zu ändern.
     *
     * @param originalSettings Das ursprüngliche Item.Settings-Objekt von FireworkRocketItem.
     * @return Das modifizierte Item.Settings-Objekt mit der maximalen Stapelgröße 16.
     */
    @ModifyArg(
            method = "<init>", // Zielt auf den Konstruktor der FireworkRocketItem-Klasse
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/item/Item;<init>(Lnet/minecraft/item/Item$Settings;)V" // Ziel ist der Item-Konstruktor
            ),
            index = 0 // Das Settings-Objekt ist das erste Argument
    )
    private static Item.Settings simplemoney_modifyFireworkStack(Item.Settings originalSettings) {

        // Setzt die Stapelgröße des Settings-Objekts auf 16.
        Item.Settings modifiedSettings = originalSettings.maxCount(16);

        Simplemoney.LOGGER.info("Successfully limited Firework Rocket stack size to 16 using constructor modification.");

        return modifiedSettings;
    }
}