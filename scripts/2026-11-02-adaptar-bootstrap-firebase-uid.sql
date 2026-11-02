-- PARA REVISAO E EXECUCAO PELA RESPONSAVEL PELO BANCO. NAO EXECUTADO PELO AGENTE.
-- Aiven MOTTAINAI, 02/11/2026. Corrige a funcao EXISTENTE, nao os usuarios.
-- Causa comprovada: a versao atual rejeita p_lookup='FIREBASE_UID' e retorna FALSE.
-- A API so fornece esse UID depois de validar criptograficamente o token Firebase.
-- Nao liberar EXECUTE publicamente nem alterar owner/grants/RLS neste script.
-- CREATE OR REPLACE preserva a assinatura e os privilegios ja existentes.
-- Nenhum INSERT/UPDATE/DELETE de funcionario, usuario, cargo, loja ou senha.
-- ROLLBACK e o padrao deste arquivo: apos revisao, a DBA troca a ultima linha
-- por COMMIT para aplicar. Toda verificacao deve ocorrer antes desse COMMIT.

BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';

CREATE OR REPLACE FUNCTION mottainai.fn_bootstrap_staff_context(p_identity text, p_lookup text)
RETURNS boolean
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'mottainai', 'pg_temp'
AS $function$
DECLARE
    v_user_id INTEGER;
    v_company_id INTEGER;
    v_store_id INTEGER;
BEGIN
    IF p_lookup NOT IN ('CPF', 'EMAIL', 'INVITED_CPF', 'FIREBASE_UID') THEN
        RETURN FALSE;
    END IF;

    SELECT u.user_id, s.company_id, s.store_id
    INTO v_user_id, v_company_id, v_store_id
    FROM mottainai.app_user u
    JOIN mottainai.employee e ON e.employee_id = u.employee_id
    JOIN mottainai.retail_store s ON s.store_id = e.store_id
    JOIN mottainai.company c ON c.company_id = s.company_id
    JOIN mottainai.employee_role r ON r.role_id = e.role_id
    WHERE u.deleted_at IS NULL
      AND e.deleted_at IS NULL
      AND s.deleted_at IS NULL
      AND c.deleted_at IS NULL
      AND r.deleted_at IS NULL
      AND s.active
      AND c.active
      AND r.active
      AND (
          (p_lookup = 'CPF' AND u.cpf = p_identity AND u.active AND e.active)
          OR
          (p_lookup = 'EMAIL' AND lower(u.email) = lower(p_identity) AND u.active AND e.active)
          OR
          (p_lookup = 'INVITED_CPF' AND u.cpf = p_identity
              AND NOT u.active AND NOT e.active AND NOT u.password_set)
          OR
          (p_lookup = 'FIREBASE_UID' AND u.firebase_uid = p_identity AND u.active AND e.active)
      );

    IF v_user_id IS NULL THEN
        RETURN FALSE;
    END IF;

    PERFORM set_config('app.current_user_id', v_user_id::TEXT, TRUE);
    PERFORM set_config('app.current_company_id', v_company_id::TEXT, TRUE);
    PERFORM set_config('app.current_store_id', v_store_id::TEXT, TRUE);
    RETURN TRUE;
END;
$function$;

-- Verificar o contexto dentro da MESMA transacao; os valores sao locais a ela.
-- Confere apenas as duas contas tecnicas ja provisionadas neste ambiente.
DO $verification$
DECLARE
    account RECORD;
    checked INTEGER := 0;
BEGIN
    FOR account IN
        SELECT u.user_id, u.firebase_uid, e.store_id, s.company_id
        FROM mottainai.app_user u
        JOIN mottainai.employee e ON e.employee_id=u.employee_id
        JOIN mottainai.retail_store s ON s.store_id=e.store_id
        WHERE u.user_id IN (505,506)
    LOOP
        IF account.store_id <> 4 OR account.company_id <> 4 OR account.firebase_uid IS NULL THEN
            RAISE EXCEPTION 'Technical account assignment differs from the approved store/company';
        END IF;
        IF NOT mottainai.fn_bootstrap_staff_context(account.firebase_uid, 'FIREBASE_UID')
            OR current_setting('app.current_user_id', TRUE) IS DISTINCT FROM account.user_id::TEXT
            OR current_setting('app.current_store_id', TRUE) IS DISTINCT FROM '4'
            OR current_setting('app.current_company_id', TRUE) IS DISTINCT FROM '4' THEN
            RAISE EXCEPTION 'Firebase UID bootstrap validation failed';
        END IF;
        checked := checked + 1;
    END LOOP;
    IF checked <> 2 THEN
        RAISE EXCEPTION 'Expected both approved technical accounts in this environment';
    END IF;
    IF mottainai.fn_bootstrap_staff_context('__nonexistent_uid_for_validation__', 'FIREBASE_UID') THEN
        RAISE EXCEPTION 'Unknown Firebase UID must not authenticate';
    END IF;
    RAISE NOTICE 'Both technical assignments validated; unknown UID refused. Review before COMMIT.';
END;
$verification$;

-- DBA: revisar o resultado e substituir SOMENTE a linha abaixo por COMMIT.
ROLLBACK;
