package com.simplebuilding.modules.simpletweaks.claims;

import com.google.gson.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.nbt.*;
import com.simplebuilding.modules.simpletweaks.claims.ClaimRights.Rules;
import net.minecraft.resources.Identifier;

/**
 * Atomic global ledger, version 2: chunk ownership plus one rights record ("land") per owner.
 * Version 1 (chunk + whitelist) and the legacy per-dimension SavedData files are migrated in memory without gaining
 * rights (every old whitelist entry becomes {@code @member}); the first write keeps a copy of the v1 file.
 * Legacy NBT files are read, never overwritten.
 */
public final class ClaimStore {
    public static final long MAX_BYTES = 64L * 1024 * 1024;
    public static final int VERSION = 2;
    public record Key(String dimension, long chunk) {
        public Key { if (!ClaimConfig.validDimension(dimension)) throw new IllegalArgumentException("Dimension"); }
        public ClaimCode.At at() { return new ClaimCode.At(dimension, net.minecraft.world.level.ChunkPos.getX(chunk), net.minecraft.world.level.ChunkPos.getZ(chunk)); }
        public static Key of(ClaimCode.At at) { return new Key(at.dimension(), net.minecraft.world.level.ChunkPos.pack(at.x(), at.z())); }
    }
    public record Claim(UUID owner) {
        public Claim { Objects.requireNonNull(owner); }
    }
    /** Rights of one owner: the top rules for all their chunks, sections replacing them for single chunks. */
    public record Land(UUID owner, String code, Rules top, Map<Key,Rules> sections) {
        public Land {
            Objects.requireNonNull(owner); Objects.requireNonNull(top);
            code = code == null ? "" : code;
            sections = Map.copyOf(sections);
        }
        /** Section of the chunk if there is one, otherwise the top rules; the owner always has everything. */
        public int rights(Key key, UUID actor) {
            if (owner.equals(actor)) return ClaimRights.ALL;
            return actor == null ? 0 : sections.getOrDefault(key, top).of(actor);
        }
        public Rules rulesAt(Key key) { return sections.getOrDefault(key, top); }
    }
    private final Path path;
    private Map<Key,Claim> claims;
    private Map<UUID,Land> lands;
    /** Land bought by expansion (owner to chunks); absent = never bought. Kept after unclaiming: the budget belongs to the player. */
    private Map<UUID,Integer> budgets = Map.of();
    /** True while the file on disk is version 1: the first write keeps it as a backup. */
    private boolean fromV1;
    public ClaimStore(Path path, Map<String,Path> legacy) throws IOException {
        this.path = path;
        Map<Key,Claim> loaded = new HashMap<>();
        Map<Key,Set<UUID>> whitelists = new HashMap<>();
        Map<UUID,Land> loadedLands = new HashMap<>();
        if (Files.exists(path)) {
            if (Files.isSymbolicLink(path) || Files.size(path) > MAX_BYTES) throw new IOException("Unsafe claims ledger");
            var root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            int version = root.get("version").getAsBigDecimal().intValueExact();
            if (version != 1 && version != VERSION) throw new IOException("Unknown claim ledger version");
            for (var entry : root.getAsJsonArray("claims")) {
                var j = entry.getAsJsonObject();
                var key = new Key(j.get("dimension").getAsString(), j.get("chunk").getAsBigDecimal().longValueExact());
                var owner = UUID.fromString(j.get("Owner").getAsString());
                if (version == 1) {
                    Set<UUID> trusted = new HashSet<>();
                    for (var id : j.getAsJsonArray("Whitelist")) {
                        if (!trusted.add(UUID.fromString(id.getAsString()))) throw new IOException("Duplicate trusted player");
                    }
                    whitelists.put(key, trusted);
                }
                add(loaded, key, new Claim(owner));
            }
            budgets = Map.copyOf(readBudgets(root));
            if (version == 1) { fromV1 = true; loadedLands.putAll(migrate(loaded, whitelists)); }
            else for (var entry : root.getAsJsonArray("lands")) {
                var j = entry.getAsJsonObject();
                var owner = UUID.fromString(j.get("Owner").getAsString());
                var sections = new HashMap<Key,Rules>();
                for (var s : j.getAsJsonArray("sections")) {
                    var sj = s.getAsJsonObject();
                    if (sections.put(new Key(sj.get("dimension").getAsString(), sj.get("chunk").getAsBigDecimal().longValueExact()), rules(sj)) != null)
                        throw new IOException("Duplicate land section");
                }
                if (loadedLands.put(owner, new Land(owner, j.get("code").getAsString(), rules(j), sections)) != null) throw new IOException("Duplicate land");
            }
        } else {
            for (var file : legacyFiles(path.toAbsolutePath().getParent(), legacy).entrySet()) {
                if (!Files.exists(file.getValue())) continue;
                if (Files.isSymbolicLink(file.getValue()) || Files.size(file.getValue()) > MAX_BYTES) throw new IOException("Unsafe legacy claims");
                var root = NbtIo.readCompressed(file.getValue(), NbtAccounter.create(MAX_BYTES));
                var data = root.getCompound("data").orElseThrow(() -> new IOException("Missing legacy data"));
                for (String chunk : data.keySet()) {
                    var row = data.getCompound(chunk).orElseThrow(() -> new IOException("Invalid legacy claim"));
                    UUID owner = UUID.fromString(row.getString("Owner").orElseThrow());
                    Set<UUID> trusted = new HashSet<>();
                    if (row.contains("Whitelist")) {
                        var list = row.getList("Whitelist").orElseThrow();
                        for (int i = 0; i < list.size(); i++) {
                            if (!trusted.add(UUID.fromString(list.getString(i).orElseThrow()))) throw new IOException("Duplicate legacy whitelist entry");
                        }
                    }
                    var key = new Key(file.getKey(), Long.parseLong(chunk));
                    add(loaded, key, new Claim(owner));
                    whitelists.put(key, trusted);
                }
            }
            loadedLands.putAll(migrate(loaded, whitelists));
        }
        claims = Map.copyOf(loaded);
        lands = Map.copyOf(loadedLands);
    }
    private static Map<UUID,Integer> readBudgets(JsonObject root) throws IOException {
        var out = new HashMap<UUID,Integer>();
        if (!root.has("budgets")) return out;
        for (var e : root.getAsJsonArray("budgets")) {
            var j = e.getAsJsonObject();
            int chunks = j.get("chunks").getAsBigDecimal().intValueExact();
            if (chunks < 1 || chunks > ClaimConfig.MAX_CLAIMS || out.put(UUID.fromString(j.get("Owner").getAsString()), chunks) != null) throw new IOException("Invalid budget");
        }
        return out;
    }
    private static Rules rules(JsonObject j) throws IOException {
        int everyone = j.get("everyone").getAsBigDecimal().intValueExact();
        var members = new HashMap<UUID,Integer>();
        for (var m : j.getAsJsonArray("members")) {
            var mj = m.getAsJsonObject();
            if (members.put(UUID.fromString(mj.get("id").getAsString()), mj.get("rights").getAsBigDecimal().intValueExact()) != null) throw new IOException("Duplicate member");
        }
        try { return new Rules(everyone, members); }
        catch (IllegalArgumentException e) { throw new IOException("Invalid rights in claim ledger", e); }
    }
    /**
     * v1 whitelists to v2 lands without any gain of rights. All chunks of an owner share one list: it becomes the top
     * rules with {@code @member} (all old rights). Otherwise every chunk with its own non-empty list gets a section.
     */
    static Map<UUID,Land> migrate(Map<Key,Claim> claims, Map<Key,Set<UUID>> whitelists) {
        var byOwner = new HashMap<UUID,List<Key>>();
        claims.forEach((k, c) -> byOwner.computeIfAbsent(c.owner(), o -> new ArrayList<>()).add(k));
        var out = new HashMap<UUID,Land>();
        for (var e : byOwner.entrySet()) {
            var lists = e.getValue().stream().map(k -> whitelists.getOrDefault(k, Set.of())).distinct().toList();
            if (lists.size() == 1 && lists.get(0).isEmpty()) continue;
            Rules top = Rules.EMPTY; var sections = new HashMap<Key,Rules>();
            if (lists.size() == 1) top = legacyRules(lists.get(0));
            else for (var k : e.getValue()) { var wl = whitelists.getOrDefault(k, Set.of()); if (!wl.isEmpty()) sections.put(k, legacyRules(wl)); }
            var at = new HashMap<ClaimCode.At,Rules>();
            sections.forEach((k, r) -> at.put(k.at(), r));
            out.put(e.getKey(), new Land(e.getKey(), ClaimCode.generate(top, at), top, sections));
        }
        return out;
    }
    private static Rules legacyRules(Set<UUID> trusted) {
        var m = new HashMap<UUID,Integer>();
        trusted.forEach(u -> m.put(u, ClaimRights.LEGACY));
        return new Rules(0, m);
    }
    private static Map<String,Path> legacyFiles(Path root, Map<String,Path> known) throws IOException {
        var files=new LinkedHashMap<>(known);
        files.putIfAbsent("minecraft:overworld",root.resolve("data/simpletweaks_claims.dat"));
        files.putIfAbsent("minecraft:the_nether",root.resolve("DIM-1/data/simpletweaks_claims.dat"));
        files.putIfAbsent("minecraft:the_end",root.resolve("DIM1/data/simpletweaks_claims.dat"));
        Path dimensions=root.resolve("dimensions");
        if (Files.exists(dimensions)) {
            // Include unloaded custom dimensions before publishing a global ledger.
            // ponytail: bounded startup scan; index dimensions if worlds exceed 100000 entries.
            try (var walk=Files.walk(dimensions,64)) {
                var iterator=walk.iterator();int scanned=0;
                while (iterator.hasNext()) {
                    Path p=iterator.next();
                    if (++scanned>100000 || Files.isSymbolicLink(p)) throw new IOException("Unsafe or oversized legacy dimension tree");
                    if (!p.getFileName().toString().equals("simpletweaks_claims.dat") || !p.getParent().getFileName().toString().equals("data")) continue;
                    var relative=dimensions.relativize(p.getParent().getParent());
                    if (relative.getNameCount()<2) throw new IOException("Invalid legacy dimension path");
                    String id=relative.getName(0)+":"+relative.subpath(1,relative.getNameCount()).toString().replace('\\','/');
                    if (!ClaimConfig.validDimension(id)) throw new IOException("Invalid legacy dimension id");
                    files.putIfAbsent(id,p);
                }
            }
        }
        return files;
    }
    private static void add(Map<Key,Claim> claims, Key key, Claim claim) throws IOException {
        if (claims.size() >= ClaimConfig.MAX_GLOBAL || claims.putIfAbsent(key,claim) != null) throw new IOException("Claim cap or duplicate chunk");
    }
    public Map<Key,Claim> view() { return claims; }
    public Map<UUID,Land> lands() { return lands; }
    public Map<UUID,Integer> budgets() { return budgets; }
    /** Rights of an actor in a chunk: everything where nothing is claimed, everything for the owner, else the land rules. */
    public int rights(Key key, UUID actor) {
        var claim = claims.get(key);
        if (claim == null) return ClaimRights.ALL;
        if (claim.owner().equals(actor)) return ClaimRights.ALL;
        var land = lands.get(claim.owner());
        return land == null ? 0 : land.rights(key, actor);
    }
    public void replace(Map<Key,Claim> next) throws IOException { replace(next, lands); }
    public void replace(Map<Key,Claim> nextClaims, Map<UUID,Land> nextLands) throws IOException { replace(nextClaims, nextLands, budgets); }
    public void replace(Map<Key,Claim> nextClaims, Map<UUID,Land> nextLands, Map<UUID,Integer> nextBudgets) throws IOException {
        if (nextBudgets.size() > ClaimConfig.MAX_GLOBAL) throw new IOException("Budget cap");
        if (nextClaims.size() > ClaimConfig.MAX_GLOBAL) throw new IOException("Claim cap");
        var snapshot = Map.copyOf(nextClaims);
        // Rules only live while the owner still owns land; a section only for a chunk the owner still owns.
        var landSnapshot = new HashMap<UUID,Land>();
        nextLands.forEach((owner, land) -> {
            if (snapshot.values().stream().noneMatch(c -> c.owner().equals(owner))) return;
            var kept = new HashMap<Key,Rules>();
            land.sections().forEach((k, r) -> { var c = snapshot.get(k); if (c != null && c.owner().equals(owner)) kept.put(k, r); });
            landSnapshot.put(owner, new Land(owner, land.code(), land.top(), kept));
        });
        var root = new JsonObject(); root.addProperty("version",VERSION);
        var rows = new JsonArray();
        snapshot.entrySet().stream().sorted(Comparator.comparing((Map.Entry<Key,Claim> e) -> e.getKey().dimension()).thenComparingLong(e -> e.getKey().chunk())).forEach(e -> {
            var row = new JsonObject(); row.addProperty("dimension",e.getKey().dimension()); row.addProperty("chunk",e.getKey().chunk());
            row.addProperty("Owner",e.getValue().owner().toString());
            rows.add(row);
        });
        root.add("claims",rows);
        var landRows = new JsonArray();
        landSnapshot.values().stream().sorted(Comparator.comparing(l -> l.owner().toString())).forEach(l -> {
            var row = new JsonObject(); row.addProperty("Owner",l.owner().toString()); row.addProperty("code",l.code());
            writeRules(row,l.top());
            var sections = new JsonArray();
            l.sections().entrySet().stream().sorted(Comparator.comparing((Map.Entry<Key,Rules> e) -> e.getKey().dimension()).thenComparingLong(e -> e.getKey().chunk())).forEach(e -> {
                var sj = new JsonObject(); sj.addProperty("dimension",e.getKey().dimension()); sj.addProperty("chunk",e.getKey().chunk());
                writeRules(sj,e.getValue()); sections.add(sj);
            });
            row.add("sections",sections); landRows.add(row);
        });
        root.add("lands",landRows);
        var budgetRows = new JsonArray();
        nextBudgets.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> { var row = new JsonObject(); row.addProperty("Owner",e.getKey().toString()); row.addProperty("chunks",e.getValue()); budgetRows.add(row); });
        root.add("budgets",budgetRows);
        if (fromV1 && Files.exists(path)) {
            // First v2 write: keep the v1 file next to the ledger, never overwrite an existing backup.
            Path backup = path.toAbsolutePath().resolveSibling(path.getFileName().toString().replaceFirst("(\\.json)?$",".v1.json"));
            if (Files.isSymbolicLink(backup)) throw new IOException("Refusing symbolic link " + backup);
            if (!Files.exists(backup)) Files.copy(path, backup);
        }
        atomicWrite(path, new Gson().toJson(root));
        fromV1 = false;
        claims = snapshot; lands = Map.copyOf(landSnapshot); budgets = Map.copyOf(nextBudgets); // Publish only after persistence succeeded.
    }
    private static void writeRules(JsonObject row, Rules rules) {
        row.addProperty("everyone",rules.everyone());
        var members = new JsonArray();
        rules.members().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(m -> { var mj = new JsonObject(); mj.addProperty("id",m.getKey().toString()); mj.addProperty("rights",m.getValue()); members.add(mj); });
        row.add("members",members);
    }
    public static void atomicWrite(Path target, String text) throws IOException {
        Path absolute = target.toAbsolutePath();
        Files.createDirectories(absolute.getParent());
        if (Files.isSymbolicLink(absolute)) throw new IOException("Refusing symbolic link " + target);
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_BYTES) throw new IOException("Claim data size cap");
        Path temp = Files.createTempFile(absolute.getParent(), ".claims-", ".tmp");
        try {
            try (var channel = FileChannel.open(temp, StandardOpenOption.WRITE)) {
                var buffer = ByteBuffer.wrap(bytes); while (buffer.hasRemaining()) channel.write(buffer); channel.force(true);
            }
            Files.move(temp, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }
}
