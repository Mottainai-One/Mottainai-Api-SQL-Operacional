"""Exercise the administrative script against a disposable PostgreSQL, never the cloud."""
from pathlib import Path
import subprocess, time

name = 'mottainai-firebase-links-test'
script = (Path(__file__).parent / 'link-mobile-firebase-users.sql').read_text(encoding='utf-8')
fixture = '''
DROP SCHEMA IF EXISTS mottainai CASCADE;
CREATE SCHEMA mottainai;
CREATE TABLE mottainai.subscription_plan(plan_id int PRIMARY KEY, user_limit int, active boolean, deleted_at timestamp);
CREATE TABLE mottainai.company(company_id int PRIMARY KEY, plan_id int, active boolean, deleted_at timestamp);
CREATE TABLE mottainai.retail_store(store_id int PRIMARY KEY,company_id int,active boolean,deleted_at timestamp);
CREATE TABLE mottainai.employee_role(role_id int PRIMARY KEY,permission_level int,active boolean,deleted_at timestamp);
CREATE TABLE mottainai.employee(employee_id int PRIMARY KEY,store_id int,role_id int,active boolean,deleted_at timestamp);
CREATE TABLE mottainai.app_user(user_id int PRIMARY KEY,employee_id int,firebase_uid text UNIQUE,active boolean,deleted_at timestamp,password_set boolean,token_version int);
CREATE TABLE mottainai.staff_session(user_id int,revoked_at timestamp);
INSERT INTO mottainai.subscription_plan VALUES(1,3,true,NULL);
INSERT INTO mottainai.company VALUES(4,1,true,NULL);
INSERT INTO mottainai.retail_store VALUES(4,4,true,NULL);
INSERT INTO mottainai.employee_role VALUES(1,100,true,NULL),(2,80,true,NULL),(4,40,true,NULL);
INSERT INTO mottainai.employee VALUES(1,4,1,false,NULL),(2,4,2,false,NULL),(3,4,4,false,NULL);
INSERT INTO mottainai.app_user VALUES(1,1,NULL,false,NULL,true,0),(2,2,NULL,false,NULL,true,0),(3,3,NULL,false,NULL,true,0);
INSERT INTO mottainai.staff_session VALUES(1,NULL),(2,NULL),(3,NULL);
'''
def run(*args, input=None):
    return subprocess.run(args,input=input,text=True,encoding='utf-8',capture_output=True)
def sql(text):
    return run('docker','exec','-i',name,'psql','-U','postgres','-v','ON_ERROR_STOP=1','-At',input=text)
filled = script
for level, ids in [(100,'1, 1, 4, 4, 1'),(80,'2, 2, 4, 4, 2'),(40,'3, 3, 4, 4, 4')]:
    filled = filled.replace(f'NULL, NULL, NULL, NULL, NULL, {level}',f'{ids}, {level}')
started = run('docker','run','-d','--name',name,'--network','none','-e','POSTGRES_HOST_AUTH_METHOD=trust','postgres:16-bookworm')
if started.returncode: raise RuntimeError(started.stderr)
try:
    for _ in range(30):
        if sql('SELECT 1;').returncode == 0: break
        time.sleep(1)
    cases = [
      ('missing IDs refused',script,False,''),
      ('valid rehearsal rolls back',filled,True,''),
      ('valid commit persists only approved records',filled.rsplit('ROLLBACK;',1)[0]+'COMMIT;',True,''),
      ('store/company mismatch refused',filled.replace('1, 1, 4, 4, 1, 100','1, 1, 4, 9, 1, 100'),False,''),
      ('occupied Firebase UID refused',filled,False,"INSERT INTO mottainai.app_user VALUES(99,99,'6Rq1eHXeCTPcfrKRb9fitRux9aJ2',false,NULL,true,0);"),
      ('foreign identity refused',filled,False,"UPDATE mottainai.app_user SET firebase_uid='other-identity' WHERE user_id=3;"),
      ('legacy permission level refused',filled,False,'UPDATE mottainai.employee_role SET permission_level=3 WHERE role_id=4;'),
      ('plan limit enforced',filled,False,'UPDATE mottainai.subscription_plan SET user_limit=2;'),
      ('deleted employee refused',filled,False,'UPDATE mottainai.employee SET deleted_at=now() WHERE employee_id=3;'),
    ]
    for label, body, ok, setup in cases:
        init = sql(fixture+setup)
        assert init.returncode == 0, init.stderr
        result = sql(body)
        assert (result.returncode==0)==ok, f'{label}: {result.stderr}'
        counts = sql("SELECT count(*) FROM mottainai.app_user WHERE active; SELECT count(*) FROM mottainai.staff_session WHERE revoked_at IS NOT NULL;").stdout.strip().splitlines()
        expected = ['3','3'] if 'commit persists' in label else ['0','0']
        assert counts == expected, (label, counts)
        print('PASS:',label,flush=True)
finally:
    run('docker','rm','-f',name)

