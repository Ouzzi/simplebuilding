package com.simplebuilding.config;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.TweaksClientHooks;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Zugriff auf den Abschnitt {@code server} der Config ({@link ServerTuningConfig}): auf dem Server
 * (auch dem integrierten) die eigene Datei, auf dem Client-Thread der Stand, den der Server zuletzt
 * geschickt hat ({@code TweaksConfigPayload}, Feld {@code serverTuning} als JSON). So rechnet ein
 * Client Anzeige (Tooltips, JEI, Abbaufortschritt fremder Pads, Hammer-Animation) mit denselben
 * Zahlen wie der Server, und seine eigene Datei hat keine Stimme. Ohne Server-Meldung (Hauptmenue,
 * Server ohne diese Mod) gilt die eigene Datei.
 *
 * <p>Hier stehen auch die Grenzen jeder Option: Geschwindigkeiten und Reichweiten sind nach oben so
 * begrenzt, dass Vanilla nicht aus dem Gleichgewicht kommt (docs/CONFIG.md).
 */
public final class ServerTuning {

    public static final int MAX_BREAK_SECONDS = 3600;
    public static final int MAX_UPGRADE_SECONDS = 30;
    public static final int MAX_UPGRADE_DAMAGE = 64;
    /** Kuerzeste Meissel-Abklingzeit: hoechstens zehn Formungen je Sekunde. */
    public static final int MIN_CHISEL_COOLDOWN = 2;
    public static final int MAX_CHISEL_COOLDOWN = 200;
    /** Schnellste Maschine: achtfach (ein Trichter-Transfer je Tick), wie die Enderit-Stufe bisher. */
    public static final int MAX_MACHINE_SPEED = 8;
    public static final double MIN_DETECTOR_RANGE = 0.25;
    /** 1,5 = 42 Bloecke mit Radius; der Kugelscan waechst kubisch, mehr kostet zu viel Serverzeit. */
    public static final double MAX_DETECTOR_RANGE = 1.5;
    /** Hoechstens zwei Pings je Sekunde. */
    public static final int MIN_SCAN_INTERVAL = 10;
    public static final int MAX_SCAN_INTERVAL = 200;
    public static final double MAX_LOOT_MULTIPLIER = 3.0;
    public static final double MIN_PRICE_MULTIPLIER = 0.25;
    public static final double MAX_PRICE_MULTIPLIER = 4.0;
    /** Ueber allem, was die Blaupause bisher je Tick setzt (4 194 304 Stellen / 180 Ticks = 23 302). */
    public static final int MAX_BLUEPRINT_BLOCKS_PER_TICK = 32768;
    public static final double MAX_TRIM_STRENGTH = 2.0;
    public static final int MIN_LENS = 64;
    public static final int MAX_LENS = 2560;
    public static final int MIN_ROTATOR = 64;
    public static final int MAX_ROTATOR = 4096;
    public static final int MIN_ECHO = 150;
    public static final int MAX_ECHO = 6000;

    private static final Gson GSON = new Gson();
    private static final ServerTuningConfig DEFAULTS = new ServerTuningConfig();
    private static final Map<String, Field> TRIM_FIELDS = new HashMap<>();

    /** Zuletzt gelesenes Server-JSON und sein Ergebnis (der Client liest oft, der Server schickt selten). */
    private static volatile @Nullable String cachedJson;
    private static volatile ServerTuningConfig cachedParsed = DEFAULTS;

    static {
        for (Field field : ServerTuningConfig.TrimStrengths.class.getFields()) {
            if (field.getType() == double.class && !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                TRIM_FIELDS.put(snake(field.getName()), field);
            }
        }
    }

    private ServerTuning() {
    }

    /** Die eigene Datei (Server-Seite, Inhalt des Sync-Pakets); vor dem Laden die Standardwerte. */
    public static ServerTuningConfig local() {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        if (config == null || config.server == null) {
            return DEFAULTS;
        }
        return config.server;
    }

    /** Was gerade gilt: auf dem Client-Thread die Werte des Servers (falls gemeldet), sonst die eigene Datei. */
    public static ServerTuningConfig get() {
        return resolve(TweaksClientHooks.onClientThread());
    }

