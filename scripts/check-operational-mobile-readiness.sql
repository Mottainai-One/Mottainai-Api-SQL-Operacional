-- Mottainai Operacional | entrega para a responsavel pelo PostgreSQL Aiven
-- Conferencia: 02/11/2026. Banco: MOTTAINAI. Schema: mottainai.
-- ESTE ARQUIVO NAO CRIA NEM ALTERA OBJETOS. As consultas abaixo sao somente leitura.
-- Nao executar novamente o provisionamento das duas contas tecnicas.
-- Os INSERTs efetivamente realizados estao documentados em:
-- 2026-11-02-operational-test-accounts-inserts.sql
-- Nenhum funcionario, cargo ou usuario preexistente foi atualizado.

-- PRIORIDADE ZERO / BLOQUEIO DO LOGIN FIREBASE:
-- fn_bootstrap_staff_context(text,text) existe, MAS sua definicao no Aiven so
-- aceita CPF, EMAIL e INVITED_CPF. FIREBASE_UID retorna FALSE imediatamente.
-- Os vinculos 505/506 existem; esse contrato antigo ainda os impede de entrar.
-- A adaptacao para revisao da DBA esta em:
-- 2026-11-02-adaptar-bootstrap-firebase-uid.sql (padrao ROLLBACK, nao executado).
-- Preserva os tres fluxos existentes e acrescenta a comparacao exata de UID.

-- 1. CONTRATO AUSENTE: criacao de usuario de funcionario
-- Assinatura esperada pela API:
-- mottainai.fn_api_create_staff_user(integer, text, boolean, boolean) RETURNS integer
-- Argumentos, na ordem: employee_id, password_hash, active, password_set.
-- Chamador: AppUserRepository.createStaffUser.
-- Deve retornar o user_id criado em app_user, nunca um ID ficticio.
-- Validar funcionario, loja, empresa, ator autorizado, limite de usuarios do plano
-- e unicidade da conta. Manter as regras e os gatilhos existentes.
-- app_user.cpf e app_user.email sao obrigatorios: usar os valores protegidos do
-- funcionario conforme o contrato atual; nao gravar CPF/e-mail em texto aberto.
-- Validar BCrypt para o hash legado. A senha Firebase NAO passa por esta funcao.
-- Nao vincular firebase_uid automaticamente por e-mail nao verificado.
-- Esta ausencia bloqueia fluxos que criam NOVOS usuarios via API; as contas
-- tecnicas ja inseridas nao dependem dessa funcao para autenticar.

-- 2. CONTRATO AUSENTE: auditoria explicita da API
-- Assinatura esperada:
-- mottainai.fn_api_record_audit(text, mottainai.audit_operation, text,
--                             integer, jsonb, jsonb) RETURNS boolean
-- Argumentos: tabela, operacao, record_id, actor_app_user_id, old_data, new_data.
-- Chamador: AuditLogRepository.record. O ator e app_user.user_id, NAO employee_id.
-- Validar ator/contexto de empresa e loja; aplicar privilegios minimos e as
-- politicas existentes. Persistir auditoria real e retornar TRUE somente apos
-- inserir. Falha deve abortar a transacao; nao capturar erro e retornar sucesso.
-- Nao duplicar/substituir indiscriminadamente os gatilhos de auditoria existentes.
-- Bloqueia operacoes que exigem auditoria explicita, incluindo decisoes do PDV,
-- e determinados fluxos de empresa/funcionario/senha/convite.
-- A API rejeita retorno FALSE/NULL; nao confirma uma decisao sem auditoria.

-- 3. IDENTIDADE DE SERVICO / RLS (configuracao, nao falta de tabela)
-- A API local esta conectada como avnadmin, com BYPASSRLS=true.
-- mottainai_api existe como NOLOGIN; isso pode ser intencional para papel de grupo.
-- Preparar credencial LOGIN de servico com associacao/ativacao correta desse papel,
-- sem BYPASSRLS, seguindo o modelo do banco. Validar contexto por transacao,
-- bootstrap de identidade e isolamento entre lojas antes da troca de credencial.
-- Nao conceder privilegio administrativo nem desativar RLS para fazer a API passar.
-- Compartilhar configuracao pelo canal privado; nenhuma senha neste arquivo.

-- 4. JA PRESENTE (nao recriar)
-- Firebase Authentication valida identidade; permissao/loja/empresa vem do SQL.
-- app_user.firebase_uid, staff_session, bootstrap por UID/token, inventory,
-- inventory_count, inventory_count_item, damage_report, product.image_url.
-- PDV: pos_manager_approval_request, pos_manager_approval_dispatch,
-- pos_cancel_request, pos_terminal, fn_request_pos_cancellation,
-- sp_decide_pos_cancellation.
-- Contas tecnicas: user/employee 505 (nivel80) e 506 (nivel40), loja4/empresa4.
-- Loja/plano ativos; duas das tres vagas ocupadas na verificacao.
-- Nao precisamos de Firestore nem de Firebase Cloud Messaging.

-- 5. CONTAS ANTIGAS / DADOS PESSOAIS
-- As contas antigas 1/2/3 tem niveis 1/2/3, que nao correspondem ao contrato
-- mobile atual (100 dono, 80 gerente, 60/40 estoquista). Nao foram alteradas.
-- Se forem reutilizadas, a responsavel deve definir a atribuicao correta; nao
-- converter cargos de todos os funcionarios por nome nem por suposicao.
-- Parte da PII historica nao foi descriptografada com a configuracao local atual.
-- Conferir versao/formato/AAD e disponibilizar a configuracao a API pelo canal
-- seguro, sem regravar dados antigos ou compartilhar chaves nesta conversa.

BEGIN READ ONLY;
SET LOCAL statement_timeout = '15s';

SELECT current_database() AS banco, current_user AS usuario,
       current_setting('transaction_read_only') AS somente_leitura;

SELECT required.signature, to_regprocedure(required.signature) IS NOT NULL AS presente
FROM (VALUES
 ('mottainai.fn_api_create_staff_user(integer,text,boolean,boolean)'),
 ('mottainai.fn_api_record_audit(text,mottainai.audit_operation,text,integer,jsonb,jsonb)')
) required(signature);

SELECT position('FIREBASE_UID' IN pg_get_functiondef(
    'mottainai.fn_bootstrap_staff_context(text,text)'::regprocedure)) > 0
    AS bootstrap_declara_suporte_a_firebase_uid;

SELECT rolname, rolcanlogin, rolbypassrls
FROM pg_roles WHERE rolname IN (current_user, 'mottainai_api', 'operational_login');

-- Sem expor CPF, e-mail, senha, hash de senha, token ou dados descriptografados.
SELECT u.user_id, e.employee_id, e.store_id, s.company_id,
       r.permission_level
FROM mottainai.app_user u
JOIN mottainai.employee e ON e.employee_id = u.employee_id
JOIN mottainai.employee_role r ON r.role_id = e.role_id
JOIN mottainai.retail_store s ON s.store_id = e.store_id
WHERE u.user_id IN (505, 506)
ORDER BY u.user_id;

ROLLBACK;
