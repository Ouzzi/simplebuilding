package com.simplebuilding.modules.simplemovement;
import net.minecraft.world.entity.player.Player;
import java.util.*;
public final class CrawlLimiter {
 private static final Map<Player,Integer> LAST=new WeakHashMap<>();
 public static boolean allow(Player p){int now=p.tickCount;Integer last=LAST.get(p);if(!SimpleMovement.active()||!SimpleMovement.config().enableManualCrawl||p.isSpectator()||p.isPassenger()||!p.isAlive()||(last!=null&&now-last<10))return false;LAST.put(p,now);return true;}
}
