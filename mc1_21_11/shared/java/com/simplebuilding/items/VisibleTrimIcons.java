package com.simplebuilding.items;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.config.SimplebuildingConfig;
import net.minecraft.resources.Identifier;

/**
 * Client-Optionen fuer die sichtbaren Besatzmuster auf Ruestungs-Icons (ArmorTrimModelProvider):
 * {@code visibleTrimIconsVanillaArmor} fuer Vanilla-Ruestung (samt Schildkroetenpanzer),
 * {@code visibleTrimIconsModArmor} fuer die Ruestung der Mod (Enderit), beide Standard an.
 *
 * <p>Jede dieser Item-Definitionen ist ein {@code minecraft:select} ueber
 * {@code simplebuilding:visible_trim_icons} mit einem Case {@link #VISIBLE} (die Muster-Ebenen) und
 * Vanillas Auswahl nach Material als {@code fallback}. Ist die Option aus, liefert die Eigenschaft
 * {@link #VANILLA}, und das Icon sieht genau wie in Vanilla aus - Ressourcenpakete oder andere Mods,
 * die Besatz-Icons gestalten, gewinnen. Die Eigenschaft wird bei jedem Zeichnen neu ausgewertet; ein
 * Umschalten wirkt sofort, ohne Neuladen der Ressourcen.
 *
 * <p>Ohne Client-Klassen, damit die Servertests die Auswahl pruefen koennen.
 */
public final class VisibleTrimIcons {

    /** Select-Wert fuer die Muster-Ebenen. */
    public static final String VISIBLE = "visible";
    /** Select-Wert, der auf den Vanilla-Fallback faellt. */
    public static final String VANILLA = "vanilla";

    private VisibleTrimIcons() {
    }

    /** Ruestung der Mod (Namensraum simplebuilding) statt Vanilla-Ruestung. */
    public static boolean isModArmour(Identifier item) {
        return Simplebuilding.MOD_ID.equals(item.getNamespace());
    }

    /** Select-Wert fuer das Item {@code item} bei den gegebenen Optionen. */
    public static String key(Identifier item, boolean vanillaArmour, boolean modArmour) {
        return (isModArmour(item) ? modArmour : vanillaArmour) ? VISIBLE : VANILLA;
    }

    /** Select-Wert nach der geladenen Konfiguration; ohne Konfiguration gilt der Standard (an). */
    public static String key(Identifier item) {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        return config == null ? VISIBLE : key(item, config.visibleTrimIconsVanillaArmor, config.visibleTrimIconsModArmor);
    }
}
