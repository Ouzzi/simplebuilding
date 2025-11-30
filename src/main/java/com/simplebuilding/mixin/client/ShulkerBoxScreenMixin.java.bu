package com.simplebuilding.mixin.client;

import com.simplebuilding.util.IEnchantableShulkerBox;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShulkerBoxScreen.class)
public abstract class ShulkerBoxScreenMixin extends HandledScreen<ShulkerBoxScreenHandler> {

    public ShulkerBoxScreenMixin(ShulkerBoxScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void renderSideEnchantments(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        ItemEnchantmentsComponent enchants = null;

        // --- 1. RAYCAST LOGIK (Fix für Range Enchantment & Private Access) ---
        // FIX: Wir nutzen den Getter 'getCameraEntity()' statt des privaten Feldes.
        if (client.getCameraEntity() != null && client.world != null) {

            // Raycast über 20 Blöcke durchführen
            // Parameter: (Distanz, TickDelta, includeFluids)
            HitResult hit = client.getCameraEntity().raycast(20.0, delta, false);

            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hit;
                BlockPos pos = blockHit.getBlockPos();
                BlockEntity be = client.world.getBlockEntity(pos);

                if (be instanceof IEnchantableShulkerBox box) {
                    enchants = box.simplebuilding$getEnchantments();
                }
            }
        }

        // --- RENDERING START ---

        int startX = this.x + this.backgroundWidth + 6;
        int startY = this.y;

        Text header = Text.literal("Enchantments").formatted(Formatting.GOLD, Formatting.UNDERLINE);

        if (enchants != null && !enchants.isEmpty()) {
            var entries = enchants.getEnchantmentEntries();
            int lineHeight = 10;
            int totalHeight = (entries.size() + 1) * lineHeight + 6;

            int maxWidth = this.textRenderer.getWidth(header);
            for (var entry : entries) {
                Text name = Enchantment.getName(entry.getKey(), entry.getIntValue());
                int w = this.textRenderer.getWidth(name);
                if (w > maxWidth) maxWidth = w;
            }
            int boxWidth = maxWidth + 8;

            // Hintergrund (Halbtransparenter schwarzer Kasten)
            context.fill(startX - 4, startY - 4, startX + boxWidth, startY + totalHeight, 0x90000000);

            // Header (Weißer Text 0xFFFFFFFF)
            context.drawTextWithShadow(this.textRenderer, header, startX, startY, 0xFFFFFFFF);
            startY += 12;

            // Liste
            for (var entry : entries) {
                Text name = Enchantment.getName(entry.getKey(), entry.getIntValue()).copy().formatted(Formatting.GREEN);
                context.drawTextWithShadow(this.textRenderer, name, startX, startY, 0xFFFFFFFF);
                startY += lineHeight;
            }

        } else {
            // "None" Anzeige (für Shulker ohne Enchants)
            Text noneText = Text.literal("None").formatted(Formatting.GRAY);
            int boxWidth = Math.max(this.textRenderer.getWidth(header), this.textRenderer.getWidth(noneText)) + 8;

            context.fill(startX - 4, startY - 4, startX + boxWidth, startY + 24, 0x90000000);
            context.drawTextWithShadow(this.textRenderer, header, startX, startY, 0xFFFFFFFF);
            context.drawTextWithShadow(this.textRenderer, noneText, startX, startY + 12, 0xFFFFFFFF);
        }
    }
}