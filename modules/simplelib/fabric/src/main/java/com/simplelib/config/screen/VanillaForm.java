package com.simplelib.config.screen;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** {@link GenericConfigScreen.Form} without Cloth Config: Vanilla widgets ({@link VanillaConfigBuilder}). */
final class VanillaForm implements GenericConfigScreen.Form {
    private final VanillaConfigBuilder builder = new VanillaConfigBuilder();
    private VanillaConfigBuilder.Category category;

    @Override
    public void category(Component name) {
        category = builder.getOrCreateCategory(name);
    }

    @Override
    public void note(Component text) {
        category.addEntry(builder.startTextDescription(text));
    }

    private static <T> void range(VanillaConfigBuilder.Entry<T> e, double[] range) {
        if (range != null) e.setMin(range[0]).setMax(range[1]);
    }

    @Override
    public void toggle(Component name, Component[] tip, boolean value, boolean def, boolean editable, Consumer<Boolean> save) {
        category.addEntry(builder.startBooleanToggle(name, value).setDefaultValue(def).setTooltip(tip).setSaveConsumer(save).setEditable(editable));
    }

    @Override
    public void integer(Component name, Component[] tip, long value, long def, double[] range, boolean editable, Consumer<Long> save) {
        var e = builder.startLongField(name, value).setDefaultValue(def).setTooltip(tip).setSaveConsumer(save).setEditable(editable);
        range(e, range);
        category.addEntry(e);
    }

    @Override
    public void decimal(Component name, Component[] tip, double value, double def, double[] range, boolean editable, Consumer<Double> save) {
        var e = builder.startDoubleField(name, value).setDefaultValue(def).setTooltip(tip).setSaveConsumer(save).setEditable(editable);
        range(e, range);
        category.addEntry(e);
    }

    @Override
    public void text(Component name, Component[] tip, String value, String def, boolean editable, Consumer<String> save) {
        category.addEntry(builder.startStrField(name, value).setDefaultValue(def).setTooltip(tip).setSaveConsumer(save).setEditable(editable));
    }

    @Override
    public void choice(Component name, Component[] tip, List<String> choices, String value, String def, boolean editable, Consumer<String> save) {
        category.addEntry(builder.startSelector(name, choices, value).setDefaultValue(def).setTooltip(tip).setSaveConsumer(save).setEditable(editable));
    }

    @Override
    public void list(Component name, Component[] tip, List<String> value, List<String> def, boolean editable, Consumer<List<String>> save) {
        category.addEntry(builder.startStrList(name, value).setDefaultValue(def).setTooltip(tip).setSaveConsumer(save).setEditable(editable));
    }

    @Override
    public Screen build(Screen parent, Component title, Runnable save) {
        return builder.setParentScreen(parent).setTitle(title).setSavingRunnable(save).build();
    }
}
