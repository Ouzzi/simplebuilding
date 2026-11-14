// Builds the CONV51B acceptance preview (phase 3, wiki style): node build_preview.js <out.html> [<screenshot dir>]
// Screenshots conv51b-*.png come from the harness click test; the converters are embedded live with the wiki's design tokens.
const fs = require('fs'), path = require('path');
const R = path.join(__dirname, '../..'), A = path.join(R, 'wiki/assets/conv'), shots = process.argv[3] || '/root/sb-tools-b/previews';
const js = ['conv-data.js', 'conv-core.js', 'conv-formats.js', 'conv-lang.js', 'conv-mesh.js', 'examples/examples.js', 'conv-ui.js'].map((f) => fs.readFileSync(path.join(A, f), 'utf8'));
const idx = fs.readFileSync(path.join(R, 'wiki/index.html'), 'utf8');
const tokens = idx.slice(idx.indexOf(':root {'), idx.indexOf('/* Grundlagen'));
const rule = (sel) => (idx.split('\n').find((l) => l.startsWith(sel + ' {')) || '') + '\n';
const css = fs.readFileSync(path.join(A, 'conv.css'), 'utf8') + ['.box', '.box-title', '.box-info', '.btn', '.btn:hover'].map(rule).join('');
const img = (f, cap) => fs.existsSync(path.join(shots, f)) ? `<figure><img src="data:image/png;base64,${fs.readFileSync(path.join(shots, f)).toString('base64')}" alt="${cap}"><figcaption>${cap}</figcaption></figure>` : `<p class="muted">(${f} fehlt)</p>`;
const vec = path.join(R, 'wiki/tests/conv51/vectors/java-vectors.json'), status = fs.existsSync(vec);
const nvec = status ? JSON.parse(fs.readFileSync(vec, 'utf8')).length : 0;
const esc = (s) => s.replace(/<\/script/gi, '<\\/script');
const html = `<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>CONV51B – Konverter im Wiki-Stil (Phase 3, Abnahme)</title>
<style>${tokens}
*{box-sizing:border-box}body{font-family:var(--font);background:var(--bg);color:var(--text);line-height:1.55;max-width:1040px;margin:1.5rem auto;padding:0 1rem}
h1{font-size:1.7rem}h2{font-size:1.12rem;border-bottom:1px solid var(--border);padding-bottom:.35rem;margin-top:2rem}h3{font-size:.8rem;color:var(--muted);text-transform:uppercase;letter-spacing:.06em}
.muted{color:var(--muted)}code{font-family:var(--mono);background:var(--code-bg);padding:.05em .35em;border-radius:4px;font-size:.85em}
figure{margin:1rem 0}figure img{max-width:100%;border:1px solid var(--border);border-radius:var(--radius-sm)}figcaption{color:var(--muted);font-size:.85rem}
.grid{display:grid;grid-template-columns:1fr 1fr;gap:1rem}@media(max-width:800px){.grid{grid-template-columns:1fr}}
section.live{background:var(--panel-2);border:1px solid var(--border);border-radius:var(--radius);padding:.2rem 1rem 1rem;margin:1rem 0}
#qbox{background:var(--warn-bg);border:1px solid var(--warn-border);color:var(--warn-text);padding:1rem;border-radius:var(--radius)}
table{border-collapse:collapse}td,th{border:1px solid var(--border);padding:.25rem .5rem;text-align:left;vertical-align:top}
${css}</style></head><body>
<h1>CONV51B – Konverter „Bauwerke“ und „3D-Modelle“ als echte Wiki-Seiten (Phase 3)</h1>
<p>Deine Vorgabe vom 14.11.: Dev-Seiten „vollständig und im selben Stil wie simplewiki“. Beide Konverter sind jetzt Seiten der Wiki-App
(<code>wiki/index.html</code>): gleicher Kopf, Seitenleiste, Farben, Schrift, Hell/Dunkel, Kästen und Knöpfe des Wikis. Bis zu deiner Freigabe
sieht man sie nur im Dev-Wiki (<code>/dev/</code>, Dev-Overlay, <code>?dev=1</code>): eigene Leistengruppe „Konverter“ mit „Bauwerke“ und
„3D-Modelle“, Kasten auf der Übersicht von Simple Building Tools bzw. Simple Models. Öffentlich: kein Eintrag, die Route zeigt „nicht gefunden“,
kein Skript wird geladen. Die alten Adressen <code>bauwerke.html</code>/<code>modelle.html</code> leiten auf die Wiki-Route weiter.</p>
<h2>So sieht es im Dev-Wiki aus (Klicktest im Test-Harness, echter Browser)</h2>
<div class="grid">${img('conv51b-bauwerke-light.png', 'Bauwerke, hell, Beispiel geladen')}${img('conv51b-modelle-dark.png', '3D-Modelle, dunkel, Beispiel .obj + .mtl')}</div>
<div class="grid">${img('conv51b-bauwerke-rueckweg.png', 'Rückweg Code → .nbt/.schem')}${img('conv51b-bauwerke-dark.png', 'Bauwerke, dunkel')}</div>
<div class="grid">${img('conv51b-public.png', 'Öffentlich (ohne /dev/): kein Konverter, keine Leistengruppe')}${img('conv51b-mobil.png', 'Handy-Breite')}</div>
<h2>Zum Ausprobieren (dieselben Skripte, hier eingebettet)</h2>
<section class="live conv-view"><h3>Bauwerke</h3><div data-conv="blueprint"></div></section>
<section class="live conv-view"><h3>3D-Modelle</h3><div data-conv="model"></div></section>
<h2>Was dazukam</h2>
<table><tr><th>Punkt</th><th>Ergebnis</th></tr>
<tr><td>Wiki-Stil</td><td>Seiten <code>?tab=buildings</code> und <code>?tab=models3d</code> in der Wiki-App, Texte DE/EN mit der Wiki-Sprache, Zustand bleibt beim Seitenwechsel erhalten, Skripte erst beim ersten Besuch.</td></tr>
<tr><td>„Beispiel laden“</td><td>lädt nicht mehr aus <code>wiki/tests/</code>: eigene, selbst erzeugte Beispiele eingebettet (<code>assets/conv/examples/</code>), Auswahl je Format (.litematic, .schem, .nbt, großer Turm, .schematic; .glb, .bbmodel, .json, .obj + .mtl).</td></tr>
<tr><td>Standardwerte aus dem Spiel</td><td>${status ? 'Ein Gametest liest alle Blöcke der 26.3-Registry (Standardzustand, Eigenschaften, Item ja/nein); die Tabelle des Konverters kommt jetzt daraus statt aus einer Handtabelle.' : 'noch offen (Lauf auf sb-test ausstehend)'}</td></tr>
<tr><td>Java-Vergleich</td><td>${status ? `Derselbe Gametest schreibt ${nvec} Codes der Mod (<code>BlueprintCode.serialize</code>) für feste Bauten (Hütte, Zufallsbauten, Umbruch/Alias, jeder Block im Standard- und in einem Zufallszustand); der Konverter schreibt zeichengleich dasselbe (automatischer Test).` : 'noch offen'}</td></tr></table>
<h2>Nicht geprüft</h2>
<ul><li>Klicktest auf der echten Dev-Instanz hinter dem Login (nur im lokalen Test-Harness mit denselben Dateien).</li>
<li>Dateien echter Fremd-Werkzeuge (Litematica, WorldEdit, MCEdit, Blockbench, Blender) und Laden der erzeugten Dateien im Spiel.</li></ul>
<div id="qbox"><h2 style="margin-top:0;border:0">Abnahme (Antwortcode CONV51B)</h2>
<p><label><input type="radio" name="v" value="ok" checked> ok</label> &nbsp; <label><input type="radio" name="v" value="Einwand"> Einwand:</label> <input id="why" size="60" placeholder="was soll anders sein?"></p>
<p>Antwort (zum Kopieren): <input id="ans" size="70" readonly> <button id="cp" type="button" class="btn">Kopieren</button></p></div>
<script>window.CONV51_PREVIEW=true;</script>
${js.map((s) => '<script>' + esc(s) + '</script>').join('\n')}
<script>(function(){var a=document.getElementById('ans'),w=document.getElementById('why');function u(){var v=document.querySelector('input[name=v]:checked').value;a.value='CONV51B: '+(v==='ok'?'ok':'Einwand: '+(w.value||'…'))}document.querySelectorAll('input[name=v]').forEach(function(i){i.onchange=u});w.oninput=function(){document.querySelector('input[value=Einwand]').checked=true;u()};u();document.getElementById('cp').onclick=function(){a.select();(navigator.clipboard?navigator.clipboard.writeText(a.value):Promise.reject()).catch(function(){document.execCommand('copy')})}})();</script></body></html>`;
fs.writeFileSync(process.argv[2], html); console.log('wrote', process.argv[2], html.length);
