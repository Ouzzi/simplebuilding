"""Wiki data the module extractor used to leave empty (N24, docs/ai/PLAN-B-WIKI24-2026-11-10.md).

Read straight from the module files, never by hand:
  config      every option the config screen shows: the paths of the lang keys text.autoconfig.<ns>.option.<path>
              (label, tooltip, group, tab), type and default from the module's config classes (Gson POJOs).
  in-world    data/<ns>/in_world_upgrades (SimpleLib's upgrade table: block + material + tool -> next tier) and
              data/<ns>/chisel_transformations (Simple Chisels' chains per tier and table), as in-world entries.
  commands    root commands the module registers in code (ConfigCommands.server/client, register(literal(..))).
"""
import json
import re
from pathlib import Path

# ---------------------------------------------------------------------------
# config
# ---------------------------------------------------------------------------

_COMMENT = re.compile(r'//[^\n]*|/\*.*?\*/', re.S)
_STRING = re.compile(r'"(?:\\.|[^"\\])*"')
_MODIFIERS = {'public', 'protected', 'private', 'static', 'final', 'transient', 'volatile'}


def _strip(code):
    """Comments out, string literals kept but neutral for brace counting (replaced by placeholders)."""
    strings = []

    def keep(match):
        strings.append(match.group(0))
        return f'"\u0000{len(strings) - 1}\u0000"'
    code = _STRING.sub(keep, code)
    code = _COMMENT.sub(' ', code)
    return code, strings


def _restore(text, strings):
    return re.sub(r'"\u0000(\d+)\u0000"', lambda m: strings[int(m.group(1))], text)


def _matching(code, start):
    """Index after the brace that closes the one at code[start]."""
    depth = 0
    for i in range(start, len(code)):
        if code[i] == '{':
            depth += 1
        elif code[i] == '}':
            depth -= 1
            if depth == 0:
                return i + 1
    return len(code)


def _split_top(text, sep=','):
    out, depth, cur = [], 0, ''
    for ch in text:
        if ch in '(<[{':
            depth += 1
        elif ch in ')>]}':
            depth -= 1
        if ch == sep and depth == 0:
            out.append(cur)
            cur = ''
        else:
            cur += ch
    out.append(cur)
    return out


def _fields(body, strings):
    """Fields declared directly in a class body: name -> (type, default text, static)."""
    fields, stmt, i = {}, '', 0
    while i < len(body):
        ch = body[i]
        if ch == '{':
            end = _matching(body, i)
            if '=' in stmt and '(' not in stmt.split('=', 1)[0]:
                stmt += body[i:end]          # array initializer: part of the field
                i = end
                continue
            stmt = ''                        # method, nested class or initializer block
            i = end
            continue
        if ch == ';':
            _field(stmt, strings, fields)
            stmt = ''
        else:
            stmt += ch
        i += 1
    return fields


def _field(stmt, strings, fields):
    stmt = re.sub(r'@[\w.]+(\s*\((?:[^()]|\([^()]*\))*\))?', ' ', stmt).strip()
    if not stmt or '(' in stmt.split('=', 1)[0]:
        return
    words, static, final = [], False, False
    for w in stmt.split():
        if w in _MODIFIERS:
            static |= w == 'static'
            final |= w == 'final'
            continue
        words.append(w)
    stmt = ' '.join(words)
    m = re.match(r'([\w.]+(?:<[^=]*?>)?(?:\[\])*)\s+(.*)$', stmt, re.S)
    if not m:
        return
    type_name, rest = m.group(1), m.group(2)
    for declarator in _split_top(rest):
        name, _, value = declarator.partition('=')
        name = name.strip()
        if re.fullmatch(r'[A-Za-z_]\w*', name):
            fields[name] = (type_name, _restore(value.strip(), strings) or None, static, final)


def _method(code, name):
    m = re.search(r'\b' + name + r'\s*\([^)]*\)\s*(?:throws[\w\s,.]+)?\{', code)
    if not m:
        return ''
    return code[m.end() - 1:_matching(code, m.end() - 1)]


