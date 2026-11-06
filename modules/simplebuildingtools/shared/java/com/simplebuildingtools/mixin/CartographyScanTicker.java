package com.simplebuildingtools.mixin;

/** Added to the cartography table menu: continues a large blueprint scan once per server tick. */
public interface CartographyScanTicker {
    void simplebuildingtools$tickScan();
}
