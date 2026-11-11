package com.simplelib.config.screen;

import java.util.*;
import java.util.function.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The config screen without Cloth Config (L13): Vanilla buttons and edit boxes, one category at a time with paging. Taken from
 * the Forge template {@code gradle/native-config/ConfigBuilder.java} (Cloth-shaped API); here the names are white text instead of
 * dead buttons (contrast, ui-lesbarkeit) and the tooltip lines come complete from {@link GenericConfigScreen}.
 */
final class VanillaConfigBuilder {
    private static final int TEXT = 0xFFFFFFFF, NOTE = 0xFFE0E0E0, BAD = 0xFFFF5555;
    private Screen parent;
    private Component title = Component.empty();
    private Runnable save = () -> {};
    private final LinkedHashMap<Component, Category> categories = new LinkedHashMap<>();

    VanillaConfigBuilder setParentScreen(Screen value) { parent = value; return this; }
    VanillaConfigBuilder setTitle(Component value) { title = value; return this; }
    VanillaConfigBuilder setSavingRunnable(Runnable value) { save = value; return this; }
    Category getOrCreateCategory(Component name) { return categories.computeIfAbsent(name, n -> new Category()); }
    Screen build() { return new SettingsScreen(); }
    Entry<Boolean> startBooleanToggle(Component name, boolean value) { return new Entry<>(name, value, Boolean::valueOf); }
    Entry<Long> startLongField(Component name, long value) { return new Entry<>(name, value, s -> Long.valueOf(s.trim())); }
    Entry<Double> startDoubleField(Component name, double value) { return new Entry<>(name, value, s -> Double.valueOf(s.trim())); }
    Entry<String> startStrField(Component name, String value) { return new Entry<>(name, value, s -> s); }
    Entry<List<String>> startStrList(Component name, List<String> value) {
        return new Entry<>(name, value, s -> {
            var json = com.google.gson.JsonParser.parseString(s);
            if (!json.isJsonArray() || json.getAsJsonArray().size() > 512) throw new IllegalArgumentException();
            var result = new ArrayList<String>();
            for (var item : json.getAsJsonArray()) {
                if (!item.isJsonPrimitive() || item.getAsString().length() > 256) throw new IllegalArgumentException();
                result.add(item.getAsString());
            }
            return result;
        });
    }
    Entry<String> startSelector(Component name, List<String> choices, String value) {
        var entry = new Entry<>(name, value, s -> s);
        entry.choices = List.copyOf(choices);
        return entry;
    }
    Entry<String> startTextDescription(Component text) { var e = startStrField(text, ""); e.note = true; e.editable = false; return e; }

    static final class Category {
        final List<Entry<?>> entries = new ArrayList<>();
        Category addEntry(Entry<?> entry) { entries.add(entry); return this; }
    }

    static final class Entry<T> {
        final Component name;
        final Function<String, T> parser;
        T value, defaults;
        String text;
        Component[] tips = {};
        List<T> choices;
        Consumer<T> consumer = v -> {};
        double min = -Double.MAX_VALUE, max = Double.MAX_VALUE;
        boolean editable = true, valid = true, note;
        Entry(Component name, T value, Function<String, T> parser) {
            this.name = name; this.value = value; defaults = value; this.parser = parser; text = format(value);
        }
        static String format(Object v) { return v instanceof List<?> ? new com.google.gson.Gson().toJson(v) : String.valueOf(v); }
        Entry<T> setDefaultValue(T v) { defaults = v; return this; }
        Entry<T> setMin(double v) { min = v; return this; }
        Entry<T> setMax(double v) { max = v; return this; }
        Entry<T> setTooltip(Component... v) { tips = v; return this; }
        Entry<T> setSaveConsumer(Consumer<T> v) { consumer = v; return this; }
        Entry<T> setEditable(boolean v) { editable = v; return this; }
        void parse(String input) {
            text = input;
            try {
                T parsed = parser.apply(input);
                valid = !(parsed instanceof Number n) || Double.isFinite(n.doubleValue()) && n.doubleValue() >= min && n.doubleValue() <= max;
                if (valid) value = parsed;
            } catch (RuntimeException ex) { valid = false; }
        }
        Component display() {
            return value instanceof Boolean b ? Component.translatable(b ? "options.on" : "options.off") : Component.literal(String.valueOf(value));
        }
        Component tooltip() {
            var result = name.copy();
            for (var tip : tips) result.append("\n").append(tip);
            return result;
        }
        void reset() { value = defaults; text = format(value); valid = true; }
        @SuppressWarnings("unchecked") void next() {
            value = value instanceof Boolean b ? (T) Boolean.valueOf(!b) : choices.get((choices.indexOf(value) + 1) % choices.size());
            text = format(value);
        }
    }

