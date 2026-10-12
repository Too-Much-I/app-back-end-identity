"""Prepare the approved four-flag withdrawal change on new prod Identity only."""
import copy
import json

FLAGS = ('FIREBASE_WITHDRAWAL_ENABLED', 'FIREBASE_WITHDRAWAL_CLEANUP_ENABLED',
         'FIREBASE_WITHDRAWAL_IDENTITY_RELEASE_ENABLED', 'USER_WITHDRAWN_PUBLISHER_ENABLED')
PROPERTY_PATHS = ('firebase-withdrawal', 'firebase-withdrawal-cleanup',
                  'firebase-withdrawal-identity-release', 'user-withdrawn-publisher')
ENDPOINT = 'https://api-prod.to-teacher.com/internal/v1/events/withdrawn'


def build_task(source):
    if source['family'] != 'tosunsaeng-identity-prod':
        raise ValueError('Only the new production Identity service is in scope')
    result = copy.deepcopy(source)
    for key in ('taskDefinitionArn', 'revision', 'status', 'requiresAttributes',
                'compatibilities', 'registeredAt', 'registeredBy', 'deregisteredAt'):
        result.pop(key, None)
    apps = [c for c in result['containerDefinitions'] if any(
        p.get('containerPort') == 8081 for p in c.get('portMappings', []))]
    if len(apps) != 1:
        raise ValueError('Expected one Identity API container')
    app = apps[0]
    values = {v['name']: v['value'] for v in app.get('environment', [])}
    if values.get('FIREBASE_AUTH_ENABLED') != 'true' or values.get('WORKLOAD_JWT_ENABLED') != 'true':
        raise ValueError('Firebase authentication and workload JWT must already be enabled')
    if values.get('USER_WITHDRAWN_PUBLISHER_ENDPOINT') not in (
            ENDPOINT, 'https://api-prod.to-teacher.com/internal/v1/events/user-withdrawn'):
        raise ValueError('Only the new Learning Core withdrawal endpoint is permitted')
    overrides = json.loads(values.get('SPRING_APPLICATION_JSON', '{}'))
    for path in PROPERTY_PATHS:
        if 'app.' + path + '.enabled' in overrides or 'enabled' in overrides.get('app', {}).get(path, {}):
            raise ValueError('Explicit JSON override requires review')
    if any(s['name'] in FLAGS for s in app.get('secrets', [])):
        raise ValueError('Unexpected secret-backed feature flag')
    if any(key not in values for key in FLAGS):
        raise ValueError('All four flags must exist in the source revision')
    for item in app['environment']:
        if item['name'] in FLAGS:
            item['value'] = 'true'
        elif item['name'] == 'USER_WITHDRAWN_PUBLISHER_ENDPOINT':
            item['value'] = ENDPOINT
    return result
