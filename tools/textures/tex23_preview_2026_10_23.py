"""TX23 preview (owner decisions 23.10., docs/ai/PLAN-A-TEX23-2026-10-23.md): one page for everything built in and
everything still to choose.

    python3.12 tools/textures/tex23_preview_2026_10_23.py --out /root/sb-tools-a/previews/tex23-preview.html [--jar client.jar]

Built in: Shellker spawn egg (animation A -> C -> B), Village Wanderer egg (B reworked), iron crucible handle (C),
milk cauldron 0-3 (B), cheese side + top (B). Candidates (not built in): the other 7 own spawn eggs in style B, butter
inner A/B/C, Piggy effect A/B/C, two airier loose Ashwood leaves. Vanilla pixels come from the 26.3 client jar and live
only in the HTML (A8); nothing in the repository is written. Run from the repo root.
"""
from __future__ import annotations

import argparse
import base64
import io
import json
import os
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(ROOT / 'modules/simpleend/tools'))

from PIL import Image  # noqa: E402

import vanilla_derived  # noqa: E402

EGG = 'modules/{m}/shared/resources/assets/{m}/textures/item/{n}_spawn_egg.png'
SAN = ROOT / 'modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/block'
CRU = ROOT / 'modules/simplecrucibles/shared/resources/assets/simplecrucibles/textures/block'
PIG = 'modules/simplefun/shared/resources/assets/simplefun/textures/mob_effect/piggy_effect.png'


def uri(im: Image.Image, fmt='PNG', **kw) -> str:
    buf = io.BytesIO()
    im.save(buf, fmt, **kw)
    return f'data:image/{fmt.lower()};base64,' + base64.b64encode(buf.getvalue()).decode()


def committed(rel: str) -> Image.Image | None:
    """The file as it is on claude-wave1 (the state before this branch), for 'alt'."""
    try:
        data = subprocess.run(['git', '-C', str(ROOT), 'show', f'claude-wave1:{rel}'], capture_output=True, check=True).stdout
        return Image.open(io.BytesIO(data)).convert('RGBA')
    except (subprocess.CalledProcessError, OSError):
        return None


def fig(im, scale, cap, cls='px'):
    return (f'<figure><img class="{cls}" src="{uri(im)}" width="{im.width * scale}" height="{im.height * scale}" alt="{cap}">'
            f'<figcaption>{cap}</figcaption></figure>')


def on_slot_bg(im, bg=(139, 139, 139, 255)):
    o = Image.new('RGBA', im.size, bg)
    o.alpha_composite(im)
    return o


def gif(frames_ticks, scale):
    """Animated GIF (on the slot grey) of [(frame, ticks)], 1 tick = 50 ms."""
    ims = [on_slot_bg(f).resize((16 * scale, 16 * scale), Image.NEAREST).convert('P', palette=Image.ADAPTIVE) for f, _ in frames_ticks]
    buf = io.BytesIO()
    ims[0].save(buf, 'GIF', save_all=True, append_images=ims[1:], duration=[t * 50 for _, t in frames_ticks], loop=0, disposal=1)
    return 'data:image/gif;base64,' + base64.b64encode(buf.getvalue()).decode()


# ---------------------------------------------------------------- Piggy effect candidates (own pixels, Vanilla-pig-like tones)
PIG_RAMP = {'K': (0x5a, 0x2c, 0x30), 'd': (0xb8, 0x62, 0x6a), 'p': (0xe0, 0x8c, 0x92), 'L': (0xf2, 0xae, 0xae),
            'H': (0xfb, 0xd0, 0xcc), 'S': (0xf0, 0x9a, 0xa0), 's': (0xd2, 0x74, 0x7e), 'n': (0x7a, 0x34, 0x3c),
            'W': (0xf4, 0xf4, 0xf4), 'B': (0x1e, 0x14, 0x18)}
