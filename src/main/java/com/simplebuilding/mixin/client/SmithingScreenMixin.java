package com.simplebuilding.mixin.client;

import com.simplebuilding.client.gui.TrimReferenceScreen;
import com.simplebuilding.client.gui.widget.CyclingTrimButton;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.ingame.ForgingScreen;
import net.minecraft.client.gui.screen.ingame.SmithingScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.SmithingScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(SmithingScreen.class)
public abstract class SmithingScreenMixin extends ForgingScreen<SmithingScreenHandler> {

    private static final TagKey<Item> TRIM_TEMPLATES = TagKey.of(RegistryKeys.ITEM, Identifier.ofVanilla("trim_templates"));

    public SmithingScreenMixin(SmithingScreenHandler handler, PlayerInventory playerInventory, Text title, Identifier texture) {
        super(handler, playerInventory, title, texture);
    }

    @Inject(method = "setup", at = @At("TAIL"))
    private void simplebuilding$addAnimatedReferenceButton(CallbackInfo ci) {
        int guiLeft = (this.width - this.backgroundWidth) / 2;
        int btnX = guiLeft - 25;
        int btnY = (this.height - this.backgroundHeight) / 2 + 5;

        List<ItemStack> trimTemplates = new ArrayList<>();

        if (this.client != null && this.client.world != null) {
            // 1. Vanilla & Modded Tags laden
            var registry = this.client.world.getRegistryManager().getOptional(RegistryKeys.ITEM);
            if (registry.isPresent()) {
                var entries = registry.get().getOptional(TRIM_TEMPLATES);
                if (entries.isPresent()) {
                    entries.get().stream().forEach(entry ->
                            trimTemplates.add(new ItemStack(entry.value()))
                    );
                }
            }

            // 2. Explizit SimpleBuilding Enderite Trim hinzufügen (falls nicht im Tag)
            tryAddStack(trimTemplates, Identifier.of("simplebuilding", "enderite_armor_trim_smithing_template"));

            // 3. Enderscape Support (Dynamisch prüfen)
            if (FabricLoader.getInstance().isModLoaded("enderscape")) {
                tryAddStack(trimTemplates, Identifier.of("enderscape", "stasis_armor_trim_smithing_template"));
            }
        }

        // Fallback, falls leer
        if (trimTemplates.isEmpty()) {
            trimTemplates.add(Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE.getDefaultStack());
        }

        this.addDrawableChild(new CyclingTrimButton(btnX, btnY, 20, 20, trimTemplates, button -> {
            if (this.client != null) {
                // Hier übergeben wir den aktuellen Screen als Parent, damit man mit "ESC" zurückkommt
                this.client.setScreen(new TrimReferenceScreen(this));
            }
        }));
    }

    private void tryAddStack(List<ItemStack> list, Identifier id) {
        if (this.client == null || this.client.world == null) return;

        Optional<Item> item = this.client.world.getRegistryManager().getOptional(RegistryKeys.ITEM)
                .flatMap(reg -> reg.getOptionalValue(id)); // FIX: getOptionalValue verwenden

        item.ifPresent(value -> list.add(new ItemStack(value)));
    }
}