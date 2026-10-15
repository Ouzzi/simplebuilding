package com.simplebuilding.modules.simplestats;

import com.simplebuilding.modules.simplestats.data.StatKeys;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;

/** Display names of statistic keys, built from translation keys only (works for ids of any mod, on client and server). */
public final class StatNames {
    private StatNames() {}

    public static Component total(String key) { return Component.translatable("stat.simplestats." + key); }
    public static Component record(String key) { return Component.translatable("stat.simplestats.record." + key); }
    public static Component list(String list) { return Component.translatable("stat.simplestats.list." + list); }

    /** Name of a list entry: block, mob (with variant), item, or a damage type. */
    public static Component entry(String list, String key) {
        return switch (list) {
            case StatKeys.L_BLOCKS, StatKeys.L_BRUSHED -> byId("block", key);
            case StatKeys.L_CATCHES -> byId("item", key);
            case StatKeys.L_KILLS -> byId("entity", key);
            case StatKeys.L_BLOCKED_BY -> isEntityId(key) ? byId("entity", key) : damageType(key);
            case StatKeys.L_KILL_VARIANTS, StatKeys.L_SHEARED -> mob(key);
            case StatKeys.L_ABSORBED -> damageType(key);
            case StatKeys.L_CATCH_KINDS -> Component.translatable("stat.simplestats.catch." + key);
            default -> Component.literal(key);
        };
    }

    private static boolean isEntityId(String key) {
        Identifier id = Identifier.tryParse(key);
        return id != null && net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(id);
    }

    private static Component damageType(String key) {
        Identifier id = Identifier.tryParse(key);
        return id == null ? Component.literal(key) : Component.translatableWithFallback("stat.simplestats.damage." + id.getPath(), title(id.getPath()));
    }

    private static Component byId(String kind, String key) {
        Identifier id = Identifier.tryParse(key);
        return id == null ? Component.literal(key) : Component.translatableWithFallback(kind + "." + id.getNamespace() + "." + id.getPath().replace('/', '.'), title(id.getPath()));
    }

    /** {@code minecraft:cat#tabby+baby} becomes "Cat (Tabby, Baby)"; without a variant just the mob name. */
    public static Component mob(String key) {
        int hash = key.indexOf('#');
        if (hash < 0) return byId("entity", key);
        List<Component> parts = new ArrayList<>();
        for (String part : key.substring(hash + 1).split("\\+")) parts.add(variantPart(part));
        var joined = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) joined.append(", ");
            joined.append(parts.get(i));
        }
        return Component.translatable("stat.simplestats.variant", byId("entity", key.substring(0, hash)), joined);
    }

    private static Component variantPart(String part) {
        if (part.equals("baby") || part.equals("charged")) return Component.translatable("stat.simplestats.variant." + part);
        if (part.startsWith("size_")) return Component.translatable("stat.simplestats.variant.size", part.substring(5));
        if (DyeColor.byName(part, null) != null) return Component.translatable("color.minecraft." + part);
        return Component.literal(title(part));
    }

    /** "stone_brick" becomes "Stone Brick" (readable fallback when no translation exists). */
    static String title(String path) {
        StringBuilder out = new StringBuilder();
        for (String word : path.split("[_/]")) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return out.toString();
    }
}
