package com.simplebuilding.modules.simplelivemap.hosted;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;

/**
 * The connection of THIS world to livemap.simplemods.de: {@code <world>/simplelivemap/hosted.json} with endpoint, map name,
 * viewer address and the upload key. Per world (not in the server config), so another world of the same installation never
 * uploads into this map. The key is a secret: owner-only file where the file system allows it, never logged, never shown.
 */
public record HostedLink(String endpoint, String map, String viewer, String key) {
    public static final String FILE = "hosted.json";

    /**
     * O8 audit 4: {@code link} only when it points to the configured service ({@code hosted.endpoint} of the server config);
     * else null. The link lives in the world folder, so a downloaded world could otherwise make the mod upload its tiles
     * (and with {@code web.showPlayers} the players) to any https address its author wrote into {@code hosted.json}.
     */
    public static HostedLink trusted(HostedLink link, String configuredEndpoint) {
        if (link == null) return null;
        String configured = HostedClient.checkEndpoint(configuredEndpoint);
        return configured != null && configured.equalsIgnoreCase(link.endpoint()) ? link : null;
    }

    public static HostedLink load(Path livemapRoot) {
        try {
            JsonObject o = JsonParser.parseString(Files.readString(livemapRoot.resolve(FILE), StandardCharsets.UTF_8)).getAsJsonObject();
            String endpoint = HostedClient.checkEndpoint(o.get("endpoint").getAsString());
            String key = o.get("key").getAsString();
            if (endpoint == null || !key.matches("slm_[A-Za-z0-9_-]{20,100}")) return null;
            return new HostedLink(endpoint, str(o, "map"), str(o, "viewer"), key);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : "";
    }

    public void save(Path livemapRoot) throws IOException {
        JsonObject o = new JsonObject();
        o.addProperty("endpoint", endpoint);
        o.addProperty("map", map);
        o.addProperty("viewer", viewer);
        o.addProperty("key", key);
        Files.createDirectories(livemapRoot);
        Path tmp = livemapRoot.resolve(FILE + ".tmp");
        Files.deleteIfExists(tmp);
        try {
            Files.createFile(tmp, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        } catch (UnsupportedOperationException e) {
            Files.createFile(tmp); // Windows: the user's profile/server folder protects it
        }
        Files.writeString(tmp, o.toString(), StandardCharsets.UTF_8);
        Files.move(tmp, livemapRoot.resolve(FILE), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    public static void delete(Path livemapRoot) {
        try {
            Files.deleteIfExists(livemapRoot.resolve(FILE));
        } catch (IOException ignored) {
        }
    }

    @Override
    public String toString() {
        return "HostedLink[" + endpoint + ", " + map + ", key ***]";
    }
}
