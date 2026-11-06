"""Vanilla-derived textures of the simplestorage module's barrels (WH26-6, moved from Simple Crucibles): the Enderite barrel faces.

Drawn by crucible_art_v2_2026_10_05.py from the Vanilla textures; this module only hands them to
tools/textures/vanilla_derived.py --module simplestorage (build/vanilla-textures must be extracted first).
"""
import crucible_art_v2_2026_10_05 as art


def derived(jar):
    return art.derived_storage(jar)
