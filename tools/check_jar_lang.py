#!/usr/bin/env python3
"""Lang keys of the release jars (O8 audit 7): every key the jars use must have an en_us AND a de_de text in the jars.

Reads every *.jar in DIR (nested META-INF/jars too; the game merges the lang files of all loaded mods) and collects
  used keys  - registry names: assets/<ns>/items/<id>.json -> item.<ns>.<id> or block.<ns>.<id>; blockstates -> block.<ns>.<id>
             - JSON assets/data: every "translate" / "fallback"-less translate key, sounds.json "subtitle",
               damage_type "message_id" -> death.attack.<id> (+ .player), enchantment/trim/painting/jukebox "description"
             - class files: string constants that look like a key of an own namespace (prefix.simple<mod>.rest or
               simple<mod>.rest), without a trailing dot (dynamic prefixes are skipped)
  texts      - assets/*/lang/en_us.json, de_de.json of all jars (+ Vanilla en_us from the client jar, optional --vanilla)
and reports: used key missing in en_us / de_de, key only in one of en_us/de_de, values with a literal backslash-n
("\\n" shown as text), values equal to their key, empty values, %-argument count differing between en and de.
Class constants are heuristic (a constant may be no lang key): reported apart as "class".
Usage: python3 tools/check_jar_lang.py DIR [--vanilla client.jar] [--json out.json]
Exit 1 on a missing JSON key, a key in only one language, a literal backslash-n, broken lang JSON or differing %-arguments;
registry names (internal placed_/held_ blocks have none by design) and class constants are only listed.
"""
from __future__ import annotations

import argparse
import collections
import glob
import io
import json
import os
import re
import struct
import sys
import zipfile

KEY = re.compile(r'^[a-z_]+(?:\.[a-z0-9_\-]+){1,8}$')
ARGS = re.compile(r'%(?:\d+\$)?[sdf%]')
DIRECTION_ONLY_IDS = re.compile(r'_(?:wall|stem|head|piston_head|arm)$')


def strings_of_class(data: bytes) -> list[str]:
    """UTF8 constants of a class file (constant pool walk)."""
    out = []
    try:
        if data[:4] != b'\xca\xfe\xba\xbe':
            return out
        n = struct.unpack('>H', data[8:10])[0]
        i, k = 10, 1
        while k < n:
            tag = data[i]
            if tag == 1:
                ln = struct.unpack('>H', data[i + 1:i + 3])[0]
                out.append(data[i + 3:i + 3 + ln].decode('utf-8', 'replace'))
                i += 3 + ln
            elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
                i += 5
            elif tag in (5, 6):
                i += 9
                k += 1
            elif tag in (7, 8, 16, 19, 20):
                i += 3
            elif tag == 15:
                i += 4
            else:
                break
            k += 1
    except (IndexError, struct.error):
        pass
    return out


class Jars:
    def __init__(self):
        self.en, self.de = {}, {}
        self.used = collections.defaultdict(set)        # key -> {where}
        self.alt = []                                   # [(where, {keys}) ] any one of them is enough
        self.class_used = collections.defaultdict(set)
        self.namespaces = set()

    def walk(self, data: bytes, where: str, depth=0):
        z = zipfile.ZipFile(io.BytesIO(data))
        for info in z.infolist():
            n = info.filename
            if info.is_dir():
                continue
            if n.startswith('META-INF/jars/') and n.endswith('.jar') and depth < 3:
                self.walk(z.read(info), f'{where}!{os.path.basename(n)}', depth + 1)
                continue
            m = re.match(r'assets/([a-z0-9_.-]+)/lang/(en_us|de_de)\.json$', n)
            if m:
                self.namespaces.add(m.group(1))
                try:
                    d = json.loads(z.read(info).decode('utf-8-sig'))
                except ValueError as e:
                    self.used[f'!broken-json {n}'].add(where)
                    continue
                (self.en if m.group(2) == 'en_us' else self.de).update({k: (v, f'{where}!{n}') for k, v in d.items()})
                continue
            m = re.match(r'assets/([a-z0-9_.-]+)/items/([a-z0-9_/.-]+)\.json$', n)
            if m and m.group(1) != 'minecraft':
                ns, i = m.group(1), m.group(2)
                self.alt.append((f'{where}!{n}', {f'item.{ns}.{i}', f'block.{ns}.{i}'}))
            m = re.match(r'assets/([a-z0-9_.-]+)/blockstates/([a-z0-9_/.-]+)\.json$', n)
            if m and m.group(1) != 'minecraft' and not DIRECTION_ONLY_IDS.search(m.group(2)):
                self.alt.append((f'{where}!{n}', {f'block.{m.group(1)}.{m.group(2)}'}))
            if n.endswith('.json'):
                try:
                    d = json.loads(z.read(info).decode('utf-8-sig'))
                except ValueError:
                    continue
                self.json_keys(d, f'{where}!{n}', n)
            elif n.endswith('.class'):
                for s in strings_of_class(z.read(info)):
                    if KEY.match(s) and not s.endswith('.'):
                        self.class_used[s].add(f'{where}!{n}')

    def json_keys(self, d, where, name):
        if re.match(r'data/[^/]+/damage_type/', name) and isinstance(d, dict) and isinstance(d.get('message_id'), str):
            mid = d['message_id']
            self.used[f'death.attack.{mid}'].add(where)
            return
        if name.endswith('sounds.json') and isinstance(d, dict):
            for ev in d.values():
                if isinstance(ev, dict) and isinstance(ev.get('subtitle'), str):
                    self.used[ev['subtitle']].add(where)
            return
        stack = [d]
        while stack:
            o = stack.pop()
            if isinstance(o, dict):
                t = o.get('translate')
                if isinstance(t, str) and 'fallback' not in o:
                    self.used[t].add(where)
                stack.extend(o.values())
            elif isinstance(o, list):
                stack.extend(o)


