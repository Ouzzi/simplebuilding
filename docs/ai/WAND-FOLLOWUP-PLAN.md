# Wand multipart follow-up

- Trace direct wand preview/build, octant fill, blueprint placement and WorldPermissions.
- Preserve a fresh bed foot until its placement callback creates the head; validate the final copied orientation and secondary footprint before mutation.
- Keep doors and other multipart blocks safe, including blocked/head cells and permission checks.
- Add actual wand-placement regressions plus obstruction/orientation negative controls; inspect blueprint/octant behavior without broad rewrites.
- Run focused Fabric/NeoForge 26.3 server tests with bounded Gradle memory/workers. Parent performs shared compilation and final gate.
- Commit scoped changes and report evidence; do not push or edit shared handover/queue.

## Implementation decisions

Direct wand preview and placement share `stateFor`: bed, door and double-plant parts remain intact until `setPlacedBy`; the second cell is checked for bounds, loaded chunk, replacement, entity collision and build permissions before mutation. Bed placement validates the copied/item-component facing before vanilla computes its footprint. Air results never count as placed material.

`ShapeFill` layouts represent independent cells and never invoke item placement callbacks. They now refuse beds, doors and double plants without charging material or durability. Captured blueprints already describe both parts and keep their existing placement semantics.

The shared WorldPermissions bed footprint uses the opposite direction for HEAD. A head must check its foot, not an unrelated third cell.

## Verification

- `python tools/testrunner/run.py --targets fabric-263,neoforge-263 --filter 'simplebuilding:wand_mode_game_test_*'`: **alles gruen: 54/54 bestanden, 0 rot**, record `2026-10-01T03-21-49Z-a73f`. Both loaders ran all 27 tests, including three new multipart cases, existing stairs/log orientation, octant fills/roofs, undo and build-permission controls.
- New cases drive actual wand inventory ticks for beds, doors and sunflowers, check one-item charging, blocked/protected second cells, copied bed orientation, entity collision, and rejected octant fills without item/durability spending.
- Initial test compile used removed RED_BED constants; fixture now resolves the shared registry ID. Initial Fabric selection needed explicit adapter methods; added and verified in the complete family run.
- `git diff --check`: clean. Parent owns final shared compilation/integration gate and the active-Claims HEAD footprint regression after both branches merge.
