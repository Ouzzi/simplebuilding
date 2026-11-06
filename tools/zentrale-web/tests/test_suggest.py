"""'Änderung vorschlagen' (a-wikiux06): POST /api/queue kind suggestion with images and the reference texture."""
import base64
import http.client
import json
import struct
import sys
import tempfile
import threading
import unittest
import zlib
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import devqueue  # noqa: E402
from devqueue import DevQueueServer  # noqa: E402

AUTH = {'Content-Type': 'application/json', 'Cf-Access-Authenticated-User-Email': 'owner@test'}


def png(w=4, h=4):
    rows = b''.join(b'\x00' + b'\xff\x00\x00\xff' * w for _ in range(h))

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xFFFFFFFF)
    return b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0)) + \
        chunk(b'IDAT', zlib.compress(rows)) + chunk(b'IEND', b'')


JPEG = b'\xff\xd8\xff\xe0' + b'\x00' * 32
WEBP = b'RIFF\x10\x00\x00\x00WEBPVP8 ' + b'\x00' * 16


def att(data, kind='image/png', name='bild.png'):
    return {'name': name, 'type': kind, 'data': base64.b64encode(data).decode()}


class SuggestTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        root = Path(self.tmp.name)
        self.data = root / 'data'
        self.repo = root / 'repo'
        wiki = self.repo / 'wiki'
        (wiki / 'data').mkdir(parents=True)
        (wiki / 'assets/textures/sheets/simplefoo/simplefoo/block').mkdir(parents=True)
        (wiki / 'assets/textures/sheets/simplefoo/simplefoo/block/lamp.png').write_bytes(png(16, 32))
        (wiki / 'assets/vanilla/block').mkdir(parents=True)
        (wiki / 'assets/vanilla/block/stone.png').write_bytes(png(16, 16))
        (self.repo / 'secret.png').write_bytes(png(2, 2))
        index = {'schema': 1, 'textures': [
            {'id': 'simplefoo:block/lamp', 'file': 'assets/textures/sheets/simplefoo/simplefoo/block/lamp.png',
             'source': 'modules/simplefoo/shared/resources/assets/simplefoo/textures/block/lamp.png', 'w': 16, 'h': 32},
            {'id': 'simplefoo:block/evil', 'file': '../secret.png', 'source': 'x', 'w': 2, 'h': 2}]}
        (wiki / 'data/textures.js').write_text('window.WIKI_TEXTURES = ' + json.dumps(index) + ';\n', encoding='utf-8')
        self.server = DevQueueServer(('127.0.0.1', 0), self.data)
        self.server.repo = self.repo
        threading.Thread(target=self.server.serve_forever, daemon=True).start()
        self.port = self.server.server_address[1]

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        self.tmp.cleanup()

    def call(self, method, path, body=None, headers=AUTH):
        conn = http.client.HTTPConnection('127.0.0.1', self.port)
        conn.request(method, path, body=json.dumps(body) if body is not None else None, headers=headers)
        resp = conn.getresponse()
        data = resp.read()
        ctype = resp.getheader('Content-Type')
        conn.close()
        return resp.status, data, ctype

    def suggestion(self, **over):
        body = {'category': 'textures', 'kind': 'suggestion', 'comment': 'heller bitte',
                'object': {'id': 'simplefoo|simplefoo:lamp', 'label': 'Lamp'},
                'targets': [{'id': 'simplefoo:block/lamp', 'label': 'Lamp (side)',
                             'path': 'assets/textures/sheets/simplefoo/simplefoo/block/lamp.png'}],
                'attachments': [att(png()), att(JPEG, 'image/jpeg', '../../etc/x.jpg'), att(WEBP, 'image/webp', 'w.webp')]}
        body.update(over)
        return body

    def test_needs_the_owner_header(self):
        status, _, _ = self.call('POST', '/api/queue', self.suggestion(), headers={'Content-Type': 'application/json'})
        self.assertEqual(status, 403)
        self.assertFalse((self.data / 'attachments').exists())

    def test_stores_images_and_reference_and_serves_them(self):
        status, data, _ = self.call('POST', '/api/queue', self.suggestion())
        self.assertEqual(status, 201, data)
        entry = json.loads(data)
        self.assertEqual(entry['kind'], 'suggestion')
        self.assertEqual(entry['object'], {'id': 'simplefoo|simplefoo:lamp', 'label': 'Lamp'})
        self.assertEqual([a['file'] for a in entry['attachments']], ['1.png', '2.jpg', '3.webp'])
        self.assertEqual(entry['attachments'][1]['name'], '.._.._etc_x.jpg')  # a label only, never a path
        self.assertEqual(entry['refs'], [{'id': 'simplefoo:block/lamp', 'file': 'ref-1.png'}])
        folder = self.data / 'attachments' / entry['id']
        self.assertEqual(sorted(p.name for p in folder.iterdir()), ['1.png', '2.jpg', '3.webp', 'ref-1.png'])
        self.assertEqual((folder / 'ref-1.png').read_bytes(), png(16, 32))
        status, body, ctype = self.call('GET', f"/api/queue/{entry['id']}/files/2.jpg", headers={'Cf-Access-Authenticated-User-Email': 'o@t'})
        self.assertEqual((status, body, ctype), (200, JPEG, 'image/jpeg'))
        status, _, _ = self.call('GET', f"/api/queue/{entry['id']}/files/2.jpg", headers={})
        self.assertEqual(status, 403)
        queue = json.loads(self.call('GET', '/api/queue')[1])
        self.assertEqual(queue[0]['id'], entry['id'])
        # deleting the entry removes its files
        self.assertEqual(self.call('DELETE', f"/api/queue/{entry['id']}")[0], 204)
        self.assertFalse(folder.exists())

    def test_vanilla_reference_from_the_dev_folder(self):
        body = self.suggestion(targets=[{'id': 'minecraft:block/stone', 'label': 'Stone', 'path': 'assets/vanilla/block/stone.png'}],
                               attachments=[])
        entry = json.loads(self.call('POST', '/api/queue', body)[1])
        self.assertEqual(entry['refs'], [{'id': 'minecraft:block/stone', 'file': 'ref-1.png'}])

    def test_reference_never_leaves_the_wiki_assets(self):
        for tid in ('simplefoo:block/evil', 'minecraft:../../secret', 'minecraft:block/missing'):
            body = self.suggestion(targets=[{'id': tid, 'label': 'x', 'path': '../secret.png'}], attachments=[])
            status, data, _ = self.call('POST', '/api/queue', body)
            self.assertEqual(status, 201)
            self.assertEqual(json.loads(data)['refs'], [], tid)

    def test_rejects_bad_images(self):
        cases = {
            'wrong magic': [att(JPEG, 'image/png')],
            'svg': [{'name': 'a.svg', 'type': 'image/svg+xml', 'data': base64.b64encode(b'<svg/>').decode()}],
            'too large': [att(b'\x89PNG\r\n\x1a\n' + b'\x00' * (devqueue.MAX_ATTACHMENT + 1))],
            'too many': [att(png()) for _ in range(devqueue.MAX_ATTACHMENTS + 1)],
            'not base64': [{'name': 'a.png', 'type': 'image/png', 'data': '***'}],
        }
        for label, images in cases.items():
            status, data, _ = self.call('POST', '/api/queue', self.suggestion(attachments=images))
            self.assertEqual(status, 400, label)
        self.assertEqual(json.loads(self.call('GET', '/api/queue')[1]), [])
        self.assertFalse((self.data / 'attachments').exists())

    def test_attachments_only_on_a_suggestion_and_something_to_say(self):
        body = self.suggestion(kind='texture')
        self.assertEqual(self.call('POST', '/api/queue', body)[0], 400)
        self.assertEqual(self.call('POST', '/api/queue', self.suggestion(comment='', attachments=[]))[0], 400)
        self.assertEqual(self.call('POST', '/api/queue', self.suggestion(comment='', attachments=[att(png())]))[0], 201)
        # items (category code) take suggestions too
        self.assertEqual(self.call('POST', '/api/queue', self.suggestion(category='code', attachments=[]))[0], 201)

    def test_file_names_are_the_servers_own(self):
        entry = json.loads(self.call('POST', '/api/queue', self.suggestion())[1])
        for name in ('../queue.json', 'queue.json', '1.svg', '%2e%2e%2fqueue.json', 'ref-1.png.bak'):
            self.assertEqual(self.call('GET', f"/api/queue/{entry['id']}/files/{name}")[0], 404, name)
        self.assertEqual(self.call('GET', '/api/queue/../files/1.png')[0], 404)


if __name__ == '__main__':
    unittest.main()
