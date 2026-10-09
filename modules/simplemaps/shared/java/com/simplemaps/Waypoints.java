package com.simplemaps;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** The up to eight waypoints of one map stack, at most one per slot, sorted by slot. */
public record Waypoints(List<Waypoint> list) {
    public static final Waypoints EMPTY = new Waypoints(List.of());
    public static final Codec<Waypoints> CODEC = Waypoint.CODEC.listOf(0, Waypoint.SLOTS).xmap(Waypoints::new, Waypoints::list);
    public static final StreamCodec<ByteBuf, Waypoints> STREAM_CODEC =
            Waypoint.STREAM_CODEC.apply(ByteBufCodecs.list(Waypoint.SLOTS)).map(Waypoints::new, Waypoints::list);

    public Waypoints {
        List<Waypoint> clean = new ArrayList<>();
        boolean[] used = new boolean[Waypoint.SLOTS];
        for (Waypoint w : list == null ? List.<Waypoint>of() : list) {
            if (w != null && !used[w.slot()]) {
                used[w.slot()] = true;
                clean.add(w);
            }
        }
        clean.sort(Comparator.comparingInt(Waypoint::slot));
        list = List.copyOf(clean);
    }

    public Optional<Waypoint> get(int slot) {
        return list.stream().filter(w -> w.slot() == slot).findFirst();
    }

    public Waypoints with(Waypoint waypoint) {
        List<Waypoint> out = new ArrayList<>(list);
        out.removeIf(w -> w.slot() == waypoint.slot());
        out.add(waypoint);
        return new Waypoints(out);
    }

    public Waypoints without(int slot) {
        List<Waypoint> out = new ArrayList<>(list);
        out.removeIf(w -> w.slot() == slot);
        return new Waypoints(out);
    }

    /** Owner F4: the first map's waypoints always stay; free slots take the other map's waypoints in order. */
    public Waypoints mergedWith(Waypoints other) {
        Waypoints out = this;
        int next = 0;
        for (Waypoint w : other.list) {
            while (next < Waypoint.SLOTS && out.get(next).isPresent()) next++;
            if (next >= Waypoint.SLOTS) break;
            out = out.with(w.withSlot(next));
        }
        return out;
    }
}
