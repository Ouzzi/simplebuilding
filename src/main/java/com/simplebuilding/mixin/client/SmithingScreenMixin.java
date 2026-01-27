package com.simplebuilding.mixin.client;

import com.simplebuilding.client.gui.TrimReferenceScreen;
import com.simplebuilding.client.gui.widget.CyclingTrimButton;
import net.minecraft.client.gui.screen.ingame.ForgingScreen;
import net.minecraft.client.gui.screen.ingame.SmithingScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
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

@Mixin(SmithingScreen.class)
public abstract class SmithingScreenMixin extends ForgingScreen<SmithingScreenHandler> {

    // FIX: Wir definieren den TagKey manuell, um Import-Probleme zu vermeiden
    private static final TagKey<Item> TRIM_TEMPLATES = TagKey.of(RegistryKeys.ITEM, Identifier.ofVanilla("trim_templates"));

    public SmithingScreenMixin(SmithingScreenHandler handler, PlayerInventory playerInventory, Text title, Identifier texture) {
        super(handler, playerInventory, title, texture);
    }

    // FIX: "setup" statt "init"
    @Inject(method = "setup", at = @At("TAIL"))
    private void simplebuilding$addAnimatedReferenceButton(CallbackInfo ci) {
        int guiLeft = (this.width - this.backgroundWidth) / 2;
        int btnX = guiLeft - 25;
        int btnY = (this.height - this.backgroundHeight) / 2 + 5;

        List<ItemStack> trimTemplates = new ArrayList<>();

        if (this.client != null && this.client.world != null) {
            var registry = this.client.world.getRegistryManager().getOptional(RegistryKeys.ITEM);

            if (registry.isPresent()) {
                // FIX: "getOptional" statt "getEntryList" (Standard in 1.21)
                var entries = registry.get().getOptional(TRIM_TEMPLATES);

                if (entries.isPresent()) {
                    entries.get().stream().forEach(entry ->
                            trimTemplates.add(new ItemStack(entry.value()))
                    );
                }
            }
        }

        // Fallback, falls keine Tags gefunden wurden
        if (trimTemplates.isEmpty()) {
            trimTemplates.add(Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE.getDefaultStack());
            trimTemplates.add(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE.getDefaultStack());
            trimTemplates.add(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE.getDefaultStack());
            trimTemplates.add(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE.getDefaultStack());
            trimTemplates.add(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE.getDefaultStack());
            trimTemplates.add(Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE.getDefaultStack());
            trimTemplates.add(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE.getDefaultStack());
            trimTemplates.add(Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE.getDefaultStack());
        }

        this.addDrawableChild(new CyclingTrimButton(btnX, btnY, 20, 20, trimTemplates, button -> {
            if (this.client != null) {
                this.client.setScreen(new TrimReferenceScreen(this));
            }
        }));
    }
}