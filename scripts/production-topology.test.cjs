"use strict";
const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path');
const read=name=>JSON.parse(fs.readFileSync(path.join(__dirname,'../docs/codex/deployment/'+name),'utf8'));
const plan=read('production-topology-2026-10-08.json');
const settings=read('production-settings-2026-10-08.json');
test('prepared plan cannot start workloads',()=>{
 assert.equal(plan.status,'SERVICES_HEALTHY_E2E_PENDING'); assert.equal(plan.deployableTaskDefinition,false);
 assert.equal(plan.initialDesiredCount,0); assert.equal(plan.createAutoscaling,false);
 assert.equal(plan.modifyExistingServices,false); assert.equal(plan.enableExecuteCommand,false);
});
test('four services use immutable image references and approved groups',()=>{
 assert.equal(Object.keys(plan.services).length,4);
 for(const [key,s] of Object.entries(plan.services)){
  assert.match(s.image,/@sha256:[a-f0-9]{64}$/);
  assert.equal(s.securityGroup,settings.runtimeNetworkProvisioning.securityGroups[key]);
  assert.equal(s.name,settings[key].plannedService);
 }
});
test('only new domains routed; exact challenge route wins',()=>{
 const [i,a,l]=plan.listenerRules;
 assert.equal(i.host,'identity-prod.to-teacher.com');
 assert.equal(a.host,l.host); assert.equal(a.host,'api-prod.to-teacher.com');
 assert.ok(a.priority<l.priority); assert.equal(a.path,'/v1/challenges/evaluations');
 assert.equal(plan.services.examAi.targetGroupName,null);
 assert.equal(new Set(plan.listenerRules.map(r=>r.priority)).size,3);
});
test('private networking and three target groups',()=>{
 assert.equal(plan.assignPublicIp,'DISABLED');
 assert.equal(Object.values(plan.services).filter(s=>s.targetGroupName).length,3);
 for(const s of Object.values(plan.services)) if(s.targetGroupName) assert.ok(s.targetGroupName.length<=32);
 assert.equal(plan.targetGroupDefaults.targetType,'ip');
});
