"""
Auswahlseite Vanilla-Naehe (Queue b-vnear22, PRINZIPIEN.md A8 a): alle Treffer von vanilla_nearness_audit.py mit Bild
(1x und vergroessert), Vanilla-Gegenstueck aus dem 26.3-Client-Jar, Merkmalen, Empfehlung und Umschalter
"ueberarbeiten"/"so lassen"; unten ein Code-Feld `VN22 ü=<id,...>` fuer die Antwort des Besitzers.

    python3.12 tools/textures/vanilla_nearness_page.py --out /var/tmp/sb/vnear22.html [--jar client.jar]

Aendert keine Textur. Die Vanilla-Pixel stehen NUR als data:-URI in der HTML-Ausgabe (Scratchpad/Vorschau), nie im Repo
(A8) - die Ausgabe deshalb nicht unter dem Repository ablegen.
"""
from __future__ import annotations

import argparse
import base64
import html
import json
import re
import sys
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import layers  # noqa: E402
import vanilla_derived  # noqa: E402
import vanilla_nearness_audit as audit  # noqa: E402

ROOT = layers.ROOT
VANILLA = "assets/minecraft/textures/"

# Vanilla counterpart by the texture's path inside its namespace (first match wins); None = no sensible counterpart.
COUNTERPART = [
    (r"item/enchanted_book_", "item/enchanted_book"),
    (r"item/brick_snowball", "item/snowball"),
    (r"item/(blank_note|money_bill|raw_bill|refined_blank_note|special_paper)", "item/paper"),
    (r"item/(resin|special)_fiber", "item/string"),
    (r"pocket_jukebox_open_front", "item/bundle_open_front"),
    (r"pocket_jukebox_open_back", "item/bundle_open_back"),
    (r"pocket_jukebox", "item/bundle"),
    (r"item/sandwich/", "item/bread"),
    (r"block/milk_cauldron/", "block/water_still"),
    (r"block/butter_block_inner", "block/cake_inner"),
    (r"block/cheese_block_side", "block/cake_side"),
    (r"block/cheese_block_top", "block/cake_top"),
    (r"_(crucible|cauldron)_bottom", "block/cauldron_bottom"),
    (r"_(crucible|cauldron)_inner", "block/cauldron_inner"),
    (r"_crucible_handle", "block/cauldron_side"),
    (r"_cauldron_extreme", "block/lava_still"),
    (r"mob_effect/soul_burn", "mob_effect/wither"),
    (r"mob_effect/shivering", "mob_effect/slowness"),
    (r"simpledimension\w*/block/portal", "block/nether_portal"),
    (r"ashwood_hanging_sign", "block/oak_hanging_sign"),
    (r"_leaves", "block/oak_leaves"),
    (r"basic_upgrade_template", "item/netherite_upgrade_smithing_template"),
    (r"shellker_spawn_egg", "item/shulker_spawn_egg"),
    (r"soulfire_blaze_spawn_egg", "item/blaze_spawn_egg"),
    (r"_blast_furnace_top", "block/blast_furnace_top"),
    (r"_furnace_front_on", "block/furnace_front_on"),
    (r"horseshoe/diamond", "entity/equipment/horse_body/diamond"),
    (r"horseshoe/golden", "entity/equipment/horse_body/gold"),
    (r"display_case_glass", "block/glass"),
    (r"item/\w+_chest$", "entity/chest/normal"),
    (r"block/shulker$", "entity/shulker/shulker"),
    (r"_hopper_inside", "block/hopper_inside"),
    (r"_hopper_top", "block/hopper_top"),
    (r"item/\w+_hopper$", "item/hopper"),
    (r"flypad", "block/iron_block"),
    (r"enderite_leggings|humanoid_leggings/enderite", "entity/equipment/humanoid_leggings/netherite"),
    (r"item/spawn_elytra", "item/elytra"),
    (r"hollow_howler_heart", "entity/warden/warden_heart"),
    (r"hollow_howler_antennae", "entity/warden/warden_bioluminescent_layer"),
    (r"item/magnet|palettes/|piggy_effect", None),
]

