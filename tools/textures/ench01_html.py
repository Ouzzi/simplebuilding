"""python3 tools/textures/ench01_html.py <png_dir> <out.html>: baut die eigenstaendige Vorschau b-ench01 (Bilder inline)."""
import base64, sys
D, OUT = sys.argv[1], sys.argv[2]
def b(n): return 'data:image/png;base64,' + base64.b64encode(open(f'{D}/{n}.png', 'rb').read()).decode()
H = {'vanilla': '8,3 / 6,2', 'dark_a': '31,5 / 22,8', 'dark_b': '9,8 / 11,8', 'end_a': '16,7 / 9,0', 'end_b': '15,2 / 6,2'}
def fig(n, cap):
    return (f'<figure><img src="{b(n+"_iso")}" width="260"><figcaption>{cap}</figcaption></figure>'
            f'<figure><img src="{b(n+"_top_big")}" width="190"><figcaption>Oberseite gross</figcaption></figure>'
            f'<figure><img src="{b(n+"_side_big")}" width="190"><figcaption>Seite gross</figcaption></figure>'
            f'<figure><img src="{b(n+"_top")}" width="16" height="16"><img src="{b(n+"_side")}" width="16" height="16"><figcaption>Originalgroesse 16x16<br>hart oben/Seite: {H[n]} %</figcaption></figure>')
Q = [
 ('stoffe', 'End-Stoffe (Regale und Boden des End-Enchanters)', [
  ('A', 'Aschholz-/Daemmerholz-Regale (Simple End) + End-Obsidian-Boden (Obsidian + Endstein)', 'Empfehlung: "End-Variante derselben Stoffe" wie BS25-20, nutzt vorhandenes Endholz, kein Recolor'),
  ('B', 'Purpur-/Chorus-Splitter-Regale + Purpur-Boden', 'neue Stoffe, kein Bezug zum Endholz'),
  ('C', 'keine neuen Bloecke: Dark-Regale/-Boden zaehlen, nur mehr Enderit', 'am wenigsten Arbeit, aber kein End-Look der Stoffe')]),
 ('upg', 'Herstellung (Hammer auf Block, 8 Nuggets)', [
  ('A', 'Dark = 8 Netherit-Nuggets (Hammer Netherit), End = Dark + 8 Enderit-Nuggets (Hammer Enderit)', 'Empfehlung: Dark laeuft auch ohne Simple Ores, Enderit nur im End-Schritt'),
  ('B', 'Dark bleibt bei 8 Enderit-Nuggets (heute), End = Dark + 8 weitere', 'nichts aendert sich fuer Dark; Dark braucht Simple Ores')]),
 ('dark', 'Textur Dark Enchanter', [
  ('A', 'A: heutiger Astral-Tisch (violett/Gold), unveraendert', 'Empfehlung: schon abgenommen, nicht anfassen; nur Name aendert sich'),
  ('B', 'B: Glut-Karmesin (weich, ~10 % hart)', 'passt zu den Nether-Stoffen, Neuzeichnung')]),
 ('end', 'Textur End-Enchanter', [
  ('A', 'A: Purpur-Tuch auf Obsidian, Ender-Auge', 'dunkel, hebt sich von Dark A ab'),
  ('B', 'B: heller Endstein-Koerper, tiefviolettes Tuch, tuerkise Kante', 'Empfehlung: passt zu Aschholz-Regalen, klar anders als Dark')]),
 ('apfel', 'Goldapfel-Kauf (BS25-22)', [
  ('A', 'an beiden Tischen 40 Level (wie heute)', 'Empfehlung: hoehere Stufe darf das gleiche Produkt nicht teurer machen'),
  ('B', 'Dark 50, End 60 (Konstanten APPLE_LEVELS_NETHER/ENDERITE)', 'Stufen waehlen den Preis')]),
 ('cap', 'Schutz V / Federfall V', [
  ('A', 'nur am End-Enchanter (Dark bleibt bei Vanilla-Maximum)', 'Empfehlung: das ist der Sinn der Endstufe; Dark hat sie heute (Tier 50), das wandert'),
  ('B', 'auch am Dark Enchanter (wie heute)', 'End-Enchanter haette dann nur mehr Budget 60')]),
]
qh = ''
for k, t, opts in Q:
    qh += f'<h3>{t}</h3>'
    for v, text, why in opts:
        chk = ' checked' if 'Empfehlung' in why else ''
        qh += f'<label><input type="radio" name="{k}" value="{v}"{chk}> <b>{v}</b> {text}<br><small>{why}</small></label>'