    /**
     * {@link #get()} mit ausdruecklicher Seite (fuer Tests): {@code clientThread} = true nimmt den
     * gemeldeten Server-Stand, sobald einer da ist.
     */
    public static ServerTuningConfig resolve(boolean clientThread) {
        if (clientThread) {
            SimpleTweaks.ServerValues synced = SimpleTweaks.syncedValues();
            if (synced != null && synced.serverTuning() != null && !synced.serverTuning().isEmpty()) {
                return parse(synced.serverTuning());
            }
        }
        return local();
    }

    /** Der Abschnitt als JSON fuer das Sync-Paket. */
    public static String toJson(ServerTuningConfig tuning) {
        return GSON.toJson(tuning);
    }

    /**
     * JSON des Servers lesen und begrenzen, zwischengespeichert (der Client liest oft); das Ergebnis ist
     * geteilt und darf nicht veraendert werden - dafuer {@link #copyOf}.
     */
    public static ServerTuningConfig parse(String json) {
        String last = cachedJson;
        if (json.equals(last)) {
            return cachedParsed;
        }
        ServerTuningConfig parsed = copyOf(json);
        cachedParsed = parsed;
        cachedJson = json;
        return parsed;
    }

    /** Ein eigenes, begrenztes Objekt aus dem JSON; ein kaputtes JSON gibt die Standardwerte. */
    public static ServerTuningConfig copyOf(String json) {
        ServerTuningConfig parsed;
        try {
            parsed = GSON.fromJson(json, ServerTuningConfig.class);
        } catch (JsonParseException e) {
            parsed = null;
        }
        if (parsed == null) {
            parsed = new ServerTuningConfig();
        }
        parsed.validate();
        return parsed;
    }

    /**
     * Client, beim Empfang: die Hoechstladungen legen die Haltbarkeit beim Start fest und kommen nicht
     * ueber das Netz. Weichen die Werte des Servers von den eigenen ab, zeigt der Client falsche
     * Ladebalken - das steht dann im Log (Modpack-Datei angleichen).
     */
    public static void checkStartupValues(@Nullable String json) {
        if (json == null || json.isEmpty()) {
            return;
        }
        ServerTuningConfig.Charges server = parse(json).charges;
        ServerTuningConfig.Charges own = local().charges;
        if (server.lensMaxCharge != own.lensMaxCharge || server.rotatorMaxCharge != own.rotatorMaxCharge
                || server.echoSounderMaxCharge != own.echoSounderMaxCharge) {
            Simplebuilding.LOGGER.warn("server.charges differ between this client and the server (lens {}/{}, rotator {}/{}, "
                            + "echo sounder {}/{}); charge bars will look wrong until both use the same config file",
                    own.lensMaxCharge, server.lensMaxCharge, own.rotatorMaxCharge, server.rotatorMaxCharge,
                    own.echoSounderMaxCharge, server.echoSounderMaxCharge);
        }
    }

    // =====================================================================================
    // Werte beim Start (Haltbarkeit der Gegenstaende)
    // =====================================================================================

    public static int startupLensMaxCharge() {
        return ServerTuningConfig.clamp(local().charges.lensMaxCharge, MIN_LENS, MAX_LENS);
    }

    public static int startupRotatorMaxCharge() {
        return ServerTuningConfig.clamp(local().charges.rotatorMaxCharge, MIN_ROTATOR, MAX_ROTATOR);
    }

    public static int startupEchoSounderMaxCharge() {
        return ServerTuningConfig.clamp(local().charges.echoSounderMaxCharge, MIN_ECHO, MAX_ECHO);
    }

    // =====================================================================================
    // Abgeleitete Werte (alle ueber get(), also auf dem Client die des Servers)
    // =====================================================================================

    /** Abbaufortschritt je Tick fuer Fremde an Pads ({@code plate} = Chunk-Loader, Kupfer-/Filterplatten). */
    public static float strangerBreakProgress(boolean plate) {
        ServerTuningConfig.Pads pads = get().pads;
        int seconds = ServerTuningConfig.clamp(plate ? pads.strangerPlateBreakSeconds : pads.strangerPadBreakSeconds,
                1, MAX_BREAK_SECONDS);
        return 1.0f / (seconds * 20.0f);
    }

    /** Schlaege einer Hammer-Aufwertung (= Sekunden). */
    public static int sledgehammerBlows() {
        return ServerTuningConfig.clamp(get().tools.sledgehammerUpgradeSeconds, 1, MAX_UPGRADE_SECONDS);
    }

