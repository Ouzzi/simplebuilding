import http.client
import json
import os
import tempfile
import time
import unittest
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from devqueue import DevQueueServer, Handler


class DevQueueTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.data = Path(self.tmp.name)
        self.server = DevQueueServer(('127.0.0.1', 0), self.data)
        self.thread = None
        import threading

        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        self.port = self.server.server_address[1]

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        self.tmp.cleanup()

    def conn(self):
        return http.client.HTTPConnection('127.0.0.1', self.port)

    def test_create_and_manage(self):
        conn = self.conn()
        conn.request('POST', '/api/queue', body=json.dumps({
            'category': 'textures',
            'kind': 'texture',
            'targets': [{'id': 't1', 'label': 'T1', 'path': 'p1'}],
            'comment': 'hi'
        }), headers={'Content-Type': 'application/json'})
        resp = conn.getresponse()
        self.assertEqual(resp.status, 403)
        resp.read()

        conn = self.conn()
        conn.request('POST', '/api/queue', body=json.dumps({
            'category': 'textures',
            'kind': 'texture',
            'targets': [{'id': 't1', 'label': 'T1', 'path': 'p1'}],
            'comment': 'hi'
        }), headers={'Content-Type': 'application/json', 'Cf-Access-Authenticated-User-Email': 'u@test'})
        resp = conn.getresponse()
        self.assertEqual(resp.status, 201)
        data = json.loads(resp.read())
        qid = data['id']
        conn.close()

        conn = self.conn()
        conn.request('GET', '/api/queue', headers={'Cf-Access-Authenticated-User-Email': 'u@test'})
        resp = conn.getresponse()
        self.assertEqual(resp.status, 200)
        items = json.loads(resp.read())
        self.assertEqual(len(items), 1)
        conn.close()

        conn = self.conn()
        conn.request('PATCH', f'/api/queue/{qid}', body=json.dumps({'status': 'done', 'comment': 'ok'}), headers={'Content-Type': 'application/json', 'Cf-Access-Authenticated-User-Email': 'u@test'})
        resp = conn.getresponse()
        self.assertEqual(resp.status, 200)
        conn.close()

        conn = self.conn()
        conn.request('DELETE', f'/api/queue/{qid}', headers={'Cf-Access-Authenticated-User-Email': 'u@test'})
        resp = conn.getresponse()
        self.assertEqual(resp.status, 204)
        conn.close()

    def test_run_flow(self):
        conn = self.conn()
        conn.request('POST', '/api/queue', body=json.dumps({
            'category': 'textures',
            'kind': 'texture',
            'targets': [{'id': 't1', 'label': 'T1', 'path': 'p1'}],
            'comment': 'c'
        }), headers={'Content-Type': 'application/json', 'Cf-Access-Authenticated-User-Email': 'u@test'})
        resp = conn.getresponse()
        resp.read()
        conn.close()

        conn = self.conn()
        conn.request('POST', '/api/run', body=json.dumps({'category': 'textures'}), headers={'Content-Type': 'application/json', 'Cf-Access-Authenticated-User-Email': 'u@test'})
        resp = conn.getresponse()
        self.assertEqual(resp.status, 201)
        res = json.loads(resp.read())
        self.assertIn('id', res)
        conn.close()

        conn = self.conn()
        conn.request('GET', '/api/runs', headers={'Cf-Access-Authenticated-User-Email': 'u@test'})
        resp = conn.getresponse()
        self.assertEqual(resp.status, 200)
        runs = json.loads(resp.read())
        self.assertTrue(len(runs) > 0)
        self.assertEqual(runs[0]['state'], 'new')
        conn.close()

        run_file = self.data / 'runs' / f"{runs[0]['id']}.json"
        self.assertTrue(run_file.exists())

    def test_validation(self):
        cases = [
            ({'category': 'bad', 'kind': 'texture', 'targets': [{'id':'a','label':'b','path':'c'}], 'comment':'c'}, 400),
            ({'category': 'textures', 'kind': 'bad', 'targets': [{'id':'a','label':'b','path':'c'}], 'comment':'c'}, 400),
            ({'category': 'textures', 'kind': 'texture', 'targets': [], 'comment':'c'}, 400),
            ({'category': 'textures', 'kind': 'texture', 'targets': [{'id':'a','label':'b','path':'c'}], 'comment':'x'*5000}, 400),
        ]
        for body, code in cases:
            conn = self.conn()
            conn.request('POST', '/api/queue', body=json.dumps(body), headers={'Content-Type': 'application/json', 'Cf-Access-Authenticated-User-Email': 'u@test'})
            resp = conn.getresponse()
            self.assertEqual(resp.status, code)
            resp.read()
            conn.close()

    def call(self, method, path, body=None, auth=True):
        conn = self.conn()
        headers = {'Content-Type': 'application/json'}
        if auth:
            headers['Cf-Access-Authenticated-User-Email'] = 'u@test'
        conn.request(method, path, body=None if body is None else json.dumps(body), headers=headers)
        resp = conn.getresponse()
        raw = resp.read()
        conn.close()
        return resp.status, (json.loads(raw) if raw else None)

    def test_needs_access_header(self):
        for method, path in (('GET', '/api/queue'), ('GET', '/api/runs'), ('POST', '/api/run'), ('DELETE', '/api/queue/1')):
            self.assertEqual(self.call(method, path, {} if method == 'POST' else None, auth=False)[0], 403, path)

    def test_run_takes_only_open_entries_of_its_category(self):
        t = {'id': 'a', 'label': 'A', 'path': 'p'}
        self.call('POST', '/api/queue', {'category': 'textures', 'kind': 'texture', 'targets': [t, dict(t, id='b')], 'comment': 'one comment'})
        self.call('POST', '/api/queue', {'category': 'code', 'kind': 'recipe', 'targets': [t], 'comment': ''})
        self.call('POST', '/api/queue', {'category': 'code', 'kind': 'note', 'targets': [t], 'comment': 'n'})
        status, run = self.call('POST', '/api/run', {'category': 'code'})
        self.assertEqual((status, run['entries']), (201, 2))
        status, queue = self.call('GET', '/api/queue')
        self.assertEqual(sorted(e['status'] for e in queue), ['open', 'started', 'started'])
        self.assertEqual(len([e for e in queue if e['category'] == 'textures'][0]['targets']), 2)
        # nothing open in code any more: no empty run file
        self.assertEqual(self.call('POST', '/api/run', {'category': 'code'})[1]['entries'], 0)
        self.assertEqual(len(list((self.data / 'runs').glob('*.json'))), 1)
        # the pickup marks the run; bad states and unknown runs are refused
        self.assertEqual(self.call('PATCH', '/api/runs/' + run['id'], {'state': 'picked', 'note': 'claude'})[0], 200)
        self.assertEqual(self.call('GET', '/api/runs')[1][0]['state'], 'picked')
        self.assertEqual(self.call('PATCH', '/api/runs/' + run['id'], {'state': 'weird'})[0], 400)
        self.assertEqual(self.call('PATCH', '/api/runs/nope', {'state': 'done'})[0], 404)
        self.assertEqual(self.call('PATCH', '/api/queue/' + queue[0]['id'], {'status': 'weird'})[0], 400)
        self.assertEqual(self.call('DELETE', '/api/queue/' + queue[0]['id'])[0], 204)


    def req(self, method, path, body=None, user='u@test'):
        conn = self.conn()
        headers = {'Content-Type': 'application/json'}
        if user:
            headers['Cf-Access-Authenticated-User-Email'] = user
        conn.request(method, path, body=None if body is None else (body if isinstance(body, str) else json.dumps(body)), headers=headers)
        resp = conn.getresponse()
        data = resp.read()
        conn.close()
        return resp.status, (json.loads(data) if data else None)

    def test_texture_save_flow(self):
        """TX22-5: listed own texture only, checked layers, pickup states; never mixed into Claude runs."""
        sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'textures'))
        import editor_save
        import layers as ly
        wl = editor_save.load_whitelist(self.server.repo)
        entry = next(t for t in wl.values() if t['w'] == 16 and t['h'] == 16 and not t['source'].startswith('build/'))
        doc = ly.from_rgba(16, 16, bytes([200, 10, 10, 255]) * 256)
        body = {'id': entry['id'], 'source': entry['source'], 'layers': ly.dumps(doc), 'comment': 'rot'}
        self.assertEqual(self.req('POST', '/api/textures', body, user=None)[0], 403)
        status, save = self.req('POST', '/api/textures', body)
        self.assertEqual(status, 201, save)
        self.assertEqual((save['state'], save['by'], save['source']), ('new', 'u@test', entry['source']))
        self.assertNotIn('layers', save)
        sid = save['sid']
        derived = next((t for t in wl.values() if t['source'].startswith('build/')), None)
        bad = [dict(body, id='simplebuilding:block/nope'), dict(body, source='build.gradle'),
               dict(body, source='src/main/resources/assets/x/textures/../../../../../build.gradle'),
               dict(body, layers='{"format": "x"}')]
        if derived:
            bad.append(dict(body, id=derived['id'], source=derived['source']))
        for b in bad:
            self.assertEqual(self.req('POST', '/api/textures', b)[0], 400, b.get('id'))
        self.assertEqual(self.req('POST', '/api/textures', 'x' * (editor_save.MAX_BODY + 10))[0], 413)
        status, full = self.req('GET', f'/api/textures/{sid}')
        self.assertEqual((status, ly.png_read(ly.png_write(16, 16, ly.compose(json.loads(full['layers']))))[0]), (200, 16))
        status, listed = self.req('GET', '/api/textures')
        self.assertEqual([s['sid'] for s in listed], [sid])
        self.assertNotIn('layers', listed[0])
        self.assertEqual(self.req('GET', '/api/textures/../queue')[0], 404)
        self.assertEqual(self.req('PATCH', f'/api/textures/{sid}', {'state': 'bogus'})[0], 400)
        status, done = self.req('PATCH', f'/api/textures/{sid}', {'state': 'ready', 'note': 'tex-x', 'branch': 'tex-x-' + sid})
        self.assertEqual((status, done['state'], done['branch']), (200, 'ready', 'tex-x-' + sid))
        self.assertEqual(self.req('GET', '/api/runs')[1], [], 'saves are no Claude runs')

if __name__ == '__main__':
    unittest.main()
