package com.simplebuilding.modules.simpletweaks.admin;

import java.nio.file.*;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

/**
 * The saved data of a player who is not online ({@code <world>/players/data/<uuid>.dat}). Read like Vanilla (DataFixer),
 * written back like Vanilla (temporary file, {@code .dat_old}). Slots use the Inventory numbering: 0–35 items,
 * 36 feet, 37 legs, 38 chest, 39 head, 40 offhand; the ender chest has 27 slots.
 */
public final class OfflineData {
    public static final int INVENTORY = 41, ENDER = 27;
    private static final EquipmentSlot[] EQUIPMENT = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.OFFHAND};
    private final MinecraftServer server;
    private final UUID id;
    private final CompoundTag tag;

    private OfflineData(MinecraftServer server, UUID id, CompoundTag tag) { this.server = server; this.id = id; this.tag = tag; }

    public static Path file(MinecraftServer server, UUID id) { return server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(id + ".dat"); }

    /** Null when the player has never played here (no data file). */
    public static OfflineData load(MinecraftServer server, UUID id) {
        Path file = file(server, id);
        if (!Files.isRegularFile(file)) file = file.resolveSibling(id + ".dat_old");
        if (!Files.isRegularFile(file)) return null;
        try {
            CompoundTag raw = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            CompoundTag fixed = DataFixTypes.PLAYER.updateToCurrentVersion(server.getFixerUpper(), raw, NbtUtils.getDataVersion(raw));
            return new OfflineData(server, id, fixed);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("simpletweaks-admin").warn("Could not read offline player data {}", file, e);
            return null;
        }
    }
    public UUID id() { return id; }
    private HolderLookup.Provider registries() { return server.registryAccess(); }

    public List<ItemStack> inventory() {
        var out = new ArrayList<ItemStack>(Collections.nCopies(INVENTORY, ItemStack.EMPTY));
        for (var e : tag.getListOrEmpty("Inventory")) if (e instanceof CompoundTag c) {
            int slot = c.getByteOr("Slot", (byte) -1) & 255;
            if (slot < 36) out.set(slot, AdminCodecs.load(registries(), c));
        }
        var eq = tag.getCompoundOrEmpty("equipment");
        for (int i = 0; i < EQUIPMENT.length; i++) {
            var t = eq.get(EQUIPMENT[i].getSerializedName());
            if (t != null) out.set(36 + i, AdminCodecs.load(registries(), t));
        }
        return out;
    }
    public void setInventory(List<ItemStack> slots) {
        var list = new ListTag();
        for (int i = 0; i < 36 && i < slots.size(); i++) if (!slots.get(i).isEmpty()) list.add(slotTag(i, slots.get(i)));
        tag.put("Inventory", list);
        var eq = tag.getCompoundOrEmpty("equipment").copy();
        for (int i = 0; i < EQUIPMENT.length; i++) {
            var stack = 36 + i < slots.size() ? slots.get(36 + i) : ItemStack.EMPTY;
            if (stack.isEmpty()) eq.remove(EQUIPMENT[i].getSerializedName());
            else eq.put(EQUIPMENT[i].getSerializedName(), AdminCodecs.save(registries(), stack));
        }
        tag.put("equipment", eq);
    }
    public List<ItemStack> ender() {
        var out = new ArrayList<ItemStack>(Collections.nCopies(ENDER, ItemStack.EMPTY));
        for (var e : tag.getListOrEmpty("EnderItems")) if (e instanceof CompoundTag c) {
            int slot = c.getByteOr("Slot", (byte) -1) & 255;
            if (slot < ENDER) out.set(slot, AdminCodecs.load(registries(), c));
        }
        return out;
    }
    public void setEnder(List<ItemStack> slots) {
        var list = new ListTag();
        for (int i = 0; i < ENDER && i < slots.size(); i++) if (!slots.get(i).isEmpty()) list.add(slotTag(i, slots.get(i)));
        tag.put("EnderItems", list);
    }
    public int[] xp() { return new int[]{tag.getIntOr("XpLevel", 0), Float.floatToIntBits(tag.getFloatOr("XpP", 0f)), tag.getIntOr("XpTotal", 0)}; }
    public void setXp(int level, float progress, int total) { tag.putInt("XpLevel", level); tag.putFloat("XpP", progress); tag.putInt("XpTotal", total); }
    private CompoundTag slotTag(int slot, ItemStack stack) {
        var t = (CompoundTag) AdminCodecs.save(registries(), stack);
        t.putByte("Slot", (byte) slot);
        return t;
    }

    /** Writes the data back (Vanilla's safe replace keeps the previous file as .dat_old). */
    public boolean save() {
        try {
            Path dir = file(server, id).getParent();
            Files.createDirectories(dir);
            Path tmp = Files.createTempFile(dir, id + "-", ".dat");
            NbtIo.writeCompressed(NbtUtils.addCurrentDataVersion(tag), tmp);
            Util.safeReplaceFile(dir.resolve(id + ".dat"), tmp, dir.resolve(id + ".dat_old"));
            return true;
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("simpletweaks-admin").warn("Could not write offline player data of {}", id, e);
            return false;
        }
    }
    /** Tests: data for a profile that has never been saved yet. */
    public static OfflineData empty(MinecraftServer server, UUID id) {
        var t = new CompoundTag();
        return new OfflineData(server, id, t);
    }
}