    /** Schaden je Hammerschlag einer Aufwertung zur verstaerkten, Netherit- oder Enderit-Stufe. */
    public static int upgradeDamagePerHit(boolean toReinforced, boolean toEnderite) {
        ServerTuningConfig.Tools tools = get().tools;
        int value = toReinforced ? tools.reinforcedUpgradeDamagePerHit
                : toEnderite ? tools.enderiteUpgradeDamagePerHit : tools.netheriteUpgradeDamagePerHit;
        return ServerTuningConfig.clamp(value, 0, MAX_UPGRADE_DAMAGE);
    }

    /**
     * Abklingzeit eines Meissels oder Spachtels nach seinem Registernamen ({@code iron_chisel},
     * {@code netherite_spatula}); {@code fallback} fuer Namen ohne bekannte Stufe.
     */
    public static int chiselCooldown(@Nullable Identifier itemId, int fallback) {
        if (itemId == null) {
            return fallback;
        }
        String path = itemId.getPath();
        ServerTuningConfig.Tools tools = get().tools;
        int value;
        if (path.startsWith("stone_")) value = tools.stoneChiselCooldownTicks;
        else if (path.startsWith("copper_")) value = tools.copperChiselCooldownTicks;
        else if (path.startsWith("iron_")) value = tools.ironChiselCooldownTicks;
        else if (path.startsWith("gold_")) value = tools.goldChiselCooldownTicks;
        else if (path.startsWith("diamond_")) value = tools.diamondChiselCooldownTicks;
        else if (path.startsWith("netherite_")) value = tools.netheriteChiselCooldownTicks;
        else if (path.startsWith("enderite_")) value = tools.enderiteChiselCooldownTicks;
        else return fallback;
        return ServerTuningConfig.clamp(value, MIN_CHISEL_COOLDOWN, MAX_CHISEL_COOLDOWN);
    }

    /** Vielfaches von Vanilla fuer Trichter (1 = verstaerkt, 2 = Netherit, 3 = Enderit). */
    public static int hopperSpeed(int tier) {
        ServerTuningConfig.Machines machines = get().machines;
        int value = tier >= 3 ? machines.enderiteHopperSpeed : tier == 2 ? machines.netheriteHopperSpeed : machines.reinforcedHopperSpeed;
        return ServerTuningConfig.clamp(value, 1, MAX_MACHINE_SPEED);
    }

    /** Transfer-Abklingzeit eines Trichters der Stufe: 8 Ticks (Vanilla) / Tempo, mindestens 1. */
    public static int hopperCooldown(int tier) {
        return Math.max(1, Math.round(8.0f / hopperSpeed(tier)));
    }

    /** Zusaetzliche Kochticks je Tick fuer Ofen/Raeucherofen/Schmelzofen der Stufe (Tempo - 1). */
    public static int furnaceExtraTicks(int tier) {
        ServerTuningConfig.Machines machines = get().machines;
        int value = tier >= 3 ? machines.enderiteFurnaceSpeed : tier == 2 ? machines.netheriteFurnaceSpeed : machines.reinforcedFurnaceSpeed;
        return ServerTuningConfig.clamp(value, 1, MAX_MACHINE_SPEED) - 1;
    }

    public static double oreDetectorRange() {
        return ServerTuningConfig.clamp(get().oreDetector.rangeMultiplier, MIN_DETECTOR_RANGE, MAX_DETECTOR_RANGE, 1.0);
    }

    public static int oreDetectorInterval() {
        return ServerTuningConfig.clamp(get().oreDetector.scanIntervalTicks, MIN_SCAN_INTERVAL, MAX_SCAN_INTERVAL);
    }

    public static double lootMultiplier() {
        return ServerTuningConfig.clamp(get().loot.globalLootMultiplier, 0.0, MAX_LOOT_MULTIPLIER, 1.0);
    }

    public static double tradePriceMultiplier() {
        return ServerTuningConfig.clamp(get().loot.tradePriceMultiplier, MIN_PRICE_MULTIPLIER, MAX_PRICE_MULTIPLIER, 1.0);
    }

    public static int blueprintBlocksPerTick() {
        return ServerTuningConfig.clamp(get().blueprint.maxBlocksPerTick, 1, MAX_BLUEPRINT_BLOCKS_PER_TICK);
    }

    /** Faktor der Besatz-Wirkung {@code bonusKey} ({@code fire_protection}, ...). */
    public static float trimStrength(String bonusKey) {
        return get().trimStrengths.of(bonusKey);
    }

