package com.simplebuilding.modules.simplecontainers.client;

/** Implemented by the villager trading screen (mixin): whether the offer button at {@code buttonY} shows the chosen offer. */
public interface OfferChoice {
    boolean simplecontainers$chosen(int buttonY);
}
