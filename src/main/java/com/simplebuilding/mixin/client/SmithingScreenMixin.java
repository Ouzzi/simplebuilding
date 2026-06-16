package com.simplebuilding.mixin.client;

import com.simplebuilding.client.gui.TrimReferenceScreen;
import com.simplebuilding.client.gui.widget.CyclingTrimButton;
import com.simplebuilding.platform.ModEnvironment;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(SmithingScreen.class)
public abstract class SmithingScreenMixin extends ItemCombinerScreen<SmithingMenu> {

    private static final TagKey<Item> TRIM_TEMPLATES = TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("trim_templates"));

    public SmithingScreenMixin(SmithingMenu handler, Inventory playerInventory, Component title, Identifier texture) {
        super(handler, playerInventory, title, texture);
    }

    @Inject(method = "subInit", at = @At("TAIL"))
    private void simplebuilding$addAnimatedReferenceButton(CallbackInfo ci) {
        int guiLeft = (this.width - this.imageWidth) / 2;
        int btnX = guiLeft - 25;
        int btnY = (this.height - this.imageHeight) / 2 + 5;

        List<ItemStack> trimTemplates = new ArrayList<>();

        if (this.minecraft != null && this.minecraft.level != null) {
            // 1. Vanilla & Modded Tags laden
            var registry = this.minecraft.level.registryAccess().lookup(Registries.ITEM);
            if (registry.isPresent()) {
                var entries = registry.get().get(TRIM_TEMPLATES);
                if (entries.isPresent()) {
                    entries.get().stream().forEach(entry ->
                            trimTemplates.add(new ItemStack(entry.value()))
                    );
                }
            }

            // 2. Explizit SimpleBuilding Enderite Trim hinzufügen (falls nicht im Tag)
            tryAddStack(trimTemplates, Identifier.fromNamespaceAndPath("simplebuilding", "enderite_armor_trim_smithing_template"));

            // 3. Enderscape Support (Dynamisch prüfen)
            if (ModEnvironment.isModLoaded("enderscape")) {
                tryAddStack(trimTemplates, Identifier.fromNamespaceAndPath("enderscape", "stasis_armor_trim_smithing_template"));
            }
        }

        // Fallback, falls leer
        if (trimTemplates.isEmpty()) {
            trimTemplates.add(Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE.getDefaultInstance());
        }

        this.addRenderableWidget(new CyclingTrimButton(btnX, btnY, 20, 20, trimTemplates, button -> {
            if (this.minecraft != null) {
                // Hier übergeben wir den aktuellen Screen als Parent, damit man mit "ESC" zurückkommt
                this.minecraft.setScreen(new TrimReferenceScreen(this));
            }
        }));
    }

    private void tryAddStack(List<ItemStack> list, Identifier id) {
        if (this.minecraft == null || this.minecraft.level == null) return;

        Optional<Item> item = this.minecraft.level.registryAccess().lookup(Registries.ITEM)
                .flatMap(reg -> reg.getOptional(id)); // FIX: getOptionalValue verwenden

        item.ifPresent(value -> list.add(new ItemStack(value)));
    }
}