// Logic of wiki/assets/zoomarea.js under node (run by wiki/tests/test_tzeditor.py): zoom steps, limits, texts.
const assert = require('node:assert/strict');
const path = require('node:path');
const Z = require(path.resolve(__dirname, '..', 'assets', 'zoomarea.js'));
assert.equal(Z.stepLevel(1, 1, 0.25, 4), 1.1);
assert.equal(Z.stepLevel(1, -1, 0.25, 4), 0.9);
assert.equal(Z.stepLevel(1.03, 1, 0.25, 4), 1.1);          // between two levels: the next one up / down
assert.equal(Z.stepLevel(1.03, -1, 0.25, 4), 1);
assert.equal(Z.stepLevel(4, 1, 0.25, 4), 4);                // stays at the limits
assert.equal(Z.stepLevel(0.5, -1, 0.5, 3), 0.5);
assert.equal(Z.stepLevel(3, 1, 0.5, 3), 3);
assert.equal(Z.clamp(9, 0.5, 3), 3);
assert.equal(Z.clamp('x', 0.5, 3), 1);                      // nonsense from sessionStorage -> 100 %
assert.equal(Z.pct(1.25), '125 %');
assert.deepEqual(Object.keys(Z.STR.en).sort(), Object.keys(Z.STR.de).sort());
assert.equal(Z.STR.de.full, 'Vollbild');
console.log('zoomarea core ok');
