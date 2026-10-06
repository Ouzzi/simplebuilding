"""Usage: python tools/chess_lang_2026_10_06.py

Schach (docs/ai/PLAN-SCHACH-2026-10-06.md): writes the English and German names of the octets, chess pieces and the
checker stairs/slabs (plus the JEI page and the test centre signs) into both lang trees (src/main/resources and the
26.3 overlay). Existing keys are replaced in place, new ones appended; idempotent.
"""
import json
import os
import re

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TREES = [os.path.join(REPO, 'src', 'main', 'resources', 'assets', 'simplebuilding', 'lang'),
         os.path.join(REPO, 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding', 'lang')]

# id, English, German (masculine prefix, feminine prefix), checker block id or None
COLOURS = [
    ('quartz', 'Quartz', 'Quarz-', 'Quarz-', None),
    ('purpur', 'Purpur', 'Purpur-', 'Purpur-', 'purpur_quartz_checker'),
    ('lapis', 'Lapis', 'Lapis-', 'Lapis-', 'lapis_quartz_checker'),
    ('blackstone', 'Blackstone', 'Schwarzstein-', 'Schwarzstein-', 'blackstone_quartz_checker'),
    ('resin', 'Resin', 'Harz-', 'Harz-', 'resin_quartz_checker'),
    ('nether_brick', 'Nether Brick', 'Netherziegel-', 'Netherziegel-', 'nether_brick_quartz_checker'),
    ('red_nether_brick', 'Red Nether Brick', 'Roter Netherziegel-', 'Rote Netherziegel-', 'red_nether_brick_quartz_checker'),
    ('nihilith', 'Nihilit', 'Nihilit-', 'Nihilit-', 'nihilith_quartz_checker'),
    ('astralit', 'Astralit', 'Astralit-', 'Astralit-', 'astralit_quartz_checker'),
    ('ender_quartz', 'Ender Quartz', 'Enderquarz-', 'Enderquarz-', 'ender_quartz_checker'),
    ('polished_astralit', 'Polished Astralit', 'Polierter Astralit-', 'Polierte Astralit-', 'polished_astralit_checker'),
    ('polished_nihilith', 'Polished Nihilit', 'Polierter Nihilit-', 'Polierte Nihilit-', 'polished_nihilith_checker'),
    ('polished_ender_quartz', 'Polished Ender Quartz', 'Polierter Enderquarz-', 'Polierte Enderquarz-', 'polished_ender_quartz_checker'),
]
# id, English, German, feminine
PIECES = [('pawn', 'Pawn', 'Bauer', False), ('rook', 'Rook', 'Turm', False), ('knight', 'Knight', 'Springer', False),
          ('bishop', 'Bishop', 'Läufer', False), ('queen', 'Queen', 'Dame', True), ('king', 'King', 'König', False)]

JEI_EN = ("Octets and chess pieces come from the stonecutter: a quartz checker (quartz: a quartz block) gives 8 octets of "
          "its color, one octet any piece. An octet goes next to the clicked one or onto the clicked half of a face; a cell "
          "holds up to 8 of one color and holds water until it is full. A piece stands on a quarter of a block top - one "
          "field of the checker below - so a block holds four, facing where you look. Sneak + right-click with a piece "
          "swaps it with the standing one, which goes to your hand. Sneak + empty hand picks a piece or an octet up, "
          "an empty hand turns a piece. Breaking drops everything in the cell.")
JEI_DE = ("Achtel und Schachfiguren kommen aus dem Steinmetz: ein Quarz-Schachbrett (Quarz: ein Quarzblock) ergibt 8 Achtel "
          "seiner Farbe, ein Achtel jede Figur. Ein Achtel kommt neben das angeklickte oder auf die angeklickte Hälfte einer "
          "Fläche; eine Zelle fasst bis zu 8 einer Farbe und hält Wasser, bis sie voll ist. Eine Figur steht auf einem "
          "Viertel einer Blockoberseite - einem Feld des Schachbretts darunter -, ein Block trägt also vier; sie schaut in "
          "deine Blickrichtung. Schleichen + Rechtsklick mit einer Figur tauscht sie gegen die stehende, die in deine Hand "
          "kommt. Schleichen + leere Hand nimmt eine Figur oder ein Achtel auf, die leere Hand dreht eine Figur. Abbauen "
          "droppt alles aus der Zelle.")

TC = [
    ('section.chess', 'Chess', 'Schach'),
    ('section.chess.sub', 'octets, pieces, checker stairs', 'Achtel, Figuren, Schachbrett-Treppen'),
    ('chess.place', 'Pieces', 'Figuren'),
    ('chess.place.sub', 'one per checker field', 'eine je Schachbrettfeld'),
    ('chess.place.sub2', 'empty hand: turn', 'leere Hand: drehen'),
    ('chess.swap', 'Swap', 'Tauschen'),
    ('chess.swap.sub', 'sneak + piece: replace', 'Schleichen + Figur: ersetzen'),
    ('chess.swap.sub2', 'sneak + hand: pick up', 'Schleichen + Hand: aufnehmen'),
    ('chess.octet', 'Octets', 'Achtel'),
    ('chess.octet.sub', 'placed where you click', 'sitzen, wo du klickst'),
    ('chess.octet.sub2', 'hold water until full', 'halten Wasser bis voll'),
]


def entries(lang_file, lang):
    out = {}
    with open(os.path.join(lang_file), encoding='utf-8') as f:
        existing = json.load(f)
    for cid, en, de_m, de_f, checker in COLOURS:
        out[f'item.simplebuilding.{cid}_octet'] = f'{en} Octet' if lang == 'en' else f'{de_m}Achtelblock'
        for flat in (False, True):
            for pid, pen, pde, fem in PIECES:
                key = f'item.simplebuilding.{cid}_chess_{pid}' + ('_flat' if flat else '')
                if lang == 'en':
                    out[key] = f'{en} {pen}' + (' (Flat)' if flat else '')
                else:
                    out[key] = (de_f if fem else de_m) + pde + (' (flach)' if flat else '')
        if checker:
            base = existing[f'block.simplebuilding.{checker}']
            for shape, en_s, de_s in (('stairs', ' Stairs', 'treppe'), ('slab', ' Slab', 'stufe')):
                if lang == 'en':
                    name = base + en_s
                else:
                    name = re.sub(r'^(Rot|Poliert)es ', r'\1e ', base) + de_s
                out[f'block.simplebuilding.{checker}_{shape}'] = name
                out[f'item.simplebuilding.{checker}_{shape}'] = name
    out['block.simplebuilding.checker_octet'] = 'Octet Cell' if lang == 'en' else 'Achtelzelle'
    out['block.simplebuilding.chess_pieces'] = 'Chess Pieces' if lang == 'en' else 'Schachfiguren'
    out['jei.simplebuilding.info.chess'] = JEI_EN if lang == 'en' else JEI_DE
    for key, en, de in TC:
        out['simplebuilding.testcentre.' + key] = en if lang == 'en' else de
    return out


def apply(path, new):
    with open(path, encoding='utf-8') as f:
        text = f.read()
    lines = text.rstrip().split('\n')
    assert lines[-1] == '}'
    body = lines[:-1]
    index = {}
    for i, line in enumerate(body):
        m = re.match(r'\s*"((?:[^"\\]|\\.)*)"\s*:', line)
        if m:
            index[json.loads('"' + m.group(1) + '"')] = i
    added = []
    for key, value in new.items():
        rendered = '  ' + json.dumps(key, ensure_ascii=False) + ': ' + json.dumps(value, ensure_ascii=False)
        if key in index:
            i = index[key]
            body[i] = rendered + (',' if body[i].rstrip().endswith(',') else '')
        else:
            added.append(rendered)
    if added:
        if not body[-1].rstrip().endswith(','):
            body[-1] = body[-1] + ','
        body.extend(a + ',' for a in added[:-1])
        body.append(added[-1])
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write('\n'.join(body + ['}']) + '\n')
    json.loads(open(path, encoding='utf-8').read())
    return len(added)


def main():
    for tree in TREES:
        for lang, name in (('en', 'en_us.json'), ('de', 'de_de.json')):
            path = os.path.join(tree, name)
            n = apply(path, entries(path, lang))
            print(f'{path}: {n} new keys')


if __name__ == '__main__':
    main()
