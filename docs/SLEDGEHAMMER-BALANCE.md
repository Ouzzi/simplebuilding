# Vorschlaghammer: Abbauzeit (Besitzer 2026-09-30, Hauptlinie 26.3)

`x` ist die ungerundete Abbauzeit der Spitzhacke gleichen Materials mit derselben Effizienz.
Ein Schlag mit `n` wirklich abgebauten Bloecken braucht:

`x * (FIRST_BLOCK_TIME_FACTOR + min(n - 1, 8) * EARLY_BLOCK_TIME_FACTOR + max(n - 9, 0) * LATE_BLOCK_TIME_FACTOR)`

| Bloecke | Gesamte Spitzhackenzeit |
|---|---|
| 1 | 1,5x |
| 2 | 2,3x |
| 9 | 7,9x |
| 10 | 8,6x |
| 18 | 14,2x |

Konstanten in `SledgehammerUtils`: erster Block 1,5; Bloecke 2-9 je 0,8; ab Block 10 je 0,7;
`EARLY_BLOCK_COUNT_LIMIT` = 9. Die Balancing-Zentrale liest diese benannten Konstanten aus dem Code
und kann ihre Literale bearbeiten; `balance/` ist ein lokaler, nicht eingecheckter Speicher fuer Entwuerfe.
Die Zahlenformel hat Vorrang vor der ungefaehren Angabe "0,75-mal so schnell": 1,5x Zeit ergibt 2/3 Tempo.
Client und Server wenden denselben Teiler in `BlockStateBaseMixin` an. Vanilla rundet den gesamten
Abbau auf Ticks; weder jeden Block noch jeden Summanden einzeln runden. Effizienz, Eile und
Umgebung wirken wie bei der gleichstufigen Spitzhacke. Es gibt keinen Wechsel zur niedrigeren Stufe.

Die Oktant-Auswahl bekommt denselben Mengenrabatt wie der Flaechenabbau (Besitzer N61 E8, 15.10.):
`OCTANT_PICKAXE_TIME_FACTOR` (0,5, der bisherige Oktant-Faktor je Block) mal die Flaechenformel oben
mit `n` = Anzahl der Oktant-Bloecke. Statt fester 0,5x je Block also:

| Oktant-Bloecke | Gesamte Spitzhackenzeit |
|---|---|
| 1 | 0,75x |
| 2 | 1,15x |
| 9 | 3,95x |
| 10 | 4,3x |
| 18 | 7,1x |

(vorher linear 0,5x je Block: 18 Bloecke = 9x). Ein einzelner Oktant-Block ist damit minimal langsamer als die Spitzhacke
(0,75x), grosse Auswahlen sparen mehr als zuvor.
Oktant mit beiden Ecken in der Nebenhand, Ursprung in der Figur, hoechstens 32 Bloecke je Kante
und 4096 Plaetze in der Box. Override, Werkzeug-, Spawnschutz- und Claimpruefungen gelten je Block.
Schleichen waehlt genau einen Block, ohne Oktant-Modus: 1,5x Zeit.

Haltbarkeit bleibt unveraendert: 2 je Block, bei einem mitgenommenen Block mit falschem Werkzeug 3;
Basiswerte x4 (Diamant 6244, Netherit 8124, Enderit 10000). Kein neuer GUI-Text fuer den Hammer.

26.2 behaelt bis zum gesonderten Port-Run seine alte Formel (1,2 fuer einen Block, sonst n mal die
Zeit der Spitzhacke eine Stufe darunter, beim Oktant nochmals x2 via `OCTANT_TIME_FACTOR`, unveraendert). Keine 1.21.11-/26.4-Portierung.

Verifikation: `SledgehammerTests` prueft n=1,2,9,10,18 am echten Block-Abbaufortschritt (Flaeche; Oktant-Faktor 0,5 x Flaechenfaktor fuer dieselben n),
Schleichen und den 18-Block-Oktant (7,1x); `checkBalance` prueft die Balance-Quellen.
