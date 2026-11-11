# PLAN-B-MIXLOCAL27 (11.11.2026) – Mixin-Prüfer: @Local-Captures (prio27 #10, MIXCHECK31-3 Rest)

Branch `b-mixlocal27`, Basis `hub/b-bundle179-gen` 089fdc551. Nur Python (`tools/check_client_mixins.py`).

## Ist-Stand
- Prüfer liest Klassendateien selbst (eigener Parser), prüft Ziele, @Shadow/@Accessor/@Invoker, Injector-Selektoren, @At, Signaturen; `@Local` stand ausdrücklich unter „nicht geprüft“.
- 26.3-Jar (merged-deobf, unverschleiert) hat eine LocalVariableTable → statische Prüfung möglich.
- **Nebenbefund:** Die MixinExtras-Konstante hieß `Lcom/llamalab/...` statt `Lcom/llamalad7/...`. Damit wurden @WrapOperation, @ModifyExpressionValue, @ModifyReturnValue, @WrapWithCondition, @ModifyReceiver und @WrapMethod seit A-MIXCHECK31 **nie** geprüft (Annotation nie erkannt). Korrigiert.

## Umsetzung
- Parser: je Methode Parameter-Annotationen (Runtime(In)VisibleParameterAnnotations), `Signature`, LocalVariableTable (Unterattribut von Code) und Code-Offsets je Instruktion/Return (`Member.params/lvt/pcs/sig`, Konstruktor rückwärtskompatibel). Dabei Sprung über die Exception-Tabelle korrekt.
- `check_locals` (in `check_injector`): `argsOnly` → Parameter der Zielmethode; sonst Locals im Gültigkeitsbereich am Injektionspunkt (@At INVOKE/INVOKE_ASSIGN/FIELD/NEW inkl. `ordinal`, HEAD, RETURN, TAIL; andere @At-Arten: ganze Methode ohne Eindeutigkeitsregel). `name`/`index` müssen mit Typ existieren, `ordinal` braucht ordinal+1 Locals des Typs, implizit genau einen (MixinExtras-Regel). `this` zählt nicht. Local*Ref → Primitiv, `LocalRef<T>` → T aus der generischen Signatur (sonst beliebiges Objekt). Methode ohne LVT: kein Urteil (gezählt und ausgegeben). Mehrere Selektoren = Versions-Alternativen: eine passende reicht.
- Ausgabezeile `N @Local captures seen, M without a verdict`.
- Selbsttest `tools/test_check_client_mixins.py` Klasse `LocalCapture` (8 Fälle: eindeutig/mehrdeutig im Scope, fehlender Typ, ordinal/name/index, this, argsOnly, Ref-Typen inkl. LocalRef<T>, HEAD/unmodelliertes @At, ohne LVT).

## Tests
- Lokal: Unit-Tests 17/17 OK. Lokal kompilierte 22 Modul-Mixins mit @Local (javac gegen 26.3-Jar) → 30 Captures, 0 Befunde.
- sbt: `:integration:checkClientMixins` (alle 26.3-Fabric-Module, jetzt inkl. MixinExtras-Injektoren) + Unit-Tests.

## Befunde des Vollaufs
(siehe unten, nach sbt)