PIGGY = {
    'A': ('Kopf frontal', ['................',
                           '..KK........KK..',
                           '.KdpK......KpdK.',
                           '.KppKKKKKKKKppK.',
                           '.KLLHLLLLLLLLpK.',
                           '.KLHLLLLLLLLppK.',
                           '.KLWBLLLLLLBWpK.',
                           '.KLLLLLLLLLLppK.',
                           '.KLpSSSSSSSSpdK.',
                           '.KLpSnSSSSnSpdK.',
                           '.KppSSSSSSSspdK.',
                           '.KpppsssssspddK.',
                           '..KdpppppppddK..',
                           '...KKKKKKKKKK...',
                           '................',
                           '................']),
    'B': ('Kopf schräg, Rüssel vorn', ['................',
                                      '.....KK....KK...',
                                      '....KdpK..KpdK..',
                                      '...KLLHKKKKppK..',
                                      '..KLHLLLLLLLpK..',
                                      '..KLLLLLLLLLppK.',
                                      '..KLWBLLLLWBpK..',
                                      '..KLLLLLLLLLppKK',
                                      '..KLLLLLLLKSSSSK',
                                      '..KLpppppKSnSnSK',
                                      '..KppppppKSSSSsK',
                                      '...KdppppdKsssK.',
                                      '....KKdddddKKK..',
                                      '......KKKKK.....',
                                      '................',
                                      '................']),
    'C': ('ganzes Schwein seitlich', ['................',
                                     '................',
                                     '................',
                                     '..........KK....',
                                     '.........KdpKK..',
                                     '..KKKKKKKKLLHLK.',
                                     '.KLHHLLLLLLLWBKK',
                                     'KdLLLLLLLLLLLLSK',
                                     '.KLLLLLLLLLLLKnK',
                                     '.KppppppppppppKK',
                                     '.KdpppppppppdK..',
                                     '..KddKKKKKddK...',
                                     '..KdK.....KdK...',
                                     '..KKK.....KKK...',
                                     '................',
                                     '................']),
}


def piggy(v):
    rows = PIGGY[v][1]
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, r in enumerate(rows):
        assert len(r) == 16, (v, y, r)
        for x, ch in enumerate(r):
            if ch != '.':
                im.putpixel((x, y), PIG_RAMP[ch] + (255,))
    return im


