# Firebase no mobile operacional

## Fluxo implementado

1. O Android autentica e-mail/senha no Firebase Authentication.
2. Cada requisição à API SQL envia `Authorization: Bearer <Firebase ID Token>`.
3. Spring valida assinatura RS256, issuer, audience e validade do projeto configurado em `security.firebase.project-id` (`FIREBASE_PROJECT_ID`).
4. Somente depois da validação, a API inicializa o contexto SQL com `fn_bootstrap_staff_context(uid, 'FIREBASE_UID')` e consulta a atribuição ativa.
5. Autoridades vêm do banco a cada requisição, nunca de custom claims Firebase ou parâmetros do cliente.
6. `GET /api/v1/auth/mobile-profile` retorna vínculo, papel, loja, empresa e permissões. `name` e `email` são nulos: o app exibe nome/e-mail da identidade Firebase, sem descriptografar dados pessoais para entrar.

Usuário, funcionário, cargo, loja e empresa precisam estar ativos e não excluídos. Níveis suportados: 100=dono, 80=gerente, 60/40=estoquista. Níveis legados 1/2/3 não são convertidos pelo nome do cargo. UID sem vínculo é recusado, sem criar ou vincular contas por e-mail.

O Android obtém tokens atuais pelo SDK Firebase e tenta uma única renovação forçada após 401. Restauração da sessão na tela de login revalida o perfil SQL. Sair remove a sessão local e chama signOut do Firebase. A API mantém validação criptográfica por JWKS, sem consulta de revogação Firebase em tempo real; desativação SQL é conferida em cada requisição.

## Compatibilidade

O login legado e suas sessões/refresh permanecem disponíveis. `POST /auth/firebase` é uma ponte opcional para IA/Redis, que ainda exigem JWT operacional com claims próprios; não é pré-requisito para entrar no app ou consultar a API SQL. O app usa clientes separados e não envia token Firebase a esses serviços. Rotas legadas de logout e senha SQL não recebem token Firebase. O perfil legado `/auth/profile` continua com seu contrato anterior.

Sem migration nova: `app_user.firebase_uid` já existe. Não sincronizar senha Firebase com password_hash. Não criar documentos Firestore ou custom claims para autorizar acesso SQL.

## Preparação do banco

O script `scripts/link-mobile-firebase-users.sql` contém os três UIDs informados e IDs obrigatórios em branco. A responsável pelo banco deve identificar ou provisionar as contas autorizadas, preencher user_id, employee_id, store_id, company_id e role_id, e conferir o vínculo antes de executar. O script não cria funcionários fictícios, não troca lojas e não modifica dados pessoais criptografados nem senhas.

Executar com psql, conexão privada e credencial administrativa aprovada. O script recusa IDs ausentes, vínculos conflitantes, registros excluídos, cargo incompatível e excesso do plano. Ele ativa somente os três usuários/funcionários escolhidos, atribui os cargos aprovados e revoga sessões SQL anteriores. Todas as alterações são transacionais. A execução padrão termina em ROLLBACK; revisar os resultados e substituir apenas a última instrução por COMMIT para gravar. Em caso de erro, reconectar ou executar ROLLBACK antes de tentar novamente.

Exemplo (credenciais por configuração privada do PostgreSQL, sem senha no comando):

```powershell
psql --service=mottainai-operacional --file=scripts/link-mobile-firebase-users.sql
```

Não é seguro deduzir um ID SQL de `loja04`. Uma conta não existente precisa ser provisionada pelo administrador antes deste vínculo. Usar credencial própria da aplicação com a política RLS prevista pelo banco; conexão BYPASSRLS não comprova isolamento.

## Como validar

Publicar API e instalar APK atualizados. Com USB autorizado: `adb reverse tcp:8080 tcp:8080`; no app de debug `apiBaseUrl=http://127.0.0.1:8080/`.

Testar as três contas: login, perfil com UID e loja corretos, produtos, inventário, acesso negado a operações de outro papel, troca de contas e restauração. Sem token, token inválido ou UID sem vínculo: 401. Papel sem autorização para a operação: 403. Uma conta SQL desativada deve ser recusada mesmo com token Firebase ainda válido.

Testes automatizados cobrem provider/bearer, autorização SQL, conta desativada/sem vínculo, perfil sem criptografia e compatibilidade com sessões antigas. Os testes HTTP usam decoder controlado; não comprovam disponibilidade dos servidores Firebase ou vínculo no banco real.

O vínculo no banco real não foi executado. Permanecem fora desta mudança: funções SQL de auditoria/cadastro ausentes no ambiente, contrato de dados pessoais da equipe, migração Firestore dos fluxos PDV/FCM, configuração dos serviços de IA/Redis e validação offline completa.

Referências: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html e https://firebase.google.com/docs/auth/admin/verify-id-tokens
