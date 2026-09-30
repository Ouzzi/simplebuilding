package com.simplefun.test;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.registries.BuiltInRegistries;

/** Optional producer export, obtained from the actual booted registry, never from copied balance data. */
public final class RegistryExport {
  public static void writeIfRequested() {
    String requested = System.getenv("SIMPLEFUN_EXPORT_ROOT");
    if (requested == null) return;
    Path root = Path.of(requested).toAbsolutePath().normalize();
    if (!root.getFileName().toString().equals("simplefun")
        || !root.getParent().getFileName().toString().equals("modules"))
      throw new IllegalArgumentException("Export must target modules/simplefun");
    JsonArray items = new JsonArray();
    var blockIds = BuiltInRegistries.BLOCK.keySet().stream()
        .filter(id -> id.getNamespace().equals("simplefun")).sorted().toList();
    for (var id : blockIds) {
      JsonObject entry = new JsonObject();
      entry.addProperty("id", id.toString());
      entry.addProperty("kind", "block");
      entry.addProperty("hasItem", BuiltInRegistries.ITEM.containsKey(id));
      items.add(entry);
    }
    BuiltInRegistries.ITEM.keySet().stream()
        .filter(id -> id.getNamespace().equals("simplefun") && !blockIds.contains(id))
        .sorted().forEach(id -> {
          JsonObject entry = new JsonObject();
          entry.addProperty("id", id.toString());
          items.add(entry);
        });
    JsonObject data = new JsonObject();data.add("items", items);
    try {
      Path target = root.resolve("generated/resources/wiki/items.json");
      Files.createDirectories(target.getParent());
      Files.writeString(target, new GsonBuilder().setPrettyPrinting().create().toJson(data) + "\n");
    } catch (java.io.IOException e) { throw new IllegalStateException(e); }
  }
}
