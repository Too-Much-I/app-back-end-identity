"use strict";
const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path');
const config=JSON.parse(fs.readFileSync(path.join(__dirname,'../docs/codex/deployment/production-settings-2026-10-08.json'),'utf8'));
test('configuration does not authorize service start or mutate old services',()=>{
    assert.equal(config.status,'SERVICES_HEALTHY_E2E_PENDING');
    assert.equal(config.deployableTaskDefinition,false); assert.equal(config.existingServicesUnchanged,true);
});
test('operating identity database and JWT trust are retained',()=>{
    const i=config.identity,l=config.learningCore;
    assert.equal(i.environment.MONGODB_DATABASE,'to-teacher-identity');
    assert.equal(l.environment.MONGODB_DATABASE,'to-teacher-app');
    assert.equal(i.environment.JWT_ISSUER,l.environment.IDENTITY_ISSUER);
    assert.equal(i.environment.JWT_ISSUER,'https://identity-staging.to-teacher.com');
    assert.equal(i.environment.JWT_AUDIENCE,l.environment.IDENTITY_AUDIENCE);
    assert.deepEqual(i.inheritSecretReferencesFromOperatingTask.names,['MONGODB_URI','JWT_PRIVATE_KEY_PEM','JWT_PUBLIC_KEY_PEM']);
    assert.equal(i.inheritSecretReferencesFromOperatingTask.task,'tosunsaeng-identity:23');
    assert.equal(i.environment.AUTH_REISSUE_ENCRYPTION_ENVIRONMENT,'prod');
});
test('challenge endpoint and both callback directions point to new HTTPS host',()=>{
    assert.equal(config.identity.environment.USER_WITHDRAWN_PUBLISHER_ENDPOINT,
        'https://api-prod.to-teacher.com/internal/v1/events/withdrawn');
    const endpoint=new URL(config.learningCore.environment.CHALLENGE_AI_ENDPOINT);
    assert.equal(endpoint.protocol,'https:'); assert.equal(endpoint.host,'api-prod.to-teacher.com');
    assert.equal(endpoint.pathname,'/v1/challenges/evaluations');
    assert.equal(config.challengeAi.workerEnvironment.LEARNING_RESULT_CALLBACK_URL,
        'https://api-prod.to-teacher.com/internal/v1/challenges/grading/callback');
    for(const k of ['FEEDBACK_CALLBACK_URL','AZURE_CALLBACK_URL'])
        assert.equal(new URL(config.examAi.environmentForApiAndAllWorkers[k]).host,'api-prod.to-teacher.com');
    assert.equal(config.plannedAlbRouting[1].path,endpoint.pathname);
});
test('both AI queues and job namespaces differ from old and test',()=>{
    for(const k of ['EVALUATION_REDIS_QUEUE','EVALUATION_REDIS_PROCESSING_QUEUE','EVALUATION_REDIS_JOB_PREFIX'])
        assert.ok(config.examAi.environmentForApiAndAllWorkers[k].startsWith('eval_agent-prod:'));
    for(const k of ['LEARNING_REDIS_QUEUE','LEARNING_REDIS_PROCESSING_QUEUE','LEARNING_REDIS_JOB_PREFIX'])
        assert.ok(config.challengeAi.environmentForApiAndWorker[k].startsWith('app-ai-learning-prod:{learning-jobs}:'));
});
test('launch dependencies and deferred workers are explicit',()=>{
    const i=config.identity,l=config.learningCore;
    assert.equal(i.environment.OWNER_EVENT_MERGE_COMPLETION_PROFILE,'LEARNING_CORE_ONLY');
    for(const k of ['WORKLOAD_JWT_ENABLED','OWNER_EVENT_USER_MERGED_CAPTURE_ENABLED','OWNER_EVENT_LEARNING_CORE_USER_MERGED_PUBLISHER_ENABLED'])
        assert.equal(i.launchTargetFlags[k],'true');
    assert.equal(l.launchTargetFlags.USER_WITHDRAWN_DENY_GATE_ENABLED,'true');
    assert.equal(l.environment.IDENTITY_CLOCK_SKEW,l.environment.USER_WITHDRAWN_ALLOWED_VERIFIER_CLOCK_SKEW);
    assert.equal(l.environment.USER_WITHDRAWN_MAX_ACCESS_TOKEN_LIFETIME,i.environment.JWT_ACCESS_TOKEN_TTL);
    assert.ok(i.environment.PHONE_ELIGIBILITY_BINDING_CONSUMER_SCOPE_ID);
    for(const service of [i,l]){
        assert.ok(Object.values(service.holdOffFlags).every(v=>v==='false'));
        assert.ok(Object.keys(service.holdOffFlags).every(k=>!(k in service.launchTargetFlags)));
    }
    assert.equal(l.springPropertyOverrides['app.notifications.sending-enabled'],false);
    assert.equal(l.springPropertyOverrides['app.notifications.dry-run'],true);
});
test('Identity flag names and endpoint names exist in current configuration',()=>{
    const source=fs.readFileSync(path.join(__dirname,'../src/main/resources/application.yml'),'utf8');
    for(const name of [...Object.keys(config.identity.launchTargetFlags),...Object.keys(config.identity.holdOffFlags)])
        assert.ok(source.includes('${'+name+':'),name);
});
