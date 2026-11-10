"""The two book pages of the wiki (N24, docs/ai/PLAN-B-WIKI24-2026-11-10.md), generated with every wiki run.

  data/guidebook.js    the Simple Modpack guide as the game builds it (SimpleLib ModpackGuide): every mod's
                       contribution assets/<ns>/simplelib/modpack_guide.json with its chapters and their pages
                       (lang <page_key>.<n>.title / <page_key>.<n>, en_us + de_de). The page groups contributions into
                       sections like ModpackGuide.sections, for the mods of the chosen modpack. SimpleBuilding's own
                       book screen (its section has "screen": true) comes from GuideBooks/GuideContent: topics with
                       title, intro and chapters (title, text, icon, items, recipes).
  data/questionary.js  Simple Questionary's texts, assets/<ns>/questionary/<lang>.json of every namespace: own entries
                       (lore / use / secret), families (item tag or path regex) and the fallback variants.

Item names (en/de) for every id the pages show: the mods' lang files, Vanilla from the 26.3 client jar and Loom's
asset index. Without jar or index the committed files stay as they are (like wiki/enchanting.py).
"""
import json
import re
import zipfile
from pathlib import Path

GUIDE_TARGET = 'data/guidebook.js'
QUESTIONARY_TARGET = 'data/questionary.js'
KEYBINDS = {'%1$s': 'K', '%2$s': 'B'}   # default keys of the two keybinds every SimpleBuilding chapter text gets


def _read(path):
    return json.loads(path.read_text(encoding='utf-8'))


def _lang_dirs(repo, entries):
    """namespace -> [lang dirs], later wins (module lang dir; SimpleBuilding: main resources, then the 26.3 overlay)."""
    out = {}
    for e in entries:
        ns = e.get('namespace', e['id'])
        if e['id'] == 'simplebuilding':
            out[ns] = [repo / 'src/main/resources/assets/simplebuilding/lang', repo / 'mc26_3/overlay/resources/assets/simplebuilding/lang']
        else:
            out[ns] = [repo / e['paths']['lang']]
    return out


def _langs(repo, entries):
    """{'en_us': {...}, 'de_de': {...}} of every mod together (keys carry their namespace)."""
    tables = {'en_us': {}, 'de_de': {}}
    for dirs in _lang_dirs(repo, entries).values():
        for d in dirs:
            for locale in tables:
                path = d / (locale + '.json')
                if path.is_file():
                    tables[locale].update(_read(path))
    return tables


def _resource_roots(repo, entries):
    """[(namespace, resources dir)] of every mod: shared resources, loader resources, generated."""
    out = []
    for e in entries:
        if e['id'] == 'simplebuilding':
            for r in ('src/main/resources', 'mc26_3/overlay/resources', 'src/main/generated', 'mc26_3/generated'):
                out.append((e['id'], repo / r))
            continue
        p = e['paths']
        out.append((e['id'], repo / p['shared'] / 'resources'))
        for loader in e['loaders']:
            if p.get(loader):
                out.append((e['id'], repo / p[loader] / 'src/main/resources'))
        out.append((e['id'], repo / p['generated']))
    return out


def _both(lang, key, fallback=None):
    en = lang['en_us'].get(key)
    de = lang['de_de'].get(key)
    if en is None and de is None:
        return None if fallback is None else {'en': fallback, 'de': fallback}
    return {'en': en if en is not None else de, 'de': de if de is not None else en}


# ---------------------------------------------------------------------------
# guide
# ---------------------------------------------------------------------------

def _mc_constants(repo):
    src = (repo / 'mc26_3/overlay/java/com/simplebuilding/version/McVersion.java').read_text(encoding='utf-8')
    strings = dict(re.findall(r'public static final String (\w+) = "([^"]*)";', src))
    flags = {k: v == 'true' for k, v in re.findall(r'public static final boolean (\w+) = (true|false);', src)}
    return strings, flags


