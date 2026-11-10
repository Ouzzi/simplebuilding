package com.simplebuilding.modules.simplelayers.backrooms.api;

/**
 * What the client sees of a level besides its blocks (BACK22 core §4.1): the fog colour (RGB) used for the door veil and the fog
 * pulse after a passage. Fog distance, light, music and ambience live in the level's biome ({@code tools/gen_data.py} BANDS).
 */
public record BandView(int fogColor) {}
