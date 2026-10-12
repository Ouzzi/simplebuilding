package com.simplebuilding.client.gui;

import com.simplebuilding.util.TrimBonusCatalog;
import com.simplebuilding.util.TrimMultiplierLogic;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
    /** N43: panel in the smithing palette with the list in a light field (26.3, style on). */
    private final boolean styled = ModScreenStyle.panels();
    private ModScreenLayout.Box panel, field;

    private static final Identifier SCROLLER_SPRITE = Identifier.withDefaultNamespace("widget/scroller");
    private static final Identifier SCROLLER_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("widget/scroller_background");

    private record ReferenceEntry(ItemStack icon, Component text, Component info, boolean isHeader) {}

    public TrimReferenceScreen(Screen parent) {
        super(Component.translatable("screen.simplebuilding.trim_reference"));
        this.parent = parent;
        if (Minecraft.getInstance().player != null) {
            this.playerMultiplier = TrimMultiplierLogic.getMultiplier(Minecraft.getInstance().player);
        }

        populateEntries();
    }

    /**
     * Alle Zahlen kommen aus TrimBonusCatalog, also aus denselben Konstanten, mit denen
     * TrimEffectUtil rechnet. Namen sind die Vanilla-Uebersetzungen der Muster und Materialien.
     */
    private void populateEntries() {
        float resonance = (float) playerMultiplier;
        addHeader(Component.translatable("screen.simplebuilding.trim_reference.resonance", TrimBonusCatalog.format(resonance)));
        addHeader(Component.translatable("screen.simplebuilding.trim_reference.materials"));

        addMaterial(Items.DIAMOND, "minecraft", "diamond", resonance);
        addMaterial(Items.GOLD_INGOT, "minecraft", "gold", resonance);
        addMaterial(Items.IRON_INGOT, "minecraft", "iron", resonance);
        addMaterial(Items.EMERALD, "minecraft", "emerald", resonance);
        addMaterial(Items.NETHERITE_INGOT, "minecraft", "netherite", resonance);
        addMaterial(Items.COPPER_INGOT, "minecraft", "copper", resonance);
        addMaterial(Items.RESIN_BRICK, "minecraft", "resin", resonance);
        addMaterial(Items.REDSTONE, "minecraft", "redstone", resonance);
        addMaterial(Items.QUARTZ, "minecraft", "quartz", resonance);
        addMaterial(Items.AMETHYST_SHARD, "minecraft", "amethyst", resonance);
        addMaterial(Items.LAPIS_LAZULI, "minecraft", "lapis", resonance);
        tryAddModMaterial("astralit_dust", "astralit", resonance);
        tryAddModMaterial("nihilith_shard", "nihilith", resonance);
        tryAddModMaterial("enderite_ingot", "enderite", resonance);

        entries.add(new ReferenceEntry(ItemStack.EMPTY, Component.empty(), Component.empty(), false));

        addHeader(Component.translatable("screen.simplebuilding.trim_reference.patterns"));
        addPattern(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, "sentry", resonance);
        addPattern(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE, "vex", resonance);
        addPattern(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, "wild", resonance);
        addPattern(Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, "dune", resonance);
        addPattern(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, "coast", resonance);
        addPattern(Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE, "wayfinder", resonance);
        addPattern(Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE, "raiser", resonance);
        addPattern(Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE, "host", resonance);
        addPattern(Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, "ward", resonance);
        addPattern(Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, "silence", resonance);
        addPattern(Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, "tide", resonance);
        addPattern(Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, "snout", resonance);
        addPattern(Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE, "rib", resonance);
        addPattern(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE, "eye", resonance);
        addPattern(Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, "spire", resonance);
        addPattern(Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE, "flow", resonance);
        addPattern(Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE, "bolt", resonance);
        addPattern(Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE, "shaper", resonance);
    }

    private void addMaterial(Item item, String namespace, String path, float resonance) {
        addEntry(item, Component.translatable("trim_material." + namespace + "." + path),
                TrimBonusCatalog.forMaterial(path), resonance);
    }

    private void addPattern(Item item, String path, float resonance) {
        addEntry(item, Component.translatable("trim_pattern.minecraft." + path),
                TrimBonusCatalog.forPattern(path), resonance);
    }

    /** Werte je Teil bei der aktuellen Resonanz, im Stil der Vanilla-Attributzeilen. */
    private void addEntry(Item item, Component name, List<TrimBonusCatalog.Bonus> bonuses, float resonance) {
        MutableComponent info = Component.empty();
        for (int i = 0; i < bonuses.size(); i++) {
            if (i > 0) info.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
            // Vanilla's attribute blue; dark blue on the light field of the styled panel.
            info.append(bonuses.get(i).describe(1.0f, resonance).copy().withStyle(styled ? ChatFormatting.DARK_BLUE : ChatFormatting.BLUE));
        }
        if (bonuses.isEmpty()) {
            info.append(Component.translatable("screen.simplebuilding.trim_reference.none").withStyle(ChatFormatting.DARK_GRAY));
        }
        entries.add(new ReferenceEntry(new ItemStack(item), name, info, false));
    }

    private void tryAddModMaterial(String itemPath, String materialPath, float resonance) {
        if (Minecraft.getInstance().level == null) return;
        Optional<Item> item = Minecraft.getInstance().level.registryAccess()
                .lookup(Registries.ITEM)
                .flatMap(reg -> reg.getOptional(Identifier.fromNamespaceAndPath("simplebuilding", itemPath)));
        item.ifPresent(value -> addMaterial(value, "simplebuilding", materialPath, resonance));
    }

    private void addHeader(Component text) {
        entries.add(new ReferenceEntry(ItemStack.EMPTY, styled ? text.copy() : text.copy().withStyle(ChatFormatting.YELLOW), null, true));
    }

    @Override
    protected void init() {
        if (styled) {
            int contentW = 0;
            for (ReferenceEntry entry : entries) {
                contentW = Math.max(contentW, font.width(entry.text) + (entry.isHeader ? 0 : 22));
                if (entry.info != null) contentW = Math.max(contentW, font.width(entry.info) + 22);
            }
            ModScreenLayout.Box[] boxes = ModScreenLayout.trimReference(this.width, this.height, contentW);
            this.panel = boxes[0];
            this.field = boxes[1];
            ModScreenLayout.Box done = boxes[2];
            this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                    .bounds(done.x(), done.y(), done.width(), done.height()).build());
            this.maxScroll = Math.max(0, entries.size() * rowHeight + 8 - field.height());
            this.scrollOffset = Math.min(this.scrollOffset, this.maxScroll);
            return;
        }
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(this.width / 2 - 50, this.height - 25, 100, 20)
                .build());

        int contentHeight = entries.size() * rowHeight + 40;
        this.maxScroll = Math.max(0, contentHeight - (this.height - 40));
    }

    /** The styled look: smithing-coloured box, title top left, rows clipped to the light field, scroll bar inside it. */
    private boolean extractPanel(GuiGraphicsExtractor context) {
        int fill = ModScreenLayout.TRIM_REFERENCE_FILL;
        if (panel == null || !ModScreenStyle.panel(context, fill, panel.x(), panel.y(), panel.width(), panel.height())) return false;
        ModScreenStyle.field(context, fill, field.x(), field.y(), field.width(), field.height());
        context.text(this.font, this.title, panel.x() + ModScreenLayout.SIDE, panel.y() + 6, ModScreenStyle.label(fill, 0xFF404040), false);
        int textColor = 0xFF404040;
        int left = field.x() + 4;
        int scrollX = field.right() - ModScreenLayout.TRIM_SCROLL_W - 2;
        context.enableScissor(field.x() + 1, field.y() + 1, field.right() - 1, field.bottom() - 1);
        int y = field.y() + 5 - scrollOffset;
        for (ReferenceEntry entry : entries) {
            if (y > field.y() - rowHeight && y < field.bottom()) {
                if (entry.isHeader) {
                    context.text(this.font, entry.text, left, y + 6, textColor, false);
                } else if (!entry.icon.isEmpty()) {
                    context.item(entry.icon, left, y);
                    context.text(this.font, entry.text, left + 22, y - 1, textColor, false);
                    if (entry.info != null) context.text(this.font, entry.info, left + 22, y + 10, textColor, false);
                }
            }
            y += rowHeight;
        }
        context.disableScissor();
        if (maxScroll > 0) {
            int track = field.height() - 4;
            int barH = Math.max(12, track * track / (track + maxScroll));
            int barY = field.y() + 2 + (int) ((float) scrollOffset / maxScroll * (track - barH));
            context.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_BACKGROUND_SPRITE, scrollX, field.y() + 2, ModScreenLayout.TRIM_SCROLL_W, track);
            context.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_SPRITE, scrollX, barY, ModScreenLayout.TRIM_SCROLL_W, barH);
        }
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Kein eigener Verlauf: der Screen zeichnet Unschaerfe und Menue-Hintergrund schon selbst,
        // ein zweiter Verlauf dunkelte ihn doppelt ab.

        if (styled && extractPanel(context)) {
            super.extractRenderState(context, mouseX, mouseY, delta);
            return;
        }
        int startY = 15 - scrollOffset;
        int y = startY;

        context.centeredText(this.font, this.title, this.width / 2, 5, 0xFFFFFFFF);

        for (ReferenceEntry entry : entries) {
            // Nur sichtbare Elemente rendern
            if (y > -rowHeight && y < this.height - 30) {
                if (entry.isHeader) {
                    context.centeredText(this.font, entry.text, this.width / 2, y + 10, 0xFFFFFFFF);
                } else if (!entry.icon.isEmpty()) {
                    int centerX = this.width / 2;
                    int iconX = centerX - 100;

                    // Icon
                    context.item(entry.icon, iconX, y);

                    // Titel (z.B. "Diamond: Hard Shell")
                    context.text(this.font, entry.text, iconX + 22, y - 1, 0xFFFFFFFF);

                    // Stats Zeile (z.B. "Resist: 1.5% -> 2.1%")
                    if (entry.info != null) {
                        context.text(this.font, entry.info, iconX + 22, y + 10, 0xFFFFFFFF);
                    }
                }
            }
            y += rowHeight;
        }

        if (maxScroll > 0) {
            int scrollBarH = (int)((float)(this.height - 40) * ((float)(this.height - 40) / (entries.size() * rowHeight)));
            int scrollBarY = 30 + (int)((float)scrollOffset / maxScroll * (this.height - 40 - scrollBarH));
            // Vanilla-Scrollleiste (AbstractScrollArea): Sprites, 6 px breit
            context.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_BACKGROUND_SPRITE, this.width - 8, 30, 6, this.height - 40);
            context.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_SPRITE, this.width - 8, scrollBarY, 6, scrollBarH);
        }

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        this.scrollOffset = Math.max(0, Math.min(this.maxScroll, (int) (this.scrollOffset - verticalAmount * 20)));
        return true;
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }
}