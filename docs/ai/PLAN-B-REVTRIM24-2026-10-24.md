# Plan b-revtrim24 (REV24-F-10 Rest)
- Ist: TrimBonusCatalog.forPattern/forMaterial (Tooltip, TrimReferenceScreen) und AdvancementChecks.wearsFullBonusTrimSet erkennen per String.contains; TrimEffectUtil (Wirkung) ist seit b-revrest24 exakt (isOwn).
- Aenderung: TrimEffectUtil.isOwn public; Catalog-API auf Identifier (isOwn je Muster/Material), tooltipLines mit Materialgewicht wie getTrimCount; AdvancementChecks reicht die Identifier durch; TrimReferenceScreen baut Identifier (minecraft / simplebuilding).
- Weitere contains-Stellen per grep: ModTags enderite (Item-Tag, nicht Trim), SledgehammerEntityInteraction/PlacedTemplates (Item-Pfad trim_smithing_template) - nicht Trim-Erkennung, unveraendert.
- Test: TrimEffectTests um Catalog-Asserts (Fremd/Look-alike leer, Vanilla/own exakt).
- Verifikation: Compile 26.3 Fabric, fabric-263 trim*/advancement*, module-simpletrims (+standalone), gradlew check.