def parse_classes(paths):
    """class simple name -> {'fields', 'file', 'code', 'strings', 'docs'} for every class in the given Java files."""
    classes = {}
    for path in paths:
        raw = path.read_text(encoding='utf-8', errors='replace')
        docs = {}
        for m in re.finditer(r'/\*\*(.*?)\*/\s*(?:@\w+(?:\([^)]*\))?\s*)*(?:(?:public|private|protected|static|final|transient|volatile)\s+)*'
                             r'[\w.<>\[\], ?]+?\s+(\w+)\s*[=;]', raw, re.S):
            text = re.sub(r'\s*\n\s*\*\s?', ' ', m.group(1)).strip(' *')
            text = re.sub(r'\{@\w+\s+([^}]*)\}', r'\1', text)
            docs[m.group(2)] = re.sub(r'<[^>]+>', '', text).strip()
        code, strings = _strip(raw)
        for m in re.finditer(r'\b(?:class|record)\s+(\w+)[^{;]*\{', code):
            start = m.end() - 1
            body = code[start + 1:_matching(code, start) - 1]
            classes.setdefault(m.group(1), {'fields': _fields(body, strings), 'file': path, 'code': body,
                                            'strings': strings, 'docs': docs})
    return classes


def _simple_type(type_name):
    return re.sub(r'<.*>', '', type_name).split('.')[-1]


def _literal(v):
    """A Java literal as the config file shows it, or None."""
    v = (v or '').strip()
    if re.fullmatch(r'-?(\d+\.?\d*|\.\d+)([eE]-?\d+)?[fFdDlL]?', v):
        v = v.rstrip('fFdDlL')
        if '.' not in v and 'e' not in v.lower():
            return str(int(v))
        return repr(float(v))
    if re.fullmatch(r'"[^"]*"', v):
        return v[1:-1]
    if v in ('true', 'false'):
        return v
    return None


def _type_label(type_name):
    t = _simple_type(type_name or '')
    return {'Integer': 'int', 'Boolean': 'boolean', 'Float': 'float', 'Double': 'double', 'Long': 'long',
            'String': 'string'}.get(t, t) or None


class _Constants:
    """Resolves literals and named constants (NAME, Class.NAME, -NAME, a * b of literals) of one module."""

    def __init__(self, classes):
        self.classes = classes

    def value(self, expr, home=None, depth=0):
        expr = (expr or '').strip()
        if depth > 4 or not expr:
            return None
        if home is not None:
            expr = _restore(expr, home['strings'])
        lit = _literal(expr)
        if lit is not None:
            return lit
        if expr.startswith('-'):
            inner = self.value(expr[1:], home, depth + 1)
            return ('-' + inner) if inner and re.fullmatch(r'[\d.]+', inner) else None
        m = re.fullmatch(r'\(?\s*([\w.]+)\s*\*\s*([\w.]+)\s*\)?', expr)
        if m:
            a, b = self.value(m.group(1), home, depth + 1), self.value(m.group(2), home, depth + 1)
            try:
                product = float(a) * float(b)
                return str(int(product)) if product.is_integer() and '.' not in a + b else repr(product)
            except (TypeError, ValueError):
                return None
        m = re.fullmatch(r'(?:([\w.]+)\.)?([A-Za-z_]\w*)', expr)
        if not m:
            return None
        owner = self.classes.get(m.group(1).split('.')[-1]) if m.group(1) else home
        if owner is None:
            return None
        field = owner['fields'].get(m.group(2))
        if field is None or field[1] is None:
            return None
        return self.value(_restore(field[1], owner['strings']), owner, depth + 1)


_HELPER_TYPES = (('bool', 'boolean'), ('int', 'int'), ('long', 'long'), ('dec', 'float'), ('float', 'float'),
                 ('double', 'double'), ('real', 'float'), ('percent', 'int'), ('str', 'string'), ('text', 'string'))


