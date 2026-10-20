package com.simpleshops.block;

import com.simpleshops.logic.EnderShops;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The link of a shop block entity to its entry in {@link EnderShops} (server side only). A block entity without a valid entry
 * (new block, old block from before the rework, copied block data) gets one on first use; an old block's own items move into
 * it through {@code migrate} (loss-free: until then the block entity keeps and saves them itself).
 */
final class VaultLink {
    static final String KEY = "ender_id";
    private String id = "";

    String id() {
        return this.id;
    }

    boolean linked() {
        return !this.id.isEmpty();
    }

    void read(ValueInput input) {
        this.id = input.getStringOr(KEY, "");
    }

    void write(ValueOutput output) {
        if (!this.id.isEmpty()) output.putString(KEY, this.id);
    }

    /** The entry if it exists and belongs to this block; never creates one. */
    EnderShops.@Nullable Vault peek(BlockEntity be, EnderShops.Kind kind) {
        if (!(be.getLevel() instanceof ServerLevel level) || this.id.isEmpty()) return null;
        EnderShops.Vault v = EnderShops.get(level.getServer()).find(this.id);
        return v != null && v.kind == kind && v.at(level, be.getBlockPos()) ? v : null;
    }

    /** The entry of this block, created (and filled by {@code migrate}) when missing; {@code null} on the client or for a removed block entity. */
    EnderShops.@Nullable Vault resolve(BlockEntity be, EnderShops.Kind kind, Consumer<EnderShops.Vault> migrate) {
        if (!(be.getLevel() instanceof ServerLevel level) || be.isRemoved()) return null;
        EnderShops data = EnderShops.get(level.getServer());
        EnderShops.Vault v = peek(be, kind);
        if (v == null) {
            v = data.create(kind, level, be.getBlockPos());
            if (v == null) return null;
            this.id = v.id;
            migrate.accept(v);
            be.setChanged();
        }
        // The lists are changed in place by menus and trades: the world data is always saved with the next autosave.
        data.setDirty();
        return v;
    }

    /** Forgets the entry (after the block dropped its items) and deletes it. */
    void drop(BlockEntity be, EnderShops.Kind kind) {
        EnderShops.Vault v = peek(be, kind);
        if (v != null && be.getLevel() instanceof ServerLevel level) EnderShops.get(level.getServer()).delete(v);
        this.id = "";
    }

    /** A fresh entry (placement): the old one, if any, is deleted with its contents (placement starts empty). */
    void reset(BlockEntity be, EnderShops.Kind kind) {
        drop(be, kind);
    }
}
