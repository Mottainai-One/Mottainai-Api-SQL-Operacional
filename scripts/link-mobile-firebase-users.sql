-- Administrative, manually reviewed data repair; NOT a Flyway migration.
-- Fill NULL IDs with the actual authorized accounts. Never infer store_id from "loja04".
-- Run first with ROLLBACK (default). Review results, then explicitly replace the final
-- ROLLBACK with COMMIT for the approved write. No PII encryption keys are needed.
\set ON_ERROR_STOP on
BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';
SET LOCAL search_path = mottainai, public;

CREATE TEMP TABLE mobile_links (
    account_label text NOT NULL,
    firebase_uid text PRIMARY KEY,
    user_id integer UNIQUE NOT NULL,
    employee_id integer UNIQUE NOT NULL,
    store_id integer NOT NULL,
    company_id integer NOT NULL,
    role_id integer NOT NULL,
    permission_level integer NOT NULL CHECK (permission_level IN (100,80,40))
) ON COMMIT DROP;

-- Replace ALL NULLs. The script intentionally stops until real IDs are supplied.
INSERT INTO mobile_links VALUES
 ('dono@mottainai.com', '5hNPg54PYMPThVMomTDj4Phc9vj1', NULL, NULL, NULL, NULL, NULL, 100),
 ('gerente@mottainai.com', 'msD7sQjhiPPcNKFBqr3RJ4cFcjB2', NULL, NULL, NULL, NULL, NULL, 80),
 ('estoquista@mottainai.com', '6Rq1eHXeCTPcfrKRb9fitRux9aJ2', NULL, NULL, NULL, NULL, NULL, 40);

-- Serialize account/assignment changes during the controlled bootstrap.
LOCK TABLE mottainai.app_user, mottainai.employee, mottainai.employee_role,
           mottainai.retail_store, mottainai.company, mottainai.subscription_plan IN SHARE ROW EXCLUSIVE MODE;

DO $$
BEGIN
 IF (SELECT count(*) FROM mobile_links) <> 3 THEN
   RAISE EXCEPTION 'Expected exactly three explicitly approved test accounts';
 END IF;
 IF EXISTS (
   SELECT 1 FROM mobile_links m
   LEFT JOIN mottainai.app_user u ON u.user_id=m.user_id AND u.employee_id=m.employee_id
   LEFT JOIN mottainai.employee e ON e.employee_id=m.employee_id AND e.store_id=m.store_id
   LEFT JOIN mottainai.retail_store s ON s.store_id=m.store_id AND s.company_id=m.company_id
   LEFT JOIN mottainai.company c ON c.company_id=m.company_id
   LEFT JOIN mottainai.employee_role r ON r.role_id=m.role_id AND r.permission_level=m.permission_level
   WHERE u.user_id IS NULL OR u.deleted_at IS NOT NULL
      OR e.employee_id IS NULL OR e.deleted_at IS NOT NULL
      OR s.store_id IS NULL OR NOT s.active OR s.deleted_at IS NOT NULL
      OR c.company_id IS NULL OR NOT c.active OR c.deleted_at IS NOT NULL
      OR r.role_id IS NULL OR NOT r.active OR r.deleted_at IS NOT NULL
 ) THEN
   RAISE EXCEPTION 'Invalid account/employee/store/company/canonical role. No automatic reassignment of stores is allowed';
 END IF;
 IF EXISTS (SELECT 1 FROM mobile_links m JOIN mottainai.app_user u
            ON u.firebase_uid=m.firebase_uid WHERE u.user_id<>m.user_id) THEN
   RAISE EXCEPTION 'A Firebase UID is already linked to another SQL account';
 END IF;
 IF EXISTS (SELECT 1 FROM mobile_links m JOIN mottainai.app_user u USING(user_id)
            WHERE NULLIF(btrim(u.firebase_uid),'') IS NOT NULL AND u.firebase_uid<>m.firebase_uid) THEN
   RAISE EXCEPTION 'Target SQL account already belongs to another Firebase identity';
 END IF;
END $$;

-- Same occupied-seat rule as the API, excluding the targets then adding their final seats.
DO $$
BEGIN
 IF EXISTS (
   SELECT 1 FROM mottainai.company c
   JOIN mottainai.subscription_plan p ON p.plan_id=c.plan_id
   WHERE c.company_id IN (SELECT company_id FROM mobile_links)
     AND (NOT p.active OR p.deleted_at IS NOT NULL OR p.user_limit IS NULL OR
       (SELECT count(*) FROM mottainai.app_user u
        JOIN mottainai.employee e ON e.employee_id=u.employee_id
        JOIN mottainai.retail_store s ON s.store_id=e.store_id
        WHERE s.company_id=c.company_id AND u.deleted_at IS NULL AND e.deleted_at IS NULL
          AND u.user_id NOT IN (SELECT user_id FROM mobile_links)
          AND ((u.active AND e.active) OR
               (NOT u.active AND NOT u.password_set AND u.firebase_uid IS NULL)))
        + (SELECT count(*) FROM mobile_links m WHERE m.company_id=c.company_id) > p.user_limit)
 ) THEN
   RAISE EXCEPTION 'Activating the approved accounts would exceed the plan or use an inactive plan';
 END IF;
END $$;

-- Review these BEFORE values against the approved employee identities privately.
SELECT m.account_label, u.user_id, u.employee_id, u.firebase_uid AS old_uid,
       u.active AS old_user_active, e.active AS old_employee_active,
       e.role_id AS old_role_id, m.role_id AS approved_role_id, m.store_id, m.company_id
FROM mobile_links m JOIN mottainai.app_user u USING(user_id)
JOIN mottainai.employee e ON e.employee_id=u.employee_id;

-- Changes are limited to these three approved employee/account records.
UPDATE mottainai.employee e SET role_id=m.role_id, active=true
FROM mobile_links m WHERE e.employee_id=m.employee_id;
UPDATE mottainai.app_user u SET firebase_uid=m.firebase_uid, active=true,
       token_version=COALESCE(u.token_version,0)+1
FROM mobile_links m WHERE u.user_id=m.user_id;
UPDATE mottainai.staff_session s SET revoked_at=CURRENT_TIMESTAMP
FROM mobile_links m WHERE s.user_id=m.user_id AND s.revoked_at IS NULL;

SELECT m.account_label, u.user_id, u.employee_id, u.firebase_uid, u.active,
       e.role_id, r.permission_level, e.store_id, m.company_id
FROM mobile_links m JOIN mottainai.app_user u USING(user_id)
JOIN mottainai.employee e ON e.employee_id=u.employee_id
JOIN mottainai.employee_role r ON r.role_id=e.role_id;

-- Default is a rehearsal: nothing is persisted.
ROLLBACK;
