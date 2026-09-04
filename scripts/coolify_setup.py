#!/usr/bin/env python3
"""Create Coolify project + static app for Fine Volume Tuner landing."""
import json, urllib.request, re, sys

ENV = {}
for line in open('/home/hermeswebui/.hermes/profiles/dev/.env'):
    m = re.match(r'^(COOLIFY_BASE_URL|COOLIFY_ACCESS_TOKEN)=(.*)$', line.strip())
    if m:
        ENV[m.group(1)] = m.group(2)

BASE = ENV['COOLIFY_BASE_URL'].rstrip('/')
TOK = ENV['COOLIFY_ACCESS_TOKEN']
UA = 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36'

def api(method, path, payload=None):
    data = json.dumps(payload).encode() if payload is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header('Authorization', f'Bearer {TOK}')
    req.add_header('User-Agent', UA)
    if data:
        req.add_header('Content-Type', 'application/json')
    with urllib.request.urlopen(req, timeout=20) as r:
        body = r.read().decode()
        return json.loads(body) if body else {}

# 1. project
projects = api('GET', '/api/v1/projects')
proj = next((p for p in projects if p['name'] == 'Fine Volume Tuner'), None)
if not proj:
    proj = api('POST', '/api/v1/projects', {'name': 'Fine Volume Tuner', 'description': 'Landing + APK download'})
else:
    proj = api('GET', f"/api/v1/projects/{proj['uuid']}")  # detail embeds environments
print('project:', proj['uuid'])

# 2. environment (embedded in project object)
env = proj['environments'][0]
print('environment:', env['uuid'])

# 3. static app via dockerfile (listed/created through /api/v1/applications)
apps = api('GET', '/api/v1/applications')
existing = next((a for a in apps if a.get('name') == 'fvt-landing'), None)
if existing:
    app = existing
    print('app exists:', app['uuid'])
else:
    payload = {
        'project_uuid': proj['uuid'],
        'environment_name': 'production',
        'name': 'fvt-landing',
        'static_dockerfile': True,
        'dockerfile': 'FROM nginx:alpine\nCOPY site /usr/share/nginx/html',
    }
    app = api('POST', f"/api/v1/environments/{env['uuid']}/applications", payload)
    print('app created:', app['uuid'])

with open('/tmp/fvt-app-uuid', 'w') as f:
    f.write(app['uuid'])
print('OK')
