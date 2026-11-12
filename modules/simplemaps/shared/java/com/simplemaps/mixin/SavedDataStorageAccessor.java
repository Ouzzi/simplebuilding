package com.simplemaps.mixin;

import java.util.Map;
import java.util.Optional;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * The web export reads wayfinder maps without filling Vanilla's saved-data cache (AUDSB31E1 F9): {@code get}/{@code computeIfAbsent}
 * keep every map they load until the server stops, so a full export would leave every map ever made in memory. The cache
 * is only looked at (a map the game has open is used as it is, unsaved changes included); a map that is not in it is read
 * with Vanilla's own {@code readSavedData} (file, data fixers, codec, error log) and not put into it.
 */
@Mixin(SavedDataStorage.class)
public interface SavedDataStorageAccessor {
    @Accessor("cache")
    Map<SavedDataType<?>, Optional<SavedData>> simplemaps$cache();

    @Invoker("readSavedData")
    <T extends SavedData> T simplemaps$read(SavedDataType<T> type);
}
