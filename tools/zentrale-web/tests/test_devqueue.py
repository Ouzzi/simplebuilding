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


if __name__ == '__main__':
    unittest.main()
