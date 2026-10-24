#!/usr/bin/env python3
"""Lang keys used in module Java must exist (REV2-A, 24.10.): every literal key in a translatable call of
modules/<mod>/{shared,fabric,clienttest} that names the module's own namespace needs an EN and a DE text in that
module's lang files (or any lang file of the repo, for shared keys). Vanilla keys (gui.done, container.chest) are not
checked - the client jar is not in the repo. Dynamic keys ("prefix." + suffix) are skipped. Exit 1 on a missing key.
Usage: check_lang_keys.py [module ...]"""
import glob
import json
import os
import re
import sys

R = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
CALL = re.compile(r'(?:translatable|translatableWithFallback|translatableEscape|I18n\.get|I18n\.exists)\(\s*"([A-Za-z0-9_.\-]+)"\s*[,)]')
SKIP = ('/build/', '/neoforge/', '/forge/', '/generated/')


def lang_keys(pattern):
    keys = {'en_us': set(), 'de_de': set()}
    for lang in keys:
        for path in glob.glob(os.path.join(R, pattern, f'{lang}.json'), recursive=True):
            if any(s in path.replace(os.sep, '/') for s in SKIP[:1]):
                continue
            with open(path, encoding='utf-8') as f:
                keys[lang] |= set(json.load(f))
    return keys


def namespaces(mod):
    found = {mod}
    for path in glob.glob(os.path.join(R, 'modules', mod, 'shared', 'resources', 'assets', '*', 'lang')):
        found.add(os.path.basename(os.path.dirname(path)))
    return found


def main(argv):
    every = lang_keys('**/lang')
    mods = argv or sorted(os.listdir(os.path.join(R, 'modules')))
    problems = []
    for mod in mods:
        own = namespaces(mod)
        for path in glob.glob(os.path.join(R, 'modules', mod, '**', '*.java'), recursive=True):
            norm = path.replace(os.sep, '/')
            if any(s in norm for s in SKIP):
                continue
            with open(path, encoding='utf-8') as f:
                src = f.read()
            for m in CALL.finditer(src):
                key = m.group(1)
                if key.endswith('.') or not any(ns in key.split('.') for ns in own):
                    continue
                for lang in ('en_us', 'de_de'):
                    if key not in every[lang]:
                        line = src[:m.start()].count('\n') + 1
                        problems.append(f'{os.path.relpath(path, R)}:{line}: {lang} has no "{key}"')
    for p in problems:
        print(p)
    if problems:
        return 1
    print(f'lang keys: every literal own-namespace key of {len(mods)} modules has EN and DE')
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
