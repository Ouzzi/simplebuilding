package com.simplebuilding.mixin;

import com.simplebuilding.util.IEnchantableShulkerBox;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntity.class)
public abstract class ShulkerBoxBlockEntityMixin implements IEnchantableShulkerBox {

    @Unique
    private ItemEnchantmentsComponent simplebuilding$storedEnchantments = ItemEnchantmentsComponent.DEFAULT;

    public ShulkerBoxBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {}

    // --- 1. ITEM -> BLOCK (Platzieren) ---
    @Inject(method = "readComponents(Lnet/minecraft/component/ComponentsAccess;)V", at = @At("TAIL"))
    private void captureEnchantsFromComponents(ComponentsAccess components, CallbackInfo ci) {
        if ((Object) this instanceof ShulkerBoxBlockEntity) {
            ItemEnchantmentsComponent enchants = components.get(DataComponentTypes.ENCHANTMENTS);
            if (enchants != null && !enchants.isEmpty()) {
                this.simplebuilding$storedEnchantments = enchants;
                ((BlockEntity)(Object)this).markDirty();
            }
        }
    }

    // --- 2. BLOCK -> ITEM (Creative Pick) ---
    @Inject(method = "addComponents", at = @At("TAIL"))
    private void addEnchantsToItem(ComponentMap.Builder builder, CallbackInfo ci) {
        if ((Object) this instanceof ShulkerBoxBlockEntity) {
            if (this.simplebuilding$storedEnchantments != null && !this.simplebuilding$storedEnchantments.isEmpty()) {
                builder.add(DataComponentTypes.ENCHANTMENTS, this.simplebuilding$storedEnchantments);
            }
        }
    }

    // --- 3. LESEN (Festplatte) - Bleibt als Basis ---
    @Inject(method = "readData", at = @At("TAIL"))
    private void readCustomEnchants(ReadView view, CallbackInfo ci) {
        if ((Object) this instanceof ShulkerBoxBlockEntity) {
            view.read("SimpleBuildingEnchants", ItemEnchantmentsComponent.CODEC)
                    .ifPresent(enchants -> {
                        this.simplebuilding$storedEnchantments = (ItemEnchantmentsComponent) enchants;
                        // Optional: Log für Erfolg
                        // System.out.println("readData success: " + enchants);
                    });
        }
    }

    // --- 3b. LESEN (Netzwerk) - DER NEUE FIX ---
    // Wir versuchen in die Methode zu injecten, die NBT liest.
    // Da 'read' oft final ist oder readData aufruft (was bei dir aber fehlschlägt),
    // versuchen wir es über den NBT-Provider.
    // Wenn deine IDE 'method_11014' nicht mag, versuche den Namen "readNbt" EXPLICIT.
    // Wenn auch das fehlschlägt, gibt es noch eine letzte Option:
    // Wir injecten in 'createComponentMap' (das wird oft nach dem Laden aufgerufen).

    // ABER: Der sicherste Weg ist, 'readData' zu vertrauen, wenn wir sicher sind, dass das NBT ankommt.
    // Wenn 'readData' fehlschlägt, liegt es daran, dass view.read() leer zurückgibt.
    // Das passiert, wenn der Key nicht im NBT ist.

    // Wir fügen einen DEBUG PRINT in readData hinzu, um zu sehen, WELCHE Keys da sind.
    // Da ReadView keine Keys auflisten kann, injecten wir testweise in eine Methode, die NbtCompound hat.

    // --- 4. SCHREIBEN (Festplatte & Netzwerk) ---
    @Inject(method = "writeData", at = @At("TAIL"))
    private void writeCustomEnchants(WriteView view, CallbackInfo ci) {
        if ((Object) this instanceof ShulkerBoxBlockEntity) {
            if (this.simplebuilding$storedEnchantments != null && !this.simplebuilding$storedEnchantments.isEmpty()) {
                view.put("SimpleBuildingEnchants", ItemEnchantmentsComponent.CODEC, this.simplebuilding$storedEnchantments);
            }
        }
    }

    // --- 5. NETZWERK: Initial Chunk Data ---
    // Das funktioniert laut Log ("SERVER: Sending Initial NBT").
    @Inject(method = "toInitialChunkDataNbt", at = @At("RETURN"), cancellable = true)
    private void addEnchantsToInitialData(RegistryWrapper.WrapperLookup registryLookup, CallbackInfoReturnable<NbtCompound> cir) {
        if ((Object) this instanceof ShulkerBoxBlockEntity) {
            NbtCompound nbt = cir.getReturnValue();
            if (nbt == null) nbt = new NbtCompound();

            if (this.simplebuilding$storedEnchantments != null && !this.simplebuilding$storedEnchantments.isEmpty()) {
                final NbtCompound finalNbt = nbt;
                ItemEnchantmentsComponent.CODEC.encodeStart(NbtOps.INSTANCE, this.simplebuilding$storedEnchantments)
                        .resultOrPartial(error -> {})
                        .ifPresent(tag -> finalNbt.put("SimpleBuildingEnchants", tag));
                // Log bestätigt, dass es gesendet wird
            }
            cir.setReturnValue(nbt);
        }
    }

    // --- 6. NETZWERK: Update Packet ---
    @Inject(method = "toUpdatePacket", at = @At("HEAD"), cancellable = true)
    private void forceShulkerUpdatePacket(CallbackInfoReturnable<Packet<ClientPlayPacketListener>> cir) {
        if ((Object) this instanceof ShulkerBoxBlockEntity) {
            cir.setReturnValue(BlockEntityUpdateS2CPacket.create((BlockEntity)(Object)this));
        }
    }

    // --- Interface ---
    @Override
    public void simplebuilding$setEnchantments(ItemEnchantmentsComponent enchants) {
        this.simplebuilding$storedEnchantments = enchants;
        ((BlockEntity)(Object)this).markDirty();

        World world = ((BlockEntity)(Object)this).getWorld();
        if (world != null && !world.isClient()) {
            world.updateListeners(((BlockEntity)(Object)this).getPos(), ((BlockEntity)(Object)this).getCachedState(), ((BlockEntity)(Object)this).getCachedState(), 3);
        }
    }

    @Override
    public ItemEnchantmentsComponent simplebuilding$getEnchantments() {
        return this.simplebuilding$storedEnchantments;
    }
}