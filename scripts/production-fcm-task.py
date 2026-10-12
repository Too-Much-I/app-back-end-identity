"""Build a new LC task revision with an ephemeral, private FCM credential file.

No credentials are accepted or emitted by this helper. Secret ARN only.
"""
import copy
import json

ENTRYPOINT_SCRIPT = '''set -eu
umask 077
credential_dir=$(mktemp -d /tmp/lc-fcm.XXXXXX)
trap 'rm -f "$credential_dir/service-account.json"; rmdir "$credential_dir"' EXIT
test -n "$FCM_SERVICE_ACCOUNT_JSON"
printf '%s' "$FCM_SERVICE_ACCOUNT_JSON" > "$credential_dir/service-account.json"
unset FCM_SERVICE_ACCOUNT_JSON
export APP_NOTIFICATIONS_CREDENTIAL_FILE="$credential_dir/service-account.json"
exec java -jar /app/app.jar
'''


def build_task(source, secret_arn, ready_at):
    if source['family'] != 'tosunsaeng-learning-core-prod':
        raise ValueError('Only new production LC may be changed')
    if ':secret:tosunsaeng/prod/learning-core/fcm-' not in secret_arn:
        raise ValueError('Dedicated FCM secret required')
    if not ready_at.endswith('Z'):
        raise ValueError('Explicit UTC tracking readiness required')
    result = copy.deepcopy(source)
    for field in ('taskDefinitionArn', 'revision', 'status', 'requiresAttributes',
                  'compatibilities', 'registeredAt', 'registeredBy', 'deregisteredAt'):
        result.pop(field, None)
    apps = [c for c in result['containerDefinitions'] if any(
        p.get('containerPort') == 8080 for p in c.get('portMappings', []))]
    if len(apps) != 1:
        raise ValueError('Exactly one LC API container required')
    app = apps[0]
    env = {x['name']: x['value'] for x in app.get('environment', [])}
    config = json.loads(env.get('SPRING_APPLICATION_JSON', '{}'))
    notifications = config.setdefault('app', {}).setdefault('notifications', {})
    for key in list(config):
        if key.startswith('app.notifications.'):
            notifications[key.removeprefix('app.notifications.')] = config.pop(key)
    if notifications.get('api-enabled') is not True or notifications.get('tracking-enabled') is not True:
        raise ValueError('API and tracking must already be enabled')
    notifications.update({'sending-enabled': True, 'dry-run': False,
                          'project-id': 'to-teacher-firebase', 'tracking-ready-at': ready_at})
    # Do not let higher-priority JSON override the temporary file environment value.
    notifications.pop('credential-file', None)
    config.pop('app.notifications.credential-file', None)
    env['SPRING_APPLICATION_JSON'] = json.dumps(config, separators=(',', ':'))
    app['environment'] = [{'name': k, 'value': v} for k, v in env.items()]
    app['secrets'] = [s for s in app.get('secrets', []) if s['name'] != 'FCM_SERVICE_ACCOUNT_JSON']
    app['secrets'].append({'name': 'FCM_SERVICE_ACCOUNT_JSON', 'valueFrom': secret_arn})
    app['entryPoint'] = ['/bin/sh', '-c']
    app['command'] = [ENTRYPOINT_SCRIPT]
    return result
