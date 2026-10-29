package com.simplelib.api.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

/**
 * One config command for every option of a module (owner 24.10., CMD24, docs/ai/PLAN-B-CMDCFG24-2026-10-24.md), the same
 * shape as SimpleBuilding's {@code /simplebuilding config}:
 * <pre>
 * /&lt;modid&gt; config [list [filter] | get &lt;option&gt; | set &lt;option&gt; &lt;value&gt; | reset &lt;option&gt; | help]
 * /simpleclient &lt;modid&gt; ...   (client configs, a client-side command)
 * </pre>
 * A module announces its config once, after loading it:
 * {@code ConfigCommands.server(MOD_ID).json(X::toJson, X::apply).file(dir.resolve(X.FILE)).register();}
 * The options are the leaves of the config's JSON ({@code group.field}), so a new field is reachable at once. The type comes
 * from the default value; the config class keeps its own bounds ({@code apply}/{@code normalize}), and a value it changed
 * is reported as clamped. Set and reset save the file and take effect at once (the modules read their fields when used);
 * options that are only read at start are marked with {@link Builder#restart} and get a hint. The Fabric entry of SimpleLib
 * adds every announced config under {@code /<modid>} in a late registration phase, so an existing root keeps its own
 * permission check; the config node itself needs permission level 2. Client configs get their own root
 * {@code /simpleclient}: Fabric runs a client command before the server's, so a client root {@code /<modid>} would hide
 * the server's {@code /<modid> ...} (see {@link #forwardToServer} for client roots that must share the name).
 */
public final class ConfigCommands {
    /** Value kinds, from the default value of an option. */
    public enum Kind { BOOL, INT, DECIMAL, TEXT, LIST, OBJECT }

    /** Outcome of {@link Spec#set} and {@link Spec#reset}. */
    public enum Status { SET, CLAMPED, UNKNOWN, INVALID, FAILED }

    public record Option(String path, Kind kind, JsonElement defaultValue) {}

    public record Result(Status status, String path, String value) {
        public boolean ok() {
            return status == Status.SET || status == Status.CLAMPED;
        }
    }

    /** Feedback to a command source (server: chat of the source; client: the local chat). */
    public interface Io<S> {
        void info(S source, Component text);

        /** A change: on the server it is also shown to the other operators like Vanilla's admin messages. */
        void changed(S source, Component text);

        void fail(S source, Component text);
    }

    public static final Io<CommandSourceStack> SERVER_IO = new Io<>() {
        @Override public void info(CommandSourceStack s, Component t) { s.sendSuccess(() -> t, false); }
        @Override public void changed(CommandSourceStack s, Component t) { s.sendSuccess(() -> t, true); }
        @Override public void fail(CommandSourceStack s, Component t) { s.sendFailure(t); }
    };

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Pattern KEY = Pattern.compile("[A-Za-z0-9_-]+");
    private static final Map<String, Spec> SERVER = new LinkedHashMap<>();
    private static final Map<String, Spec> CLIENT = new LinkedHashMap<>();

    private ConfigCommands() {}

    /** A server config of {@code modId} ({@code /<modid> config}). */
    public static Builder server(String modId) {
        return new Builder(modId, false);
    }

    /** A client config of {@code modId} ({@code /simpleclient <modid>}, registered by the client entry only). */
    public static Builder client(String modId) {
        return new Builder(modId, true);
    }

    public static synchronized List<Spec> specs(boolean client) {
        return List.copyOf((client ? CLIENT : SERVER).values());
    }

    public static synchronized Spec spec(String modId, boolean client) {
        return (client ? CLIENT : SERVER).get(modId);
    }

    /**
     * A Simple Optimization sub-mod file ({@code config/<id>.json}, {@code OptSettings}): read from disk on every call, written
     * back complete on set/reset. The sub-mods read it once while the game starts, so every option needs a restart; numbers
     * are bounded when the area reads them, not here.
     */
    public static Builder optSettings(Builder b, Path dir, String file, Map<String, Map<String, Object>> defaults) {
        Path path = dir.resolve(file);
        Supplier<JsonObject> read = () -> {
            JsonObject root = null;
            try {
                if (Files.isRegularFile(path)) root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            } catch (Exception ignored) {
                // unreadable: the defaults show, the next write replaces the file
            }
            return JsonParser.parseString(com.simplebuilding.framework.api.OptSettings.of(root, defaults).json()).getAsJsonObject();
        };
        b.json(read, json -> {
            try {
                Files.createDirectories(dir);
                com.simplebuilding.framework.api.ConfigFiles.writeAtomic(path,
                        com.simplebuilding.framework.api.OptSettings.of(json, defaults).json());
            } catch (Exception e) {
                com.simplelib.SimpleLib.LOG.warn("Could not write {}: {}", file, e.toString());
            }
        });
        b.defaults(() -> JsonParser.parseString(com.simplebuilding.framework.api.OptSettings.of(null, defaults).json()).getAsJsonObject());
        b.save(() -> {});
        b.restart("areas", "conflictScan");
        for (String area : defaults.keySet()) b.choices("areas." + area + ".mode", "auto", "ours", "theirs", "off");
        return b;
    }

