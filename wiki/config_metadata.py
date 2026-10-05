"""Conservative metadata from executable config declarations and validation."""
import re


def clean(text):
    return re.sub(r'//[^\n]*|/\*.*?\*/', '', text, flags=re.S)


def read_metadata(files, version_flags=None):
    texts = {path: clean(path.read_text(encoding='utf-8')) for path in files}
    if version_flags is not None:
        for path, text in texts.items():
            texts[path] = re.sub(r'if\s*\((?:[\w.]+\.)?McVersion\.(\w+)\)\s*\{[^{}]*\}',
                                 lambda match: '' if version_flags.get(match[1]) is False else match[0], text)
    constants = {}
    for path, text in texts.items():
        for name, value in re.findall(r'static final (?:int|long|double|float) (\w+)\s*=\s*([\d.]+)[fFdDlL]?\s*;', text):
            constants[path.stem + '.' + name] = float(value)

    def number(token, path):
        token = token.strip()
        try:
            return float(token.rstrip('fFdDlL'))
        except ValueError:
            return constants.get('.'.join(token.split('.')[-2:]) if '.' in token else path.stem + '.' + token)

    bounds = {}
    for path, text in texts.items():
        depth, stack = 0, []
        for line in text.splitlines():
            cls = re.search(r'\bclass (\w+)', line)
            if cls:
                stack.append((cls[1], depth))
            # Only assignments that clamp the same field are accepted. Unknown expressions stay unknown.
            match = re.search(r'([\w.]+)\s*=\s*(?:\(\w+\)\s*)?(?:clamp|bounded)\((?:"[^"]+",\s*)?\1,\s*([^,]+),\s*([^,)]+)', line)
            if not match:
                match = re.search(r'([\w.]+)\s*=\s*Math.max\(([^,]+),\s*Math.min\(([^,]+),\s*\1\)\)', line)
            if match and stack:
                lo, hi = number(match[2], path), number(match[3], path)
                conditional_max = re.search(r'McVersion\.(\w+)\s*\?\s*max\s*:\s*Double.MAX_VALUE', text)
                if ('bounded(' in line and conditional_max and version_flags is not None
                        and version_flags.get(conditional_max[1]) is False):
                    hi = None
                if lo is not None or hi is not None:
                    bounds[(path, stack[-1][0], match[1])] = [lo, hi]
            if not match and stack:
                lower = re.search(r'([\w.]+)\s*=\s*Math.max\(([^,]+),\s*\1\)', line)
                nonnegative = re.search(r'([\w.]+)\s*=\s*nonNegative\(\1,', line)
                if lower and number(lower[2], path) is not None:
                    bounds[(path, stack[-1][0], lower[1])] = [number(lower[2], path), None]
                elif nonnegative and 'return Double.isFinite(value) ? Math.max(0.0, value) : fallback;' in text:
                    bounds[(path, stack[-1][0], nonnegative[1])] = [0, None]
            reflective = re.search(r'field.setDouble\(this, clamp\(field.getDouble\(this\),\s*([^,]+),\s*([^,]+),', line)
            if reflective and stack and (stack[-1][0] + '.class.getFields()') in text:
                lo, hi = number(reflective[1], path), number(reflective[2], path)
                if lo is not None and hi is not None:
                    bounds[(path, stack[-1][0], '*double*')] = [lo, hi]
            depth += line.count('{') - line.count('}')
            while stack and depth <= stack[-1][1]:
                stack.pop()
    return bounds


def option_sets(path):
    if not path.exists():
        return None
    text = clean(path.read_text(encoding='utf-8'))
    result = {}
    for key in ('CLIENT_SIDE', 'APPLY_ON_RELOAD', 'RECIPES_ON_RELOAD', 'RESTART_REQUIRED'):
        match = re.search(r'\b' + key + r'\s*=\s*Set.of\((.*?)\);', text, re.S)
        if not match:
            return None
        result[key] = set(re.findall(r'"([^"]+)"', match[1]))
    return result
