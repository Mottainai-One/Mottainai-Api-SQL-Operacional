# Integração do mobile operacional — 02/11/2026

O Android autentica e-mail/senha no Firebase Authentication. A API SQL valida o ID Token (assinatura RS256, projeto/audience, emissor e validade) e resolve UID, usuário, funcionário, permissões, loja e empresa no PostgreSQL. Claims de cargo/loja do Firebase não autorizam operações. Não há Firestore nem Firebase Cloud Messaging neste fluxo.

`GET /api/v1/auth/mobile-profile` permite entrar sem descriptografar PII. O perfil contém o vínculo SQL; nome/e-mail apresentados pelo app vêm da identidade Firebase. Token sem vínculo ativo é recusado. Níveis suportados: 100 dono, 80 gerente, 60/40 estoquista; cargos legados 1/2/3 precisam de atribuição aprovada, sem conversão por nome.

Cada requisição SQL usa o ID Token atual. O app tenta uma renovação forçada após 401. A ponte `POST /api/v1/auth/firebase` produz uma sessão operacional real para integrações IA/Redis, sem receber senha Firebase. Não armazenar essa senha no PostgreSQL. O login SQL legado continua compatível. A validação criptográfica do Firebase não faz consulta de revogação em tempo real.

## Notificações

A caixa interna consulta a API Redis, inclusive contagem de não lidas e marcação de leitura. Não há registro de dispositivo, push de sistema, permissão de notificações nem necessidade de chave FCM. A API Redis exige o JWT operacional da ponte, não o ID Token Firebase. Em ambiente USB, as portas locais usadas são SQL 8080, IA 8000, Mongo API 8082 e Redis API 18082.

## Aprovações do PDV

`GET /api/v1/pos-approvals?status=PENDING&limit=50` consulta a fila da loja atribuída ao gerente/dono no SQL. `POST /api/v1/pos-approvals/{uuid}/decision` recebe `decision` (APPROVED/REJECTED) e `observation` opcional, até 250 caracteres. Não recebe ator nem loja escolhidos pelo cliente.

A decisão bloqueia o registro em transação, exige PENDING, confere a venda da própria loja e, em cancelamentos, exige solicitação de cancelamento correspondente. Usa a procedure existente do banco; cancelamento aprovado termina EXECUTED. Outros pedidos aprovados permanecem APPROVED para execução pelo fluxo específico do PDV. Não representar aprovação de desconto/devolução como execução financeira.

Auditoria usa o `app_user.user_id` autenticado. Erro ou retorno FALSE/NULL da função de auditoria aborta a decisão. Ausência de tabela/função é indisponibilidade, nunca uma aprovação simulada. As decisões reais não foram executadas contra vendas da nuvem durante os testes automatizados.

## Banco: entrega para a responsável

Consultar `scripts/check-operational-mobile-readiness.sql`. É um roteiro de requisitos com consultas somente leitura, sem DDL/grants/atualizações. As duas funções ausentes na conferência são `fn_api_create_staff_user(integer,text,boolean,boolean)` e `fn_api_record_audit(text,mottainai.audit_operation,text,integer,jsonb,jsonb)`.

O bloqueio imediato do login foi identificado na definição da função existente `fn_bootstrap_staff_context`: ela só aceita CPF, EMAIL e INVITED_CPF, recusando FIREBASE_UID antes da consulta. Portanto, a existência da função e dos vínculos não comprova compatibilidade. `scripts/2026-11-02-adaptar-bootstrap-firebase-uid.sql` propõe a comparação exata por UID preservando os três modos anteriores. O script contém DDL para revisão da DBA, termina em ROLLBACK e NÃO foi executado pelo agente; a DBA deve revisar, validar e usar COMMIT para aplicá-lo. O projeto Firebase do contêiner foi conferido e corresponde ao mobile. O teste ponta a ponta ainda retorna 401 enquanto esse contrato não for adaptado.

As contas técnicas 505/506 já foram inseridas e vinculadas à loja/empresa 4; não reprovisionar. Os INSERTs completos e credenciais de teste permanecem no ambiente privado, fora desta PR. Não publicar senhas, chaves, envelopes de PII ou arquivos de configuração. As contas antigas não foram alteradas.

A API local ainda usa credencial administrativa BYPASSRLS. A responsável deve fornecer/configurar identidade de serviço com privilégios mínimos e validar o contexto RLS. A configuração de criptografia histórica também precisa ser compatível para fluxos que leem PII da equipe; autenticar não exige descriptografar esses campos.

## Validação

A suíte final da API executou 339 testes, sem falhas/erros; 4 testes de integração ficaram ignorados por suas condições de ambiente. Cobertura adicionada: assinatura/issuer/audience/expiração Firebase, limite de espera das chaves, autorização e isolamento do PDV, decisão repetida e confirmação obrigatória de auditoria. Esses resultados não substituem testes ponta a ponta do ambiente e do APK.

O roteiro local `test-real-operational-api.py --integrations` autentica as contas técnicas e testa consultas reais, sem executar decisões do PDV. O relatório local registra falhas reais; não tratar sucesso unitário como integração disponível. O APK ainda depende de compilação/instalação atualizadas; houve falha do Gradle no Windows ao estabelecer conexão de loopback.
