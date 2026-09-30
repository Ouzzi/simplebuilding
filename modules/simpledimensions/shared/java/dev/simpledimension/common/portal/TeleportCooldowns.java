package dev.simpledimension.common.portal;
import java.util.*;
/** Instance-owned server tick clock; discard it on server stop and forget on logout. */
public final class TeleportCooldowns {
 private final Map<UUID,Long> last=new HashMap<>();
 public boolean tryConsume(UUID player,long tick,long delay){if(isOnCooldown(player,tick,delay))return false;last.put(player,tick);return true;}
 public boolean isOnCooldown(UUID player,long tick,long delay){var v=last.get(player);return v!=null&&tick-v<delay;}
 public void forget(UUID player){last.remove(player);}
}
