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
 * ueberschreiben, bleiben unberuehrt. Dasselbe gilt fuer die Mod-Verzauberungen mit der Option
 * {@code modEnchantedBookTextures} ({@link #select}). Die Eigenschaft wird bei jedem Zeichnen neu
 * ausgewertet, ein Umschalten wirkt also sofort, ohne Neuladen der Ressourcen.
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

    /**
     * Mod-Verzauberungen mit eigenem Buch, in der Vorrang-Reihenfolge der Select-Eigenschaft: traegt
     * ein Buch mehrere, zeigt es das erste. Der Case-Wert ist der Pfad des Schluessels.
     */
    public static final List<ResourceKey<Enchantment>> MOD_BOOKS = List.of(
            ModEnchantments.FAST_CHISELING, ModEnchantments.CONSTRUCTORS_TOUCH, ModEnchantments.COLOR_PALETTE,
            ModEnchantments.MASTER_BUILDER, ModEnchantments.BREAK_THROUGH, ModEnchantments.RADIUS,
            ModEnchantments.COVER, ModEnchantments.BRIDGE, ModEnchantments.LINEAR, ModEnchantments.VEIN_MINER,
            ModEnchantments.DEEP_POCKETS, ModEnchantments.STRIP_MINER, ModEnchantments.VERSATILITY,
            ModEnchantments.DRAWER, ModEnchantments.KINETIC_PROTECTION, ModEnchantments.DOUBLE_JUMP,
            ModEnchantments.OVERRIDE, ModEnchantments.FUNNEL, ModEnchantments.RANGE);

    private VanillaBookTextures() {
    }

    /** Die Client-Option; ohne geladene Konfiguration gilt der Standard (an). */
    public static boolean enabled() {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        return config == null || config.vanillaEnchantedBookTextures;
    }

    /**
     * Die Client-Option {@code modEnchantedBookTextures} (Standard an): an zeigen Mod-Verzauberungen
     * ihr eigenes Buch, aus das schlichte Vanilla-Buch.
     */
    public static boolean modEnabled() {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        return config == null || config.modEnchantedBookTextures;
    }

    /**
     * Der ganze Select-Wert von {@code simplebuilding:enchant_type}: zuerst die erste Mod-Verzauberung
     * aus {@link #MOD_BOOKS} (nur wenn {@code modBooks}), sonst die erste Vanilla-Verzauberung (nur
     * wenn {@code vanillaBooks}), sonst {@link #NONE} und damit das Vanilla-Modell. Ist die
     * Mod-Option aus, zeigt ein Buch mit Mod- und Vanilla-Verzauberung das Vanilla-Verzauberungsbuch.
     */
    public static String select(ItemEnchantments enchantments, boolean modBooks, boolean vanillaBooks) {
        if (enchantments == null) {
            return NONE;
        }
        if (modBooks) {
            for (ResourceKey<Enchantment> book : MOD_BOOKS) {
                for (Holder<Enchantment> holder : enchantments.keySet()) {
                    if (holder.is(book)) {
                        return book.identifier().getPath();
                    }
                }
            }
        }
        return key(enchantments, vanillaBooks);
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
