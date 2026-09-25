package com.simplebuilding.enchantment;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.config.SimplebuildingConfig;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.List;
import java.util.Optional;

/**
 * Eigene Buch-Texturen fuer die Vanilla-Verzauberungen (Runde 9 der Texturarbeit).
 *
 * <p>{@code assets/minecraft/items/enchanted_book.json} waehlt ueber die Select-Eigenschaft
 * {@code simplebuilding:enchant_type} ein Modell. Fuer Mod-Verzauberungen liefert die Eigenschaft
 * deren Namen, fuer Vanilla-Verzauberungen den Schluessel {@link #caseKey}, der auf
 * {@code simplebuilding:item/enchanted_book_vanilla_<pfad>} zeigt. Ist die Client-Option
 * {@code vanillaEnchantedBookTextures} aus, liefert sie {@link #NONE} - das Buch faellt auf das
 * Vanilla-Modell zurueck, und Ressourcenpakete oder andere Mods, die dieselben Buecher
 * ueberschreiben, bleiben unberuehrt.
 *
 * <p>Ohne Client-Klassen, damit die Servertests die Auswahl pruefen koennen.
 */
public final class VanillaBookTextures {

    /** Wert der Select-Eigenschaft, der auf das Standardmodell zurueckfaellt. */
    public static final String NONE = "none";

    /** Alle Vanilla-Verzauberungen (beide Minecraft-Linien), je ein Buch; Datagen und Tests lesen die Liste. */
    public static final List<String> VANILLA = List.of(
            "aqua_affinity", "bane_of_arthropods", "binding_curse", "blast_protection", "breach", "channeling",
            "density", "depth_strider", "efficiency", "feather_falling", "fire_aspect", "fire_protection", "flame",
            "fortune", "frost_walker", "impaling", "infinity", "knockback", "looting", "loyalty", "luck_of_the_sea",
            "lunge", "lure", "mending", "multishot", "piercing", "power", "projectile_protection", "protection",
            "punch", "quick_charge", "respiration", "riptide", "sharpness", "silk_touch", "smite", "soul_speed",
            "sweeping_edge", "swift_sneak", "thorns", "unbreaking", "vanishing_curse", "wind_burst");

    private VanillaBookTextures() {
    }

    /** Die Client-Option; ohne geladene Konfiguration gilt der Standard (an). */
    public static boolean enabled() {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        return config == null || config.vanillaEnchantedBookTextures;
    }

    /** Case-Wert im Item-Modell fuer die Vanilla-Verzauberung {@code minecraft:<path>}. */
    public static String caseKey(String path) {
        return "minecraft_" + path;
    }

    /** Modell- und Texturpfad (Namensraum simplebuilding) des Buchs fuer {@code minecraft:<path>}. */
    public static String modelPath(String path) {
        return "item/enchanted_book_vanilla_" + path;
    }

    /**
     * Der Select-Wert fuer die erste Vanilla-Verzauberung auf dem Buch, oder {@link #NONE}, wenn
     * die Option aus ist oder keine Vanilla-Verzauberung darauf liegt. Mod-Verzauberungen prueft
     * die Eigenschaft vorher selbst.
     */
    public static String key(ItemEnchantments enchantments, boolean enabled) {
        if (!enabled || enchantments == null) {
            return NONE;
        }
        for (Holder<Enchantment> holder : enchantments.keySet()) {
            Optional<ResourceKey<Enchantment>> key = holder.unwrapKey();
            if (key.isPresent() && "minecraft".equals(key.get().identifier().getNamespace())) {
                return caseKey(key.get().identifier().getPath());
            }
        }
        return NONE;
    }
}
