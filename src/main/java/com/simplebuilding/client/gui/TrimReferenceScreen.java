package com.simplebuilding.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class TrimReferenceScreen extends Screen {
    private final Screen parent;
    private final List<ReferenceEntry> entries = new ArrayList<>();

    private int scrollOffset = 0;
    private int maxScroll = 0;
    private final int rowHeight = 24;

    // Record mit Tooltip
    private record ReferenceEntry(ItemStack icon, Text text, Text tooltip, boolean isHeader) {}

    public TrimReferenceScreen(Screen parent) {
        super(Text.literal("Trim Reference"));
        this.parent = parent;
        populateEntries();
    }

    private void populateEntries() {
        // --- MATERIALS ---
        addHeader("--- MATERIALS (Scales with Level & Survival) ---");

        addMaterial(Items.DIAMOND, "Diamond: Hard Shell", Formatting.AQUA,
                "Reduces physical damage taken.\nBase: 1.5% per piece.");

        addMaterial(Items.GOLD_INGOT, "Gold: Magic Dampening", Formatting.GOLD,
                "Greatly reduces magic damage (Potions, Evokers).\nBase: 3.0% per piece.");

        addMaterial(Items.IRON_INGOT, "Iron: Blunt Resistance", Formatting.GRAY,
                "Reduces projectile damage (Arrows, Tridents).\nBase: 2.0% per piece.");

        addMaterial(Items.EMERALD, "Emerald: Illager Resistance", Formatting.DARK_GREEN,
                "Reduces damage taken from Illagers (Pillagers, Vindicators).\nBase: 4.0% per piece.");

        addMaterial(Items.NETHERITE_INGOT, "Netherite: Pattern Boost", Formatting.DARK_GRAY,
                "Boosts the effect of the applied Trim Pattern by 50%.\nAlso reduces damage from Bosses.");

        addMaterial(Items.COPPER_INGOT, "Copper: Lightning Rod", Formatting.GOLD,
                "Reduces Lightning damage.\nBase: 5.0% per piece.");

        addMaterial(Items.REDSTONE, "Redstone: Trap Awareness", Formatting.RED,
                "Increases movement speed on land.\nBase: +1.5% Speed per piece.");

        addMaterial(Items.QUARTZ, "Quartz: Heat Shield", Formatting.WHITE,
                "Reduces Fire and Lava damage.\nBase: 2.5% per piece.");

        addMaterial(Items.AMETHYST_SHARD, "Amethyst: Resonance", Formatting.LIGHT_PURPLE,
                "Reduces Sonic Boom damage and grants\na chance to regenerate health over time.");

        addMaterial(Items.LAPIS_LAZULI, "Lapis: Curse Dampening", Formatting.BLUE,
                "Increases Experience (XP) gained from all sources.\nBase: +2.0% XP per piece.");

        entries.add(new ReferenceEntry(ItemStack.EMPTY, Text.empty(), null, false)); // Spacer

        // --- PATTERNS ---
        addHeader("--- TRIM PATTERNS (Thematic Effects) ---");

        addTrim(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, "Sentry: Projectile Dampening", Formatting.GRAY,
                "Reduces incoming projectile damage.\nBase: 2.5% per piece.");

        addTrim(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE, "Vex: Magic Dampening", Formatting.DARK_AQUA,
                "Reduces incoming magic damage.\nBase: 3.0% per piece.");

        addTrim(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, "Wild: Trap Resilience", Formatting.GREEN,
                "Reduces damage from Cacti and Sweet Berry Bushes.\nBase: 5.0% per piece.");

        addTrim(Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, "Dune: Blast Dampening", Formatting.GOLD,
                "Reduces explosion damage.\nBase: 3.0% per piece.");

        addTrim(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, "Coast: Oxygen Efficiency", Formatting.AQUA,
                "Chance to not consume air while underwater.\nBase: 10% Chance per piece.");

        addTrim(Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE, "Wayfinder: Travel Efficiency", Formatting.YELLOW,
                "Reduces hunger exhaustion while sprinting.\nBase: 5.0% reduction per piece.");

        addTrim(Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE, "Raiser: Experience Affinity", Formatting.DARK_GREEN,
                "Increases Experience (XP) gain.\nBase: +5.0% XP per piece.");

        addTrim(Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE, "Host: Trade Negotiator", Formatting.WHITE,
                "Increases the player's Luck attribute significantly.\nBase: +0.5 Luck per piece.");

        addTrim(Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, "Ward: Damage Absorption", Formatting.DARK_BLUE,
                "Reduces ALL incoming damage slightly.\nBase: 1.5% per piece.");

        addTrim(Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, "Silence: Stealth Mobility", Formatting.DARK_GRAY,
                "Reduces the detection range of mobs.\nBase: -10% Range per piece.");

        addTrim(Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, "Tide: Swim Agility", Formatting.BLUE,
                "Increases swim speed underwater.\nBase: +5.0% Speed per piece.");

        addTrim(Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, "Snout: Fire Dampening", Formatting.GOLD,
                "Reduces Fire damage.\nBase: 2.0% per piece.");

        addTrim(Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE, "Rib: Wither Resistance", Formatting.DARK_RED,
                "Reduces the duration of the Wither effect.\nBase: -1.0s Duration per piece.");

        addTrim(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE, "Eye: Ender Stability", Formatting.LIGHT_PURPLE,
                "Reduces Dragon Breath and Void damage.\nBase: 5.0% per piece.");

        addTrim(Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, "Spire: Feather Falling", Formatting.LIGHT_PURPLE,
                "Reduces Fall damage.\nBase: 4.0% per piece.");

        addTrim(Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE, "Flow: Aerial Agility", Formatting.WHITE,
                "Reduces damage from Wind Charges.\nBase: 5.0% per piece.");

        addTrim(Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE, "Bolt: Kinetic Response", Formatting.YELLOW,
                "Reduces Lightning damage and boosts Speed.\nBase: 10% Ltn Red. & +3% Speed.");
    }

    private void addHeader(String text) {
        entries.add(new ReferenceEntry(ItemStack.EMPTY, Text.literal(text).formatted(Formatting.BOLD, Formatting.YELLOW), null, true));
    }

    private void addMaterial(Item item, String text, Formatting color, String desc) {
        entries.add(new ReferenceEntry(new ItemStack(item), Text.literal(text).formatted(color), Text.literal(desc).formatted(Formatting.GRAY), false));
    }

    private void addTrim(Item item, String text, Formatting color, String desc) {
        entries.add(new ReferenceEntry(new ItemStack(item), Text.literal(text).formatted(color), Text.literal(desc).formatted(Formatting.GRAY), false));
    }

    @Override
    protected void init() {
        this.addDrawableChild(ButtonWidget.builder(Text.literal("X"), button -> this.close())
                .dimensions(this.width - 30, 5, 20, 20)
                .build());

        int contentHeight = entries.size() * rowHeight + 40;
        this.maxScroll = Math.max(0, contentHeight - this.height);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xD0000000); // Hintergrund

        int startY = 20 - scrollOffset;
        int y = startY;

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 5, 0xFFFFFFFF);

        // Variable, um den Tooltip zu speichern, der gezeichnet werden soll
        Text tooltipToRender = null;

        for (ReferenceEntry entry : entries) {
            // Culling (nur sichtbare zeichnen)
            if (y > -rowHeight && y < this.height) {

                if (entry.isHeader) {
                    context.drawCenteredTextWithShadow(this.textRenderer, entry.text, this.width / 2, y + 6, 0xFFFFFFFF);
                } else if (!entry.icon.isEmpty()) {
                    // Icon
                    int iconX = 20;
                    context.drawItem(entry.icon, iconX, y);

                    // Text
                    context.drawTextWithShadow(this.textRenderer, entry.text, iconX + 24, y + 4, 0xFFFFFFFF);

                    // Hover Check für Tooltip
                    if (entry.tooltip != null &&
                            mouseY >= y && mouseY < y + rowHeight &&
                            mouseX >= 10 && mouseX < this.width - 10) {
                        tooltipToRender = entry.tooltip;
                    }
                }
            }
            y += rowHeight;
        }

        super.render(context, mouseX, mouseY, delta);

        // Scrollbar
        if (maxScroll > 0) {
            int scrollbarHeight = (int) ((float) (this.height * this.height) / (float) (entries.size() * rowHeight + 40));
            int scrollbarY = (int) ((float) scrollOffset / maxScroll * (this.height - scrollbarHeight));
            int scrollbarX = this.width - 6;
            context.fill(scrollbarX, scrollbarY, scrollbarX + 2, scrollbarY + scrollbarHeight, 0xFF808080);
        }

        // Tooltip ganz am Ende zeichnen
        if (tooltipToRender != null) {
            // FIX: drawOrderedTooltip verwenden
            context.drawOrderedTooltip(this.textRenderer, this.textRenderer.wrapLines(tooltipToRender, 200), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        this.scrollOffset = Math.max(0, Math.min(this.maxScroll, (int) (this.scrollOffset - verticalAmount * 20)));
        return true;
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
}