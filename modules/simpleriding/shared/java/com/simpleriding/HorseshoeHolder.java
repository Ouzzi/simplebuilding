package com.simpleriding;

import net.minecraft.world.SimpleContainer;

/** Implemented on every AbstractHorse by HorseshoeHorseMixin; only horses in the entity tag expose slots. */
public interface HorseshoeHolder {
 SimpleContainer simpleriding$horseshoes();
 /** Synced value (see {@link Horseshoes#syncedValue}); valid on client and server. */
 int simpleriding$code();
 /** Server: recompute and publish the synced value. */
 void simpleriding$sync();
 double simpleriding$travel();
 void simpleriding$setTravel(double blocks);
 boolean simpleriding$hasLast();
 double simpleriding$lastX();
 double simpleriding$lastZ();
 void simpleriding$setLast(double x,double z,boolean valid);
}
