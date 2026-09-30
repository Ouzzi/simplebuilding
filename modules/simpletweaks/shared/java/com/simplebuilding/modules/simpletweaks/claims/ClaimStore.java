package com.simplebuilding.modules.simpletweaks.claims;

import com.google.gson.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.Identifier;

/** Atomic global ledger. Legacy per-dimension SavedData files are read, never overwritten. */
public final class ClaimStore {
    public static final long MAX_BYTES = 32L * 1024 * 1024;
    public record Key(String dimension, long chunk) {
        public Key { if (!ClaimConfig.validDimension(dimension)) throw new IllegalArgumentException("Dimension"); }
    }
    public record Claim(UUID owner, Set<UUID> whitelist) {
        public Claim {
            Objects.requireNonNull(owner);
            if (whitelist.size() > ClaimConfig.MAX_TRUSTED) throw new IllegalArgumentException("Trusted player cap");
            whitelist = Set.copyOf(whitelist);
        }
        public boolean permits(UUID player) { return owner.equals(player) || whitelist.contains(player); }
    }
    private final Path path;
    private Map<Key,Claim> claims;
    public ClaimStore(Path path, Map<String,Path> legacy) throws IOException {
        this.path = path;
        Map<Key,Claim> loaded = new HashMap<>();
        if (Files.exists(path)) {
            if (Files.isSymbolicLink(path) || Files.size(path) > MAX_BYTES) throw new IOException("Unsafe claims ledger");
            var root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            if (root.get("version").getAsBigDecimal().intValueExact() != 1) throw new IOException("Unknown claim ledger version");
            for (var entry : root.getAsJsonArray("claims")) {
                var j = entry.getAsJsonObject();
                Set<UUID> trusted = new HashSet<>();
                for (var id : j.getAsJsonArray("Whitelist")) {
                    if (!trusted.add(UUID.fromString(id.getAsString()))) throw new IOException("Duplicate trusted player");
                }
                add(loaded, new Key(j.get("dimension").getAsString(), j.get("chunk").getAsBigDecimal().longValueExact()),
                        new Claim(UUID.fromString(j.get("Owner").getAsString()), trusted));
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
                    add(loaded, new Key(file.getKey(), Long.parseLong(chunk)), new Claim(owner,trusted));
                }
            }
        }
        claims = Map.copyOf(loaded);
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
    public void replace(Map<Key,Claim> next) throws IOException {
        if (next.size() > ClaimConfig.MAX_GLOBAL) throw new IOException("Claim cap");
        var snapshot = Map.copyOf(next);
        var root = new JsonObject(); root.addProperty("version",1);
        var rows = new JsonArray();
        snapshot.entrySet().stream().sorted(Comparator.comparing((Map.Entry<Key,Claim> e) -> e.getKey().dimension()).thenComparingLong(e -> e.getKey().chunk())).forEach(e -> {
            var row = new JsonObject(); row.addProperty("dimension",e.getKey().dimension()); row.addProperty("chunk",e.getKey().chunk());
            row.addProperty("Owner",e.getValue().owner().toString());
            var trusted = new JsonArray(); e.getValue().whitelist().stream().sorted().forEach(id -> trusted.add(id.toString()));
            row.add("Whitelist",trusted); rows.add(row);
        });
        root.add("claims",rows);
        atomicWrite(path, new Gson().toJson(root));
        claims = snapshot; // Publish only after persistence succeeded.
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