def json_config_options(cls, consts):
    """
    Options of a JSON config class (ConfigCommands .json(X::toJson, X::apply)): names and groups from toJson's
    addProperty/add calls, default and bounds from apply's reader calls (helper(json, "key", default[, min, max]),
    clamp(...)), type and fallback default from the field, description from the field's Javadoc.
    """
    code, strings = cls['code'], cls['strings']
    to_json, apply = _method(code, 'toJson'), _method(code, 'apply')
    if not to_json:
        return []
    parent = {}                                  # json var -> (parent var, group key)
    for var, key, child in re.findall(r'(\w+)\.add\(\s*"\u0000(\d+)\u0000"\s*,\s*(\w+)\s*\)', to_json):
        parent[child] = (var, strings[int(key)][1:-1])

    def prefix(var, seen=()):
        if var not in parent or var in seen:
            return ''
        up, key = parent[var]
        return prefix(up, seen + (var,)) + key + '.'
    readers = {}                                 # json var in apply -> group path prefix
    for m in re.finditer(r'(\w+)\s*=\s*(?:group|section|object|obj|child|sub)\(\s*(\w+)\s*,\s*"\u0000(\d+)\u0000"\s*\)', apply):
        readers[m.group(1)] = (m.group(2), strings[int(m.group(3))][1:-1])
    for m in re.finditer(r'(\w+)\s*=\s*(\w+)\.getAsJsonObject\(\s*"\u0000(\d+)\u0000"\s*\)', apply):
        readers[m.group(1)] = (m.group(2), strings[int(m.group(3))][1:-1])

    def rprefix(var, seen=()):
        if var not in readers or var in seen:
            return ''
        up, key = readers[var]
        return rprefix(up, seen + (var,)) + key + '.'
    reads = {}
    for stmt in apply.split(';'):
        for m in re.finditer(r'\b(\w+)\(\s*(\w+)\s*,\s*"\u0000(\d+)\u0000"\s*,\s*([^,()]+(?:\([^()]*\))?)\s*((?:,\s*[^,()]+)*)\)', stmt):
            helper, var, key, default, extra = m.groups()
            path = rprefix(var) + strings[int(key)][1:-1]
            bounds = [b.strip() for b in extra.split(',') if b.strip()]
            after = stmt[m.end():]
            clamp = re.match(r'\s*,\s*([^,()]+)\s*,\s*([^,()]+)\s*\)', after)
            if clamp and not bounds:
                bounds = [clamp.group(1), clamp.group(2)]
            reads.setdefault(path, (helper, default, bounds[:2]))
    out = []
    for var, key, value in re.findall(r'(\w+)\.addProperty\(\s*"\u0000(\d+)\u0000"\s*,\s*([^;]+?)\)\s*;', to_json):
        path = prefix(var) + strings[int(key)][1:-1]
        field_name = value.strip().split('.')[-1]
        field = cls['fields'].get(field_name)
        helper, default_expr, bounds = reads.get(path, (None, None, []))
        default = consts.value(default_expr, cls) if default_expr else None
        if default is None and field is not None and field[1] is not None:
            default = consts.value(field[1], cls)
        type_name = _type_label(field[0]) if field else None
        if not type_name and helper:
            type_name = next((t for h, t in _HELPER_TYPES if helper.lower().startswith(h) or h in helper.lower()), None)
        rng = [consts.value(b, cls) for b in bounds] if len(bounds) == 2 else None
        if rng and any(r is None or not re.fullmatch(r'-?[\d.]+', r) for r in rng):
            rng = None
        out.append({'name': path, 'type': type_name, 'default': default,
                    'range': [float(r) if '.' in r else int(r) for r in rng] if rng else None,
                    'doc': cls['docs'].get(field_name)})
    return out


def pojo_options(cls, classes, consts, prefix=''):
    """Options of a Gson POJO config class (.gson(X.class, ...)): every instance field, groups by field type."""
    out = []
    for name, (type_name, value, static, final) in cls['fields'].items():
        if static:
            continue
        nested = classes.get(_simple_type(type_name))
        if nested is not None and nested is not cls and _simple_type(type_name)[0].isupper() and \
                _simple_type(type_name) not in ('String', 'Integer', 'Boolean', 'Float', 'Double', 'Long', 'List', 'Map', 'Set'):
            out += pojo_options(nested, classes, consts, prefix + name + '.')
            continue
        out.append({'name': prefix + name, 'type': _type_label(type_name), 'default': consts.value(value, cls) if value else None,
                    'range': None, 'doc': cls['docs'].get(name)})
    return out


