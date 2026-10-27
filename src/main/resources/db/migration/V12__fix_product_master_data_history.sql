CREATE OR REPLACE FUNCTION mottainai.fn_trg_product_history()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path TO mottainai, pg_catalog
AS $$
DECLARE
    v_user_id INTEGER := mottainai.fn_get_current_user_id();
BEGIN
    IF TG_OP <> 'UPDATE' THEN
        RETURN NEW;
    END IF;

    IF OLD.name IS DISTINCT FROM NEW.name THEN
        INSERT INTO mottainai.product_history (product_id, field_name, old_value, new_value, changed_by)
        VALUES (NEW.product_id, 'name', OLD.name, NEW.name, v_user_id);
    END IF;

    IF OLD.brand IS DISTINCT FROM NEW.brand THEN
        INSERT INTO mottainai.product_history (product_id, field_name, old_value, new_value, changed_by)
        VALUES (NEW.product_id, 'brand', OLD.brand, NEW.brand, v_user_id);
    END IF;

    IF OLD.weight IS DISTINCT FROM NEW.weight THEN
        INSERT INTO mottainai.product_history (product_id, field_name, old_value, new_value, changed_by)
        VALUES (NEW.product_id, 'weight', OLD.weight::TEXT, NEW.weight::TEXT, v_user_id);
    END IF;

    IF OLD.unit_measure IS DISTINCT FROM NEW.unit_measure THEN
        INSERT INTO mottainai.product_history (product_id, field_name, old_value, new_value, changed_by)
        VALUES (NEW.product_id, 'unit_measure', OLD.unit_measure, NEW.unit_measure, v_user_id);
    END IF;

    RETURN NEW;
END;
$$;