    /** Root of the client configs. */
    public static final String CLIENT_ROOT = "simpleclient";

    /** Adds {@code /<modid> config} (server) or {@code /simpleclient <modid>} (client) for every announced config of that side. */
    public static <S> void registerAll(CommandDispatcher<S> dispatcher, boolean client, Predicate<S> requires, Io<S> io) {
        for (Spec spec : specs(client)) {
            dispatcher.register(LiteralArgumentBuilder.<S>literal(client ? CLIENT_ROOT : spec.modId).then(tree(spec, requires, io)));
        }
    }

    /**
     * For a client command root that has the name of a server command ({@code /simplemaps picture} next to the server's
     * {@code /simplemaps web}): Fabric executes client commands first and does not pass on a command whose root it knows,
     * so the client tree hides the server's subcommands. This greedy argument sends everything the client tree does not
     * know to the server ({@code send} must bypass the client command hook, e.g. a raw command packet).
     */
    public static <S> RequiredArgumentBuilder<S, String> forwardToServer(String prefix, Consumer<String> send) {
        return RequiredArgumentBuilder.<S, String>argument("server_command", StringArgumentType.greedyString()).executes(c -> {
            send.accept(prefix + " " + StringArgumentType.getString(c, "server_command"));
            return 1;
        });
    }

    public static final class Builder {
        private final String modId;
        private final boolean client;
        private Supplier<JsonObject> read;
        private Consumer<JsonObject> apply;
        private Supplier<JsonObject> defaults;
        private Runnable save;
        private Path file;
        private Set<String> keys;
        private final List<String> restart = new ArrayList<>();
        private final Set<String> secret = new LinkedHashSet<>();
        private final Map<String, List<String>> choices = new LinkedHashMap<>();
        private Runnable after = () -> {};

        private Builder(String modId, boolean client) {
            this.modId = modId;
            this.client = client;
        }

        /** Static configs: the current values as JSON and the loader that reads (and clamps) such a JSON. */
        public Builder json(Supplier<JsonObject> read, Consumer<JsonObject> apply) {
            this.read = read;
            this.apply = apply;
            return this;
        }

        /**
         * Object configs (Gson): the current object, how a new object replaces it (normalise there) and a fresh object with
         * the defaults (normalised the same way).
         */
        public <T> Builder gson(Class<T> type, Supplier<T> current, Consumer<T> replace, Supplier<T> fresh) {
            Gson plain = new Gson();
            this.read = () -> plain.toJsonTree(current.get()).getAsJsonObject();
            this.apply = json -> replace.accept(plain.fromJson(json, type));
            this.defaults = () -> plain.toJsonTree(fresh.get()).getAsJsonObject();
            return this;
        }

        /** The defaults, when {@code apply(empty)} would not give them (otherwise taken from a snapshot of the live config). */
        public Builder defaults(Supplier<JsonObject> defaults) {
            this.defaults = defaults;
            return this;
        }

        /** Saves by writing the current JSON to this file (atomic). */
        public Builder file(Path file) {
            this.file = file;
            return this;
        }

        /** {@link #file(Path)} for {@code dir/name}; no file when {@code dir} is null (tests without a config folder). */
        public Builder file(Path dir, String name) {
            return file(dir == null ? null : dir.resolve(name));
        }

        /** Saves through the module's own method instead of {@link #file}. */
        public Builder save(Runnable save) {
            this.save = save;
            return this;
        }

        /** Only these top-level keys are options (a file that keeps keys of other owners). */
        public Builder keys(Collection<String> keys) {
            this.keys = new LinkedHashSet<>(keys);
            return this;
        }

        /** Options (exact paths or group prefixes) that are only read at start: set/reset add a restart hint. */
        public Builder restart(String... paths) {
            restart.addAll(List.of(paths));
            return this;
        }

