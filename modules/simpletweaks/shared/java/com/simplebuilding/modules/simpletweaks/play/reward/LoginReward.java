package com.simplebuilding.modules.simpletweaks.play.reward;

import com.simplebuilding.modules.simpletweaks.play.*;
import java.time.*;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * T20 (off by default): the first join of a calendar day (server time) gives the reward items; the streak grows by one
 * per day in a row (a gap starts again at 1). With Simple Money loaded, {@code billsPerDay} money bills per streak day
 * (at most {@code maxBills}); every {@code bonusEvery} days the bonus items too.
 */
public final class LoginReward {
    public static final Identifier BILL = Identifier.fromNamespaceAndPath("simplemoney", "money_bill");
    static Clock clock = Clock.systemDefaultZone();

    /** Tests: a fixed clock. */
    public static void useClock(Clock value) { clock = value; }
    public static boolean active() { return Play.on() && Play.server() != null && Play.config().loginReward().enabled(); }

    public static void joined(ServerPlayer p) { if (active()) claim(p); }

    /** Streak day of this login, or 0 when today was already rewarded. */
    public static int claim(ServerPlayer p) {
        var c = Play.config().loginReward();
        var s = Play.state();
        var today = LocalDate.now(clock);
        var streak = s.streaks.computeIfAbsent(p.getUUID().toString(), k -> new PlayState.Streak());
        if (today.toString().equals(streak.lastDay)) return 0;
        streak.days = today.minusDays(1).toString().equals(streak.lastDay) ? streak.days + 1 : 1;
        streak.lastDay = today.toString();
        s.save();
        var stacks = new ArrayList<ItemStack>();
        for (var line : c.items()) stacks.add(LoginRewardStacks.stack(line));
        if (c.bonusEvery() > 0 && streak.days % c.bonusEvery() == 0) for (var line : c.bonusItems()) stacks.add(LoginRewardStacks.stack(line));
        int bills = Math.min(c.maxBills(), c.billsPerDay() * streak.days);
        if (bills > 0 && BuiltInRegistries.ITEM.containsKey(BILL)) stacks.add(new ItemStack(BuiltInRegistries.ITEM.getValue(BILL), bills));
        for (var st : stacks) PlayItems.give(p, st);
        p.sendSystemMessage(PlayText.t("reward.claimed", streak.days).withStyle(ChatFormatting.GOLD));
        return streak.days;
    }
    private LoginReward() {}
}
