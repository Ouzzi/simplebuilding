package com.simplelib.config.screen;

import java.util.List;
import java.util.function.Consumer;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** {@link GenericConfigScreen.Form} with Cloth Config (only loaded while Cloth is there), like the mods' own screens. */
final class ClothForm implements GenericConfigScreen.Form {
    private final ConfigBuilder builder = ConfigBuilder.create();
    private final ConfigEntryBuilder entries = builder.entryBuilder();
    private ConfigCategory category;

    @Override
    public void category(Component name) {
        category = builder.getOrCreateCategory(name);
    }

    @Override
    public void note(Component text) {
        category.addEntry(entries.startTextDescription(text).build());
    }

    private void add(AbstractConfigListEntry<?> entry, boolean editable) {
        entry.setEditable(editable);
        category.addEntry(entry);
    }

    @Override
    public void toggle(Component name, Component[] tip, boolean value, boolean def, boolean editable, Consumer<Boolean> save) {
        add(entries.startBooleanToggle(name, value).setDefaultValue(def).setTooltip(tip).setSaveConsumer(save).build(), editable);
    }

    @Override
    public void integer(Component name, Component[] tip, long value, long def, double[] range, boolean editable, Consumer<Long> save) {
        var b = entries.startLongField(name, value).setDefaultValue(def).setTooltip(tip).setSaveConsumer(save);
        if (range != null) b.setMin((long) Math.ceil(range[0])).setMax((long) Math.floor(range[1]));
        add(b.build(), editable);
    }

    @Override
    public void decimal(Component name, Component[] tip, double value, double def, double[] range, boolean editable, Consumer<Double> save) {
        var b = entries.startDoubleField(name, value).setDefaultValue(def).setTooltip(tip).setSaveConsumer(save);
        if (range != null) b.setMin(range[0]).setMax(range[1]);
        add(b.build(), editable);
    }

    @Override
    public void text(Component name, Component[] tip, String value, String def, boolean editable, Consumer<String> save) {
        add(entries.startStrField(name, value).setDefaultValue(def).setTooltip(tip).setSaveConsumer(save).build(), editable);
    }

    @Override
    public void choice(Component name, Component[] tip, List<String> choices, String value, String def, boolean editable, Consumer<String> save) {
        add(entries.startSelector(name, choices.toArray(String[]::new), value).setDefaultValue(def).setTooltip(tip).setSaveConsumer(save).build(), editable);
    }

    @Override
    public void list(Component name, Component[] tip, List<String> value, List<String> def, boolean editable, Consumer<List<String>> save) {
        add(entries.startStrList(name, value).setDefaultValue(def).setTooltip(tip).setSaveConsumer(save).build(), editable);
    }

    @Override
    public Screen build(Screen parent, Component title, Runnable save) {
        return builder.setParentScreen(parent).setTitle(title).setSavingRunnable(save).build();
    }
}
