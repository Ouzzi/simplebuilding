package com.simplebuilding.client.gui;

import com.simplebuilding.util.TrimMultiplierLogic;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class TrimReferenceScreen extends Screen {
    private final Screen parent;
    private final List<ReferenceEntry> entries = new ArrayList<>();

    private int scrollOffset = 0;
    private int maxScroll = 0;
    private final int rowHeight = 26;

    private double playerMultiplier = 1.0d;

    private record ReferenceEntry(ItemStack icon, Component text, Component info, boolean isHeader) {}

    public TrimReferenceScreen(Screen parent) {
        super(Component.literal("Trim Resonance Reference"));
        this.parent = parent;
        if (Minecraft.getInstance().player != null) {
            this.playerMultiplier = TrimMultiplierLogic.getMultiplier(Minecraft.getInstance().player);
        }

        populateEntries();
    }

    private void populateEntries() {
        // --- HEADER ---
        String multText = String.format("Current Resonance: %.2fx", playerMultiplier);
        addHeader("--- MATERIALS (Base -> Current) ---");
        addHeader(multText);

        addDynamicMaterial(Items.DIAMOND, "Diamond: Hard Shell", ChatFormatting.AQUA, 3.0, "Damage Reduction");
        addDynamicMaterial(Items.GOLD_INGOT, "Gold: Magic Dampening", ChatFormatting.GOLD, 6.0, "Magic Resist");
        addDynamicMaterial(Items.IRON_INGOT, "Iron: Blunt Resistance", ChatFormatting.GRAY, 5.0, "Projectile Resist");
        addDynamicMaterial(Items.EMERALD, "Emerald: Illager Bane", ChatFormatting.DARK_GREEN, 8.0, "Illager Resist");
        addDynamicMaterial(Items.NETHERITE_INGOT, "Netherite: Amplifier", ChatFormatting.DARK_GRAY, 5.0, "Pattern Boost");
        addDynamicMaterial(Items.COPPER_INGOT, "Copper: Lightning Rod", ChatFormatting.GOLD, 5.0, "Lightning Resist");
        addDynamicMaterial(Items.REDSTONE, "Redstone: Speed", ChatFormatting.RED, 3.0, "Movement Speed");
        addDynamicMaterial(Items.QUARTZ, "Quartz: Heat Shield", ChatFormatting.WHITE, 5.0, "Fire/Lava Resist");
        addDynamicMaterial(Items.AMETHYST_SHARD, "Amethyst: Resonance", ChatFormatting.LIGHT_PURPLE, 25.0, "Regen Chance");
        addDynamicMaterial(Items.LAPIS_LAZULI, "Lapis: Wisdom", ChatFormatting.BLUE, 5.0, "XP Gain Boost");

        // --- NEW MOD MATERIALS ---
        tryAddModMaterial("simplebuilding", "astralit_dust", "Astralit", ChatFormatting.YELLOW, 0.0, "Jump Boost (Height)");
        tryAddModMaterial("simplebuilding", "nihilith_shard", "Nihilith", ChatFormatting.DARK_PURPLE, 0.0, "Gravity Pull (Sneak in Air)");
        tryAddModMaterial("simplebuilding", "enderite_ingot", "Enderite", ChatFormatting.DARK_PURPLE, 10.0, "Void Shield (4x Pattern Boost!)");


        entries.add(new ReferenceEntry(ItemStack.EMPTY, Component.empty(), Component.empty(), false));

        // --- PATTERNS ---
        addHeader("--- TRIM PATTERNS (Scalable Effects) ---");
        addDynamicTrim(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, "Sentry", ChatFormatting.GRAY, 5.0, "Projectile Dampening");
        addDynamicTrim(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE, "Vex", ChatFormatting.DARK_AQUA, 6.0, "Magic Dampening");
        addDynamicTrim(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, "Wild", ChatFormatting.GREEN, 10.0, "Trap/Cactus Resilience");
        addDynamicTrim(Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, "Dune", ChatFormatting.GOLD, 8.0, "Blast Dampening");
        addDynamicTrim(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, "Coast", ChatFormatting.AQUA, 20.0, "Oxygen Efficiency (Chance)");
        addDynamicTrim(Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE, "Wayfinder", ChatFormatting.YELLOW, 10.0, "Sprint Efficiency");
        addDynamicTrim(Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE, "Raiser", ChatFormatting.DARK_GREEN, 10.0, "XP Affinity");
        addDynamicTrim(Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE, "Host", ChatFormatting.WHITE, 1.0, "Luck Bonus");
        addDynamicTrim(Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, "Ward", ChatFormatting.DARK_BLUE, 3.0, "Damage Absorption");
        addDynamicTrim(Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, "Silence", ChatFormatting.DARK_GRAY, 15.0, "Stealth (Detection Range)");
        addDynamicTrim(Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, "Tide", ChatFormatting.BLUE, 10.0, "Swim Agility");
        addDynamicTrim(Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, "Snout", ChatFormatting.GOLD, 5.0, "Fire Dampening");
        addDynamicTrim(Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE, "Rib", ChatFormatting.DARK_RED, 2.0, "Wither Resist (Sec.)");
        addDynamicTrim(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE, "Eye", ChatFormatting.LIGHT_PURPLE, 10.0, "Ender Stability");
        addDynamicTrim(Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, "Spire", ChatFormatting.LIGHT_PURPLE, 8.0, "Feather Falling");
        addDynamicTrim(Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE, "Flow", ChatFormatting.WHITE, 10.0, "Aerial Agility");
        addDynamicTrim(Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE, "Bolt", ChatFormatting.YELLOW, 25.0, "Kinetic Response");

        tryAddModTrim("enderscape", "stasis_armor_trim_smithing_template", "Stasis", ChatFormatting.LIGHT_PURPLE, 5.0, "End Statis Effect");
    }

    // Hilfsmethode für dynamische Anzeige
    private void addDynamicMaterial(Item item, String name, ChatFormatting color, double baseValue, String statName) {
        double currentVal = baseValue * playerMultiplier;

        Component text = Component.literal(name).withStyle(color);
        Component info;

        if (baseValue > 0) {
            // Zeigt: "Damage: 1.5% -> 2.1%"
            info = Component.literal(statName + ": ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.format("%.1f%%", baseValue)).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" -> ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(String.format("%.1f%%", currentVal)).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        } else {
            // Für Special Effects ohne Zahlenwert
            info = Component.literal(statName).withStyle(ChatFormatting.GRAY);
        }

        entries.add(new ReferenceEntry(new ItemStack(item), text, info, false));
    }

    private void tryAddModMaterial(String namespace, String path, String name, ChatFormatting color, double baseValue, String statName) {
        if (Minecraft.getInstance().level == null) return;
        Optional<Item> item = Minecraft.getInstance().level.registryAccess()
                .lookup(Registries.ITEM)
                .flatMap(reg -> reg.getOptional(Identifier.fromNamespaceAndPath(namespace, path)));
        item.ifPresent(value -> addDynamicMaterial(value, name, color, baseValue, statName));
    }

    private void addDynamicTrim(Item item, String name, ChatFormatting color, double baseValue, String statName) {
        addDynamicMaterial(item, name + " Trim", color, baseValue, statName);
    }

    private void tryAddModTrim(String namespace, String path, String name, ChatFormatting color, double baseValue, String statName) {
        if (Minecraft.getInstance().level == null) return;

        Optional<Item> item = Minecraft.getInstance().level.registryAccess()
                .lookup(Registries.ITEM)
                .flatMap(reg -> reg.getOptional(Identifier.fromNamespaceAndPath(namespace, path)));

        item.ifPresent(value -> addDynamicTrim(value, name, color, baseValue, statName));
    }

    private void addHeader(String text) {
        entries.add(new ReferenceEntry(ItemStack.EMPTY, Component.literal(text).withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW), null, true));
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(Component.literal("Close"), button -> this.onClose())
                .bounds(this.width / 2 - 50, this.height - 25, 100, 20)
                .build());

        int contentHeight = entries.size() * rowHeight + 40;
        this.maxScroll = Math.max(0, contentHeight - (this.height - 40));
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);

        int startY = 15 - scrollOffset;
        int y = startY;

        context.drawCenteredString(this.font, this.title, this.width / 2, 5, 0xFFFFFFFF);

        for (ReferenceEntry entry : entries) {
            // Nur sichtbare Elemente rendern
            if (y > -rowHeight && y < this.height - 30) {
                if (entry.isHeader) {
                    context.drawCenteredString(this.font, entry.text, this.width / 2, y + 10, 0xFFFFFFFF);
                } else if (!entry.icon.isEmpty()) {
                    int centerX = this.width / 2;
                    int iconX = centerX - 100;

                    // Icon
                    context.renderItem(entry.icon, iconX, y);

                    // Titel (z.B. "Diamond: Hard Shell")
                    context.drawString(this.font, entry.text, iconX + 22, y - 1, 0xFFFFFFFF);

                    // Stats Zeile (z.B. "Resist: 1.5% -> 2.1%")
                    if (entry.info != null) {
                        context.drawString(this.font, entry.info, iconX + 22, y + 10, 0xFFDDDDDD, false);
                    }
                }
            }
            y += rowHeight;
        }

        if (maxScroll > 0) {
            int scrollBarH = (int)((float)(this.height - 40) * ((float)(this.height - 40) / (entries.size() * rowHeight)));
            int scrollBarY = 30 + (int)((float)scrollOffset / maxScroll * (this.height - 40 - scrollBarH));
            context.fill(this.width - 6, 30, this.width - 2, this.height - 10, 0x40000000);
            context.fill(this.width - 6, scrollBarY, this.width - 2, scrollBarY + scrollBarH, 0xFF808080);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        this.scrollOffset = Math.max(0, Math.min(this.maxScroll, (int) (this.scrollOffset - verticalAmount * 20)));
        return true;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}