package com.simplemoney;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.*;

/** One persistent budget per player across all merchants, items and dimensions. */
public final class LinkBudget extends SavedData {
    public static final int DAY_TICKS = 24000, MAX_PLAYERS = 100000;
    public record Entry(UUID player, long window, long last, int count) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("player").forGetter(Entry::player),
                Codec.LONG.fieldOf("window").forGetter(Entry::window),
                Codec.LONG.fieldOf("last").forGetter(Entry::last),
                Codec.intRange(0, MoneyLinks.MAX_DAILY).fieldOf("count").forGetter(Entry::count)
        ).apply(i, Entry::new));
    }
    public static final Codec<LinkBudget> CODEC = RecordCodecBuilder.create(i -> i.group(
            Entry.CODEC.listOf(0, MAX_PLAYERS).fieldOf("players").forGetter(LinkBudget::entries)
    ).apply(i, LinkBudget::new));
    public static final SavedDataType<LinkBudget> TYPE = new SavedDataType<>(Identifier.parse("simplemoney:link_budget"),
            LinkBudget::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
    private final Map<UUID, Entry> players = new LinkedHashMap<>();
    public LinkBudget() {}
    private LinkBudget(List<Entry> entries) { entries.forEach(e -> players.put(e.player(), e)); }
    public List<Entry> entries() { return new ArrayList<>(players.values()); }
    public static LinkBudget get(ServerPlayer player) {
        return player.level().getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }
    public boolean allowed(UUID player, long now) {
        SimpleMoney.config.links.normalize();
        Entry e = players.get(player);
        if (now < 0) return false;
        if (e == null) return players.size() < MAX_PLAYERS;
        if (now < e.last() || now - e.last() < SimpleMoney.config.links.cooldownTicks) return false;
        return now >= e.window() && (now - e.window() >= DAY_TICKS || e.count() < SimpleMoney.config.links.dailyLimit);
    }
    public boolean spend(UUID player, long now) {
        if (!allowed(player, now)) return false;
        Entry e = players.get(player);
        boolean reset = e == null || now - e.window() >= DAY_TICKS;
        players.put(player, new Entry(player, reset ? now : e.window(), now, reset ? 1 : e.count() + 1));
        setDirty();
        return true;
    }
}
