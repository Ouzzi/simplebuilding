// Logic of wiki/assets/tzeditor.js under node (run by wiki/tests/test_tzeditor.py): column move, save payload, duplicates.
const assert = require('node:assert/strict');
const path = require('node:path');
const T = require(path.resolve(__dirname, '..', 'assets', 'tzeditor.js'));
const cols = ['button', 'plate', 'storage'];
const rows = [{ key: 'iron', label: 'Iron', cells: ['minecraft:iron_button', null, '#red'], tops: { storage: 'x:octet', gone: 'x:y' } }];
T.moveColumn(cols, rows, 0, 2);
assert.deepEqual(cols, ['plate', 'storage', 'button']);
assert.deepEqual(rows[0].cells, [null, '#red', 'minecraft:iron_button']);
const save = T.layoutOf({ variant: 'lines', area: 'metals', excluded: ['a:b'] }, cols, rows);
assert.deepEqual(save.rows[0].tops, { storage: 'x:octet' });   // tops of unknown columns are dropped
assert.deepEqual(save.excluded, ['a:b']);
assert.deepEqual(T.placedCount([{ cells: ['a:a', 'a:a', '#red', null] }]), { 'a:a': 2 });
assert.ok(T.STR.en.title && T.STR.de.title);
assert.deepEqual(Object.keys(T.STR.en).sort(), Object.keys(T.STR.de).sort());
console.log('tzeditor core ok');
