package com.simplebuilding.client.gui;

import com.simplebuilding.util.TrimMultiplierLogic;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TrimReferenceScreen extends Screen {
    private final Screen parent;
    private final List<ReferenceEntry> entries = new ArrayList<>();

    private int scrollOffset = 0;
    private int maxScroll = 0;
    private final int rowHeight = 26;

    private double playerMultiplier = 1.0d;

    private record ReferenceEntry(ItemStack icon, Text text, Text info, boolean isHeader) {}

    public TrimReferenceScreen(Screen parent) {
        super(Text.literal("Trim Resonance Reference"));
        this.parent = parent;
        if (MinecraftClient.getInstance().player != null) {
            this.playerMultiplier = TrimMultiplierLogic.getMultiplier(MinecraftClient.getInstance().player);
        }
        populateEntries();
    }

    private void populateEntries() {
        String multText = String.format("Current Resonance: %.2fx", playerMultiplier);
        addHeader("--- MATERIALS (Base -> Current) ---");
        addHeader(multText);

        addDynamicMaterial(Items.DIAMOND, "Diamond: Hard Shell", Formatting.AQUA, 3.0, "Damage Reduction");
        addDynamicMaterial(Items.GOLD_INGOT, "Gold: Magic Dampening", Formatting.GOLD, 6.0, "Magic Resist");
        addDynamicMaterial(Items.IRON_INGOT, "Iron: Blunt Resistance", Formatting.GRAY, 5.0, "Projectile Resist");
        addDynamicMaterial(Items.EMERALD, "Emerald: Illager Bane", Formatting.DARK_GREEN, 8.0, "Illager Resist");
        addDynamicMaterial(Items.NETHERITE_INGOT, "Netherite: Amplifier", Formatting.DARK_GRAY, 5.0, "Pattern Boost");
        addDynamicMaterial(Items.COPPER_INGOT, "Copper: Lightning Rod", Formatting.GOLD, 5.0, "Lightning Resist");
        addDynamicMaterial(Items.REDSTONE, "Redstone: Speed", Formatting.RED, 3.0, "Movement Speed");
        addDynamicMaterial(Items.QUARTZ, "Quartz: Heat Shield", Formatting.WHITE, 5.0, "Fire/Lava Resist");
        addDynamicMaterial(Items.AMETHYST_SHARD, "Amethyst: Resonance", Formatting.LIGHT_PURPLE, 25.0, "Regen Chance");
        addDynamicMaterial(Items.LAPIS_LAZULI, "Lapis: Wisdom", Formatting.BLUE, 5.0, "XP Gain Boost");

        tryAddModMaterial("simplebuilding", "astralit_dust", "Astralit", Formatting.YELLOW, 0.0, "Jump Boost (Height)");
        tryAddModMaterial("simplebuilding", "nihilith_shard", "Nihilith", Formatting.DARK_PURPLE, 0.0, "Gravity Pull (Sneak in Air)");
        tryAddModMaterial("simplebuilding", "enderite_ingot", "Enderite", Formatting.DARK_PURPLE, 10.0, "Void Shield (4x Pattern Boost!)");

        entries.add(new ReferenceEntry(ItemStack.EMPTY, Text.empty(), Text.empty(), false));

        addHeader("--- TRIM PATTERNS (Scalable Effects) ---");
        addDynamicTrim(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, "Sentry", Formatting.GRAY, 5.0, "Projectile Dampening");
        addDynamicTrim(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE, "Vex", Formatting.DARK_AQUA, 6.0, "Magic Dampening");
        addDynamicTrim(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, "Wild", Formatting.GREEN, 10.0, "Trap/Cactus Resilience");
        addDynamicTrim(Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, "Dune", Formatting.GOLD, 8.0, "Blast Dampening");
        addDynamicTrim(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, "Coast", Formatting.AQUA, 20.0, "Oxygen Efficiency (Chance)");
        addDynamicTrim(Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE, "Wayfinder", Formatting.YELLOW, 10.0, "Sprint Efficiency");
        addDynamicTrim(Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE, "Raiser", Formatting.DARK_GREEN, 10.0, "XP Affinity");
        addDynamicTrim(Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE, "Host", Formatting.WHITE, 1.0, "Luck Bonus");
        addDynamicTrim(Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, "Ward", Formatting.DARK_BLUE, 3.0, "Damage Absorption");
        addDynamicTrim(Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, "Silence", Formatting.DARK_GRAY, 15.0, "Stealth (Detection Range)");
        addDynamicTrim(Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, "Tide", Formatting.BLUE, 10.0, "Swim Agility");
        addDynamicTrim(Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, "Snout", Formatting.GOLD, 5.0, "Fire Dampening");
        addDynamicTrim(Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE, "Rib", Formatting.DARK_RED, 2.0, "Wither Resist (Sec.)");
        addDynamicTrim(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE, "Eye", Formatting.LIGHT_PURPLE, 10.0, "Ender Stability");
        addDynamicTrim(Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, "Spire", Formatting.LIGHT_PURPLE, 8.0, "Feather Falling");
        addDynamicTrim(Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE, "Flow", Formatting.WHITE, 10.0, "Aerial Agility");
        addDynamicTrim(Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE, "Bolt", Formatting.YELLOW, 25.0, "Kinetic Response");

        tryAddModTrim("simplebuilding", "enderite_armor_trim_smithing_template", "Enderite", Formatting.DARK_PURPLE, 10.0, "Void Protection");
        tryAddModTrim("enderscape", "stasis_armor_trim_smithing_template", "Stasis", Formatting.LIGHT_PURPLE, 5.0, "End Statis Effect");
    }

    private void addDynamicMaterial(Item item, String name, Formatting color, double baseValue, String statName) {
        double currentVal = baseValue * playerMultiplier;

        Text text = Text.literal(name).formatted(color);
        Text info;

        if (baseValue > 0) {
            info = Text.literal(statName + ": ")
                    .formatted(Formatting.GRAY)
                    .append(Text.literal(String.format("%.1f%%", baseValue)).formatted(Formatting.YELLOW))
                    .append(Text.literal(" -> ").formatted(Formatting.DARK_GRAY))
                    .append(Text.literal(String.format("%.1f%%", currentVal)).formatted(Formatting.GREEN, Formatting.BOLD));
        } else {
            info = Text.literal(statName).formatted(Formatting.GRAY);
        }

        entries.add(new ReferenceEntry(new ItemStack(item), text, info, false));
    }

    private void tryAddModMaterial(String namespace, String path, String name, Formatting color, double baseValue, String statName) {
        Optional<Item> item = Registry.ITEM.getOrEmpty(new Identifier(namespace, path));
        item.ifPresent(value -> addDynamicMaterial(value, name, color, baseValue, statName));
    }

    private void addDynamicTrim(Item item, String name, Formatting color, double baseValue, String statName) {
        addDynamicMaterial(item, name + " Trim", color, baseValue, statName);
    }

    private void tryAddModTrim(String namespace, String path, String name, Formatting color, double baseValue, String statName) {
        Optional<Item> item = Registry.ITEM.getOrEmpty(new Identifier(namespace, path));
        item.ifPresent(value -> addDynamicTrim(value, name, color, baseValue, statName));
    }

    private void addHeader(String text) {
        entries.add(new ReferenceEntry(ItemStack.EMPTY, Text.literal(text).formatted(Formatting.BOLD, Formatting.YELLOW), null, true));
    }

    @Override
    protected void init() {
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> this.close())
                .dimensions(this.width / 2 - 50, this.height - 25, 100, 20)
                .build());

        int contentHeight = entries.size() * rowHeight + 40;
        this.maxScroll = Math.max(0, contentHeight - (this.height - 40));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        DrawableHelper.fillGradient(matrices, 0, 0, this.width, this.height, 0xC0101010, 0xD0101010);

        int startY = 15 - scrollOffset;
        int y = startY;

        DrawableHelper.drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 5, 0xFFFFFFFF);

        for (ReferenceEntry entry : entries) {
            if (y > -rowHeight && y < this.height - 30) {
                if (entry.isHeader) {
                    DrawableHelper.drawCenteredText(matrices, this.textRenderer, entry.text, this.width / 2, y + 10, 0xFFFFFFFF);
                } else if (!entry.icon.isEmpty()) {
                    int centerX = this.width / 2;
                    int iconX = centerX - 100;

                    this.itemRenderer.renderInGuiWithOverrides(entry.icon, iconX, y);
                    this.textRenderer.drawWithShadow(matrices, entry.text, iconX + 22, y - 1, 0xFFFFFFFF);

                    if (entry.info != null) {
                        this.textRenderer.draw(matrices, entry.info, iconX + 22, y + 10, 0xFFDDDDDD);
                    }
                }
            }
            y += rowHeight;
        }

        if (maxScroll > 0) {
            int scrollBarH = (int) ((float) (this.height - 40) * ((float) (this.height - 40) / (entries.size() * rowHeight)));
            int scrollBarY = 30 + (int) ((float) scrollOffset / maxScroll * (this.height - 40 - scrollBarH));
            DrawableHelper.fill(matrices, this.width - 6, 30, this.width - 2, this.height - 10, 0x40000000);
            DrawableHelper.fill(matrices, this.width - 6, scrollBarY, this.width - 2, scrollBarY + scrollBarH, 0xFF808080);
        }

        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        this.scrollOffset = Math.max(0, Math.min(this.maxScroll, (int) (this.scrollOffset - amount * 20)));
        return true;
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
}