package com.simplebuilding.modules.simplelivemap;

import java.nio.file.Path;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Livemap: a live web map of the world as the players really explored it (visited chunks plus held Vanilla maps),
 * served by an optional built-in web server. Own mod next to Simple Maps (owner ABN30-12), no classes of other mods.
 * Plan {@code docs/ai/PLAN-B-LIVE32-2026-10-31.md}, concept {@code docs/ai/KONZEPT-A-WP21-2026-10-29.md}.
 */
public final class SimpleLivemap {
    public static final String MOD_ID = "simplelivemap";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);
    public static Path configDir = Path.of("config");
    /** Set by the loader adapter. */
    public static Predicate<String> modLoaded = id -> false;

    private SimpleLivemap() {}

    public static void init(Path dir, Predicate<String> loaded) {
        configDir = dir;
        modLoaded = loaded;
        LivemapConfig.load(dir);
        // CMD24: /simplelivemap config (every value clamped by LivemapConfig.apply; the token is shown as ***).
        com.simplelib.api.config.ConfigCommands.server(MOD_ID).json(LivemapConfig::toJson, LivemapConfig::apply)
                .file(dir, LivemapConfig.FILE).secret("web.token").choices("web.visibility", LivemapConfig.PRIVATE, LivemapConfig.PUBLIC)
                .after(() -> {
                    // A reset token would leave the private map without a key: make a new one (an empty token never opens it).
                    if (LivemapConfig.token.isEmpty()) {
                        LivemapConfig.token = LivemapConfig.newToken();
                        LivemapConfig.save(configDir);
                    }
                }).register();
    }

    /** Own switch and the super-mod switch ("Enable Simple Livemap" of a super-mod, framework contract). */
    public static boolean on() {
        return LivemapConfig.enabled && com.simplebuilding.framework.api.SubMods.enabled(MOD_ID);
    }
}