KEYS = [k for k, _, _ in Q]
html = f'''<!doctype html><meta charset="utf-8"><title>b-ench01 Dark und End Enchanter</title>
<style>body{{font:15px sans-serif;background:#1b1b1f;color:#eee;max-width:1000px;margin:2em auto;padding:0 1em}}
figure{{display:inline-block;margin:4px;text-align:center;font-size:12px;vertical-align:bottom}}img{{image-rendering:pixelated}}
h2{{border-top:1px solid #444;padding-top:1em}}table{{border-collapse:collapse}}td,th{{border:1px solid #444;padding:4px 8px;text-align:left;font-size:13px}}
label{{display:block;background:#26262c;margin:4px 0;padding:8px;border-radius:4px;cursor:pointer}}label:hover{{background:#32323a}}small{{color:#bbb}}
code,#code{{background:#000;padding:.3em .6em;border-radius:4px;color:#9f9}}button{{padding:.4em 1em}}
.mc{{display:inline-block;background:#c6c6c6;color:#3f3f3f;border:3px solid;border-color:#fff #555 #555 #fff;padding:8px 10px;margin:6px;font:13px monospace;vertical-align:top;width:300px}}
.mc h4{{margin:0 0 6px;color:#3f3f3f;font:bold 14px monospace}}.slot{{display:inline-block;width:34px;height:34px;background:#8b8b8b;border:2px solid;border-color:#373737 #fff #fff #373737;vertical-align:middle;margin-right:4px}}
.row{{margin:5px 0}}.bar{{display:inline-block;width:110px;height:10px;background:#555;vertical-align:middle;position:relative}}.bar i{{position:absolute;left:0;top:0;bottom:0;background:#6a3fc0}}.bar i.e{{background:#2d8f78}}
.gold{{color:#7a4f00;font-weight:bold}}</style>
<h1>b-ench01: Dark Enchanter und End-Enchanter (Konzept + Vorschau)</h1>
<p>Nur Konzept und Vorschau, nichts gebaut. Entschieden (ZO7 = A, 01.11.): der heutige Astral-Tisch wird <b>Dark Enchanter</b>, darueber kommt ein neuer <b>End-Enchanter</b>. Offen sind nur die Stoffe und Details unten. Plan: <code>docs/ai/KONZEPT-B-ENCH01-2026-11-02.md</code>.</p>
<h2>1. Was macht welcher Tisch</h2>
<table><tr><th></th><th>Verzauberungstisch (Vanilla)</th><th>Dark Enchanter (Nether)</th><th>End-Enchanter (End)</th></tr>
<tr><th>Herstellung</th><td>Vanilla</td><td>Tisch + Hammer (Netherit), 8 Netherit-Nuggets</td><td>Dark + Hammer (Enderit), 8 Enderit-Nuggets</td></tr>
<tr><th>Stoffe</th><td>Buecherregale</td><td>Lohen-Obsidian-Boden 5x5, Crimson-/Warped-Lohenholz-Regale</td><td>End-Obsidian-Boden 5x5, Aschholz-/Daemmerholz-Regale (Empf.)</td></tr>
<tr><th>Tier / Preisbudget</th><td>30</td><td>50 (wie heute)</td><td>60</td></tr>
<tr><th>Regler</th><td>3 Angebote</td><td>jede Verzauberung bis Vanilla-Maximum</td><td>wie Dark + Schutz V, Federfall V (Aufschlag +6/Stufe, kein VI)</td></tr>
<tr><th>Goldapfel</th><td>-</td><td>40 Level</td><td>40 Level (Empf.)</td></tr>
<tr><th>Modul</th><td>-</td><td>Simple Building (Motor + Block)</td><td>Block + Menue in Simple Building, Regale/Boden in Simple End (Tag <code>#simplebuilding:end_enchant_shelves</code>); ohne Simple End/Ores kein End-Tisch</td></tr></table>
<p><b>Vanilla-Nutzen / Eingriff (A12):</b> Nutzen: lohnendes Endspiel, Vanilla-Maximum bleibt (Amboss kann nie V bauen). Eingriff gering: Vanilla-Tisch unveraendert, Schalter <code>endgameEnchantLevels</code>; Schutz V bleibt hinter Aufwand (End-Stoffe + Enderit). Umbenennung per IdMove (<code>astral_enchanting_table</code> bleibt als Alias, Welten/Menues bleiben). WI26-1: Abbauen eines Tisches gibt die Nuggets passend zum Rezept (8 Stueck der jeweiligen Stufe) zurueck.</p>
<h2>2. Texturen</h2>
<p>Eigene Pixel (Generator <code>tools/textures/ench01_preview.py</code>), weiche Rampen nach Endholz-Prozess A8b. Vanilla-Tisch nur zum Vergleich (nicht im Repo). Wert = Anteil harter Uebergaenge (Sprung &gt; 48), Richtwert ~7 %, ueber 12 % zu hart. End A/B liegen mit ~15-17 % noch darueber (Nachschliff nach Wahl); Dark A ist der abgenommene Altstand (kantig).</p>
<h3>Vanilla-Verzauberungstisch (Vergleich)</h3>{fig("vanilla","Vanilla")}
<h3>Dark Enchanter</h3>{fig("dark_a","Dark A: heutiger Astral-Tisch")}<br>{fig("dark_b","Dark B: Glut-Karmesin")}
<h3>End-Enchanter</h3>{fig("end_a","End A: Purpur-Tuch / Ender-Auge")}<br>{fig("end_b","End B: Endstein / violettes Tuch")}
<h3>End-Stoffe (Option A der Rueckfrage)</h3>
<figure><img src="{b("shelf")}" width="128"><figcaption>Endholz-Regal (Aschholz)</figcaption></figure><figure><img src="{b("floor")}" width="128"><figcaption>End-Obsidian-Boden</figcaption></figure>
<h2>3. Menue-Mock</h2><p>Gleiches Menue wie heute (Item-Platz, Regler je Verzauberung, Preis, Lapis/Lohenstaub). Neu am End-Enchanter nur die Stufenanzeige 60 und der violette Endgame-Abschnitt.</p>
<div class="mc"><h4>Dark Enchanter</h4><div class="row"><span class="slot"></span><span class="slot"></span><span class="slot"></span> Stufe 50</div>
<div class="row">Schutz <span class="bar"><i style="width:80%"></i></span> IV/IV</div><div class="row">Haltbarkeit <span class="bar"><i style="width:100%"></i></span> III/III</div><div class="row">Preis: 32 Level (3 Lapis)</div></div>
<div class="mc"><h4>End-Enchanter</h4><div class="row"><span class="slot"></span><span class="slot"></span><span class="slot"></span> Stufe 60</div>
<div class="row">Schutz <span class="bar"><i class="e" style="width:100%"></i></span> <span class="gold">V/IV Endgame</span></div><div class="row">Federfall <span class="bar"><i class="e" style="width:100%"></i></span> <span class="gold">V/IV Endgame</span></div><div class="row">Haltbarkeit <span class="bar"><i style="width:100%"></i></span> III/III</div><div class="row">Preis: 44 Level (Aufschlag +6 je Stufe)</div></div>
<h2>4. Rueckfragen (Empfehlung vorausgewaehlt)</h2>{qh}
<p>Antwortcode: <span id="code"></span> <button onclick="navigator.clipboard.writeText(document.getElementById('code').textContent)">kopieren</button></p>
<script>const K={KEYS};function u(){{document.getElementById('code').textContent='ENCH01|'+K.map(k=>k+'='+(document.querySelector('input[name='+k+']:checked')||{{value:'?'}}).value).join('|')}}document.addEventListener('change',u);u();</script>'''
open(OUT, 'w').write(html)