    private final class SettingsScreen extends Screen {
        private int tab, page;
        private Button saveButton;
        private Component error = Component.empty();
        private final List<EditBox> fields = new ArrayList<>();
        private final List<Component> labels = new ArrayList<>(categories.keySet());
        SettingsScreen() { super(VanillaConfigBuilder.this.title); }
        private int rows() { return Math.max(1, (height - 135) / 27); }
        private Category category() { return categories.get(labels.get(tab)); }
        private void change(Runnable action) { fields.forEach(f -> f.setFocused(false)); action.run(); rebuildWidgets(); }
        @Override protected void init() {
            fields.clear();
            int x = Math.max(8, (width - 640) / 2), w = Math.min(640, width - 16);
            if (!labels.isEmpty()) {
                if (labels.size() > 1) {
                    addRenderableWidget(Button.builder(Component.literal("<"), b -> change(() -> { tab = Math.floorMod(tab - 1, labels.size()); page = 0; })).bounds(x, 30, 30, 20).build());
                    addRenderableWidget(Button.builder(Component.literal(">"), b -> change(() -> { tab = (tab + 1) % labels.size(); page = 0; })).bounds(x + w - 30, 30, 30, 20).build());
                }
                addRenderableWidget(Button.builder(labels.get(tab), b -> change(() -> { tab = (tab + 1) % labels.size(); page = 0; })).bounds(x + 34, 30, w - 68, 20).build())
                        .active = labels.size() > 1;
                var entries = category().entries;
                page = Math.clamp(page, 0, Math.max(0, (entries.size() - 1) / rows()));
                for (int i = page * rows(); i < Math.min(entries.size(), (page + 1) * rows()); i++) addRow(entries.get(i), x, 58 + (i - page * rows()) * 27, w);
                if (entries.size() > rows()) {
                    addRenderableWidget(Button.builder(Component.literal("<"), b -> change(() -> page--)).bounds(x, height - 66, 35, 20).build()).active = page > 0;
                    addRenderableWidget(new StringWidget(x + 40, height - 66, 90, 20, Component.literal((page + 1) + " / " + ((entries.size() + rows() - 1) / rows())), font));
                    addRenderableWidget(Button.builder(Component.literal(">"), b -> change(() -> page++)).bounds(x + 135, height - 66, 35, 20).build()).active = (page + 1) * rows() < entries.size();
                }
            }
            saveButton = addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> commit()).bounds(width / 2 - 155, height - 29, 150, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose()).bounds(width / 2 + 5, height - 29, 150, 20).build());
            validity();
        }
        private void addRow(Entry<?> e, int x, int y, int w) {
            var tip = Tooltip.create(e.tooltip());
            if (e.note) {
                addRenderableWidget(new StringWidget(x, y, w, 20, e.name.copy().withColor(NOTE & 0xFFFFFF), font).setMaxWidth(w, StringWidget.TextOverflow.SCROLLING));
                return;
            }
            var label = addRenderableWidget(new StringWidget(x, y, w / 2 - 5, 20, e.name.copy().withColor(TEXT & 0xFFFFFF), font)
                    .setMaxWidth(w / 2 - 5, StringWidget.TextOverflow.SCROLLING));
            label.setTooltip(tip);
            int vx = x + w / 2, vw = w / 2 - 54;
            if (e.value instanceof Boolean || e.choices != null) {
                var button = addRenderableWidget(Button.builder(e.display(), b -> { e.next(); b.setMessage(e.display()); }).bounds(vx, y, vw, 20).build());
                button.active = e.editable; button.setTooltip(tip);
            } else {
                var box = new EditBox(font, vx, y, vw, 20, e.name) {
                    @Override public void setFocused(boolean focused) { super.setFocused(focused); Minecraft.getInstance().onTextInputFocusChange(this, focused); }
                };
                box.setMaxLength(32768); box.setValue(e.text); box.setEditable(e.editable); box.setTooltip(tip);
                box.setTextColor(e.valid ? NOTE : BAD);
                box.setResponder(s -> { e.parse(s); box.setTextColor(e.valid ? NOTE : BAD); validity(); });
                fields.add(addRenderableWidget(box));
            }
            var reset = addRenderableWidget(Button.builder(Component.translatable("controls.reset"), b -> change(e::reset)).bounds(x + w - 50, y, 50, 20).build());
            reset.active = e.editable; reset.setTooltip(tip);
        }
        private void validity() { if (saveButton != null) saveButton.active = categories.values().stream().flatMap(c -> c.entries.stream()).allMatch(e -> e.valid); }
        private void commit() {
            if (!saveButton.active) return;
            try {
                categories.values().forEach(c -> c.entries.forEach(SettingsScreen::apply));
                save.run(); onClose();
            } catch (RuntimeException ex) {
                error = Component.translatable("gui.simplelib.config_screen.save_failed");
                com.simplelib.SimpleLib.LOG.error("Cannot save configuration", ex);
            }
        }
        private static <T> void apply(Entry<T> e) { if (e.editable) e.consumer.accept(e.value); }
        @Override public void onClose() { fields.forEach(f -> f.setFocused(false)); minecraft.setScreenAndShow(parent); }
        @Override public boolean isPauseScreen() { return false; }
        @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            super.extractRenderState(graphics, mouseX, mouseY, delta);
            graphics.text(font, title, Math.max(8, (width - font.width(title)) / 2), 10, TEXT, false);
            graphics.text(font, error, Math.max(8, width / 2 - 155), height - 42, BAD, false);
        }
    }
}