def _config_classes(repo, entry, classes):
    """[(kind 'json'|'pojo', class, side)] announced to SimpleLib's ConfigCommands in the module's shared code."""
    found = []
    for path in (repo / entry['paths']['shared']).rglob('*.java'):
        if '/test/' in path.as_posix():
            continue
        code = _COMMENT.sub(' ', path.read_text(encoding='utf-8', errors='replace'))
        for m in re.finditer(r'ConfigCommands\s*\.\s*(server|client)\([^;]*?\.(json|gson)\(\s*([\w.]+?)(?:::toJson|\.class)', code, re.S):
            name = m.group(3).split('.')[-1]
            if name in classes:
                found.append((m.group(2), classes[name], m.group(1)))
    return found


def _pretty(key):
    words = re.sub(r'(?<=[a-z0-9])(?=[A-Z])', ' ', key).replace('_', ' ').split()
    return ' '.join([words[0][:1].upper() + words[0][1:]] + [w if w.isupper() else w.lower() for w in words[1:]]) if words else key


def module_config(repo, entry, lang):
    """The module's options in the shape of SimpleBuilding's config rows (wiki/generate.py collect_config)."""
    ns = entry.get('namespace', entry['id'])
    en, de = lang.get('en_us', {}), lang.get('de_de', {})
    shared = repo / entry['paths']['shared']
    files = [p for p in shared.rglob('*.java') if '/test/' not in p.as_posix()]
    classes = parse_classes(files)
    consts = _Constants(classes)
    # 1. Options from code: the config classes announced to /<ns> config (one source with the command).
    code_opts = {}
    for kind, cls, side in _config_classes(repo, entry, classes):
        opts = json_config_options(cls, consts) if kind == 'json' else pojo_options(cls, classes, consts)
        for o in opts:
            o['side'] = side
            o['source'] = cls['file'].relative_to(repo).as_posix()
            code_opts.setdefault(o['name'], o)
    # 2. Options the config screen labels (Cloth AutoConfig keys or the module's own config.<ns>.* keys).
    schemes = [(f'text.autoconfig.{ns}.option.', '.@Tooltip'), (f'config.{ns}.', '.tooltip')]
    lang_paths = {}
    for pre, tip in schemes:
        for table in (en, de):
            for k in table:
                if k.startswith(pre):
                    rest = k[len(pre):]
                    if any(s.startswith('@') for s in rest.split('.')) or rest.endswith(('.tooltip', '.default', '.tab', '.title')) \
                            or rest in ('title', 'tab'):
                        continue
                    lang_paths.setdefault(rest, (pre, tip))
    groups = {p for p in lang_paths if any(o.startswith(p + '.') for o in lang_paths)}
    paths = [p for p in lang_paths if p not in groups]
    if code_opts and not paths:
        paths = list(code_opts)
    elif code_opts:
        paths += [p for p in code_opts if p not in lang_paths and p not in groups]

    def resolve(path):
        if path in code_opts:
            return code_opts[path]
        segments = path.split('.')
        for cls in classes.values():           # walk POJO field types from any class that has the first segment
            cur, field = cls, None
            for n, seg in enumerate(segments):
                field = cur['fields'].get(seg)
                if field is None:
                    break
                if n < len(segments) - 1:
                    cur = classes.get(_simple_type(field[0]))
                    if cur is None:
                        field = None
                        break
            if field is not None:
                return {'type': _type_label(field[0]), 'default': consts.value(field[1], cur) if field[1] else None,
                        'range': None, 'doc': cur['docs'].get(segments[-1]),
                        'source': cur['file'].relative_to(repo).as_posix(), 'side': None}
        return {}

    def text(table, path, suffix=''):
        for pre, tip in schemes:
            value = table.get(pre + path + (tip if suffix == 'tip' else ''))
            if value:
                return value
        return None
    title_en = en.get(f'text.autoconfig.{ns}.title') or entry.get('displayName')
    title_de = de.get(f'text.autoconfig.{ns}.title') or title_en
    out = []
    for path in sorted(set(paths), key=lambda p: (p.count('.') == 0, p)):
        segments = path.split('.')
        info = resolve(path)
        top = segments[0] if len(segments) > 1 else None
        parent = '.'.join(segments[:-1]) if len(segments) > 2 else None
        tab_en = (en.get(f'config.{ns}.{top}.tab') or text(en, top) or en.get(f'text.autoconfig.{ns}.category.{top}') or top) if top else title_en
        tab_de = (de.get(f'config.{ns}.{top}.tab') or text(de, top) or de.get(f'text.autoconfig.{ns}.category.{top}') or tab_en) if top else title_de
        doc = re.sub(r'^(?:[A-Z]{1,8}\d+[\w-]*[,:]\s*)+', '', info.get('doc') or '') or None
        tip_en = text(en, path, 'tip') or doc
        default = info.get('default')
        if default is None and tip_en:
            # the tooltip names the default ("Default: 60.") where the code reads it indirectly
            m = re.search(r'Default: ([^\n]+?)\.?(?:\n|$| [A-Z])', tip_en.replace('\n', ' ') + '\n')
            default = m.group(1).strip() if m else None
        out.append({
            'range': info.get('range'),
            'side': info.get('side'),
            'reload': None,
            'source': info.get('source'),
            'name': path,
            'shortName': segments[-1],
            'type': info.get('type'),
            'default': default,
            'note': None,
            'category': tab_en,
            'categoryDe': tab_de,
            'group': (text(en, parent) or parent) if parent else None,
            'groupDe': (text(de, parent) or text(en, parent) or parent) if parent else None,
            'label': text(en, path) or _pretty(segments[-1]),
            'labelDe': text(de, path) or text(en, path) or _pretty(segments[-1]),
            'tooltip': tip_en,
            'tooltipDe': text(de, path, 'tip') or tip_en,
        })
    return out


