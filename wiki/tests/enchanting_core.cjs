// Logic of wiki/assets/enchanting.js under node (run by wiki/tests/test_enchanting.py).
// Reference values of java.util.Random from jshell (JDK 17) on sb-test, 2026-10-21.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const E = require(path.resolve(__dirname, '..', 'assets', 'enchanting.js'));

const r = new E.JavaRandom(42);
assert.equal(r.nextInt(), -1170105035);
assert.equal(r.nextInt(10), 3);
assert.equal(r.nextInt(16), 10);
assert.equal(r.nextInt(7), 3);
assert.equal(Math.fround(r.nextFloat()), Math.fround(0.3087194));
assert.equal(r.nextInt(50), 25);
const q = new E.JavaRandom(-123456789);
assert.deepEqual([q.nextInt(8), q.nextInt(16)], [2, 12]);
assert.equal(Math.fround(q.nextFloat()), Math.fround(0.96539897));
assert.equal(q.nextInt(1000), 408);

// costs: 15 shelves -> bottom row always 30, top row at least 1; no enchantability -> 0
for (let seed = -50; seed < 50; seed++) {
  const rand = new E.JavaRandom(seed);
  const c = [0, 1, 2].map(j => E.tableCost(rand, j, 15, 10));
  assert.equal(c[2], 30);
  assert.ok(c[0] >= 1 && c[0] <= 10 && c[1] >= 1);
}
assert.equal(E.tableCost(new E.JavaRandom(1), 0, 15, 0), 0);

// small synthetic registry
const data = {
  strings: {
    en: { 'container.enchant.clue': '%s . . . ?', 'container.enchant.lapis.one': '1 Lapis Lazuli', 'container.enchant.lapis.many': '%s Lapis Lazuli',
      'container.enchant.level.one': '1 Enchantment Level', 'container.enchant.level.many': '%s Enchantment Levels', 'container.enchant.level.requirement': 'Level Requirement: %s',
      'item.minecraft.enchanted_book': 'Enchanted Book', 'enchantment.level.1': 'I', 'enchantment.level.2': 'II', 'enchantment.level.3': 'III' },
  },
  enchantments: [
    { id: 'm:a', name: { en: 'Alpha' }, maxLevel: 3, weight: 10, minCost: { base: 1, per_level_above_first: 10 }, maxCost: { base: 51, per_level_above_first: 10 }, exclusive: ['m:b'], curse: false },
    { id: 'm:b', name: { en: 'Beta' }, maxLevel: 1, weight: 5, minCost: { base: 1, per_level_above_first: 0 }, maxCost: { base: 60, per_level_above_first: 0 }, exclusive: ['m:a'], curse: false },
    { id: 'm:c', name: { en: 'Gamma' }, maxLevel: 1, weight: 1, minCost: { base: 25, per_level_above_first: 0 }, maxCost: { base: 50, per_level_above_first: 0 }, exclusive: [], curse: true },
  ],
  items: [],
};
const tool = { id: 'm:tool', name: { en: 'Tool' }, enchantability: 10, table: ['m:a', 'm:b', 'm:c'] };
const book = { id: 'minecraft:book', name: { en: 'Book' }, enchantability: 1, table: ['m:a', 'm:b', 'm:c'] };
assert.deepEqual(E.available(data, tool, 25).map(x => x.id + x.level), ['m:a3', 'm:b1', 'm:c1']);
assert.deepEqual(E.available(data, tool, 5).map(x => x.id + x.level), ['m:a1', 'm:b1']);
for (let seed = 0; seed < 300; seed++) {
  const o = E.offers(data, tool, 15, seed);
  assert.deepEqual(o, E.offers(data, tool, 15, seed), 'same seed, same offers');
  o.forEach(x => {
    const ids = x.list.map(l => l.id);
    assert.ok(!(ids.includes('m:a') && ids.includes('m:b')), 'exclusive pair never together');
    assert.equal(new Set(ids).size, ids.length);
    if (x.clue) assert.ok(ids.includes(x.clue.id));
  });
  E.offers(data, book, 15, seed).forEach(x => assert.ok(x.list.length <= 2, 'book drops one when more than one'));
}

// tooltip lines like EnchantmentScreen
assert.deepEqual(E.fullName(data, 'en', 'm:b', 1), ['Beta', 'gray']);
assert.deepEqual(E.fullName(data, 'en', 'm:a', 1), ['Alpha I', 'gray']);
assert.deepEqual(E.fullName(data, 'en', 'm:c', 1), ['Gamma', 'red']);
const offer = { slot: 2, cost: 30, clue: { id: 'm:a', level: 3 } };
assert.deepEqual(E.offerTooltip(data, 'en', offer, 30, 3), [[['Alpha III', 'gray'], [' . . . ?', 'white']], [], [['3 Lapis Lazuli', 'gray']], [['3 Enchantment Levels', 'gray']]]);
assert.deepEqual(E.offerTooltip(data, 'en', offer, 30, 2)[2], [['3 Lapis Lazuli', 'red']]);
assert.deepEqual(E.offerTooltip(data, 'en', offer, 29, 3).slice(1), [[], [['Level Requirement: 30', 'red']]]);
assert.deepEqual(E.offerTooltip(data, 'en', { slot: 0, cost: 3, clue: { id: 'm:b', level: 1 } }, 30, 3).slice(2), [[['1 Lapis Lazuli', 'gray']], [['1 Enchantment Level', 'gray']]]);
assert.deepEqual(E.itemTooltip(data, 'en', { id: 'minecraft:enchanted_book' }, [{ id: 'm:a', level: 2 }], false), [[['Enchanted Book', 'yellow']], [['Alpha II', 'gray']]]);
assert.deepEqual(E.itemTooltip(data, 'en', tool, [{ id: 'm:c', level: 1 }], false)[0], [['Tool', 'aqua']]);
assert.match(E.runes(5, 1), /^[^a-z]+$/);

// the generated file, when present: every sample item gives offers without throwing
const file = path.resolve(__dirname, '..', 'data', 'enchanting.js');
if (fs.existsSync(file)) {
  const real = JSON.parse(fs.readFileSync(file, 'utf8').split('window.WIKI_ENCHANTING = ')[1].trim().replace(/;$/, ''));
  real.items.forEach(item => {
    for (let s = 0; s < 20; s++) E.offers(real, item, 15, s).forEach(x => { if (x.clue) E.offerTooltip(real, 'de', x, 30, 3); });
  });
}
console.log('enchanting core ok');
