package com.simplebuilding.modules.simpletweaks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.simplebuilding.framework.api.ConfigFiles;
import com.simplebuilding.modules.simpletweaks.admin.Admin;
import com.simplebuilding.modules.simpletweaks.admin.AdminConfig;
import com.simplebuilding.modules.simpletweaks.claims.ClaimConfig;
import com.simplebuilding.modules.simpletweaks.claims.ClaimStore;
import com.simplebuilding.modules.simpletweaks.play.Play;
import com.simplebuilding.modules.simpletweaks.play.PlayConfig;
import com.simplebuilding.modules.simpletweaks.protect.FreecamRule;
import com.simplebuilding.modules.simpletweaks.protect.Protect;
import com.simplebuilding.modules.simpletweaks.protect.ProtectConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

/**
 * CMD24-3: {@code /simpletweaks config} for the four record configs. The options are the four files under one tree:
 * {@code play.*} ({@code simpletweaks-play.json}), {@code admin.*}, {@code protect.*} and {@code claims.*}. A section is
 * rewritten only when one of its values changed, so a broken file the owner has not fixed yet is never overwritten by
 * an unrelated change. The records clamp and reject like they do at load; a value they refuse leaves the file as it was.
 */
public final class TweaksConfigCommand {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("simpletweaks-config");

    private TweaksConfigCommand() {}

    public static void announce() {
        com.simplelib.api.config.ConfigCommands.server("simpletweaks").json(TweaksConfigCommand::read, TweaksConfigCommand::apply)
                .defaults(TweaksConfigCommand::defaults).restart("claims").after(TweaksConfigCommand::after).register();
    }

    /** The values in the files (not the live objects, which have the partner-mod switches applied). */
    static JsonObject read() {
        JsonObject o = new JsonObject();
        o.add("play", GSON.toJsonTree(PlayConfig.load(PlayConfig.PATH)));
        o.add("admin", GSON.toJsonTree(AdminConfig.load(AdminConfig.PATH)));
        o.add("protect", GSON.toJsonTree(ProtectConfig.load(ProtectConfig.PATH)));
        o.add("claims", GSON.toJsonTree(ClaimConfig.load(Path.of("config/simpletweaks-claims.json"))));
        return o;
    }

    static JsonObject defaults() {
        JsonObject o = new JsonObject();
        o.add("play", GSON.toJsonTree(PlayConfig.DEFAULT));
        o.add("admin", GSON.toJsonTree(AdminConfig.DEFAULT));
        o.add("protect", GSON.toJsonTree(ProtectConfig.DEFAULT));
        o.add("claims", GSON.toJsonTree(ClaimConfig.DEFAULT));
        return o;
    }

    static void apply(JsonObject json) {
        JsonObject now = read();
        section(json, now, "play", PlayConfig.PATH, PlayConfig.class, PlayConfig::json, null);
        section(json, now, "admin", AdminConfig.PATH, AdminConfig.class, c -> GSON.toJson(c), null);
        section(json, now, "protect", ProtectConfig.PATH, ProtectConfig.class, c -> GSON.toJson(c), null);
        section(json, now, "claims", Path.of("config/simpletweaks-claims.json"), ClaimConfig.class, c -> GSON.toJson(c), o -> o.addProperty("configVersion", ClaimConfig.CONFIG_VERSION));
    }

    private static <T> void section(JsonObject json, JsonObject now, String name, Path path, Class<T> type, Function<T, String> text,
            java.util.function.Consumer<JsonObject> extra) {
        if (!json.has(name) || !json.get(name).isJsonObject() || json.get(name).equals(now.get(name))) return;
        try {
            T cfg = GSON.fromJson(json.getAsJsonObject(name), type);
            String out = text.apply(cfg);
            if (extra != null) {
                JsonObject o = GSON.fromJson(out, JsonObject.class);
                extra.accept(o);
                out = GSON.toJson(o);
            }
            if (name.equals("claims")) ClaimStore.atomicWrite(path, out);
            else {
                Files.createDirectories(path.toAbsolutePath().getParent());
                ConfigFiles.writeAtomic(path, out);
            }
        } catch (Exception e) {
            LOG.warn("Refused the new {} settings, file unchanged: {}", name, e.toString());
        }
    }

    /** Live: the three hubs reload from their files, the protection rule goes to the players. Claims need a restart. */
    static void after() {
        Play.configure(null);
        Admin.configure(null);
        Protect.configure(null);
        var server = Protect.server();
        if (server != null) FreecamRule.broadcast(server);
    }
}
