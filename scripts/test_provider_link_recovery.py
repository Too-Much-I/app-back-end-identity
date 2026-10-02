import copy
import unittest
from datetime import datetime, timedelta, timezone
from types import SimpleNamespace
from provider_link_recovery import validate, apply_resolution, UnsafeRecovery


class RecoveryTests(unittest.TestCase):
    def setUp(self):
        self.now = datetime(2026, 10, 2, tzinfo=timezone.utc)
        before = self.now - timedelta(minutes=10)
        self.s = {
            'attempt': {'_id':'attempt', 'version':1, 'state':'STARTED', 'expiresAt':before,
                        'userId':'owner','bindingId':'binding','bindingCreatedAt':before,
                        'projectId':'test-project','firebaseUid':'test-uid','epoch':0,'revision':1,
                        'provider':'GOOGLE','startedAt':before},
            'control': {'_id':'owner','version':2,'sessionEpoch':0,'activeLogoutId':'link:attempt','revision':2},
            'methods': {'_id':'owner','version':3,'bindingId':'binding','revision':1,
                        'blocked':{'GOOGLE':before},'authenticationFloors':{}},
            'binding': {'_id':'binding','userId':'owner','createdAt':before,'firebaseProjectId':'test-project','firebaseUid':'test-uid'},
            'user': {'_id':'owner','accountType':'MEMBER','status':'ACTIVE'},
            'socials':[{'provider':'KAKAO','providerSubject':'fake-subject'}],
            'otherLinks':[], 'unlinks':[], 'relinks':[], 'logouts':[]}
        self.remote = {'localId':'test-uid','disabled':False,'phoneNumber':'+820000000000',
                       'providerUserInfo':[{'providerId':'oidc.kakao','rawId':'fake-subject'}]}

    def test_valid_but_does_not_change_state(self):
        before = copy.deepcopy(self.s)
        validate(self.s, self.remote, self.now)
        self.assertEqual(self.s, before)

    def test_wrong_versions_slot_epoch_owner_binding_or_non_member_rejected(self):
        for section, field, value in [('attempt','state','COMPLETED'),('attempt','version',None),
            ('control','activeLogoutId','other'),('control','sessionEpoch',2),('methods','revision',2),
            ('binding','userId','other'),('user','status','WITHDRAWN'),('user','accountType','GUEST')]:
            with self.subTest(section=section, field=field):
                s=copy.deepcopy(self.s); s[section][field]=value
                with self.assertRaises(UnsafeRecovery): validate(s,self.remote,self.now)

    def test_live_permit_and_concurrent_operations_rejected(self):
        s=copy.deepcopy(self.s); s['attempt']['expiresAt']=self.now+timedelta(seconds=1)
        with self.assertRaises(UnsafeRecovery): validate(s,self.remote,self.now)
        for key in ['otherLinks','unlinks','relinks','logouts']:
            s=copy.deepcopy(self.s); s[key]=[{}]
            with self.assertRaises(UnsafeRecovery): validate(s,self.remote,self.now)

    def test_remote_target_or_drift_or_disabled_rejected(self):
        for change in [lambda r:r.update(disabled=True),lambda r:r.update(localId='other'),
                       lambda r:r.update(phoneNumber=None),
                       lambda r:r['providerUserInfo'].append({'providerId':'google.com','rawId':'fake-google'}),
                       lambda r:r['providerUserInfo'].append({'providerId':'apple.com','rawId':'fake-apple'})]:
            r=copy.deepcopy(self.remote);change(r)
            with self.assertRaises(UnsafeRecovery):validate(self.s,r,self.now)

    def test_resolution_uses_exact_cas_and_preserves_security_fields(self):
        calls=[]
        class Collection:
            def __init__(self,name):self.name=name
            def insert_one(self,doc,**kwargs):calls.append((self.name,doc))
            def update_one(self,query,update,**kwargs):
                calls.append((self.name,query,update));return SimpleNamespace(modified_count=1)
        class DB:
            provider_link_recovery_audit=Collection('audit')
            def __getitem__(self,name):return Collection(name)
        apply_resolution(DB(),self.s,'digest','TMI-191','FAILED',self.now,object())
        self.assertEqual(len(calls),4)
        for _,query,update in calls[1:]:
            self.assertIn('version',query)
            self.assertFalse({'blocked','authenticationFloors','sessionEpoch'} & set(update.get('$set',{})))
        self.assertEqual(calls[-1][2]['$set']['state'],'FAILED')
        self.assertIn('snapshots',calls[0][1])

    def test_changed_document_fails_before_further_writes(self):
        class Collection:
            def insert_one(self,*a,**k):pass
            def update_one(self,*a,**k):return SimpleNamespace(modified_count=0)
        class DB:
            provider_link_recovery_audit=Collection()
            def __getitem__(self,name):return Collection()
        with self.assertRaises(UnsafeRecovery):
            apply_resolution(DB(),self.s,'digest','TMI-191','FAILED',self.now,object())


if __name__ == '__main__':unittest.main()
