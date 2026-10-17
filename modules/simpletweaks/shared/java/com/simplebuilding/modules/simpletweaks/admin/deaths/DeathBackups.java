package com.simplebuilding.modules.simpletweaks.admin.deaths;

import com.simplebuilding.modules.simpletweaks.admin.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

/**
 * T11: the last N deaths of every player (inventory incl. armor and offhand, XP, time, cause, position), newest first,
 * in {@code <world>/simpletweaks/deaths/<uuid>.dat}. Restored in the invsee screen ({@link com.simplebuilding.modules.simpletweaks.admin.invsee.Invsee}).
 */
public final class DeathBackups {
    public record Backup(long time, String cause, String dimension, int x, int y, int z, int xpLevel, float xpProgress, int xpTotal, List<ItemStack> slots) {
        public Backup { slots = new ArrayList<>(slots); while (slots.size() < OfflineData.INVENTORY) slots.add(ItemStack.EMPTY); }
        public boolean empty() { return slots.stream().allMatch(ItemStack::isEmpty); }
    }

    public static Path file(MinecraftServer server, UUID id) {
        return server.getWorldPath(LevelResource.ROOT).resolve("simpletweaks").resolve("deaths").resolve(id + ".dat");
    }
    public static boolean active() { return Admin.on() && Admin.server() != null && Admin.config().deathBackups().enabled(); }

    /** Before the player drops anything (mixin at the start of {@code ServerPlayer.die}). */
    public static void died(ServerPlayer player, DamageSource source) {
        if (!active()) return;
        var server = player.level().getServer();
        String cause = player.getCombatTracker().getDeathMessage().getString();
        if (cause.length() > 256) cause = cause.substring(0, 256);
        add(server, player.getUUID(), snapshot(player, cause));
    }
    public static Backup snapshot(ServerPlayer p, String cause) {
        var slots = new ArrayList<ItemStack>();
        for (int i = 0; i < OfflineData.INVENTORY; i++) slots.add(p.getInventory().getItem(i).copy());
        var pos = p.blockPosition();
        return new Backup(System.currentTimeMillis(), cause, p.level().dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ(),
                p.experienceLevel, p.experienceProgress, p.totalExperience, slots);
    }
    public static void add(MinecraftServer server, UUID id, Backup backup) {
        var list = new ArrayList<>(list(server, id));
        list.addFirst(backup);
        int keep = Admin.config().deathBackups().keep();
        while (list.size() > keep) list.removeLast();
        write(server, id, list);
    }

    public static List<Backup> list(MinecraftServer server, UUID id) {
        Path file = file(server, id);
        if (!Files.isRegularFile(file)) return List.of();
        try {
            var root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            var out = new ArrayList<Backup>();
            for (var e : root.getListOrEmpty("deaths")) if (e instanceof CompoundTag c) {
                var slots = new ArrayList<ItemStack>(Collections.nCopies(OfflineData.INVENTORY, ItemStack.EMPTY));
                for (var s : c.getListOrEmpty("items")) if (s instanceof CompoundTag st) {
                    int slot = st.getByteOr("Slot", (byte) -1) & 255;
                    if (slot < OfflineData.INVENTORY) slots.set(slot, AdminCodecs.load(server.registryAccess(), st));
                }
                out.add(new Backup(c.getLongOr("time", 0), c.getStringOr("cause", ""), c.getStringOr("dimension", ""), c.getIntOr("x", 0), c.getIntOr("y", 0),
                        c.getIntOr("z", 0), c.getIntOr("xpLevel", 0), c.getFloatOr("xpProgress", 0), c.getIntOr("xpTotal", 0), slots));
            }
            return out;
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("simpletweaks-admin").warn("Could not read death backups {}", file, e);
            return List.of();
        }
    }
    public static void write(MinecraftServer server, UUID id, List<Backup> list) {
        var root = new CompoundTag();
        var deaths = new ListTag();
        for (var b : list) {
            var c = new CompoundTag();
            c.putLong("time", b.time()); c.putString("cause", b.cause()); c.putString("dimension", b.dimension());
            c.putInt("x", b.x()); c.putInt("y", b.y()); c.putInt("z", b.z());
            c.putInt("xpLevel", b.xpLevel()); c.putFloat("xpProgress", b.xpProgress()); c.putInt("xpTotal", b.xpTotal());
            var items = new ListTag();
            for (int i = 0; i < b.slots().size(); i++) if (!b.slots().get(i).isEmpty()) {
                var t = (CompoundTag) AdminCodecs.save(server.registryAccess(), b.slots().get(i));
                t.putByte("Slot", (byte) i);
                items.add(t);
            }
            c.put("items", items);
            deaths.add(c);
        }
        root.put("deaths", deaths);
        try {
            Path file = file(server, id);
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            NbtIo.writeCompressed(NbtUtils.addCurrentDataVersion(root), tmp);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("simpletweaks-admin").warn("Could not write death backups of {}", id, e);
        }
    }
    /** Replaces one backup's slots (single items taken out in the screen). */
    public static void update(MinecraftServer server, UUID id, int index, List<ItemStack> slots) {
        var list = new ArrayList<>(list(server, id));
        if (index < 0 || index >= list.size()) return;
        var b = list.get(index);
        list.set(index, new Backup(b.time(), b.cause(), b.dimension(), b.x(), b.y(), b.z(), b.xpLevel(), b.xpProgress(), b.xpTotal(), slots));
        write(server, id, list);
    }

    /**
     * Restores backup {@code index} into the player's inventory (online or offline). The current inventory is saved as a
     * new backup first ("before restore"), so a restore never destroys items. Returns false if nothing was restored.
     */
    public static boolean restore(MinecraftServer server, UUID id, int index) {
        var list = list(server, id);
        if (index < 0 || index >= list.size()) return false;
        var b = list.get(index);
        String before = net.minecraft.network.chat.Component.translatable("simpletweaks.admin.deaths.before_restore").getString();
        var online = server.getPlayerList().getPlayer(id);
        if (online != null) {
            var current = snapshot(online, before);
            var inv = online.getInventory();
            for (int i = 0; i < OfflineData.INVENTORY; i++) inv.setItem(i, b.slots().get(i).copy());
            online.experienceProgress = b.xpProgress(); online.totalExperience = b.xpTotal();
            // two calls: the second always differs, so the XP bar is sent again
            online.setExperienceLevels(b.xpLevel() + 1); online.setExperienceLevels(b.xpLevel());
            online.inventoryMenu.broadcastChanges();
            add(server, id, current);
            return true;
        }
        var data = OfflineData.load(server, id);
        if (data == null) return false;
        int[] xp = data.xp();
        var current = new Backup(System.currentTimeMillis(), before, b.dimension(), b.x(), b.y(), b.z(), xp[0], Float.intBitsToFloat(xp[1]), xp[2], data.inventory());
        data.setInventory(b.slots());
        data.setXp(b.xpLevel(), b.xpProgress(), b.xpTotal());
        if (!data.save()) return false;
        add(server, id, current);
        return true;
    }
    private DeathBackups() {}
}
