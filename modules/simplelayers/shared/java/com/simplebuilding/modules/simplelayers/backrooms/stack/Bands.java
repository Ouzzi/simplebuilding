package com.simplebuilding.modules.simplelayers.backrooms.stack;

import com.simplebuilding.modules.simplelayers.backrooms.api.Band;
import com.simplebuilding.modules.simplelayers.backrooms.level.archive.ArchiveBand;
import com.simplebuilding.modules.simplelayers.backrooms.level.electro.ElectroBand;
import com.simplebuilding.modules.simplelayers.backrooms.level.hotel.HotelBand;
import com.simplebuilding.modules.simplelayers.backrooms.level.lightless.LightlessBand;
import com.simplebuilding.modules.simplelayers.backrooms.level.lobby.LobbyBand;
import com.simplebuilding.modules.simplelayers.backrooms.level.pipes.PipesBand;
import com.simplebuilding.modules.simplelayers.backrooms.level.pools.PoolsBand;
import com.simplebuilding.modules.simplelayers.backrooms.level.suburb.SuburbBand;
import com.simplebuilding.modules.simplelayers.backrooms.level.warehouse.WarehouseBand;
import com.simplebuilding.modules.simplelayers.backrooms.level.wheatfield.WheatfieldBand;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The ten levels in stack order (BACK22, owner g1 a = R9): the only place that knows positions. All ten are always listed (the
 * level packages start as stubs), so a level branch never edits this file.
 */
public final class Bands {
    public static final List<Band> ORDER = List.of(new LobbyBand(), new PoolsBand(), new ArchiveBand(), new WarehouseBand(), new PipesBand(),
            new ElectroBand(), new HotelBand(), new LightlessBand(), new SuburbBand(), new WheatfieldBand());

    private Bands() {}

    /** Level {@code index} (1..10). */
    public static Band get(int index) {
        return ORDER.get(index - 1);
    }

    public static int indexOf(Band band) {
        return ORDER.indexOf(band) + 1;
    }

    public static @Nullable Band byId(String id) {
        for (Band b : ORDER) if (b.id().equals(id)) return b;
        return null;
    }

    /** The level whose open part holds world {@code y}, or null inside bedrock. */
    public static @Nullable Band at(int y) {
        int i = BandStack.bandAt(y);
        return i == 0 ? null : get(i);
    }
}