# Decisions already taken (memory texturen-nicht-vermischen: accepted textures are never proposed for a redraw) and
# intentional deviations. Regex on the repository path -> reason for "so lassen".
KEEP = [
    (r"simplebuilding/textures/item/enchanted_book_",
     "Besitzer-Original: 04.10. auf Wunsch zurückgesetzt, 05.10. Kontrast +35 % – abgenommen"),
    (r"simpleriding/textures/item/enchanted_book_", "Riding-Bücher B vom Besitzer gewählt – abgenommen"),
    (r"(brick_snowball|spawn_elytra)\.png|simplemoney/textures/item/(blank_note|money_bill|raw_bill|refined_blank_note"
     r"|special_paper)\.png", "Besitzer-Original: Texfix-Rücksetzung 04.10. auf Wunsch – abgenommen"),
    (r"simplemoney/textures/item/(resin|special)_fiber", "Besitzer-Wahl N41 Runde 3 (12.10.) – abgenommen"),
    (r"_pocket_jukebox", "Taschen-Jukebox A vom Besitzer gewählt (MU21, 21.10.); Färbefarbe ergibt „licht“/„grell“"),
    (r"item/sandwich/layer_", "Sandwich-Schichten B vom Besitzer gewählt (11.10.); kräftige Zutatenfarben gewollt"),
    (r"simpleend/textures/block/\w+_leaves", "End-Blätter abgenommen (16.10., N71)"),
    (r"flypad\.png", "im Spiel unbenutzt; Besitzer hebt die alten Flypads als Quelle der Trank-Pads auf"),
    (r"item/(netherite|reinforced)_chest\.png|block/shulker\.png",
     "im Spiel unbenutzt (kein Modell verweist darauf), nur Generator-/Wiki-Quelle"),
    (r"palettes/shulker/", "Färbe-Palette: Lime/Gelb sind von Natur aus kräftig"),
    (r"hollow_howler_(heart|antennae)", "Leucht-Ebene wie beim Warden: flach und kräftig ist gewollt"),
    (r"basic_upgrade_template", "Aufwertungs-Vorlage Runde 8 (25.09.) abgenommen; Kupfer-Orange ist die Materialfarbe"),
    (r"horseshoe/(diamond|golden)", "Materialfarbe Diamant/Gold (Rüstungsschicht am Pferd); Vanilla-Pferderüstung ebenso"),
    (r"(enderite|netherite|reinforced)_furnace_front_on", "Glut unten wie Vanilla-Ofen (Licht unten = Feuer); abgenommen"),
    (r"_hopper_(inside|top)\.png|item/netherite_hopper\.png|_blast_furnace_top\.png",
     "Maschinen nach Vanilla-Vorbild (27.09.) abgenommen; Vanilla-Trichter ist ebenso kontrastarm"),
    (r"enderite_leggings|humanoid_leggings/enderite", "Besitzer-Original (Ende-Update); Rüstungsschicht, im Spiel abgenommen"),
    (r"item/magnet\.png", "Besitzer-Original (Trim-Update 11.01.); dünne helle Pole sind das Motiv"),
    (r"simpledimension\w*/textures/block/portal\.png",
     "Graustufen-Vorlage, wird je Dimension eingefärbt (tintindex) – wie Vanilla-Masken absichtlich grau"),
    (r"_cauldron_extreme\.png", "Hitze-Animation leuchtet absichtlich (wie Lava/Seelenfeuer)"),
    (r"ashwood_hanging_sign\.png", "Aufbau 1:1 wie Vanilla-Hängeschild; heller unterer Teil ist das blasse Brett, kein Lichtfehler"),
]

