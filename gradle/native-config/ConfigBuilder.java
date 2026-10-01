package nativeconfig;

import java.util.*;
import java.util.function.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Native Minecraft widgets. Build-time relocation keeps each Forge mod independent. */
public final class ConfigBuilder {
    private Screen parent;
    private Component title = Component.empty();
    private Runnable save = () -> {};
    private final LinkedHashMap<Component, Category> categories = new LinkedHashMap<>();
    public static ConfigBuilder create() { return new ConfigBuilder(); }
    public ConfigBuilder setParentScreen(Screen value) { parent = value; return this; }
    public ConfigBuilder setTitle(Component value) { title = value; return this; }
    public ConfigBuilder setSavingRunnable(Runnable value) { save = value; return this; }
    public Category getOrCreateCategory(Component name) { return categories.computeIfAbsent(name, _ -> new Category()); }
    public ConfigBuilder entryBuilder() { return this; }
    public Screen build() { return new SettingsScreen(); }
    public Entry<Boolean> startBooleanToggle(Component name, boolean value) { return new Entry<>(name, value, Boolean::valueOf); }
    public Entry<Integer> startIntField(Component name, int value) { return new Entry<>(name, value, Integer::valueOf); }
    public Entry<Long> startLongField(Component name, long value) { return new Entry<>(name, value, Long::valueOf); }
    public Entry<Float> startFloatField(Component name, float value) { return new Entry<>(name, value, Float::valueOf); }
    public Entry<Double> startDoubleField(Component name, double value) { return new Entry<>(name, value, Double::valueOf); }
    public Entry<String> startStrField(Component name, String value) { return new Entry<>(name, value, s -> s); }
    public Entry<List<String>> startStrList(Component name, List<String> value) {
        return new Entry<>(name, value, s -> {
            var json = com.google.gson.JsonParser.parseString(s);
            if (!json.isJsonArray() || json.getAsJsonArray().size() > 128) throw new IllegalArgumentException();
            var result = new ArrayList<String>();
            for (var item : json.getAsJsonArray()) {
                if (!item.isJsonPrimitive() || !item.getAsJsonPrimitive().isString() || item.getAsString().length() > 256) throw new IllegalArgumentException();
                result.add(item.getAsString());
            }
            return result;
        });
    }
    public <T extends Enum<T>> Entry<T> startEnumSelector(Component name, Class<T> type, T value) {
        var entry = new Entry<>(name, value, s -> Enum.valueOf(type, s));
        entry.choices = Arrays.asList(type.getEnumConstants());
        return entry;
    }
    public Entry<String> startTextDescription(Component text) { var e = startStrField(text, ""); e.editable = false; return e; }
    public static final class Category {
        final List<Entry<?>> entries = new ArrayList<>();
        public Category addEntry(Entry<?> entry) { entries.add(entry); return this; }
    }
    public static final class Entry<T> {
        final Component name;
        final Function<String, T> parser;
        T value, initial, defaults;
        String text;
        Component[] tips = {};
        List<T> choices;
        Function<T, Component> enumName = v -> Component.literal(v.toString());
        Consumer<T> consumer = _ -> {};
        double min = -Double.MAX_VALUE, max = Double.MAX_VALUE;
        boolean editable = true, valid = true;
        Entry(Component name, T value, Function<String, T> parser) {
            this.name = name; this.value = value; initial = value; defaults = value; this.parser = parser; text = format(value);
        }
        static String format(Object v) { return v instanceof List<?> ? new com.google.gson.Gson().toJson(v) : v.toString(); }
        public Entry<T> setDefaultValue(T v) { defaults = v; return this; }
        public Entry<T> setMin(double v) { min = v; return this; }
        public Entry<T> setMax(double v) { max = v; return this; }
        public Entry<T> setTooltip(Component... v) { tips = v; return this; }
        public Entry<T> setSaveConsumer(Consumer<T> v) { consumer = v; return this; }
        public Entry<T> setEnumNameProvider(Function<T, Component> v) { enumName = v; return this; }
        public Entry<T> build() { return this; }
        public void setEditable(boolean v) { editable = v; }
        void parse(String input) {
            text = input;
            try {
                T parsed = parser.apply(input);
                valid = !(parsed instanceof Number n) || Double.isFinite(n.doubleValue()) && n.doubleValue() >= min && n.doubleValue() <= max;
                if (valid) value = parsed;
            } catch (RuntimeException ex) { valid = false; }
        }
        Component display() { return value instanceof Boolean b ? Component.translatable(b ? "options.on" : "options.off") : enumName.apply(value); }
        Component tooltip() {
            var result = name.copy();
            for (var tip : tips) result.append("\n").append(tip);
            result.append("\n").append(Component.translatable("simplemods.config.default", format(defaults)));
            if (min != -Double.MAX_VALUE || max != Double.MAX_VALUE) result.append("\n").append(Component.translatable("simplemods.config.range", min, max));
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
        SettingsScreen() { super(ConfigBuilder.this.title); }
        private int rows() { return Math.max(1, (height - 135) / 27); }
        private Category category() { return categories.get(labels.get(tab)); }
        private void change(Runnable action) { fields.forEach(f -> f.setFocused(false)); action.run(); rebuildWidgets(); }
        @Override protected void init() {
            fields.clear();
            int x = Math.max(8, (width - 640) / 2), w = Math.min(640, width - 16);
            if (!labels.isEmpty()) {
                addRenderableWidget(Button.builder(Component.literal("<"), _ -> change(() -> { tab = Math.floorMod(tab - 1, labels.size()); page = 0; })).bounds(x, 30, 30, 20).build());
                addRenderableWidget(Button.builder(labels.get(tab), _ -> change(() -> { tab = (tab + 1) % labels.size(); page = 0; })).bounds(x + 34, 30, w - 68, 20).build());
                addRenderableWidget(Button.builder(Component.literal(">"), _ -> change(() -> { tab = (tab + 1) % labels.size(); page = 0; })).bounds(x + w - 30, 30, 30, 20).build());
                var entries = category().entries;
                page = Math.clamp(page, 0, Math.max(0, (entries.size() - 1) / rows()));
                for (int i = page * rows(); i < Math.min(entries.size(), (page + 1) * rows()); i++) addRow(entries.get(i), x, 58 + (i - page * rows()) * 27, w);
                addRenderableWidget(Button.builder(Component.literal("<"), _ -> change(() -> page--)).bounds(x, height - 66, 35, 20).build()).active = page > 0;
                addRenderableWidget(Button.builder(Component.literal((page + 1) + " / " + Math.max(1, (entries.size() + rows() - 1) / rows())), _ -> {}).bounds(x + 40, height - 66, 90, 20).build()).active = false;
                addRenderableWidget(Button.builder(Component.literal(">"), _ -> change(() -> page++)).bounds(x + 135, height - 66, 35, 20).build()).active = (page + 1) * rows() < entries.size();
            }
            saveButton = addRenderableWidget(Button.builder(Component.translatable("gui.done"), _ -> commit()).bounds(width / 2 - 155, height - 29, 150, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), _ -> onClose()).bounds(width / 2 + 5, height - 29, 150, 20).build());
            validity();
        }
        private void addRow(Entry<?> e, int x, int y, int w) {
            var tip = Tooltip.create(e.tooltip());
            var label = addRenderableWidget(Button.builder(e.name, _ -> {}).bounds(x, y, w / 2 - 5, 20).build());
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
                box.setTextColor(e.valid ? 0xFFE0E0E0 : 0xFFFF5555);
                box.setResponder(s -> { e.parse(s); box.setTextColor(e.valid ? 0xFFE0E0E0 : 0xFFFF5555); validity(); });
                fields.add(addRenderableWidget(box));
            }
            var reset = addRenderableWidget(Button.builder(Component.translatable("controls.reset"), _ -> change(e::reset)).bounds(x + w - 50, y, 50, 20).build());
            reset.active = e.editable; reset.setTooltip(tip);
        }
        private void validity() { if (saveButton != null) saveButton.active = categories.values().stream().flatMap(c -> c.entries.stream()).allMatch(e -> e.valid); }
        private void commit() {
            if (!saveButton.active) return;
            try {
                categories.values().forEach(c -> c.entries.forEach(SettingsScreen::apply));
                save.run(); onClose();
            } catch (RuntimeException ex) { error = Component.translatable("simplemods.config.saveFailed"); org.slf4j.LoggerFactory.getLogger("simplemods-config").error("Cannot save configuration", ex); }
        }
        private static <T> void apply(Entry<T> e) { if (e.editable) e.consumer.accept(e.value); }
        @Override public void onClose() { fields.forEach(f -> f.setFocused(false)); minecraft.setScreenAndShow(parent); }
        @Override public boolean isPauseScreen() { return false; }
        @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            super.extractRenderState(graphics, mouseX, mouseY, delta);
            graphics.text(font, title, Math.max(8, (width - font.width(title)) / 2), 10, 0xFFFFFFFF, false);
            graphics.text(font, error, Math.max(8, width / 2 - 155), height - 42, 0xFFFF5555, false);
        }
    }
}
