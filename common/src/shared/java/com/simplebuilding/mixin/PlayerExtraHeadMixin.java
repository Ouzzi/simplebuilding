package com.simplebuilding.mixin;

import com.simplebuilding.networking.ExtraHeadPayload;
import com.simplebuilding.platform.PlatformServices;
import com.simplebuilding.util.ExtraHead;
import com.simplebuilding.version.McVersion;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The extra head slot's stack (owner N49, {@link ExtraHead}): a field of the player, saved with it, dropped on death
 * (unless {@code keepInventory}; Curse of Vanishing destroys it like any equipment). The server sends every change -
 * and the current head after a dimension change - to the players of the level ({@link ExtraHeadPayload}); a client
 * that starts seeing the player gets it from {@code EntityExtraHeadSeenMixin}. No synced entity data: NeoForge
 * forbids adding it to a foreign entity.
 */
@Mixin(Player.class)
public abstract class PlayerExtraHeadMixin implements ExtraHead.Holder {
    @Unique
    private ItemStack simplebuilding$extraHead = ItemStack.EMPTY;
    @Unique
    private @Nullable ItemStack simplebuilding$sentHead;
    @Unique
    private @Nullable Level simplebuilding$sentLevel;

    @Override
    public ItemStack simplebuilding$getExtraHead() {
        return this.simplebuilding$extraHead;
    }

    @Override
    public void simplebuilding$setExtraHead(ItemStack stack) {
        this.simplebuilding$extraHead = stack == null ? ItemStack.EMPTY : stack;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void simplebuilding$syncExtraHead(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (!(self.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack head = this.simplebuilding$extraHead;
        if (this.simplebuilding$sentHead != null && level == this.simplebuilding$sentLevel
                && ItemStack.matches(head, this.simplebuilding$sentHead)) {
            return;
        }
        this.simplebuilding$sentHead = head.copy();
        this.simplebuilding$sentLevel = level;
        ExtraHeadPayload payload = new ExtraHeadPayload(self.getId(), head.copy());
        for (ServerPlayer viewer : level.players()) {
            if (PlatformServices.canSendToPlayer(viewer, ExtraHeadPayload.ID)) {
                PlatformServices.sendToPlayer(viewer, payload);
            }
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void simplebuilding$saveExtraHead(ValueOutput output, CallbackInfo ci) {
        if (!this.simplebuilding$extraHead.isEmpty()) {
            output.store(ExtraHead.SAVE_KEY, ItemStack.CODEC, this.simplebuilding$extraHead);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void simplebuilding$loadExtraHead(ValueInput input, CallbackInfo ci) {
        simplebuilding$setExtraHead(input.read(ExtraHead.SAVE_KEY, ItemStack.CODEC).filter(ExtraHead::fits).orElse(ItemStack.EMPTY));
    }

    @Inject(method = "dropEquipment", at = @At("TAIL"))
    private void simplebuilding$dropExtraHead(ServerLevel level, CallbackInfo ci) {
        ItemStack head = this.simplebuilding$extraHead;
        if (head.isEmpty() || level.getGameRules().get(GameRules.KEEP_INVENTORY)) {
            return;
        }
        simplebuilding$setExtraHead(ItemStack.EMPTY);
        if (!EnchantmentHelper.has(head, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
            McVersion.drop((Player) (Object) this, head, true, false);
        }
    }
}