        /** Options never shown in chat (tokens, passwords): list/get/set print {@code ***}. */
        public Builder secret(String... paths) {
            secret.addAll(List.of(paths));
            return this;
        }

        /** A text option with a fixed set of values (suggested, checked case-insensitively, stored as given here). */
        public Builder choices(String path, String... values) {
            choices.put(path, List.of(values));
            return this;
        }

        /** A text option stored as the lower-case name of an enum constant. */
        public Builder choices(String path, Class<? extends Enum<?>> type) {
            List<String> names = new ArrayList<>();
            for (Enum<?> e : type.getEnumConstants()) names.add(e.name().toLowerCase(Locale.ROOT));
            choices.put(path, List.copyOf(names));
            return this;
        }

        /** Runs after every saved change (client sync and the like). */
        public Builder after(Runnable after) {
            this.after = after;
            return this;
        }

        /** The spec without announcing it (tests). */
        public Spec build() {
            if (read == null || apply == null) throw new IllegalStateException("config of " + modId + " needs json(...) or gson(...)");
            return new Spec(this);
        }

        /** Announces the config; a second call for the same mod and side replaces the first. */
        public Spec register() {
            Spec spec = build();
            synchronized (ConfigCommands.class) {
                (client ? CLIENT : SERVER).put(modId, spec);
            }
            return spec;
        }
    }

    public static final class Spec {
        private final String modId;
        private final boolean client;
        private final Supplier<JsonObject> read;
        private final Consumer<JsonObject> apply;
        private final Supplier<JsonObject> defaultsSource;
        private final Runnable save;
        private final Path file;
        private final Set<String> keys;
        private final List<String> restart;
        private final Set<String> secret;
        private final Map<String, List<String>> choices;
        private final Runnable after;
        private JsonObject defaults;

        private Spec(Builder b) {
            modId = b.modId;
            client = b.client;
            read = b.read;
            apply = b.apply;
            defaultsSource = b.defaults;
            save = b.save;
            file = b.file;
            keys = b.keys;
            restart = List.copyOf(b.restart);
            secret = Set.copyOf(b.secret);
            choices = Map.copyOf(b.choices);
            after = b.after;
        }

        public String modId() {
            return modId;
        }

        public boolean client() {
            return client;
        }

        /** The file {@link Builder#file} writes, or null (own save method or none). */
        public Path file() {
            return file;
        }

        /** The literal under the root: {@code config} (under {@code /<modid>}) or the mod id (under {@code /simpleclient}). */
        public String literal() {
            return client ? modId : "config";
        }

        /** The command as typed: {@code /<modid> config} or {@code /simpleclient <modid>}. */
        public String command() {
            return client ? "/" + CLIENT_ROOT + " " + modId : "/" + modId + " config";
        }

        /** The defaults; without a source a snapshot: current values, {@code apply(empty)}, current values back. */
        public synchronized JsonObject defaults() {
            if (defaults == null) {
                if (defaultsSource != null) {
                    defaults = defaultsSource.get();
                } else {
                    JsonObject live = read.get().deepCopy();
                    apply.accept(new JsonObject());
                    defaults = read.get().deepCopy();
                    apply.accept(live);
                }
            }
            return defaults;
        }

        private JsonObject owned(JsonObject json) {
            if (keys == null) return json;
            JsonObject out = new JsonObject();
            for (String key : keys) if (json.has(key)) out.add(key, json.get(key));
            return out;
        }

        /** Every option in file order: the leaves of the current JSON, plus options only the defaults have. */
        public List<Option> options() {
            Map<String, JsonElement> current = new LinkedHashMap<>(), def = new LinkedHashMap<>();
            flatten("", owned(read.get()), current);
            flatten("", owned(defaults()), def);
            Map<String, JsonElement> all = new LinkedHashMap<>(current);
            for (var e : def.entrySet()) {
                boolean clash = all.keySet().stream().anyMatch(p -> p.startsWith(e.getKey() + ".") || e.getKey().startsWith(p + "."));
                if (!clash) all.putIfAbsent(e.getKey(), e.getValue());
            }
            List<Option> out = new ArrayList<>();
            for (var e : all.entrySet()) {
                JsonElement d = def.getOrDefault(e.getKey(), e.getValue());
                Kind kind = choices.containsKey(e.getKey()) ? Kind.TEXT : kind(d);
                out.add(new Option(e.getKey(), kind, d));
            }
            return out;
        }

        public Option option(String path) {
            for (Option o : options()) if (o.path().equals(path)) return o;
            return null;
        }

