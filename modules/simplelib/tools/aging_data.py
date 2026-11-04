"""Data of a SimpleLib aging chain (com.simplelib.api.aging.AgingChain, DW04B-1): one call per chain returns every JSON file the
chain's blocks need, so a module's own generator only lists its chains (see modules/simpleblocks/tools/generate_data.py).

Per chain (base block + aged stages, the last is the end stage, + waxed copies of the base and every stage but the end):
  - blockstates in the base's shape (STATE_KINDS: single, facing = horizontal y-rotation, axis = pillar) pointing at models
    that inherit the Vanilla model of the base and only swap its textures (``block/<stage>_<vanilla sprite>``);
  - waxed blocks use the models of their stage (like waxed copper), the waxed base the base's own model;
  - item definitions, loot (drops itself unless the chain gives its own, e.g. melon slices), shapeless wax recipes
    (block + honeycomb, like Vanilla's waxed copper) with recipe advancements, and the base block's tags mirrored.

    from aging_data import Chain, files
    for rel, data in files(Chain(ns='mymod', base='minecraft:pumpkin', stages=['faded_pumpkin', 'pale_pumpkin'],
                                 model='minecraft:block/pumpkin', textures={'end': 'pumpkin_top', 'side': 'pumpkin_side'},
                                 block_tags=['mineable/axe'])).items():
        write(rel, data)   # rel under the module's resources root, data a JSON-ready dict
"""
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Callable

STATE_KINDS = ('single', 'facing', 'axis')


@dataclass
class Chain:
    ns: str
    base: str                      # 'minecraft:pumpkin' (or an own block id)
    stages: list                   # aged stage paths in ns, oldest last
    model: str                     # model of the base, e.g. 'minecraft:block/pumpkin'
    textures: dict                 # texture slot of that model -> sprite name (stage texture: '<ns>:block/<stage word>_<sprite>')
    state: str = 'single'
    horizontal_model: str | None = None   # axis kind: the base's horizontal model (e.g. hay_block_horizontal)
    stage_words: list | None = None       # prefix of the stage textures (default: stage path minus the base name)
    wax: bool = True
    waxed_prefix: str = 'waxed_'
    block_tags: list = field(default_factory=list)   # Vanilla tags of the base ('mineable/axe' -> minecraft:mineable/axe)
    item_tags: list = field(default_factory=list)
    loot: Callable[[str], dict] | None = None        # block id -> loot table (default: drops itself)
    recipe_category: str = 'building'

    @property
    def base_path(self):
        return self.base.split(':')[1]

    def waxed(self):
        """(waxed path, stage index) for the base and every stage but the end."""
        if not self.wax:
            return []
        names = [self.base_path] + self.stages[:-1]
        return [(self.waxed_prefix + n, i) for i, n in enumerate(names)]

    def words(self):
        return self.stage_words or [s[:-len(self.base_path) - 1] if s.endswith('_' + self.base_path) else s for s in self.stages]

    def blocks(self):
        """Every created block path (stages, then waxed), the order of AgingChain.createdBlocks."""
        return list(self.stages) + [w for w, _ in self.waxed()]


def self_drop(block):
    return {'type': 'minecraft:block', 'pools': [{'condition': {'type': 'minecraft:survives_explosion'},
                                                   'entries': [{'type': 'minecraft:item', 'name': block}], 'rolls': 1}]}


def _models(c: Chain, stage: int):
    """(model id, horizontal model id or None) of stage index (0 = base)."""
    if stage == 0:
        return c.model, c.horizontal_model
    path = c.stages[stage - 1]
    return f'{c.ns}:block/{path}', (f'{c.ns}:block/{path}_horizontal' if c.horizontal_model else None)


def _state(c: Chain, model, horizontal):
    if c.state == 'single':
        return {'variants': {'': {'model': model}}}
    if c.state == 'facing':
        rot = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
        return {'variants': {f'facing={f}': ({'model': model, 'y': r} if r else {'model': model}) for f, r in sorted(rot.items())}}
    if c.state == 'axis':
        return {'variants': {'axis=x': {'model': horizontal, 'x': 90, 'y': 90}, 'axis=y': {'model': model},
                             'axis=z': {'model': horizontal, 'x': 90}}}
    raise ValueError(c.state)


def _recipe_advancement(c: Chain, recipe, trigger):
    return {'parent': 'minecraft:recipes/root',
            'criteria': {f'has_{trigger.split(":")[1]}': {'conditions': {'items': [{'items': trigger}]}, 'trigger': 'minecraft:inventory_changed'},
                         'has_the_recipe': {'conditions': {'recipes': f'{c.ns}:{recipe}'}, 'trigger': 'minecraft:recipe_unlocked'}},
            'requirements': [['has_the_recipe', f'has_{trigger.split(":")[1]}']],
            'rewards': {'recipes': [f'{c.ns}:{recipe}']}}


def files(c: Chain) -> dict:
    """Resource-relative path -> JSON data of every file of the chain (tags: rel -> list of ids to merge, see tag_members)."""
    assert c.state in STATE_KINDS and c.stages
    out = {}
    words = c.words()
    for i, path in enumerate(c.stages, start=1):
        tex = {slot: f'{c.ns}:block/{words[i - 1]}_{sprite}' for slot, sprite in c.textures.items()}
        out[f'assets/{c.ns}/models/block/{path}.json'] = {'parent': c.model, 'textures': tex}
        if c.horizontal_model:
            out[f'assets/{c.ns}/models/block/{path}_horizontal.json'] = {'parent': c.horizontal_model, 'textures': tex}
    for path, stage in [(p, i) for i, p in enumerate(c.stages, start=1)] + c.waxed():
        model, horizontal = _models(c, stage)
        out[f'assets/{c.ns}/blockstates/{path}.json'] = _state(c, model, horizontal)
        out[f'assets/{c.ns}/items/{path}.json'] = {'model': {'type': 'minecraft:model', 'model': model}}
        block = f'{c.ns}:{path}'
        out[f'data/{c.ns}/loot_table/blocks/{path}.json'] = (c.loot or self_drop)(block)
    stage_ids = [c.base] + [f'{c.ns}:{p}' for p in c.stages]
    for path, stage in c.waxed():
        recipe = f'{path}_from_honeycomb'
        out[f'data/{c.ns}/recipe/{recipe}.json'] = {'type': 'minecraft:crafting_shapeless', 'category': c.recipe_category,
                                                     'group': path, 'ingredients': [stage_ids[stage], 'minecraft:honeycomb'],
                                                     'result': {'id': f'{c.ns}:{path}'}}
        out[f'data/{c.ns}/advancement/recipes/building_blocks/{recipe}.json'] = _recipe_advancement(c, recipe, stage_ids[stage])
    return out


def tag_members(chains) -> dict:
    """Tag file (resource-relative) -> sorted block/item ids of all chains, mirroring their base's tags."""
    tags = {}
    for c in chains:
        ids = [f'{c.ns}:{p}' for p in c.blocks()]
        for t in c.block_tags:
            ns, _, path = t.rpartition(':')
            tags.setdefault(f'data/{ns or "minecraft"}/tags/block/{path}.json', []).extend(ids)
        for t in c.item_tags:
            ns, _, path = t.rpartition(':')
            tags.setdefault(f'data/{ns or "minecraft"}/tags/item/{path}.json', []).extend(ids)
    return tags