# Recommendation to redraw: the texture is not explicitly accepted and the deviation is visible (reason).
REDRAW = [
    (r"milk_cauldron/milk_\d", "Milch flach (3 Farben): wie Vanilla-Wasser mit 4–6 Stufen leicht verrauschen; "
                              "Käse/Butter/Kessel-Abnahme laut Memory offen"),
    (r"butter_block_inner", "Butter innen flach: Schattierung in Stufen wie Kuchen-Innenseite; Abnahme offen"),
    (r"cheese_block_(side|top)", "Käse sehr grell: Sättigung dämpfen wie Vanilla-Kuchen/Honig; Abnahme offen"),
    (r"(shellker|soulfire_blaze)_spawn_egg", "1 halbtransparentes Pixel: deckend machen (Aussehen bleibt)"),
    (r"mob_effect/piggy_effect", "nur die Augenpixel sind reines #000000: auf dunkles Grau setzen, Motiv bleibt"),
    (r"display_case_glass", "flächig grau (3 Farben): Vanilla-Glas hat hellen Rahmen und Glanzstriche auf klarem Grund; "
                            "keine ausdrückliche Abnahme gefunden"),
    (r"iron_crucible_handle", "Griff flach dunkelgrau ohne Stufen; Vanilla-Kessel schattiert in Stufen; "
                              "keine ausdrückliche Abnahme gefunden"),
]

FLAG_HINT = {
    "halbtransparent": "Pixel mit 0 < Alpha < 255 (Vanilla nur bei Glas/Eis/Portal)",
    "schwarz": "reines #000000 (Vanilla nie)",
    "kontur": "Randpixel nicht dunkler als das Innere (Vanilla-Items: 1-px-Kontur)",
    "licht": "unten rechts heller als oben links (Vanilla: Licht von oben links)",
    "farben": "mehr als 64 Farben = Verlauf/Weichzeichnung (Vanilla: 4–6 Stufen je Material)",
    "flach": "höchstens 3 Farben auf größerer Fläche (ohne Schattierung)",
    "grell": "mittlere Sättigung > 0,75 bei Helligkeit > 0,5",
    "kontrast": "Helligkeitsspanne < 0,12 (flach, ohne Hell-Dunkel-Stufen)",
}


def module_of(rel: str) -> str:
    m = re.match(r"modules/(\w+)/", rel)
    return m.group(1) if m else "simplebuilding"


def tex_id(rel: str) -> str:
    """namespace:path without .png (the id used in the answer code)."""
    m = re.search(r"assets/(\w+)/textures/(.+)\.png$", rel)
    if not m:
        return rel
    return f"{m.group(1)}:{m.group(2)}"


def vanilla_candidates(tid: str) -> list[str]:
    """Vanilla texture paths (without .png) to try for the texture id, best first; [] = none."""
    ns, path = tid.split(":", 1)
    for pattern, target in COUNTERPART:
        if re.search(pattern, f"{ns}/{path}"):
            return [target] if target else []
    out, parts = [path], path.split("/")
    words = parts[-1].split("_")
    for i in range(1, len(words)):
        out.append("/".join(parts[:-1] + ["_".join(words[i:])]))
    return out


def first(regex_table, rel: str):
    for pattern, reason in regex_table:
        if re.search(pattern, rel):
            return reason
    return None


def kinds(flags: list[str]) -> set[str]:
    return {f.split(" ")[0] for f in flags}


def recommend(rel: str, flags: list[str], vflags: list[str] | None) -> tuple[str, str]:
    """('u'|'l', reason): keep-table first (accepted textures are never proposed), then explicit redraws, then the
    Vanilla comparison (same deviation in Vanilla = false alarm), else redraw."""
    keep = first(KEEP, rel)
    if keep:
        return "l", keep
    redraw = first(REDRAW, rel)
    if redraw:
        return "u", redraw
    if vflags is not None and kinds(flags) <= kinds(vflags):
        return "l", "Fehlalarm: das Vanilla-Gegenstück zeigt dasselbe Merkmal"
    if all(k in ("flach", "kontrast") for k in kinds(flags)) and "mob_effect/" in rel:
        return "l", "Effekt-Symbol mit wenigen Farben wie Vanilla"
    return "u", "weicht ab: " + "; ".join(FLAG_HINT.get(k, k) for k in sorted(kinds(flags)))


