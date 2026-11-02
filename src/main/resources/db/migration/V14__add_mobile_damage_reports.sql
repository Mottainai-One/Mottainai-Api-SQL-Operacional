CREATE TABLE mottainai.damage_report (
    damage_report_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    store_id INTEGER NOT NULL REFERENCES mottainai.retail_store(store_id) ON DELETE RESTRICT,
    employee_id INTEGER NOT NULL REFERENCES mottainai.employee(employee_id) ON DELETE RESTRICT,
    product_id INTEGER NOT NULL REFERENCES mottainai.product(product_id) ON DELETE RESTRICT,
    reason VARCHAR(100) NOT NULL,
    quantity NUMERIC(12, 3) NOT NULL CHECK (quantity > 0),
    note VARCHAR(500),
    photo_url VARCHAR(2048) CHECK (photo_url IS NULL OR photo_url LIKE 'https://res.cloudinary.com/%/image/upload/%'),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_damage_report_store_created ON mottainai.damage_report(store_id, created_at DESC);
ALTER TABLE mottainai.damage_report ENABLE ROW LEVEL SECURITY;
ALTER TABLE mottainai.damage_report FORCE ROW LEVEL SECURITY;
CREATE POLICY damage_report_store_policy ON mottainai.damage_report
    FOR ALL TO mottainai_api
    USING (store_id = mottainai.fn_get_current_store_id())
    WITH CHECK (store_id = mottainai.fn_get_current_store_id());
GRANT SELECT, INSERT ON mottainai.damage_report TO mottainai_api;
GRANT USAGE, SELECT ON SEQUENCE mottainai.damage_report_damage_report_id_seq TO mottainai_api;