def _java_args(text, start):
    """Arguments of the call whose '(' is at text[start]: (list of raw args, index after ')')."""
    depth, cur, args, i = 0, '', [], start
    while i < len(text):
        ch = text[i]
        if ch == '"':
            j = text.index('"', i + 1)
            cur += text[i:j + 1]
            i = j + 1
            continue
        if ch in '([{':
            depth += 1
            if depth > 1:
                cur += ch
        elif ch in ')]}':
            depth -= 1
            if depth == 0:
                args.append(cur.strip())
                return args, i + 1
            cur += ch
        elif ch == ',' and depth == 1:
            args.append(cur.strip())
            cur = ''
        else:
            cur += ch
        i += 1
    return args, i


def _ids(expr):
    """All "ns:path" string literals of an expression, in order."""
    return re.findall(r'"([a-z0-9_.-]+:[a-z0-9_./#@:-]+)"', expr)


def _guide_content(repo):
    """{book enum: [chapter dict(icon, recipes, items)]} from GuideContent.java for 26.3 (best effort, file order)."""
    path = repo / 'common/src/shared/java/com/simplebuilding/guide/GuideContent.java'
    if not path.is_file():
        return {}
    strings, flags = _mc_constants(repo)
    src = re.sub(r'//[^\n]*', '', path.read_text(encoding='utf-8'))
    src = re.sub(r'(?:com\.simplebuilding\.version\.)?McVersion\.(\w+_NAMESPACE)', lambda m: '"' + strings.get(m.group(1), m.group(1)) + '"', src)
    src = re.sub(r'"\s*\+\s*"', '', src)
    # 26.3: the trim templates are Simple Trims' (TRIMS_IN_SB false, the module in the pack)
    src = re.sub(r'trimTemplate\("(\w+)"\)', r'"simpletrims:\1_trim_template"', src)
    src = re.sub(r'trimRecipes\("([\w/]+)"\)', r'List.of("simpletrims:\1")', src)
    # blocks behind a false McVersion flag are left out
    out_src, i = '', 0
    for m in re.finditer(r'if \((?:com\.simplebuilding\.version\.)?McVersion\.(\w+)\)\s*\{', src):
        if m.start() < i:
            continue
        if flags.get(m.group(1), True):
            continue
        depth, j = 0, m.end() - 1
        while j < len(src):
            depth += {'{': 1, '}': -1}.get(src[j], 0)
            j += 1
            if depth == 0:
                break
        out_src += src[i:m.start()]
        i = j
    src = out_src + src[i:]
    books, current = {}, None
    skip_next = False
    for m in re.finditer(r'STYLES\.put\(GuideBooks\.Book\.([A-Z_]+)|GuideBooks\.Book book = GuideBooks\.Book\.([A-Z_]+)|'
                         r'addChapter\(GuideBooks\.Book\.([A-Z_]+)|for \(GuideBooks\.Book book :|\bch\(', src):
        if m.group(1) or m.group(2):
            current = m.group(1) or m.group(2)
            continue
        if m.group(3):
            current = m.group(3)
            continue
        if m.group(0).startswith('for'):
            current = None                 # a loop over several books: chapters there cannot be placed
            continue
        if skip_next:
            skip_next = False
            continue
        args, end = _java_args(src, m.end() - 1)
        before = src[:m.start()].rstrip()
        if len(args) != 3 or current is None or before.endswith('static Chapter'):
            continue
        if before.endswith('?') and re.match(r'\s*:\s*ch\(', src[end:]):
            skip_next = True               # TOOLS_BUCKETS ? ch(..) : ch(..): Simple Tools is in the full pack
        icon = (_ids(args[0]) or [None])[0]
        books.setdefault(current, []).append({'icon': icon, 'recipes': _ids(args[1]), 'items': _ids(args[2])})
    return books