        /** The stored value of {@code path} (the default if the config does not write it). */
        public JsonElement value(String path) {
            JsonElement v = at(read.get(), path);
            if (v == null) {
                Option o = option(path);
                v = o == null ? null : o.defaultValue();
            }
            return v;
        }

        public boolean restart(String path) {
            for (String r : restart) if (path.equals(r) || path.startsWith(r + ".")) return true;
            return false;
        }

        /** The value as shown in chat ({@code ***} for a secret option). */
        public String shown(String path, JsonElement value) {
            return secret.contains(path) ? "***" : show(value);
        }

        public List<String> choices(String path) {
            return choices.getOrDefault(path, List.of());
        }

        public Result set(String path, String text) {
            Option o = option(path);
            if (o == null) return new Result(Status.UNKNOWN, path, text);
            JsonElement value = parse(o, text);
            if (value == null) return new Result(Status.INVALID, path, text);
            return write(o, value);
        }

        public Result reset(String path) {
            Option o = option(path);
            if (o == null) return new Result(Status.UNKNOWN, path, "");
            return write(o, o.defaultValue().deepCopy());
        }

        private Result write(Option o, JsonElement value) {
            JsonObject next = read.get().deepCopy();
            put(next, o.path(), value);
            apply.accept(next);
            JsonElement stored = value(o.path());
            String shown = shown(o.path(), stored);
            try {
                if (save != null) {
                    save.run();
                } else if (file != null) {
                    Files.createDirectories(file.toAbsolutePath().getParent());
                    com.simplebuilding.framework.api.ConfigFiles.writeAtomic(file, GSON.toJson(read.get()));
                }
            } catch (Exception e) {
                com.simplelib.SimpleLib.LOG.warn("Could not save the config of {}: {}", modId, e.toString());
                return new Result(Status.FAILED, o.path(), shown);
            }
            after.run();
            return new Result(same(value, stored) ? Status.SET : Status.CLAMPED, o.path(), shown);
        }

        private JsonElement parse(Option o, String raw) {
            String text = raw.trim();
            List<String> allowed = choices(o.path());
            if (!allowed.isEmpty()) {
                for (String c : allowed) if (c.equalsIgnoreCase(text)) return new JsonPrimitive(c);
                return null;
            }
            return ConfigCommands.parse(o.kind(), text, o.defaultValue());
        }
    }

    // ------------------------------------------------------------ JSON helpers (public for the tests)

    static void flatten(String prefix, JsonObject o, Map<String, JsonElement> out) {
        for (var e : o.entrySet()) {
            String path = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
            JsonElement v = e.getValue();
            if (v.isJsonObject() && groupable(v.getAsJsonObject())) flatten(path, v.getAsJsonObject(), out);
            else out.put(path, v);
        }
    }

    /** A JSON object becomes a group of options when it has keys and every key is a plain name (else it is one value). */
    private static boolean groupable(JsonObject o) {
        if (o.isEmpty()) return false;
        for (String k : o.keySet()) if (!KEY.matcher(k).matches()) return false;
        return true;
    }

    public static Kind kind(JsonElement v) {
        if (v == null || v.isJsonNull()) return Kind.TEXT;
        if (v.isJsonArray()) return Kind.LIST;
        if (v.isJsonObject()) return Kind.OBJECT;
        JsonPrimitive p = v.getAsJsonPrimitive();
        if (p.isBoolean()) return Kind.BOOL;
        if (p.isNumber()) {
            Number n = p.getAsNumber();
            if (n instanceof Integer || n instanceof Long || n instanceof Short || n instanceof Byte) return Kind.INT;
            if (n instanceof Double || n instanceof Float) return Kind.DECIMAL;
            String s = n.toString();
            return s.contains(".") || s.contains("e") || s.contains("E") ? Kind.DECIMAL : Kind.INT;
        }
        return Kind.TEXT;
    }

