CREATE OR REPLACE VIEW mottainai.vw_api_system_event WITH (security_invoker = true) AS
SELECT event_id, event_uuid, event_type, aggregate_type, aggregate_id,
       event_data::TEXT AS event_data, priority, status::TEXT AS status,
       retry_count, error_message, occurred_at, processed_at, company_id
FROM mottainai.event_queue;

CREATE OR REPLACE VIEW mottainai.vw_api_system_log WITH (security_invoker = true) AS
SELECT logs.id, logs.source, logs.level, logs.module, logs.message, logs.stack_trace,
       logs.user_id, logs.ip_address, logs.created_at, linked_store.company_id
FROM (
    SELECT log_id AS id, 'SYSTEM' AS source, log_level::TEXT AS level, module, message,
           stack_trace, user_id, ip_address::TEXT AS ip_address, created_at
    FROM mottainai.system_log
    UNION ALL
    SELECT error_id AS id, 'ERROR' AS source, 'ERROR' AS level, function_name AS module,
           error_message AS message, stack_trace, user_id, NULL AS ip_address, created_at
    FROM mottainai.error_log
) logs
JOIN mottainai.app_user linked_user ON linked_user.user_id = logs.user_id
JOIN mottainai.employee linked_employee ON linked_employee.employee_id = linked_user.employee_id
JOIN mottainai.retail_store linked_store ON linked_store.store_id = linked_employee.store_id;

CREATE OR REPLACE VIEW mottainai.vw_api_system_job WITH (security_invoker = true) AS
SELECT job_id, job_name, job_type, start_time, end_time, duration_seconds,
       records_processed, success, details::TEXT AS details, company_id
FROM mottainai.job_log;

CREATE OR REPLACE VIEW mottainai.vw_api_purchase_order WITH (security_invoker = true) AS
SELECT purchase_order_id, store_id, supplier_id, employee_id, order_date,
       expected_delivery_date, status::TEXT AS status, observation, total_amount,
       version, deleted_at
FROM mottainai.purchase_order;

CREATE OR REPLACE VIEW mottainai.vw_api_purchase_order_item WITH (security_invoker = true) AS
SELECT purchase_order_item_id, purchase_order_id, product_id, requested_quantity,
       unit_price, subtotal, order_date, deleted_at
FROM mottainai.purchase_order_item;

CREATE OR REPLACE VIEW mottainai.vw_api_receiving WITH (security_invoker = true) AS
SELECT r.receiving_id, r.purchase_order_id, po.store_id, r.employee_id,
       r.receiving_date, r.status::TEXT AS status, r.observation, r.order_date,
       po.deleted_at AS purchase_order_deleted_at
FROM mottainai.receiving r
JOIN mottainai.purchase_order po
  ON po.purchase_order_id = r.purchase_order_id AND po.order_date = r.order_date;

CREATE OR REPLACE VIEW mottainai.vw_api_receiving_item WITH (security_invoker = true) AS
SELECT ri.receiving_item_id, ri.receiving_id, ri.purchase_order_item_id,
       poi.product_id, poi.requested_quantity, ri.received_quantity, ri.unit_price,
       ri.manufacture_date, ri.expiration_date, ri.observation, poi.deleted_at
FROM mottainai.receiving_item ri
JOIN mottainai.purchase_order_item poi
  ON poi.purchase_order_item_id = ri.purchase_order_item_id;

CREATE OR REPLACE VIEW mottainai.vw_api_store_product_price WITH (security_invoker = true) AS
SELECT store_product_price_id, store_id, product_id, regular_price, valid_from,
       valid_until, active, version
FROM mottainai.store_product_price;

CREATE OR REPLACE VIEW mottainai.vw_api_product_history WITH (security_invoker = true) AS
SELECT history_id, product_id, field_name, old_value, new_value, changed_by, changed_at
FROM mottainai.product_history;

CREATE OR REPLACE VIEW mottainai.vw_api_product_price_history WITH (security_invoker = true) AS
SELECT price_history_id, product_id, old_price, new_price, changed_by, changed_at
FROM mottainai.product_price_history;

