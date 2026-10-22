# Plan b-models21: Simple Models UX (SM21 a–f, Besitzer 21.10.)

Grundlage: `docs/ai/AUDIT-SIMPLEMODELS-2026-10-10.md`, Abschnitt „Bewusst nicht geaendert“. Queue: `.claude/QUEUE.md` (b-models21).
Modul: `modules/simplemodels` (26.3 Fabric/NeoForge/Forge; andere Linien hat das Modul nicht).

## Ist-Zustand
- `client/ModelBrowser` (Screen): Reiter Modelle/Servereinstellungen/Importhilfe, Suche, Zeilen-Buttons mit Text-Tooltip
  (`base | id | tags`), Seiten nur per Zurück/Weiter. Servereinstellungen per Reflexion als `Label: true`.
- `client/ModelsTab` + `mixin/ModelScreenMixin`: Lesezeichen links an Inventar/Amboss (`leftPos-20`, `topPos+6`);
  im Kreativ-Inventar keiner. Bei offenem Rezeptbuch (breit) liegt die rechte Buchkante 1–2 px links von `leftPos`,
  das Lesezeichen deckt also 20 px des Buchs (Suchfeld/Filterknopf) ab.
- SimpleLib ist eingebettet (jar-in-jar, Prinzip 6a) – das Modul bleibt allein spielbar; Zeichenbausteine
  `UiBookmarks`, `UiBoxes`, `UiPalette` dürfen genutzt werden.

## Umsetzung
- **a Schalter:** Bool-Felder von `ModelPolicy` als nur-lesender Schalter (Spur 20×10, Knopf rechts+grün = an,
  links+dunkel = aus), Zahlen als rechtsbündiger Wert; Zeile „Nur Lesen – Admins bearbeiten config/simplemodels/config.json“.
- **b Mausrad:** `ModelBrowser.mouseScrolled` blättert im Modelle-Reiter (hoch = vorige Seite), an den Enden geklemmt.
- **c Filter/Sortierung:** neue Klasse `ModelQuery` (ohne Client-Importe, testbar im Server-Gametest):
  `view(models, query, baseItem, sort)`, `baseItems(models)`, `pages`, `scroll`. Sortierung Name A–Z / Name Z–A /
  Basis-Item; Filter „Alle Items“ oder ein Basis-Item des Katalogs. Zwei `CycleButton` rechts neben der Suche
  (Suche wird schmaler). `filtered()` liefert die gefilterte + sortierte Sicht.
- **d Tooltip-Vorschau:** neue `client/ModelPreviewTooltip implements ClientTooltipComponent` (Basis-Item → Modell 2×
  vergrößert) plus Textzeilen (Name, Basis-Item, ID, Tags, Autor). Gesetzt über einen `@Invoker` auf das private
  `GuiGraphicsExtractor.setTooltipForNextFrameInternal(...)` – keine Loader-Factory nötig (Vanilla-Tooltip-Rahmen und
  -Positionierung bleiben). Zeilen-Buttons verlieren den alten Text-Tooltip.
- **e Kreativ-Inventar:** `ModelScreenMixin` akzeptiert auch `CreativeModeInventoryScreen` (Lesezeichen links,
  dort frei; Browser ohne Amboss = nur ansehen, wie im Inventar).
- **f Rezeptbuch:** Accessor auf `AbstractRecipeBookScreen.recipeBookComponent`; bei sichtbarem Buch steht das
  Lesezeichen oben am Inventarrand (`leftPos+6`, ragt 20 px nach oben, abgerundetes Ende oben, gleicher Stil),
  sonst wie bisher links. Schmales Fenster (Buch überdeckt das Inventar): Vanilla zeichnet dann keine Widgets.
- Lang EN/DE für neue Texte (Sortierung, Filter, Nur-Lesen-Hinweis).

## Risiken
- Invoker auf private Methode: Name/Signatur in Vanilla und NeoForge 26.3 gleich (geprüft in /root/vsrc263,
  /root/mcsrc263full); Forge-Compile prüft es mit.
- Smoke-Test (Client) darf heute nicht laufen (Regel b-20c: keine client-*-Läufe) – nur Compile.

## Verifikation
1. Compile `:modules:simplemodels:{fabric,neoforge,forge}` (+ `-Pforge263=true`) und Integration-Client-Quellen.
2. Server-Gametests `simplemodels:*` fabric-263 + neoforge-263 + Standalone; neuer Test `browser_query` (Sortierung,
   Filter, Basis-Items, Seiten/Mausrad-Klemmen).
3. Smoke `ModuleClientSmoke` ergänzt (mehr Modelle, Filter/Sortierung, Mausrad, Tooltip-Komponente, Kreativ-Tab,
   Rezeptbuch-Position, Screenshots) – nur kompiliert, Lauf offen.
4. layers.py sync, module_guides.py, wiki/generate.py --all, discover.py auf sb-test → `-gen`; dann `./gradlew check -q`.