def _sb_book(repo, lang):
    """SimpleBuilding's own guide (section screen): the topic books in enum order with their chapters."""
    path = repo / 'common/src/shared/java/com/simplebuilding/guide/GuideBooks.java'
    if not path.is_file():
        return None
    enum = re.findall(r'^\s+([A-Z_]+)\("([a-z_]+)", "([a-z_]+)", Shelf\.MOD\)', path.read_text(encoding='utf-8'), re.M)
    content = _guide_content(repo)
    topics = []
    for name, book, _item in enum:
        base = 'book.simplebuilding.' + book
        chapters, n = [], 1
        parsed = content.get(name, [])
        while (base + f'.{n}.title') in lang['en_us']:
            info = parsed[n - 1] if n - 1 < len(parsed) else {}
            text = _both(lang, base + f'.{n}.text', '')
            icon = info.get('icon')
            for locale in ('en', 'de'):
                icon_name = (_item_name(lang, icon, locale) if icon else '') or ''
                text[locale] = re.sub(r'%(\d)\$s', lambda m: KEYBINDS.get(m.group(0), icon_name), text[locale] or '')
            chapter = {'title': _both(lang, base + f'.{n}.title'), 'text': text}
            if info:
                chapter.update(icon=icon, recipes=[r.split('@')[-1] if '@' in r else r for r in info['recipes']],
                               items=[i.split('#')[0] for i in info['items']])
            chapters.append(chapter)
            n += 1
        topic = {'id': book, 'title': _both(lang, base + '.title', book), 'chapters': chapters}
        intro = _both(lang, base + '.intro')
        if intro:
            topic['intro'] = intro
        if name == 'ADMIN':
            topic['operatorOnly'] = True
        topics.append(topic)
    return {'title': _both(lang, 'book.simplebuilding.guide.title', 'Guide'), 'topics': topics}


def _item_name(lang, item_id, locale):
    if not item_id or ':' not in item_id:
        return None
    ns, p = item_id.split(':', 1)
    table = lang['en_us' if locale == 'en' else 'de_de']
    for kind in ('item', 'block'):
        key = f'{kind}.{ns}.{p}'
        if key in table:
            return table[key]
    return None


def guide_data(repo, entries, lang):
    contributions = []
    module_of = {e.get('modId', e.get('namespace', e['id'])): e['id'] for e in entries}
    module_of.update({e.get('namespace', e['id']): e['id'] for e in entries})
    seen = set()
    for mid, root in _resource_roots(repo, entries):
        for path in sorted(root.glob('assets/*/simplelib/modpack_guide.json')):
            ns = path.parts[-3]
            if ns in seen:
                continue
            seen.add(ns)
            data = _read(path)
            chapters = []
            for c in data.get('chapters', []):
                pages = []
                for n in range(1, int(c.get('pages', 0)) + 1):
                    pages.append({'title': _both(lang, f"{c['page_key']}.{n}.title"), 'text': _both(lang, f"{c['page_key']}.{n}", '')})
                chapter = {'id': c['id'], 'title': _both(lang, c['title'], c['id']), 'icon': c.get('icon'), 'pages': pages}
                for key in ('advancement', 'hint'):
                    if c.get(key):
                        chapter[key] = c[key]
                chapters.append(chapter)
            contribution = {'namespace': ns, 'module': module_of.get(ns, mid), 'section': data.get('section', ns),
                            'name': _both(lang, data['name'], ns), 'icon': data.get('icon'), 'order': data.get('order', 100),
                            'chapters': chapters}
            if data.get('screen'):
                contribution['screen'] = True
            contributions.append(contribution)
    contributions.sort(key=lambda c: c['namespace'])
    book = _sb_book(repo, lang)
    return {'contributions': contributions, 'simplebuilding': book,
            'ui': {k: _both(lang, 'book.simplebuilding.' + k) for k in ('back',) if _both(lang, 'book.simplebuilding.' + k)}}


# ---------------------------------------------------------------------------
# questionary
# ---------------------------------------------------------------------------

def _pages(value):
    return {page: ([value[page]] if isinstance(value[page], str) else list(value[page]))
            for page in ('lore', 'use', 'secret') if page in value}


