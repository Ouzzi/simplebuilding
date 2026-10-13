package com.simplebuilding.mixin;

import com.simplebuilding.util.ExtraHead;
import com.simplebuilding.version.McVersion;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The extra head slot's stack (owner N49, {@link ExtraHead}): synced entity data of the player, so the owner's menus
 * and every client that sees the player share one value; saved with the player, dropped on death (unless
 * {@code keepInventory}; Curse of Vanishing destroys it like any equipment).
 */
@Mixin(Player.class)
public abstract class PlayerExtraHeadMixin implements ExtraHead.Holder {
    @Unique
    private static final EntityDataAccessor<ItemStack> SIMPLEBUILDING_EXTRA_HEAD =
            SynchedEntityData.defineId(Player.class, EntityDataSerializers.ITEM_STACK);

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void simplebuilding$defineExtraHead(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(SIMPLEBUILDING_EXTRA_HEAD, ItemStack.EMPTY);
    }

    @Override
    public ItemStack simplebuilding$getExtraHead() {
        return ((Player) (Object) this).getEntityData().get(SIMPLEBUILDING_EXTRA_HEAD);
    }

    @Override
    public void simplebuilding$setExtraHead(ItemStack stack) {
        ((Player) (Object) this).getEntityData().set(SIMPLEBUILDING_EXTRA_HEAD, stack == null ? ItemStack.EMPTY : stack);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void simplebuilding$saveExtraHead(ValueOutput output, CallbackInfo ci) {
        ItemStack head = simplebuilding$getExtraHead();
        if (!head.isEmpty()) {
            output.store(ExtraHead.SAVE_KEY, ItemStack.CODEC, head);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void simplebuilding$loadExtraHead(ValueInput input, CallbackInfo ci) {
        simplebuilding$setExtraHead(input.read(ExtraHead.SAVE_KEY, ItemStack.CODEC).filter(ExtraHead::fits).orElse(ItemStack.EMPTY));
    }

    @Inject(method = "dropEquipment", at = @At("TAIL"))
    private void simplebuilding$dropExtraHead(ServerLevel level, CallbackInfo ci) {
        ItemStack head = simplebuilding$getExtraHead();
        if (head.isEmpty() || level.getGameRules().get(GameRules.KEEP_INVENTORY)) {
            return;
        }
        simplebuilding$setExtraHead(ItemStack.EMPTY);
        if (!EnchantmentHelper.has(head, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
            McVersion.drop((Player) (Object) this, head, true, false);
        }
    }
}