    static @Nullable Field trimStrengthField(String bonusKey) {
        return TRIM_FIELDS.get(bonusKey);
    }

    /**
     * Ob die Beute-Tabelle {@code table} (z. B. {@code minecraft:chests/end_city_treasure}) Mod-Beute
     * bekommt: der Schalter ihrer Struktur. Tabellen ohne Schalter (Koepfe, fremde Tabellen) immer.
     */
    public static boolean lootEnabledFor(Identifier table) {
        if (!"minecraft".equals(table.getNamespace())) {
            return true;
        }
        ServerTuningConfig.Loot loot = get().loot;
        String path = table.getPath();
        if (path.startsWith("chests/stronghold")) return loot.strongholdLoot;
        if (path.startsWith("chests/end_city")) return loot.endCityLoot;
        if (path.startsWith("chests/ancient_city")) return loot.ancientCityLoot;
        if (path.startsWith("chests/bastion")) return loot.bastionLoot;
        if (path.startsWith("chests/nether_bridge")) return loot.netherFortressLoot;
        if (path.startsWith("chests/pillager_outpost")) return loot.pillagerOutpostLoot;
        if (path.startsWith("chests/woodland_mansion")) return loot.woodlandMansionLoot;
        if (path.startsWith("chests/buried_treasure")) return loot.buriedTreasureLoot;
        if (path.startsWith("chests/simple_dungeon")) return loot.dungeonLoot;
        if (path.startsWith("chests/shipwreck")) return loot.shipwreckLoot;
        if (path.startsWith("chests/igloo")) return loot.iglooLoot;
        if (path.startsWith("chests/abandoned_mineshaft")) return loot.mineshaftLoot;
        if (path.startsWith("chests/trial_chambers")) return loot.trialChambersLoot;
        if (path.startsWith("chests/ruined_portal")) return loot.ruinedPortalLoot;
        if (path.startsWith("gameplay/fishing")) return loot.fishingLoot;
        return true;
    }

    /**
     * Ob {@code dimension} in einer Sperrliste steht (Dimension-IDs, durch Komma, Semikolon oder
     * Leerzeichen getrennt; ohne Namensraum gilt {@code minecraft}).
     */
    public static boolean dimensionListed(@Nullable String list, Identifier dimension) {
        if (list == null || list.isBlank()) {
            return false;
        }
        for (String entry : list.split("[,;\\s]+")) {
            String id = entry.trim().toLowerCase(Locale.ROOT);
            if (id.isEmpty()) {
                continue;
            }
            if (!id.contains(":")) {
                id = "minecraft:" + id;
            }
            if (id.equals(dimension.toString())) {
                return true;
            }
        }
        return false;
    }

    public static boolean chunkLoaderBlockedIn(Identifier dimension) {
        return dimensionListed(get().dimensionLocks.chunkLoaderBlockedDimensions, dimension);
    }

    public static boolean flypadBlockedIn(Identifier dimension) {
        return dimensionListed(get().dimensionLocks.flypadBlockedDimensions, dimension);
    }

    public static boolean echoSounderBlockedIn(Identifier dimension) {
        return dimensionListed(get().dimensionLocks.echoSounderBlockedDimensions, dimension);
    }

    // =====================================================================================
    // Rueckmeldung
    // =====================================================================================

    /**
     * Fuer Funktionsschalter: {@code enabled} = false heisst abgelehnt, und ein Server-Spieler bekommt die
     * Meldung "auf diesem Server abgeschaltet" in der Aktionsleiste. Auf dem Client nur die Antwort.
     *
     * @return true, wenn die Funktion abgeschaltet ist (der Aufrufer bricht ab)
     */
    public static boolean featureDenied(boolean enabled, @Nullable Player player) {
        if (enabled) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            notify(serverPlayer, "message.simplebuilding.feature_disabled");
        }
        return true;
    }

    /** Rote Meldung in der Aktionsleiste. */
    public static void notify(ServerPlayer player, String key) {
        Component message = Component.translatable(key).withStyle(ChatFormatting.RED);
        player.displayClientMessage(message, true);
    }

    private static String snake(String camel) {
        StringBuilder out = new StringBuilder();
        for (char c : camel.toCharArray()) {
            if (Character.isUpperCase(c)) {
                out.append('_').append(Character.toLowerCase(c));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