def questionary_data(repo, entries):
    """{namespaces: {ns: {module, entries: {path: {en, de}}, families: [...]}}, fallback: {...}} (later file wins per key)."""
    out, fallback = {}, {}
    for mid, root in _resource_roots(repo, entries):
        for path in sorted(root.glob('assets/*/questionary/*.json')):
            ns, locale = path.parts[-3], path.stem
            short = {'en_us': 'en', 'de_de': 'de'}.get(locale)
            if short is None:
                continue
            target = out.setdefault(ns, {'module': mid, 'entries': {}, 'families': {}})
            for key, value in _read(path).items():
                if key.startswith('_') or not isinstance(value, dict):
                    continue
                if key == 'fallback':
                    for cls, pages in value.items():
                        if isinstance(pages, dict):
                            fallback.setdefault(cls, {})[short] = _pages(pages)
                    continue
                if key.startswith('family:'):
                    fam = target['families'].setdefault(key[len('family:'):], {'items': value.get('items', [])})
                    fam[short] = _pages(value)
                    continue
                target['entries'].setdefault(key, {})[short] = _pages(value)
    for ns in out.values():
        ns['entries'] = dict(sorted(ns['entries'].items()))
        ns['families'] = [dict(id=k, **v) for k, v in sorted(ns['families'].items())]
    return {'namespaces': dict(sorted(out.items())), 'fallback': fallback}


# ---------------------------------------------------------------------------
# names + sync
# ---------------------------------------------------------------------------

def _vanilla_names(g):
    """(en, de) Vanilla lang tables of 26.3, or None without the client jar / asset index."""
    import enchanting
    version = g.LINES['26.3']['client_jar_version']
    jar = g.client_jar(version)
    german = enchanting.vanilla_german(version)
    if not jar.exists() or german is None:
        return None
    with zipfile.ZipFile(jar) as archive:
        english = json.loads(archive.read('assets/minecraft/lang/en_us.json'))
    return english, german


def _collect_ids(value, out):
    if isinstance(value, dict):
        for k, v in value.items():
            if k in ('icon', 'hint') and isinstance(v, str):
                out.add(v)
            elif k in ('items', 'recipes') and isinstance(v, list):
                out.update(x for x in v if isinstance(x, str) and ':' in x and not x.startswith(('#', 'regex:')))
            else:
                _collect_ids(v, out)
    elif isinstance(value, list):
        for v in value:
            _collect_ids(v, out)


def _names(ids, lang):
    names = {}
    for item_id in sorted(ids):
        en, de = _item_name(lang, item_id, 'en'), _item_name(lang, item_id, 'de')
        if en or de:
            names[item_id] = {'en': en or de, 'de': de or en}
    return names


def sync(g, check=False):
    """Writes (or with check compares) data/guidebook.js and data/questionary.js; problems as strings."""
    import modules as module_wiki
    vanilla = _vanilla_names(g)
    targets = [g.WIKI / GUIDE_TARGET, g.WIKI / QUESTIONARY_TARGET]
    if vanilla is None:
        missing = [t for t in targets if not t.exists()]
        return [f'wiki/{t.relative_to(g.WIKI).as_posix()} is missing and there is no client jar/asset index' for t in missing]
    entries = module_wiki.discover(g.REPO)
    lang = _langs(g.REPO, entries)
    for locale, table in zip(('en_us', 'de_de'), vanilla):
        lang[locale] = {**table, **lang[locale]}
    guide = guide_data(g.REPO, entries, lang)
    quest = questionary_data(g.REPO, entries)
    ids = set()
    _collect_ids(guide, ids)
    for ns, value in quest['namespaces'].items():
        ids |= {f'{ns}:{p}' for p in value['entries']}
    guide['names'] = _names({i for i in ids if i in ids}, lang)
    quest['names'] = _names({f'{ns}:{p}' for ns, v in quest['namespaces'].items() for p in v['entries']}, lang)
    problems = []
    for target, var, payload in ((targets[0], 'WIKI_GUIDEBOOK', guide), (targets[1], 'WIKI_QUESTIONARY', quest)):
        text = ('// Generated by wiki/books.py - do not edit.\n'
                f'window.{var} = ' + json.dumps(payload, separators=(',', ':'), ensure_ascii=False, sort_keys=False) + ';\n')
        if check:
            if not target.exists() or target.read_text(encoding='utf-8') != text:
                problems.append(f'wiki/{target.relative_to(g.WIKI).as_posix()} is OUT OF DATE (python wiki/generate.py --all)')
        else:
            g.write_atomic(target, text)
    return problems
