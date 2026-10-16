# Zwei Claude-Sitzungen parallel (Besitzer 16.10.)

Zwei Accounts (Besitzer + Mitarbeiter) arbeiten gleichzeitig auf demselben Repo und derselben Queue.
Beide laufen auf sb-agent: Sitzung A mit `claude` (Konfig `/root/.claude`), Sitzung B mit `claude-b`
(Konfig `/root/.claude-b`, eigener Login, Memory/Regeln/Skills/Hooks per Verknüpfung geteilt).

## Regeln
1. **Auftrag beanspruchen, bevor man anfängt:** In `.claude/QUEUE.md` den Punkt von `- [ ]` auf
   `- [~] (A|B, <branch>, <Datum>)` setzen, sofort committen und nach `claude-wave1` pushen
   (`git push /root/sb.git HEAD:claude-wave1`). Schlägt der Push fehl (die andere Sitzung war schneller):
   fetch, prüfen, ob der Punkt schon beansprucht ist, sonst erneut. Beanspruchte Punkte der anderen Sitzung
   nicht anfassen.
2. **Eigene Branches mit Präfix:** Sitzung A `claude-…`/`a-…`, Sitzung B `b-…`; Worktrees unter `/root/wt/<branch>`.
3. **Mergen nach `claude-wave1`** nur per fetch + merge + push (nie force). Konflikte löst, wer mergt.
4. **Gates:** `/root/sb-gate.sh` stellt sich ohnehin hinter die Sperre an. Vor dem Start prüfen, ob schon ein
   Gate auf einem neueren Stand läuft (`/root/t ls -t /root/gate | head`); dann keins zusätzlich starten.
5. **Memory:** gemeinsam. Jede Sitzung pflegt ihre eigene Übergabe-Datei (`uebergabe-A.md` bzw.
   `uebergabe-B.md`) und trägt Neues in `MEMORY.md` nur additiv ein (eine Zeile, nichts Fremdes löschen).
6. **Wünsche des Besitzers oder Mitarbeiters** kommen als neuer Nachtrag in die Queue (mit Name/Kürzel), egal in
   welcher Sitzung sie eingegeben wurden.
7. **Ressourcen:** sb-test hat 2 Testplätze für beide Sitzungen zusammen. Höchstens je ~3 Unteragenten pro Sitzung.
   sb-agent hat 3 GB RAM – bei zwei Sitzungen mit Unteragenten auf mindestens 6 GB erhöhen.
8. **Keine Wiki-/Textur-Neuerzeugung auf sb-agent committen** (ohne Client-Jar entstehen Platzhalter) – immer auf sb-test.