    /** {@code text} as a value of {@code kind}, or null when it is not one. */
    public static JsonElement parse(Kind kind, String text, JsonElement like) {
        try {
            switch (kind) {
                case BOOL -> {
                    if (text.equalsIgnoreCase("true")) return new JsonPrimitive(true);
                    if (text.equalsIgnoreCase("false")) return new JsonPrimitive(false);
                    return null;
                }
                case INT -> {
                    long l = Long.parseLong(text);
                    return l == (int) l ? new JsonPrimitive((int) l) : new JsonPrimitive(l);
                }
                case DECIMAL -> {
                    double d = Double.parseDouble(text);
                    return Double.isFinite(d) ? new JsonPrimitive(d) : null;
                }
                case TEXT -> {
                    String s = text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"") ? text.substring(1, text.length() - 1) : text;
                    return new JsonPrimitive(s);
                }
                case LIST -> {
                    if (text.startsWith("[")) {
                        JsonElement e = JsonParser.parseString(text);
                        return e.isJsonArray() ? e : null;
                    }
                    Kind element = like != null && like.isJsonArray() && !like.getAsJsonArray().isEmpty() ? kind(like.getAsJsonArray().get(0)) : Kind.TEXT;
                    JsonArray out = new JsonArray();
                    for (String part : text.split(",")) {
                        String s = part.trim();
                        if (s.isEmpty()) continue;
                        JsonElement e = parse(element == Kind.LIST || element == Kind.OBJECT ? Kind.TEXT : element, s, null);
                        if (e == null) return null;
                        out.add(e);
                    }
                    return out;
                }
                case OBJECT -> {
                    JsonElement e = JsonParser.parseString(text);
                    return e.isJsonObject() ? e : null;
                }
                default -> {
                    return null;
                }
            }
        } catch (RuntimeException e) {
            return null;
        }
    }

    static JsonElement at(JsonObject root, String path) {
        JsonElement cur = root;
        for (String part : path.split("\\.")) {
            if (cur == null || !cur.isJsonObject() || !cur.getAsJsonObject().has(part)) return null;
            cur = cur.getAsJsonObject().get(part);
        }
        return cur;
    }

    static void put(JsonObject root, String path, JsonElement value) {
        String[] parts = path.split("\\.");
        JsonObject cur = root;
        for (int i = 0; i < parts.length - 1; i++) {
            JsonElement next = cur.get(parts[i]);
            if (next == null || !next.isJsonObject()) {
                next = new JsonObject();
                cur.add(parts[i], next);
            }
            cur = next.getAsJsonObject();
        }
        cur.add(parts[parts.length - 1], value);
    }

    /** Equal values; numbers within float precision (a float field stores 0.3 as 0.30000001). */
    static boolean same(JsonElement a, JsonElement b) {
        if (a == null || b == null) return a == b;
        if (a.isJsonPrimitive() && b.isJsonPrimitive() && a.getAsJsonPrimitive().isNumber() && b.getAsJsonPrimitive().isNumber()) {
            double x = a.getAsDouble(), y = b.getAsDouble();
            return Math.abs(x - y) <= 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
        }
        return a.equals(b);
    }

    public static String show(JsonElement v) {
        if (v == null || v.isJsonNull()) return "-";
        return v.isJsonPrimitive() ? v.getAsString() : v.toString();
    }

    // ------------------------------------------------------------ command tree

    private static Component type(Kind kind) {
        return Component.translatable("commands.simplelib.config.type." + kind.name().toLowerCase(Locale.ROOT));
    }

    /** {@code config} (or the mod id under {@code /simpleclient}) with list, get, set, reset and help for one spec. */
    public static <S> LiteralArgumentBuilder<S> tree(Spec spec, Predicate<S> requires, Io<S> io) {
        SuggestionProvider<S> options = (c, b) -> SharedSuggestionProvider.suggest(spec.options().stream().map(Option::path), b);
        SuggestionProvider<S> groups = (c, b) -> SharedSuggestionProvider.suggest(spec.options().stream()
                .map(o -> o.path().contains(".") ? o.path().substring(0, o.path().lastIndexOf('.')) : o.path()).distinct(), b);
        SuggestionProvider<S> values = (c, b) -> {
            Option o = spec.option(StringArgumentType.getString(c, "option"));
            if (o == null) return b.buildFuture();
            Set<String> out = new LinkedHashSet<>(spec.choices(o.path()));
            if (o.kind() == Kind.BOOL) out.addAll(List.of("true", "false"));
            if (!spec.secret.contains(o.path())) out.add(show(spec.value(o.path())));
            out.add(show(o.defaultValue()));
            return SharedSuggestionProvider.suggest(out, b);
        };
        return LiteralArgumentBuilder.<S>literal(spec.literal()).requires(requires)
                .executes(c -> list(spec, "", c.getSource(), io))
                .then(LiteralArgumentBuilder.<S>literal("help").executes(c -> {
                    io.info(c.getSource(), Component.translatable("commands.simplelib.config.help", spec.command())
                            .withStyle(ChatFormatting.YELLOW));
                    return 1;
                }))
                .then(LiteralArgumentBuilder.<S>literal("list")
                        .executes(c -> list(spec, "", c.getSource(), io))
                        .then(RequiredArgumentBuilder.<S, String>argument("filter", StringArgumentType.word()).suggests(groups)
                                .executes(c -> list(spec, StringArgumentType.getString(c, "filter"), c.getSource(), io))))
                .then(LiteralArgumentBuilder.<S>literal("get")
                        .then(RequiredArgumentBuilder.<S, String>argument("option", StringArgumentType.word()).suggests(options)
                                .executes(c -> get(spec, c, io))))
                .then(LiteralArgumentBuilder.<S>literal("set")
                        .then(RequiredArgumentBuilder.<S, String>argument("option", StringArgumentType.word()).suggests(options)
                                .then(RequiredArgumentBuilder.<S, String>argument("value", StringArgumentType.greedyString()).suggests(values)
                                        .executes(c -> report(spec, spec.set(StringArgumentType.getString(c, "option"),
                                                StringArgumentType.getString(c, "value")), "commands.simplelib.config.set", c.getSource(), io)))))
                .then(LiteralArgumentBuilder.<S>literal("reset")
                        .then(RequiredArgumentBuilder.<S, String>argument("option", StringArgumentType.word()).suggests(options)
                                .executes(c -> report(spec, spec.reset(StringArgumentType.getString(c, "option")),
                                        "commands.simplelib.config.reset", c.getSource(), io))));
    }

    private static <S> int list(Spec spec, String filter, S source, Io<S> io) {
        int count = 0;
        for (Option o : spec.options()) {
            if (!filter.isEmpty() && !(o.path().equals(filter) || o.path().startsWith(filter + "."))) continue;
            JsonElement v = spec.value(o.path());
            boolean changed = !same(v, o.defaultValue());
            io.info(source, Component.literal(o.path() + " = " + spec.shown(o.path(), v)).withStyle(changed ? ChatFormatting.GOLD : ChatFormatting.GRAY));
            count++;
        }
        if (count == 0) {
            io.fail(source, Component.translatable("commands.simplelib.config.unknown", filter));
            return 0;
        }
        io.info(source, Component.translatable("commands.simplelib.config.list", spec.modId(), count).withStyle(ChatFormatting.YELLOW));
        return count;
    }

    private static <S> int get(Spec spec, CommandContext<S> c, Io<S> io) {
        String path = StringArgumentType.getString(c, "option");
        Option o = spec.option(path);
        if (o == null) {
            io.fail(c.getSource(), Component.translatable("commands.simplelib.config.unknown", path));
            return 0;
        }
        io.info(c.getSource(), Component.translatable("commands.simplelib.config.get", path, spec.shown(path, spec.value(path)), show(o.defaultValue()), type(o.kind()))
                .withStyle(ChatFormatting.YELLOW));
        List<String> allowed = spec.choices(path);
        if (!allowed.isEmpty()) io.info(c.getSource(), Component.translatable("commands.simplelib.config.choices", String.join(", ", allowed)).withStyle(ChatFormatting.GRAY));
        if (spec.restart(path)) io.info(c.getSource(), Component.translatable("commands.simplelib.config.restart").withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static <S> int report(Spec spec, Result r, String key, S source, Io<S> io) {
        switch (r.status()) {
            case UNKNOWN -> {
                io.fail(source, Component.translatable("commands.simplelib.config.unknown", r.path()));
                return 0;
            }
            case INVALID -> {
                Option o = spec.option(r.path());
                List<String> allowed = spec.choices(r.path());
                Component expected = allowed.isEmpty() ? type(o.kind()) : Component.literal(String.join(", ", allowed));
                io.fail(source, Component.translatable("commands.simplelib.config.invalid", r.value(), expected, r.path()));
                return 0;
            }
            case FAILED -> {
                io.fail(source, Component.translatable("commands.simplelib.config.save_failed", r.path(), r.value()));
                return 0;
            }
            default -> {
            }
        }
        io.changed(source, Component.translatable(key, spec.modId(), r.path(), r.value()).withStyle(ChatFormatting.GREEN));
        if (r.status() == Status.CLAMPED) io.info(source, Component.translatable("commands.simplelib.config.clamped", r.value()).withStyle(ChatFormatting.GOLD));
        if (spec.restart(r.path())) io.info(source, Component.translatable("commands.simplelib.config.restart").withStyle(ChatFormatting.GOLD));
        return 1;
    }
}
