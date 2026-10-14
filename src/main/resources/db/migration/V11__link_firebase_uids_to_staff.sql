ALTER TABLE mottainai.app_user
    ADD COLUMN firebase_uid VARCHAR(128);

CREATE UNIQUE INDEX uq_app_user_firebase_uid
    ON mottainai.app_user (firebase_uid)
    WHERE firebase_uid IS NOT NULL;
