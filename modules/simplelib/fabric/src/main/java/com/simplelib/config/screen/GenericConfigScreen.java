package com.simplelib.config.screen;

import com.google.gson.JsonElement;
import com.simplelib.api.config.ConfigCommands;
import com.simplelib.api.config.ConfigScreenModel;
import com.simplelib.api.config.ConfigScreenModel.Access;
import com.simplelib.api.config.ConfigScreenModel.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The config screen of every Simple mod (L13, owner 11.11. RELSB31 f2=A, docs/ai/PLAN-B-CFGSCREEN31-2026-11-11.md), built from
 * the options the mod announced with {@link ConfigCommands} ({@link ConfigScreenModel}). Mod Menu gets it through SimpleLib's
 * {@code getProvidedConfigScreenFactories}; a mod's own screen stays first. With Cloth Config the Cloth builder draws it, without
 * Cloth the Vanilla widgets of {@link VanillaConfigBuilder}. Server options follow {@link ConfigScreenModel#access}.
 */
public final class GenericConfigScreen {
    private GenericConfigScreen() {}

    private static boolean has(String key) {
        return net.minecraft.locale.Language.getInstance().has(key);
    }

    /** One way to draw the fields: Cloth Config or Vanilla widgets. */
    interface Form {
        void category(Component name);

        void note(Component text);

        void toggle(Component name, Component[] tip, boolean value, boolean def, boolean editable, Consumer<Boolean> save);

        void integer(Component name, Component[] tip, long value, long def, double[] range, boolean editable, Consumer<Long> save);

        void decimal(Component name, Component[] tip, double value, double def, double[] range, boolean editable, Consumer<Double> save);

        void text(Component name, Component[] tip, String value, String def, boolean editable, Consumer<String> save);

        void choice(Component name, Component[] tip, List<String> choices, String value, String def, boolean editable, Consumer<String> save);

        void list(Component name, Component[] tip, List<String> value, List<String> def, boolean editable, Consumer<List<String>> save);

        Screen build(Screen parent, Component title, Runnable save);
    }

    /** Loaded mods with an announced config (server or client); Mod Menu offers the screen for these. */
    public static Set<String> modIds() {
        Set<String> out = new LinkedHashSet<>();
        for (boolean client : new boolean[] {false, true}) {
            for (ConfigCommands.Spec spec : ConfigCommands.specs(client)) {
                if (FabricLoader.getInstance().isModLoaded(spec.modId())) out.add(spec.modId());
            }
        }
        return out;
    }

    /** The screen of {@code modId}, or null when it announced no config. */
    public static Screen create(String modId, Screen parent) {
        ConfigCommands.Spec server = ConfigCommands.spec(modId, false), client = ConfigCommands.spec(modId, true);
        if (server == null && client == null) return null;
        Minecraft mc = Minecraft.getInstance();
        boolean inWorld = mc.level != null;
        boolean remote = inWorld && !mc.hasSingleplayerServer();
        boolean operator = mc.player != null && mc.player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
        Form form = FabricLoader.getInstance().isModLoaded("cloth-config") ? new ClothForm() : new VanillaForm();
        Map<String, String> serverEdits = new LinkedHashMap<>(), clientEdits = new LinkedHashMap<>();
        boolean both = server != null && client != null;
        if (server != null) {
            Access access = ConfigScreenModel.access(false, inWorld, remote, operator);
            Component hint = access == Access.EDIT ? null : Component.translatable(remote ? "gui.simplelib.config_screen.remote" : "gui.simplelib.config_screen.operator",
                    server.command());
            fill(form, modId, server, both ? "gui.simplelib.config_screen.server" : null, access, hint, serverEdits);
        }
        if (client != null) fill(form, modId, client, both ? "gui.simplelib.config_screen.client" : null, Access.EDIT, null, clientEdits);
        Component title = has(ConfigScreenModel.titleKey(modId)) ? Component.translatable(ConfigScreenModel.titleKey(modId))
                : Component.literal(FabricLoader.getInstance().getModContainer(modId).map(c -> c.getMetadata().getName()).orElse(modId));
        return form.build(parent, title, () -> save(mc, server, serverEdits, client, clientEdits));
    }

    private static void fill(Form form, String modId, ConfigCommands.Spec spec, String sideKey, Access access, Component hint, Map<String, String> edits) {
        for (ConfigScreenModel.Category c : ConfigScreenModel.categories(spec)) {
            Component name = has(ConfigScreenModel.categoryKey(modId, c.group())) ? Component.translatable(ConfigScreenModel.categoryKey(modId, c.group()))
                    : c.group().equals(ConfigScreenModel.GENERAL) ? Component.translatable("gui.simplelib.config_screen.general")
                    : Component.literal(ConfigScreenModel.humanize(c.group()));
            form.category(sideKey == null ? name : Component.translatable(sideKey, name));
            if (hint != null) form.note(hint);
            for (Field f : c.fields()) field(form, modId, f, access == Access.EDIT && !f.secret(), edits);
        }
    }

    private static void field(Form form, String modId, Field f, boolean editable, Map<String, String> edits) {
        String key = ConfigScreenModel.labelKey(modId, f.path());
        Component name = has(key) ? Component.translatable(key) : Component.literal(ConfigScreenModel.humanize(f.path()));
        Component[] tip = tooltip(key, f);
        Consumer<String> put = text -> {
            if (editable) edits.put(f.path(), text);
        };
        switch (f.widget()) {
            case TOGGLE -> form.toggle(name, tip, f.value() != null && f.value().getAsBoolean(), f.defaultValue().getAsBoolean(), editable,
                    v -> put.accept(Boolean.toString(v)));
            case INTEGER -> form.integer(name, tip, f.value() == null ? 0 : f.value().getAsLong(), f.defaultValue().getAsLong(), f.range(), editable,
                    v -> put.accept(Long.toString(v)));
            case DECIMAL -> form.decimal(name, tip, f.value() == null ? 0 : f.value().getAsDouble(), f.defaultValue().getAsDouble(), f.range(), editable,
                    v -> put.accept(Double.toString(v)));
            case CHOICE -> form.choice(name, tip, f.choices(), choice(f.value(), f.choices()), choice(f.defaultValue(), f.choices()), editable, put);
            case LIST -> form.list(name, tip, ConfigScreenModel.items(f.value()), ConfigScreenModel.items(f.defaultValue()), editable,
                    v -> put.accept(ConfigScreenModel.listText(v, f.defaultValue())));
            default -> form.text(name, tip, f.text(), f.defaultText(), editable, put);
        }
    }

    /** The stored choice in the spelling of the list (the file may hold another case). */
    private static String choice(JsonElement v, List<String> choices) {
        String s = ConfigCommands.show(v);
        for (String c : choices) if (c.equalsIgnoreCase(s)) return c;
        return choices.isEmpty() ? s : choices.get(0);
    }

    /** Lang tooltip, then default, range and restart lines unless the lang text names them already. */
    static Component[] tooltip(String key, Field f) {
        List<Component> out = new ArrayList<>();
        String tipKey = key + ".@Tooltip";
        String text = has(tipKey) ? net.minecraft.locale.Language.getInstance().getOrDefault(tipKey) : "";
        if (!text.isEmpty()) out.add(Component.translatable(tipKey));
        if (!ConfigScreenModel.namesDefault(text)) {
            out.add(Component.translatable("gui.simplelib.config_screen.default", f.defaultText()).withStyle(ChatFormatting.GRAY));
        }
        if (f.range() != null && !ConfigScreenModel.namesRange(text)) {
            out.add(Component.translatable("gui.simplelib.config_screen.range", ConfigScreenModel.number(f.range()[0]), ConfigScreenModel.number(f.range()[1]))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (f.restart()) out.add(Component.translatable("gui.simplelib.config_screen.restart").withStyle(ChatFormatting.GOLD));
        if (f.secret()) out.add(Component.translatable("gui.simplelib.config_screen.secret").withStyle(ChatFormatting.GOLD));
        return out.toArray(Component[]::new);
    }

    /** Server options on the integrated server's thread (its own world), client options and the title screen at once. */
    private static void save(Minecraft mc, ConfigCommands.Spec server, Map<String, String> serverEdits, ConfigCommands.Spec client, Map<String, String> clientEdits) {
        if (client != null && !clientEdits.isEmpty()) report(client, ConfigScreenModel.save(client, Map.copyOf(clientEdits)));
        if (server != null && !serverEdits.isEmpty()) {
            Map<String, String> edits = new LinkedHashMap<>(serverEdits);
            var host = mc.getSingleplayerServer();
            if (host != null) host.execute(() -> report(server, ConfigScreenModel.save(server, edits)));
            else report(server, ConfigScreenModel.save(server, edits));
        }
        serverEdits.clear();
        clientEdits.clear();
    }

    private static void report(ConfigCommands.Spec spec, List<ConfigCommands.Result> results) {
        for (ConfigCommands.Result r : results) {
            if (r.ok()) {
                com.simplelib.SimpleLib.LOG.info("Config screen: {} {} = {}{}", spec.modId(), r.path(), r.value(),
                        r.status() == ConfigCommands.Status.CLAMPED ? " (clamped)" : "");
            } else {
                com.simplelib.SimpleLib.LOG.warn("Config screen: {} {} not saved ({}: {})", spec.modId(), r.path(), r.status(), r.value());
            }
        }
    }
}