CREATE INDEX IF NOT EXISTS idx_purchase_order_store_order_date_active
    ON mottainai.purchase_order (store_id, order_date DESC) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_purchase_order_supplier_order_date_active
    ON mottainai.purchase_order (supplier_id, order_date DESC) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_receiving_receiving_date
    ON mottainai.receiving (receiving_date DESC);
CREATE INDEX IF NOT EXISTS idx_event_queue_company_occurred_at
    ON mottainai.event_queue (company_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_job_log_company_start_time
    ON mottainai.job_log (company_id, start_time DESC);
CREATE INDEX IF NOT EXISTS idx_product_history_product_changed_at
    ON mottainai.product_history (product_id, changed_at DESC);
CREATE INDEX IF NOT EXISTS idx_product_price_history_product_changed_at
    ON mottainai.product_price_history (product_id, changed_at DESC);

CREATE OR REPLACE FUNCTION mottainai.fn_api_insert_purchase_order(
    p_store_id INTEGER,
    p_supplier_id INTEGER,
    p_employee_id INTEGER,
    p_expected_delivery_date DATE,
    p_observation TEXT,
    p_total_amount NUMERIC
) RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_id INTEGER;
BEGIN
    INSERT INTO mottainai.purchase_order
        (store_id, supplier_id, employee_id, expected_delivery_date, observation, total_amount)
    VALUES
        (p_store_id, p_supplier_id, p_employee_id, p_expected_delivery_date, p_observation, p_total_amount)
    RETURNING purchase_order_id INTO v_id;
    RETURN v_id;
END;
$$;

CREATE OR REPLACE FUNCTION mottainai.fn_api_insert_receiving(
    p_purchase_order_id INTEGER,
    p_employee_id INTEGER,
    p_observation TEXT,
    p_order_date TIMESTAMP
) RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_id INTEGER;
BEGIN
    INSERT INTO mottainai.receiving (purchase_order_id, employee_id, observation, order_date)
    VALUES (p_purchase_order_id, p_employee_id, p_observation, p_order_date)
    RETURNING receiving_id INTO v_id;
    RETURN v_id;
END;
$$;

CREATE OR REPLACE FUNCTION mottainai.fn_api_upsert_inventory(
    p_store_id INTEGER,
    p_batch_id INTEGER
) RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_id INTEGER;
BEGIN
    INSERT INTO mottainai.inventory
        (store_id, batch_id, inventory_type, current_quantity, minimum_quantity)
    VALUES (p_store_id, p_batch_id, 'NORMAL', 0, 0)
    ON CONFLICT (store_id, batch_id, inventory_type)
    DO UPDATE SET deleted_at = NULL, updated_at = NOW()
    RETURNING inventory_id INTO v_id;
    RETURN v_id;
END;
$$;

REVOKE ALL ON FUNCTION mottainai.fn_api_insert_purchase_order(INTEGER, INTEGER, INTEGER, DATE, TEXT, NUMERIC) FROM PUBLIC;
REVOKE ALL ON FUNCTION mottainai.fn_api_insert_receiving(INTEGER, INTEGER, TEXT, TIMESTAMP) FROM PUBLIC;
REVOKE ALL ON FUNCTION mottainai.fn_api_upsert_inventory(INTEGER, INTEGER) FROM PUBLIC;

GRANT SELECT ON
    mottainai.vw_api_system_event,
    mottainai.vw_api_system_log,
    mottainai.vw_api_system_job,
    mottainai.vw_api_purchase_order,
    mottainai.vw_api_purchase_order_item,
    mottainai.vw_api_receiving,
    mottainai.vw_api_receiving_item,
    mottainai.vw_api_store_product_price,
    mottainai.vw_api_product_history,
    mottainai.vw_api_product_price_history
TO mottainai_api;

GRANT EXECUTE ON FUNCTION mottainai.fn_api_insert_purchase_order(INTEGER, INTEGER, INTEGER, DATE, TEXT, NUMERIC) TO mottainai_api;
GRANT EXECUTE ON FUNCTION mottainai.fn_api_insert_receiving(INTEGER, INTEGER, TEXT, TIMESTAMP) TO mottainai_api;
GRANT EXECUTE ON FUNCTION mottainai.fn_api_upsert_inventory(INTEGER, INTEGER) TO mottainai_api;