def frame(w: int, h: int, rgba: bytes) -> tuple[int, int, bytes]:
    """First frame of a vertical animation strip (h a multiple of w), else the whole image."""
    if h > w and h % w == 0:
        return w, w, bytes(rgba[:w * w * 4])
    return w, h, bytes(rgba)


def data_uri(w: int, h: int, rgba: bytes) -> str:
    return "data:image/png;base64," + base64.b64encode(layers.png_write(w, h, rgba)).decode()


def collect(jar: Path | None) -> list[dict]:
    z = zipfile.ZipFile(jar) if jar else None
    names = set(z.namelist()) if z else set()
    rows = []
    for rel, w, h, flags in audit.scan():
        tid = tex_id(rel)
        fw, fh, px = frame(*layers.png_read((ROOT / rel).read_bytes()))
        entry = {"id": tid, "rel": rel, "mod": module_of(rel), "w": w, "h": h, "flags": flags,
                 "kind": sorted(kinds(flags), key=lambda k: list(FLAG_HINT).index(k) if k in FLAG_HINT else 99)[0],
                 "img": data_uri(fw, fh, px), "fw": fw, "fh": fh, "van": None}
        vflags = None
        for cand in vanilla_candidates(tid):
            name = VANILLA + cand + ".png"
            if name in names:
                vw, vh, vpx = layers.png_read(z.read(name))
                vflags = audit.features(vw, vh, bytes(vpx), "minecraft/textures/" + cand + ".png")
                vfw, vfh, vfpx = frame(vw, vh, vpx)
                entry["van"] = {"path": "minecraft:" + cand, "img": data_uri(vfw, vfh, vfpx), "fw": vfw, "fh": vfh,
                                "flags": vflags}
                break
        entry["rec"], entry["why"] = recommend(rel, flags, vflags)
        rows.append(entry)
    ids = [r["id"] for r in rows]
    if len(ids) != len(set(ids)):
        for r in rows:  # same namespace:path twice (src/main and overlay): make the id unique
            if ids.count(r["id"]) > 1 and "/overlay/" in r["rel"]:
                r["id"] += "@overlay"
    rows.sort(key=lambda r: (r["mod"], r["kind"], r["id"]))
    return rows


