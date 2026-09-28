package com.simplebuilding.config;

import com.simplebuilding.tweaks.item.EchoCompassItem;
import com.simplebuilding.tweaks.item.LaserPointerItem;
import java.util.List;
import net.minecraft.network.chat.Component;

/**
 * Zeilen "Dieser Server: ..." fuer die JEI-Infoseiten (2026-09-28): wo der feste Infotext Zahlen oder
 * Wirkungen nennt, die eine Option des Reiters "Server & Modpack Tuning" aendert, haengt JEI die Werte
 * an, die gerade gelten ({@link ServerTuning#get()} - auf dem Client die des Servers). JEI baut die
 * Seiten beim Betreten einer Welt neu.
 */
public final class ServerTuningInfo {

    private ServerTuningInfo() {
    }

    /** Zusatzzeilen fuer die JEI-Familie {@code family} (Schluessel aus {@code TweaksJeiInfo}); leer = keine. */
    public static List<Component> jeiLines(String family) {
        ServerTuningConfig tuning = ServerTuning.get();
        return switch (family) {
            case "chunk_loader" -> List.of(Component.translatable("jei.simplebuilding.server.chunk_loader",
                    Component.translatable(tuning.chunkLoaders.requireOwnerOnline
                            ? "jei.simplebuilding.server.chunk_loader.owner_online" : "jei.simplebuilding.server.chunk_loader.always"),
                    seconds(ServerTuning.strangerBreakProgress(true))));
            case "elytra_pad", "flypad", "spawn_teleporter", "launchpad", "potion_pad" -> List.of(
                    Component.translatable("jei.simplebuilding.server.pads", seconds(ServerTuning.strangerBreakProgress(false))));
            case "laser_pointer" -> List.of(Component.translatable("jei.simplebuilding.server.laser_pointer",
                    onOff(tuning.laser.igniteFlammables), onOff(tuning.laser.igniteTnt), onOff(tuning.laser.igniteEntities),
                    String.valueOf(LaserPointerItem.MAX_CHARGE)));
            case "echo_compass" -> List.of(Component.translatable("jei.simplebuilding.server.echo_compass",
                    Component.translatable(tuning.features.echoSounder ? "jei.simplebuilding.server.enabled" : "jei.simplebuilding.server.disabled"),
                    String.valueOf(EchoCompassItem.MAX_DAMAGE)));
            default -> List.of();
        };
    }

    private static Component onOff(boolean value) {
        return Component.translatable(value ? "jei.simplebuilding.server.on" : "jei.simplebuilding.server.off");
    }

    /** Fortschritt je Tick als ganze Sekunden bis zum Abbau. */
    private static String seconds(float progressPerTick) {
        return String.valueOf(Math.round(1.0f / (progressPerTick * 20.0f)));
    }
}