# ---------------------------------------------------------------------------
# in-world transformations from data files
# ---------------------------------------------------------------------------

CHISEL_TIERS = ['stone', 'iron', 'diamond', 'netherite', 'enderite']


def chisel_tools(repo):
    """Simple Chisels' chisels in tier order: [(item id, table tier)] from ChiselTier.java (one source with the game)."""
    path = repo / 'modules/simplechisels/shared/java/com/simplechisels/ChiselTier.java'
    if not path.is_file():
        return []
    return [(f'simplechisels:{name}_chisel', table) for name, table in
            re.findall(r'^\s+[A-Z]+\("(\w+)",\s*[\w.]+,\s*"(\w+)"', path.read_text(encoding='utf-8'), re.M)]


def _read_dir(dirs, sub):
    files = {}
    for base in dirs:
        root = Path(base) / sub
        if root.is_dir():
            for path in sorted(root.rglob('*.json')):
                try:
                    files[path.relative_to(root).with_suffix('').as_posix()] = (json.loads(path.read_text(encoding='utf-8')), path)
                except (OSError, json.JSONDecodeError):
                    continue
    return files


def hammer_ids(repo):
    """Simple Hammers' sledgehammers by grade (stone, copper, ...), from its language file."""
    path = repo / 'modules/simplehammers/shared/resources/assets/simplehammers/lang/en_us.json'
    if not path.is_file():
        return {}
    return {m.group(1): f'simplehammers:{m.group(1)}_sledgehammer'
            for m in (re.fullmatch(r'item\.simplehammers\.(\w+)_sledgehammer', k) for k in json.loads(path.read_text(encoding='utf-8')))
            if m}


def in_world_prose(g):
    """Kind texts of SimpleBuilding's wiki manual (one text per kind for every wiki)."""
    manual = g.read_json(g.WIKI / 'manual.json')
    return (manual.get('inWorld') or {}).get('kinds', {})