def scale_for(w: int, h: int) -> int:
    return max(1, min(16, 128 // max(w, h)))


def figure(img: str, w: int, h: int, label: str) -> str:
    s = scale_for(w, h)
    big = min(w * s, 224)
    return (f'<figure><div class="pix"><img src="{img}" width="{big}" height="{round(big * h / w)}" alt=""></div>'
            f'<div class="one"><img src="{img}" width="{w}" height="{h}" alt=""> 1×</div>'
            f'<figcaption>{html.escape(label)}</figcaption></figure>')


def render(rows: list[dict], total: int) -> str:
    mods = sorted({r["mod"] for r in rows})
    kinds_all = [k for k in FLAG_HINT if any(r["kind"] == k for r in rows)]
    cards, last = [], (None, None)
    for r in rows:
        if r["mod"] != last[0]:
            cards.append(f'<h2 class="mod" data-mod="{r["mod"]}">{r["mod"]}</h2>')
        if (r["mod"], r["kind"]) != last:
            cards.append(f'<h3 class="grp" data-mod="{r["mod"]}" data-kind="{r["kind"]}">{r["kind"]} – '
                         f'{html.escape(FLAG_HINT.get(r["kind"], ""))}</h3>')
        last = (r["mod"], r["kind"])
        van = r["van"]
        vfig = (figure(van["img"], van["fw"], van["fh"], van["path"]) if van
                else '<figure class="none"><div class="pix empty">kein Vanilla-Gegenstück</div></figure>')
        vnote = (f'<li>Vanilla gemessen: {html.escape(", ".join(van["flags"]) or "unauffällig")}</li>' if van else "")
        rec = r["rec"]
        cards.append(
            f'<article class="card" data-id="{html.escape(r["id"])}" data-mod="{r["mod"]}" data-kind="{r["kind"]}" '
            f'data-kinds="{" ".join(sorted(kinds(r["flags"])))}" data-rec="{rec}">'
            f'<header><code>{html.escape(r["id"])}</code><span class="size">{r["w"]}×{r["h"]}</span></header>'
            f'<div class="pair">{figure(r["img"], r["fw"], r["fh"], "eigen")}{vfig}</div>'
            f'<ul class="flags"><li>Merkmale: {html.escape(", ".join(r["flags"]))}</li>{vnote}</ul>'
            f'<p class="why"><b>Empfehlung: {"überarbeiten" if rec == "u" else "so lassen"}</b> – {html.escape(r["why"])}</p>'
            f'<div class="seg" role="group" aria-label="Entscheidung">'
            f'<button type="button" data-v="u" aria-pressed="{str(rec == "u").lower()}">überarbeiten</button>'
            f'<button type="button" data-v="l" aria-pressed="{str(rec == "l").lower()}">so lassen</button></div>'
            f'</article>')
    opts = lambda vals: "".join(f'<option value="{v}">{v}</option>' for v in vals)  # noqa: E731
    n_u = sum(1 for r in rows if r["rec"] == "u")
    return TEMPLATE.format(
        count=len(rows), total=total, n_u=n_u, n_l=len(rows) - n_u, mods=opts(mods), kinds=opts(kinds_all),
        cards="\n".join(cards))


TEMPLATE = """<!doctype html>
<html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Vanilla-Nähe Auswahl</title>
<style>
:root{{--bg:#f6f5f2;--card:#fff;--fg:#1d1d1f;--mute:#5a5a5e;--line:#d8d6d0;--acc:#2f6f3e;--acc-fg:#fff;
--keep:#4a5568;--chk1:#cfcfcf;--chk2:#efefef;--u:#fbe9d6;--l:#e7eef7}}
@media (prefers-color-scheme:dark){{:root:not([data-theme="light"]){{--bg:#161618;--card:#222226;--fg:#ececec;
--mute:#a8a8ad;--line:#3a3a40;--acc:#5fbf75;--acc-fg:#0d1a10;--keep:#9fb0c8;--chk1:#3a3a3a;--chk2:#4a4a4a;
--u:#3d2d1c;--l:#1f2a38}}}}
:root[data-theme="dark"]{{--bg:#161618;--card:#222226;--fg:#ececec;--mute:#a8a8ad;--line:#3a3a40;--acc:#5fbf75;
--acc-fg:#0d1a10;--keep:#9fb0c8;--chk1:#3a3a3a;--chk2:#4a4a4a;--u:#3d2d1c;--l:#1f2a38}}
*{{box-sizing:border-box}}
body{{margin:0;background:var(--bg);color:var(--fg);font:15px/1.45 system-ui,-apple-system,"Segoe UI",sans-serif}}
main{{max-width:1100px;margin:0 auto;padding:16px}}
h1{{font-size:1.4rem;margin:.2rem 0 .4rem}} h2.mod{{margin:1.6rem 0 .3rem;font-size:1.15rem;border-bottom:1px solid var(--line)}}
h3.grp{{margin:.9rem 0 .4rem;font-size:.95rem;color:var(--mute);font-weight:600}}
p.lead{{color:var(--mute);margin:.2rem 0 .8rem}}
.bar{{position:sticky;top:0;z-index:2;background:var(--bg);padding:8px 0;display:flex;flex-wrap:wrap;gap:8px;
border-bottom:1px solid var(--line)}}
.bar select,.bar input,.bar button{{font:inherit;color:var(--fg);background:var(--card);border:1px solid var(--line);
border-radius:6px;padding:6px 8px;min-width:0}}
.bar input{{flex:1 1 140px}}
.grid{{display:grid;grid-template-columns:repeat(auto-fill,minmax(min(300px,100%),1fr));gap:12px}}
.card{{background:var(--card);border:1px solid var(--line);border-radius:10px;padding:10px;min-width:0}}
.card[data-cur="u"]{{box-shadow:inset 4px 0 0 #c46a12}} .card[data-cur="l"]{{box-shadow:inset 4px 0 0 var(--keep)}}
.card header{{display:flex;justify-content:space-between;gap:6px;align-items:baseline}}
.card code{{font-size:.8rem;overflow-wrap:anywhere}} .size{{color:var(--mute);font-size:.8rem;white-space:nowrap}}
.pair{{display:flex;gap:10px;margin:8px 0;flex-wrap:wrap}}
figure{{margin:0;flex:1 1 120px;min-width:0}}
.pix{{background:repeating-conic-gradient(var(--chk1) 0 25%,var(--chk2) 0 50%) 0 0/16px 16px;border-radius:6px;
display:flex;align-items:center;justify-content:center;min-height:120px;padding:4px;overflow:hidden}}
.pix img{{image-rendering:pixelated;image-rendering:crisp-edges;max-width:100%;height:auto}}
.pix.empty{{background:none;border:1px dashed var(--line);color:var(--mute);font-size:.85rem;text-align:center}}
.one{{font-size:.75rem;color:var(--mute);margin-top:3px;display:flex;align-items:center;gap:4px}}
.one img{{image-rendering:pixelated;max-width:64px;height:auto}}
figcaption{{font-size:.75rem;color:var(--mute);overflow-wrap:anywhere}}
.flags{{margin:4px 0;padding-left:18px;font-size:.85rem}} .why{{margin:4px 0 8px;font-size:.88rem}}
.seg{{display:flex;gap:6px}} .seg button{{flex:1;font:inherit;padding:8px;border-radius:6px;border:1px solid var(--line);
background:var(--card);color:var(--fg);cursor:pointer}}
.seg button[aria-pressed="true"][data-v="u"]{{background:var(--u);border-color:#c46a12;font-weight:600}}
.seg button[aria-pressed="true"][data-v="l"]{{background:var(--l);border-color:var(--keep);font-weight:600}}
.hide{{display:none!important}}
.out{{margin:24px 0 40px}} textarea{{width:100%;min-height:90px;font:13px/1.4 ui-monospace,monospace;color:var(--fg);
background:var(--card);border:1px solid var(--line);border-radius:8px;padding:8px}}
.out button{{font:inherit;padding:8px 14px;border-radius:6px;border:0;background:var(--acc);color:var(--acc-fg);cursor:pointer}}
.stat{{color:var(--mute);font-size:.9rem}}
</style></head><body><main>
<h1>Vanilla-Nähe Auswahl</h1>
<p class="lead">{count} von {total} eigenen Texturen weichen laut Heuristik (<code>tools/textures/vanilla_nearness_audit.py</code>)
vom Vanilla-Stil ab. Je Eintrag: eigenes Bild (vergrößert und 1×), Vanilla-Gegenstück, Merkmale und Empfehlung
(vorbelegt: {n_u}× überarbeiten, {n_l}× so lassen). Abgenommene Texturen sind nie zum Überarbeiten vorgeschlagen.
Unten den Code kopieren und zurückschicken. Es wird nichts geändert, bevor du wählst.</p>
<div class="bar">
<select id="fm" aria-label="Modul"><option value="">alle Module</option>{mods}</select>
<select id="fk" aria-label="Merkmal"><option value="">alle Merkmale</option>{kinds}</select>
<select id="fr" aria-label="Wahl"><option value="">alle</option><option value="u">überarbeiten</option><option value="l">so lassen</option></select>
<input id="fq" type="search" placeholder="Suche (id)" aria-label="Suche">
<button type="button" id="th" aria-label="Hell/Dunkel">◐</button>
</div>
<p class="stat" id="st"></p>
<div class="grid" id="g">
{cards}
</div>
<section class="out"><h2>Code für die Antwort</h2>
<p class="stat">Nur die Einträge mit „überarbeiten“. Der Rest bleibt, wie er ist.</p>
<textarea id="code" readonly></textarea>
<p><button type="button" id="cp">Code kopieren</button> <span id="cpok" class="stat"></span></p></section>
</main>
<script>
(function(){{
var cards=[].slice.call(document.querySelectorAll('.card')),q=function(i){{return document.getElementById(i)}};
function cur(c){{return c.getAttribute('data-cur')}}
function set(c,v){{c.setAttribute('data-cur',v);[].forEach.call(c.querySelectorAll('.seg button'),function(b){{
b.setAttribute('aria-pressed',String(b.getAttribute('data-v')===v))}})}}
function code(){{var u=cards.filter(function(c){{return cur(c)==='u'}}).map(function(c){{return c.getAttribute('data-id')}});
q('code').value='VN22 ü='+u.join(',');q('st').textContent=u.length+' überarbeiten, '+(cards.length-u.length)+' so lassen';}}
function filt(){{var m=q('fm').value,k=q('fk').value,r=q('fr').value,s=q('fq').value.toLowerCase(),vis={{}};
cards.forEach(function(c){{var ok=(!m||c.dataset.mod===m)&&(!k||(' '+c.dataset.kinds+' ').indexOf(' '+k+' ')>=0)&&
(!r||cur(c)===r)&&(!s||c.dataset.id.toLowerCase().indexOf(s)>=0);c.classList.toggle('hide',!ok);
if(ok){{vis[c.dataset.mod]=1;vis[c.dataset.mod+'|'+c.dataset.kind]=1}}}});
[].forEach.call(document.querySelectorAll('h2.mod'),function(h){{h.classList.toggle('hide',!vis[h.dataset.mod])}});
[].forEach.call(document.querySelectorAll('h3.grp'),function(h){{h.classList.toggle('hide',!vis[h.dataset.mod+'|'+h.dataset.kind])}});}}
cards.forEach(function(c){{set(c,c.dataset.rec);c.querySelector('.seg').addEventListener('click',function(e){{
var v=e.target.getAttribute('data-v');if(v){{set(c,v);code();}}}})}});
['fm','fk','fr'].forEach(function(i){{q(i).addEventListener('change',filt)}});q('fq').addEventListener('input',filt);
q('cp').addEventListener('click',function(){{var t=q('code');t.select();var done=function(){{q('cpok').textContent='kopiert'}};
if(navigator.clipboard){{navigator.clipboard.writeText(t.value).then(done,function(){{document.execCommand('copy');done()}})}}
else{{document.execCommand('copy');done()}}}});
q('th').addEventListener('click',function(){{var d=document.documentElement,dark=d.getAttribute('data-theme')?
d.getAttribute('data-theme')==='dark':matchMedia('(prefers-color-scheme: dark)').matches;
d.setAttribute('data-theme',dark?'light':'dark')}});
code();
}})();
</script></body></html>
"""


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--out", required=True, help="HTML file outside the repository (holds Vanilla pixels)")
    ap.add_argument("--jar", help="26.3 client jar (default: vanilla_derived.find_jar())")
    ap.add_argument("--json", help="also write the entries without images (id, flags, recommendation)")
    a = ap.parse_args()
    out = Path(a.out).resolve()
    if out.is_relative_to(ROOT):
        sys.exit("--out must lie outside the repository: the page embeds Vanilla pixels (A8)")
    jar = vanilla_derived.find_jar(a.jar)
    if jar is None:
        print("warning: no 26.3 client jar found - page without Vanilla counterparts", file=sys.stderr)
    rows = collect(jar)
    out.write_text(render(rows, len(layers.own_textures())), encoding="utf-8", newline="\n")
    if a.json:
        Path(a.json).write_text(json.dumps([{k: r[k] for k in ("id", "flags", "rec", "why")} | {
            "vanilla": r["van"] and r["van"]["path"]} for r in rows], ensure_ascii=False, indent=1), encoding="utf-8")
    n_u = sum(1 for r in rows if r["rec"] == "u")
    print(f"{out}: {len(rows)} entries ({n_u} redraw, {len(rows) - n_u} keep), "
          f"{sum(1 for r in rows if r['van'])} with Vanilla counterpart, {out.stat().st_size // 1024} KiB")
    return 0


if __name__ == "__main__":
    sys.exit(main())
