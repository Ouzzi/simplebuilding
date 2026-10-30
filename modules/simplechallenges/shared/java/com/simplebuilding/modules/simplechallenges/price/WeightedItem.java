package com.simplebuilding.modules.simplechallenges.price;

/** An item a player can get by hand (no tool tier) with how common it is around a cell and its stack size. */
public record WeightedItem(String id, int weight, int maxStack) {}