def main(argv):
    ap = argparse.ArgumentParser()
    ap.add_argument('dir')
    ap.add_argument('--vanilla')
    ap.add_argument('--json')
    a = ap.parse_args(argv)
    j = Jars()
    jars = sorted(glob.glob(os.path.join(a.dir, '*.jar')))
    for p in jars:
        j.walk(open(p, 'rb').read(), os.path.basename(p))
    vanilla = {}
    if a.vanilla:
        vanilla = json.loads(zipfile.ZipFile(a.vanilla).read('assets/minecraft/lang/en_us.json'))
    own = {ns for ns in j.namespaces if ns != 'minecraft'}
    own_re = re.compile(r'(?:^|\.)(' + '|'.join(sorted(map(re.escape, own))) + r')(?:\.|$)')
    res = collections.defaultdict(list)

    def have(k, lang):
        return k in (j.en if lang == 'en' else j.de) or (k in vanilla)

    for k, where in sorted(j.used.items()):
        if k.startswith('!broken-json'):
            res['broken_json'].append([k, sorted(where)[0]])
            continue
        for lang in ('en', 'de'):
            if not have(k, lang):
                res[f'missing_{lang}'].append([k, sorted(where)[0]])
    for where, keys in j.alt:
        for lang in ('en', 'de'):
            if not any(have(k, lang) for k in keys):
                res[f'missing_{lang}_registry'].append([sorted(keys)[0], where])
    for k, where in sorted(j.class_used.items()):
        if not own_re.search(k) or k in vanilla:
            continue
        for lang in ('en', 'de'):
            if not have(k, lang):
                res[f'class_missing_{lang}'].append([k, sorted(where)[0]])
    for k in sorted(set(j.en) - set(j.de)):
        res['only_en'].append([k, j.en[k][1]])
    for k in sorted(set(j.de) - set(j.en)):
        res['only_de'].append([k, j.de[k][1]])
    for lang, d in (('en', j.en), ('de', j.de)):
        for k, (v, where) in sorted(d.items()):
            if not isinstance(v, str):
                res['non_string'].append([k, where])
                continue
            if '\\n' in v:
                res[f'literal_backslash_n_{lang}'].append([k, where])
            if v.strip() == '' and not k.endswith(('.empty', '.blank', '.spacer')):
                res[f'empty_{lang}'].append([k, where])
            if v == k:
                res[f'value_is_key_{lang}'].append([k, where])
    for k in sorted(set(j.en) & set(j.de)):
        a1, a2 = j.en[k][0], j.de[k][0]
        if isinstance(a1, str) and isinstance(a2, str):
            c1 = sorted(x for x in ARGS.findall(a1) if x != '%%')
            c2 = sorted(x for x in ARGS.findall(a2) if x != '%%')
            if len(c1) != len(c2):
                res['arg_count_differs'].append([k, j.en[k][1]])
    print(f'jars {len(jars)}, en {len(j.en)}, de {len(j.de)}, used {len(j.used)} json keys + {len(j.alt)} registry names, '
          f'{len(j.class_used)} class constants')
    for cat in sorted(res):
        print(f'{cat}: {len(res[cat])}')
        for k, w in res[cat][:15]:
            print(f'   {k}   ({w})')
    if a.json:
        json.dump(res, open(a.json, 'w'), indent=1)
    # registry names of internal blocks (placed_*, held_*, standing_*) and class constants are reported, not failed
    hard = [c for c in res if c.startswith(('missing_', 'broken_json', 'literal_backslash', 'arg_count', 'non_string', 'only_'))
            and not c.endswith('_registry')]
    return 1 if any(res[c] for c in hard) else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