# ---------------------------------------------------------------- page

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', required=True)
    ap.add_argument('--jar')
    a = ap.parse_args()
    jarp = vanilla_derived.find_jar(a.jar)
    if jarp:
        vanilla_derived.extract_vanilla_textures(jarp)  # crucibles_2026_10_04 reads build/vanilla-textures on import
    os.chdir(ROOT)
    sys.argv = [sys.argv[0]]
    import vn12_preview_2026_10_23 as vp
    import spawn_eggs_2026_10_23 as eg
    import crucibles_2026_10_04 as cru
    import sandwiches as san
    import gen_textures as gt
    import tx22_textures as tx
    jar = vp.Jar(jarp)

    def van(names, kind='item'):
        out = []
        for n in names:
            im = jar.tex(f'{kind}/{n}')
            if im is not None:
                out.append((n, im))
        return out

    sec = []

    # 1) Shellker egg animation
    strip, meta, _ = eg.shellker_animation()
    frames = [strip.crop((0, 16 * i, 16, 16 * i + 16)) for i in range(strip.height // 16)]
    seq = [(frames[f['index']], f['time']) for f in meta['animation']['frames']]
    old = committed(EGG.format(m='simplemobs', n='shellker'))
    total = sum(t for _, t in seq)
    parts = [f'<figure><img class="px" src="{gif(seq, 10)}" width="160" height="160" alt="Shellker animiert"><figcaption>animiert 10× (Spieltempo, {total / 20:.1f} s je Runde)</figcaption></figure>',
             f'<figure><span class="slot"><img class="px" src="{gif(seq, 1)}" width="16" height="16" alt=""></span>'
             f'<span class="slot"><img class="px" src="{gif(seq, 2)}" width="32" height="32" alt=""></span><figcaption>1× / 2× im Slot</figcaption></figure>']
    names = ['A (Standard, erstes Bild)', 'C', 'B', 'A→C 1/3', 'A→C 2/3', 'C→B 1/3', 'C→B 2/3', 'B→A 1/3', 'B→A 2/3']
    frame_figs = ''.join(fig(f, 6, n) for f, n in zip(frames, names))
    timing = ' → '.join(f'{names[f["index"]].split(" ")[0]} {f["time"]}' for f in meta['animation']['frames'])
    sec.append(f'''<section><h2>Shellker-Ei <span class="tag in">eingebaut</span></h2>
<p class="note">Besitzer 23.10.: animiert A → C → B. Vanilla-Animation über <code>shellker_spawn_egg.png.mcmeta</code> (Bildstreifen 16×144),
ruhiges Tempo: A {seq[0][1] / 20:.0f} s, C und B je {seq[3][1] / 20:.0f} s, dazwischen zwei Überblend-Bilder je {seq[1][1] * 50} ms
(Pixel wechseln nacheinander, keine Mischfarben). Ticks: {timing}.</p>
<div class="row">{''.join(parts)}{fig(old, 10, 'alt') if old else ''}</div>
<details><summary>Alle 9 Bilder des Streifens (Ebenen = je ein Bild)</summary><div class="row">{frame_figs}</div></details>
<div class="row van">{''.join(fig(im, 6, 'Vanilla ' + n) for n, im in van(['shulker_spawn_egg', 'endermite_spawn_egg']))}</div></section>''')

    # 2) Village Wanderer
    b2 = eg.wanderer_b2()[0]
    b = eg.wanderer('B')[0]
    old = committed(EGG.format(m='simplewanderers', n='village_wanderer'))
    sec.append(f'''<section><h2>Dorf-Wanderer-Ei <span class="tag in">eingebaut</span></h2>
<p class="note">Besitzer 23.10.: B, aber Hut besser und mit Nase. Neu: Filzhut in den Farben des Mobs (Ocker-Krone, Rostband, breite Krempe mit
Rostkante und dunkler Unterseite, steht seitlich über), Villager-Nase zwischen den Augen bis in den Bart (links Licht, rechts Schatten).</p>
<div class="row">{fig(b2, 10, 'neu (eingebaut)')}{fig(b, 10, 'B vom 23.10. (Vorschlag)')}{fig(old, 10, 'alt') if old else ''}
<figure><span class="slot"><img class="px" src="{uri(b2)}" width="16" height="16" alt=""></span><span class="slot"><img class="px" src="{uri(b2)}" width="32" height="32" alt=""></span><figcaption>1× / 2×</figcaption></figure></div>
<div class="row van">{''.join(fig(im, 6, 'Vanilla ' + n) for n, im in van(['villager_spawn_egg', 'wandering_trader_spawn_egg', 'witch_spawn_egg']))}</div></section>''')

    # 3) Other eggs, style B candidates
    cells = []
    for key, (title, fn, rel, refs) in eg.CANDIDATES_B.items():
        new = fn()[0]
        cur = Image.open(ROOT / rel).convert('RGBA')
        vv = ''.join(fig(im, 4, 'Vanilla ' + n) for n, im in van([r + '_spawn_egg' for r in refs]))
        cells.append(f'<div class="card"><h3>{title}</h3><div class="row">{fig(cur, 6, "heute")}{fig(new, 6, "Stil B (neu)")}'
                     f'<figure><span class="slot"><img class="px" src="{uri(new)}" width="16" height="16" alt=""></span><figcaption>1×</figcaption></figure></div>'
                     f'<div class="row van">{vv}</div>'
                     f'<label><input type="checkbox" name="ei" value="{key}" checked> passt</label></div>')
    sec.append(f'''<section><h2>Übrige 7 eigene Eier im Stil B <span class="tag cand">Vorschau, nicht eingebaut</span></h2>
<p class="note">Wie die neuen Vanilla-Eier: Mob-Motiv auf der Vanilla-Eiform, Licht oben links, Kontur im dunkelsten Ton des Materials;
kleine Mobs (Nibblemite, Dragonmite) auf der kleinen Eiform wie die Endermite. Farben aus den Mob-Texturen.</p>
<div class="grid">{''.join(cells)}</div></section>''')

    # 4) VN12 built in
    handle = cru.handle_vn12('C')
    old_h = committed('modules/simplecrucibles/shared/resources/assets/simplecrucibles/textures/block/iron_crucible_handle.png')
    rim = jar.tex('block/cauldron_top')
    milk = [san.milk_vn12(i, 'B') for i in range(4)]
    old_m = [committed(f'modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/block/milk_cauldron/milk_{i}.png') for i in range(4)]
    side, top = san.cheese_side_vn12('B'), san.cheese_top_vn12('B')
    old_s = committed('modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/block/cheese_block_side.png')
    old_t = committed('modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/block/cheese_block_top.png')
    sec.append(f'''<section><h2>VN12: Tiegelgriff C, Milch-Kessel B, Käse B <span class="tag in">eingebaut</span></h2>
<div class="grid">
<div class="card"><h3>Tiegelgriff Eisen – C geschmiedet</h3><div class="row">{fig(handle, 8, 'neu')}{fig(vp.crucible_iso(handle, 8), 1, 'Tiegel (Modell)', 'px plain')}{fig(handle.crop((6, 5, 10, 7)), 16, 'sichtbarer Griff')}</div>
<div class="row">{fig(old_h, 4, 'alt') if old_h else ''}{fig(vp.crucible_iso(old_h, 4), 1, 'alt im Modell', 'px plain') if old_h else ''}</div></div>
<div class="card"><h3>Milch-Kessel Stufe 0–3 – B Rahmhaut</h3><div class="row">{''.join(fig(vp.in_cauldron(m, rim), 6, f'Stufe {i}') for i, m in enumerate(milk))}</div>
<div class="row">{''.join(fig(vp.in_cauldron(m, rim), 3, f'alt {i}') for i, m in enumerate(old_m) if m)}</div></div>
<div class="card"><h3>Käse – B Emmentaler (Seite + Oben)</h3><div class="row">{fig(side, 6, 'Seite')}{fig(top, 6, 'Oben (neu passend)')}{fig(vp.cube_iso(top, side, 6), 1, 'Block', 'px plain')}</div>
<div class="row">{fig(vp.tile(side), 3, 'Wand 3×3')}{fig(vp.slot(vp.cube_iso(top, side, 2)), 4, 'Slot')}{fig(vp.cube_iso(old_t, old_s, 4), 1, 'alt', 'px plain') if old_s and old_t else ''}</div></div>
</div></section>''')

    # 5) Butter inner + Piggy (choices)
    old_b = Image.open(SAN / 'butter_block_inner.png').convert('RGBA')
    b_top, b_side = (Image.open(SAN / f'butter_block_{n}.png').convert('RGBA') for n in ('top', 'side'))
    bcards = [f'<div class="card"><h3>heute</h3><div class="row">{fig(old_b, 6, "Butter innen")}{fig(vp.cube_iso(old_b, b_side, 4), 1, "Schnitt oben", "px plain")}</div></div>']
    for v, name in (('A', 'Schnittfläche, zwei Messerzüge'), ('B', 'glatt, schräge Messerspuren'), ('C', 'Butterlocken')):
        im = san.butter_inner_vn12(v)
        bcards.append(f'<div class="card"><h3><label><input type="radio" name="Butter" value="{v}"> {v} – {name}</label></h3>'
                      f'<div class="row">{fig(im, 6, "16×")}{fig(vp.cube_iso(im, b_side, 4), 1, "Schnitt oben", "px plain")}{fig(vp.tile(im), 2, "3×3")}</div></div>')
    pig_old = Image.open(ROOT / PIG).convert('RGBA')
    pcards = [f'<div class="card"><h3>heute</h3><div class="row">{fig(pig_old, 6, "Piggy")}<figure><span class="slot"><img class="px" src="{uri(pig_old)}" width="18" height="18" alt=""></span><figcaption>1×</figcaption></figure></div></div>']
    for v in 'ABC':
        im = piggy(v)
        pcards.append(f'<div class="card"><h3><label><input type="radio" name="Piggy" value="{v}"> {v} – {PIGGY[v][0]}</label></h3>'
                      f'<div class="row">{fig(im, 6, "16×")}<figure><span class="slot"><img class="px" src="{uri(im)}" width="18" height="18" alt=""></span><figcaption>1×</figcaption></figure></div></div>')
    vanfx = ''.join(fig(im, 4, 'Vanilla ' + n) for n, im in van(['speed', 'regeneration', 'poison', 'hunger'], 'mob_effect'))
    vanpig = ''.join(fig(im.crop((0, 0, 64, 32)) if im.height >= 32 else im, 2, 'Vanilla ' + n) for n, im in van(['pig/pig_temperate', 'pig/temperate_pig', 'pig/pig'], 'entity')[:1])
    sec.append(f'''<section><h2>Butter innen <span class="tag cand">Vorschau, Wahl offen</span></h2>
<p class="note">Im Stil des gewählten Käse B (Flecken statt Rauschen, Licht oben links, Butter-Rampe), ohne Löcher.</p><div class="grid">{''.join(bcards)}</div>
<div class="row van">{''.join(fig(im, 4, 'Vanilla ' + n) for n, im in van(['cake_top', 'cake_inner', 'honeycomb_block'], 'block'))}</div></section>
<section><h2>Piggy-Effekt <span class="tag cand">Vorschau, Wahl offen</span></h2>
<p class="note">Wie die Vanilla-Effektsymbole: dunkle Kontur, 4–5 Töne der Schweine-Farbe (eigene Werte), Licht oben links.</p><div class="grid">{''.join(pcards)}</div>
<div class="row van">{vanfx}{vanpig}</div></section>''')

    # 6) Loose Ashwood leaves
    s = gt.WOODS[0]
    cur = gt.leaves_loose(s)
    alts = [('heute (Branch, ~46 %)', cur, None), ('L1 ~30 %', tx.leaves_loose_alt(s, 0.30), 'L1'), ('L2 ~20 %', tx.leaves_loose_alt(s, 0.20), 'L2')]
    med, dense = gt.leaves(s), gt.leaves_inner(s)
    lcards = []
    for name, im, key in alts:
        share = sum(1 for p in im.get_flattened_data() if p[3]) / 256
        head = f'<label><input type="radio" name="Laub" value="{key}"> {name}</label>' if key else name
        lcards.append(f'<div class="card"><h3>{head}</h3><p class="note">{share:.0%} deckend</p><div class="row">'
                      f'{fig(gt.tinted(im, tx.TINT), 6, "getönt")}{fig(tx.leaf_wall(im, med, dense), 3, "Kronen-Schnitt (außen locker)", "px plain")}'
                      f'{fig(tx.tree(gt.ashwood_log() if hasattr(gt, "ashwood_log") else Image.open(gt.TEX / "block/ashwood_log.png").convert("RGBA"), [im, med, dense]), 3, "Baum", "px plain")}</div></div>')
    vl = []
    for n in ('oak_leaves', 'azalea_leaves', 'cherry_leaves'):
        im = jar.tex(f'block/{n}')
        if im is not None:
            vl.append(fig(vp.tint(im, (0x48, 0xb5, 0x18)) if n == 'oak_leaves' else im, 4, 'Vanilla ' + n))
    sec.append(f'''<section><h2>Aschholz-Laub locker <span class="tag cand">Vorschau, Wahl offen</span></h2>
<p class="note">Besitzer 23.10.: für die lockerste Stufe eine weniger dichte Alternative. Gleiche Blattlinsen, weniger und etwas kleiner.
Mittel/dicht und die übrigen Endholz-Teile bleiben wie im Branch (Rinden-Wahl A/B/C steht noch aus).</p>
<div class="grid">{''.join(lcards)}</div><div class="row van">{''.join(vl)}</div></section>''')

    html = f'''<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Texturen TX23</title><style>
:root{{--bg:#f6f5f2;--fg:#1d1d1f;--mut:#5c5c62;--card:#fff;--line:#d9d7d2;--in:#1f7a3a;--cand:#9a5b00}}
@media (prefers-color-scheme:dark){{:root:not([data-theme="light"]){{--bg:#18181b;--fg:#ececee;--mut:#a3a3ab;--card:#232327;--line:#3a3a40;--in:#5fd38a;--cand:#f0b45a}}}}
:root[data-theme="dark"]{{--bg:#18181b;--fg:#ececee;--mut:#a3a3ab;--card:#232327;--line:#3a3a40;--in:#5fd38a;--cand:#f0b45a}}
body{{margin:0;padding:16px;background:var(--bg);color:var(--fg);font:15px/1.45 system-ui,sans-serif}}
main{{max-width:1200px;margin:auto}} h1{{font-size:1.4rem}} h2{{font-size:1.15rem;margin:0 0 6px}} h3{{font-size:1rem;margin:0 0 6px}}
section{{background:var(--card);border:1px solid var(--line);border-radius:10px;padding:12px;margin:14px 0;min-width:0}}
.grid{{display:grid;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));gap:12px}} .card{{border:1px solid var(--line);border-radius:8px;padding:10px;min-width:0}}
.row{{display:flex;flex-wrap:wrap;gap:10px;align-items:flex-end;margin:6px 0}} .van figcaption{{font-style:italic}}
figure{{margin:0;text-align:center;max-width:100%}} figcaption{{font-size:12px;color:var(--mut)}}
img.px{{image-rendering:pixelated;max-width:100%;height:auto;background:#8b8b8b}} img.plain{{background:transparent}}
.slot{{display:inline-block;background:#8b8b8b;border:2px solid;border-color:#373737 #fff #fff #373737;padding:1px;margin:0 4px;line-height:0}}
.note{{color:var(--mut);margin:4px 0 8px}} .tag{{font-size:12px;padding:2px 6px;border-radius:6px;border:1px solid}} .in{{color:var(--in)}} .cand{{color:var(--cand)}}
#code{{position:sticky;top:0;z-index:2;background:var(--card);border:1px solid var(--line);border-radius:10px;padding:10px;display:flex;gap:8px;flex-wrap:wrap;align-items:center}}
button{{font:inherit;padding:4px 10px}} code{{font-size:14px;overflow-wrap:anywhere}}
</style></head><body><main>
<h1>Texturen TX23 – Besitzer-Entscheidungen 23.10.</h1>
<p class="note">Eingebaut (grün): Shellker-Ei animiert, Dorf-Wanderer-Ei, Tiegelgriff C, Milch-Kessel B, Käse B. Offen (orange): die übrigen 7 Eier,
Butter innen, Piggy-Effekt, lockeres Aschholz-Laub. Alles eigene Pixel in Ebenen; Vanilla-Bilder (kursiv) nur auf dieser Seite.</p>
<div id="code"><span>Antwort:</span><code id="c"></code><button id="cp">Kopieren</button></div>
{''.join(sec)}
</main><script>
function upd(){{const ei=[...document.querySelectorAll('input[name=ei]')].filter(e=>!e.checked).map(e=>e.value);
const r=k=>{{const e=document.querySelector('input[name="'+k+'"]:checked');return e?e.value:'?'}};
document.getElementById('c').textContent='TX23 Eier='+(ei.length?'ok außer '+ei.join(','):'ok')+' Butter='+r('Butter')+' Piggy='+r('Piggy')+' Laub='+r('Laub');}}
document.querySelectorAll('input').forEach(e=>e.addEventListener('change',upd));upd();
document.getElementById('cp').onclick=()=>{{try{{navigator.clipboard.writeText(document.getElementById('c').textContent)}}catch(e){{}}}};
</script></body></html>'''
    Path(a.out).parent.mkdir(parents=True, exist_ok=True)
    Path(a.out).write_text(html, encoding='utf-8')
    print('preview:', a.out, f'({len(html) // 1024} KB)')


if __name__ == '__main__':
    main()
