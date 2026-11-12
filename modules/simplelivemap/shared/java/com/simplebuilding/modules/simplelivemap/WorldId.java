package com.simplebuilding.modules.simplelivemap;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * The world fingerprint of the live map (N34): a random id in {@code <world>/simplelivemap/world.json}, made once. It is what
 * the self-host lock notes and what livemap.simplemods.de uses to recognise a world from the archive. Random, not from the seed:
 * it tells nothing about the world. A copied world folder keeps its id (it is the same world).
 */
public final class WorldId {
    public static final String FILE = "world.json";

    private WorldId() {}

    public static String of(Path livemapRoot) {
        Path file = livemapRoot.resolve(FILE);
        try {
            JsonObject o = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            String id = o.get("id").getAsString();
            return UUID.fromString(id).toString();
        } catch (IOException | RuntimeException e) {
            String id = UUID.randomUUID().toString();
            JsonObject o = new JsonObject();
            o.addProperty("id", id);
            try {
                Files.createDirectories(livemapRoot);
                Path tmp = file.resolveSibling(FILE + ".tmp");
                Files.writeString(tmp, o.toString(), StandardCharsets.UTF_8);
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException | RuntimeException ex) {
                SimpleLivemap.LOG.warn("Simple Livemap: could not write {}: {}", file, ex.toString());
            }
            return id;
        }
    }
}
