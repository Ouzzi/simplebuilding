package com.simplebuilding.modules.simplelayers.backrooms.door;

import com.simplebuilding.modules.simplelayers.backrooms.api.DoorEnd;
import com.simplebuilding.modules.simplelayers.backrooms.gen.DoorPlacement;
import com.simplebuilding.modules.simplelayers.backrooms.stack.Bands;
import com.simplebuilding.modules.simplelayers.config.LayersConfig;
import com.simplebuilding.modules.simplelayers.world.gen.GenHash;
import org.jspecify.annotations.Nullable;

/**
 * The way in (BACK22 core §5.7, K6): one Overworld chunk in {@code backrooms.shaftRarity} rolls the abandoned house (pure, so the
 * Lobby knows it without the Overworld); the Lobby puts its entrance double door (symbol 0 = house) into the same chunk column. The
 * house links its cellar double door when it is built ({@code PortalLinks}, the Overworld height is only known then); a Lobby door
 * whose house never got built (steep or wet ground) leads to the surface of that chunk.
 */
public final class Entrances {
    public static final int SALT = 2500;

    private Entrances() {}

    public static boolean houseChunk(long seed, int cx, int cz) {
        int rarity = LayersConfig.shaftRarity;
        return rarity > 0 && GenHash.unit(seed, cx, cz, SALT) < 1.0 / rarity;
    }

    /** The Lobby's entrance double door in house chunk (cx, cz) (the Lobby's up-door slot, it has no level above). */
    public static @Nullable DoorEnd lobbyEnd(long seed, int cx, int cz) {
        if (!houseChunk(seed, cx, cz)) return null;
        DoorEnd e = DoorPlacement.find(Bands.get(1).layout(), seed, cx, cz, 10, 10, 2);
        DoorEnd down = DoorNet.down(seed, 1, cx, cz);
        return e != null && down != null && DoorPlacement.overlaps(down, e) ? null : e; // R3: never into the Lobby's door down
    }

    public static long link(int cx, int cz) {
        return 0x5B22_E000_0000_0000L ^ ((long) cx & 0x3FFFFFFL) << 26 ^ ((long) cz & 0x3FFFFFFL);
    }
}
