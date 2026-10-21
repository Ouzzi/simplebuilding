package com.simplebuilding.client;

import com.simplebuilding.config.ConfigOptions;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;
import me.shedaniel.autoconfig.gui.registry.GuiRegistry;

/**
 * Split stage 9: the AutoConfig screen leaves out the options a sub-mod took over on this line
 * ({@link ConfigOptions#hidden(String)}) - no row, no tooltip. The fields stay in the file and keep their values, because
 * the screen saves the whole config object. Fabric registers this on {@code AutoConfigClient.getGuiRegistry} (ModMenu's
 * screen), NeoForge on the registry it builds for its config button; Forge's own screen skips them itself.
 */
public final class ConfigScreenHiding {
    private ConfigScreenHiding() {
    }

    public static void apply(GuiRegistry registry) {
        Set<Field> hidden = ConfigOptions.all().stream().filter(ConfigOptions.Option::hidden)
                .map(ConfigOptions.Option::field).collect(Collectors.toSet());
        if (hidden.isEmpty()) {
            return;
        }
        registry.registerPredicateTransformer((guis, i18n, field, config, defaults, access) -> new ArrayList<>(), hidden::contains);
    }
}
