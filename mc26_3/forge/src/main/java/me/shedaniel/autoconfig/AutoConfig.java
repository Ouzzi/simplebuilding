package me.shedaniel.autoconfig;

import com.google.gson.*;
import java.nio.file.*;
import java.util.concurrent.ConcurrentHashMap;

/** Forge 26.3 compatibility entrypoint backed by validated, atomic JSON storage. */
public final class AutoConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ConcurrentHashMap<Class<?>, ConfigHolder<?>> HOLDERS = new ConcurrentHashMap<>();
    private static final long MAX_BYTES = 1024 * 1024;
    private AutoConfig() {}

    @SuppressWarnings("unchecked")
    public static <T extends ConfigData> ConfigHolder<T> getConfigHolder(Class<T> type) {
        return (ConfigHolder<T>) HOLDERS.computeIfAbsent(type, _ -> {
            Path path = path(type);
            T config = read(path, type);
            if (!Files.exists(path)) write(path, config);
            return new ConfigHolder<T>() {
                public T getConfig() { return config; }
                public void save() { write(path, config); }
            };
        });
    }
    public static Path path(Class<?> type) {
        String name = type.getAnnotation(me.shedaniel.autoconfig.annotation.Config.class).name();
        if (!name.matches("[a-z][a-z0-9_]*")) throw new IllegalArgumentException("Unsafe config name");
        return net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve(name + ".json");
    }
    public static <T extends ConfigData> T read(Path path, Class<T> type) {
        T fallback = defaults(type);
        if (!Files.exists(path)) return fallback;
        try {
            if (!Files.isRegularFile(path) || Files.size(path) > MAX_BYTES) throw new IllegalArgumentException("Config exceeds limit");
            T config = GSON.fromJson(Files.readString(path), type);
            if (config == null) throw new IllegalArgumentException("Empty config");
            fillMissing(config, fallback);
            config.validatePostLoad();
            return config;
        } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger("simplebuilding-config").warn("Invalid configuration {}; original retained, using defaults", path, ex);
            return fallback;
        }
    }
    private static <T extends ConfigData> T defaults(Class<T> type) {
        try { T config = type.getDeclaredConstructor().newInstance(); config.validatePostLoad(); return config; }
        catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
    }
    private static void fillMissing(Object config, Object defaults) throws IllegalAccessException {
        for (var field : config.getClass().getFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
            Object value = field.get(config), fallback = field.get(defaults);
            if (value == null) field.set(config, fallback);
            else if (fallback != null && !field.getType().isPrimitive() && field.getType().getPackageName().startsWith("com.simplebuilding")) fillMissing(value, fallback);
        }
    }
    public static void write(Path path, ConfigData config) {
        config.validatePostLoad();
        String json = GSON.toJson(config);
        if (json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_BYTES) throw new IllegalArgumentException("Config exceeds limit");
        try {
            Path parent = path.toAbsolutePath().getParent(); Files.createDirectories(parent);
            Path temporary = Files.createTempFile(parent, path.getFileName().toString(), ".tmp");
            try {
                Files.writeString(temporary, json);
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally { Files.deleteIfExists(temporary); }
        } catch (java.io.IOException ex) { throw new IllegalStateException("Cannot save " + path, ex); }
    }
}
