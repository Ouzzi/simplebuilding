package com.simplevisuals.effects;
import java.util.*;
/** Shared pure policy; no packets, gameplay changes, or client classes. */
public final class ParticleBudget {
 public static final int MAX_PARTICLES_PER_TICK=24, MAX_PARTICLES_PER_PLAYER=4, MAX_TRACKED_PLAYERS=8;
 private final Map<Integer,Integer> players=new HashMap<>(); private int total,limit;
 public void begin(int setting){players.clear();total=0;limit=setting==2?0:setting==1?8:MAX_PARTICLES_PER_TICK;}
 public int claim(int player,int requested){
  if(!players.containsKey(player)&&players.size()>=MAX_TRACKED_PLAYERS)return 0;
  int count=Math.max(0,Math.min(requested,Math.min(limit-total,MAX_PARTICLES_PER_PLAYER-players.getOrDefault(player,0))));
  if(count>0){players.merge(player,count,Integer::sum);total+=count;}return count;
 }
 public int used(){return total;}
}
