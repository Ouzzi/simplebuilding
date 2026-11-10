package com.simplebuilding.modules.simplelayers.backrooms.api;

/** Where a level writes one chunk of itself (BACK22 core §4.2): x/z 0..15 inside the chunk, ly 0..95 band-local. */
public interface BandWriter {
    void set(int x, int ly, int z, BandPiece piece);
}