def data_in_world(repo, dirs, item_ids=None):
    """
    In-world entries from the data files of one namespace (dirs = its data/<ns> folders, later wins per file):
    in_world_upgrades -> kind sledgehammer_upgrade, chisel_transformations -> chisel / chisel_reverse.
    Returns (entries, kinds used).
    """
    entries, kinds = [], []
    hammers = hammer_ids(repo)
    upgrades = _read_dir(dirs, 'in_world_upgrades')
    for name, (data, path) in sorted(upgrades.items()):
        for up in data.get('upgrades', []) if isinstance(data, dict) else []:
            if not isinstance(up, dict) or not up.get('from') or not up.get('to'):
                continue
            grade = up.get('min_tool') or up.get('min_hammer')
            tool = hammers.get(grade) if grade else None
            stats = {'damagePerHit': up.get('damage_per_hit', 1)}
            if up.get('strikes_per_material'):
                stats['hits'] = up['strikes_per_material'] * up.get('material_count', 1)
            entry = {
                'id': f"sledgehammer_upgrade/{up['from']}",
                'kind': 'sledgehammer_upgrade',
                'inputs': [{'id': up['from'], 'count': 1}, {'id': up.get('material'), 'count': up.get('material_count', 1)}],
                'tools': [tool] if tool else [up.get('tool', '#simplelib:upgrade_tools')],
                'toolOrBetter': bool(tool),
                'output': {'id': up['to'], 'count': 1},
                'stats': stats,
                'source': path.relative_to(repo).as_posix(),
            }
            if grade and not tool:
                entry['minTool'] = grade
            entries.append(entry)
        if upgrades and 'sledgehammer_upgrade' not in kinds:
            kinds.append('sledgehammer_upgrade')
    chains = _read_dir(dirs, 'chisel_transformations')
    if chains:
        tools = chisel_tools(repo)
        rank = {tier: n for n, tier in enumerate(CHISEL_TIERS)}
        # pair -> lowest tier rank per (kind, touch); a higher tier inherits every lower tier
        found = {}
        for name, (data, path) in sorted(chains.items()):
            if not isinstance(data, dict) or data.get('tier') not in rank:
                continue
            touch = data.get('table') == 'touch'
            for chain in data.get('chains', []):
                blocks = [b for b in chain.get('blocks', []) if isinstance(b, str)]
                pairs = list(zip(blocks, blocks[1:]))
                if chain.get('cyclic') and len(blocks) > 2:
                    pairs.append((blocks[-1], blocks[0]))
                for a, b in pairs:
                    for kind, src, dst in (('chisel', a, b), ('chisel_reverse', b, a)):
                        key = (kind, src, dst, touch)
                        r = rank[data['tier']]
                        if key not in found or r < found[key][0]:
                            found[key] = (r, path)
        for (kind, src, dst, touch), (r, path) in sorted(found.items()):
            if touch and (kind, src, dst, False) in found and found[(kind, src, dst, False)][0] <= r:
                continue
            entry_tools = [tool for tool, tier in tools if rank.get(tier, -1) >= r]
            stats = {'tier': CHISEL_TIERS[r]}
            if touch:
                stats['touch'] = True
            entries.append({
                'id': f'{kind}/{src}/{dst}' + ('/touch' if touch else ''),
                'kind': kind,
                'inputs': [{'id': src, 'count': 1}],
                'tools': entry_tools,
                'output': {'id': dst, 'count': 1},
                'stats': stats,
                'source': path.relative_to(repo).as_posix(),
            })
        for kind in ('chisel', 'chisel_reverse'):
            if any(e['kind'] == kind for e in entries) and kind not in kinds:
                kinds.append(kind)
    return entries, kinds


# ---------------------------------------------------------------------------
# commands
# ---------------------------------------------------------------------------

def scanned_commands(repo, entry):
    """Root commands a module registers in its shared code (no test sources)."""
    ns = entry.get('namespace', entry['id'])
    found = set()
    for path in (repo / entry['paths']['shared']).rglob('*.java'):
        if '/test/' in path.as_posix():
            continue
        code = _COMMENT.sub(' ', path.read_text(encoding='utf-8', errors='replace'))
        found |= set(re.findall(r'ConfigCommands\.(?:server|client)\(\s*"([a-z0-9_]+)"', code))
        found |= set(re.findall(r'\.register\(\s*(?:Commands|ClientCommand\w*)\.literal\(\s*"([a-z0-9_]+)"', code))
        found |= {c for c in re.findall(r'(?:Commands|ClientCommand\w*)\.literal\(\s*"([a-z0-9_]+)"\s*\)', code)
                  if c in (ns, entry['id'])}
    return sorted(found)
