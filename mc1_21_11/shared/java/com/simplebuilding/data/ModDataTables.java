package com.simplebuilding.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.simplebuilding.networking.DataTablesSyncPayload;
import com.simplebuilding.platform.PlatformServices;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The mod's datapack tables ({@link ChiselTables}, {@link SledgehammerUpgradeData}): one server
 * reload listener reads both directories on every datapack load ({@code /reload} included), and
 * the loader's datapack sync hook sends the same files to each client
 * ({@link DataTablesSyncPayload}), which applies them exactly like the server - the chisel,
 * the upgrade prediction and JEI then show what the server does.
 *
 * <p>Registered by each loader: Fabric {@code ResourceLoader#registerReloadListener}, NeoForge
 * {@code AddServerReloadListenersEvent}, Forge {@code AddReloadListenerEvent}; synced from Fabric
 * {@code ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS} and NeoForge/Forge {@code OnDatapackSyncEvent}.
 */
public final class ModDataTables {

    private static final Logger LOGGER = LoggerFactory.getLogger("simplebuilding/data_tables");

    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath("simplebuilding", "data_tables");

    /** The raw files of the last server load, kept for the client sync (file id -> JSON text). */
    private static volatile Map<String, String> lastChisel = Map.of();
    private static volatile Map<String, String> lastUpgrades = Map.of();

    private ModDataTables() {
    }

    /** Files in the {@code simplebuilding} namespace first, then all others, each group in id order. */
    static List<Map.Entry<Identifier, JsonElement>> inApplyOrder(Map<Identifier, JsonElement> files) {
        List<Map.Entry<Identifier, JsonElement>> entries = new ArrayList<>(files.entrySet());
        entries.sort(Comparator.<Map.Entry<Identifier, JsonElement>, Boolean>comparing(
                        e -> !"simplebuilding".equals(e.getKey().getNamespace()))
                .thenComparing(e -> e.getKey().toString()));
        return entries;
    }

    /** A fresh reload listener for the loader to register. */
    public static SimplePreparableReloadListener<Map<String, Map<Identifier, String>>> reloadListener() {
        return new SimplePreparableReloadListener<>() {
            @Override
            protected Map<String, Map<Identifier, String>> prepare(ResourceManager manager, ProfilerFiller profiler) {
                Map<String, Map<Identifier, String>> result = new HashMap<>();
                result.put(ChiselTables.DIRECTORY, read(manager, ChiselTables.DIRECTORY));
                result.put(SledgehammerUpgradeData.DIRECTORY, read(manager, SledgehammerUpgradeData.DIRECTORY));
                return result;
            }

            @Override
            protected void apply(Map<String, Map<Identifier, String>> files, ResourceManager manager, ProfilerFiller profiler) {
                applyFiles(toStrings(files.get(ChiselTables.DIRECTORY)), toStrings(files.get(SledgehammerUpgradeData.DIRECTORY)));
                lastChisel = toStrings(files.get(ChiselTables.DIRECTORY));
                lastUpgrades = toStrings(files.get(SledgehammerUpgradeData.DIRECTORY));
            }
        };
    }

    private static Map<Identifier, String> read(ResourceManager manager, String directory) {
        Map<Identifier, String> files = new TreeMap<>();
        FileToIdConverter lister = FileToIdConverter.json(directory);
        for (Map.Entry<Identifier, Resource> entry : lister.listMatchingResources(manager).entrySet()) {
            Identifier id = lister.fileToId(entry.getKey());
            try (Reader reader = entry.getValue().openAsReader()) {
                files.put(id, JsonParser.parseReader(reader).toString());
            } catch (Exception e) {
                LOGGER.warn("Could not read {} ({}): {}", id, directory, e.toString());
            }
        }
        return files;
    }

    private static Map<String, String> toStrings(Map<Identifier, String> files) {
        Map<String, String> result = new TreeMap<>();
        if (files != null) {
            files.forEach((id, json) -> result.put(id.toString(), json));
        }
        return Map.copyOf(result);
    }

    /** Applies raw files (id -> JSON text) to both tables; the server after a load, the client on sync. */
    public static void applyFiles(Map<String, String> chisel, Map<String, String> upgrades) {
        ChiselTables.apply(parse(chisel));
        SledgehammerUpgradeData.apply(parse(upgrades));
    }

    private static Map<Identifier, JsonElement> parse(Map<String, String> files) {
        Map<Identifier, JsonElement> result = new HashMap<>();
        files.forEach((id, json) -> {
            Identifier key = Identifier.tryParse(id);
            if (key != null) {
                try {
                    result.put(key, JsonParser.parseString(json));
                } catch (RuntimeException e) {
                    LOGGER.warn("Could not parse {}: {}", id, e.toString());
                }
            }
        });
        return result;
    }

    /** The loader's datapack sync hook: sends the server's tables to {@code player}. */
    public static void sync(ServerPlayer player) {
        if (PlatformServices.canSendToPlayer(player, DataTablesSyncPayload.ID)) {
            PlatformServices.sendToPlayer(player, new DataTablesSyncPayload(lastChisel, lastUpgrades));
        }
    }

    /** Client side of {@link DataTablesSyncPayload}. */
    public static void receive(DataTablesSyncPayload payload) {
        applyFiles(payload.chisel(), payload.upgrades());
    }
}
