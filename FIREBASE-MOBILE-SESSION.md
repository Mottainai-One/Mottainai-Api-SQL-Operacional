# Login Firebase do mobile operacional

O mobile autentica e-mail e senha diretamente no Firebase. Em seguida envia somente `firebaseIdToken` por JSON para `POST /api/v1/auth/firebase`, sem senha e sem Authorization de uma sessão anterior.

A API verifica o token Firebase, resolve o UID vinculado ao funcionário e exige usuário, funcionário, papel, loja e empresa ativos. Emite os tokens operacional e de renovação pelo mecanismo de sessões existente. Não cria vínculos automaticamente por e-mail e não concede acesso a uma loja arbitrária.

O endpoint existente `/api/v1/auth/login` permanece disponível com seu contrato anterior. Nenhuma migration foi adicionada. O verificador atual valida assinatura e claims do ID token; não consulta revogação em tempo real no Firebase Admin SDK.

Após a troca, o mobile consulta `GET /api/v1/auth/mobile-profile` com o access token operacional. A resposta inclui `id`, `firebaseUid`, `name`, `email`, `role`, `active`, `storeId`, `companyId` e `permissions`. Não recebe UID, loja ou empresa para escolher a conta. Parâmetros enviados pelo cliente não substituem o principal autenticado.

O papel do mobile é estável (`dono`, `gerente`, `estoquista`), derivado de `employee_role.permission_level`: 100, 80 e 60/40, respectivamente. O nome do cargo no banco é apenas um rótulo e pode mudar sem quebrar o login. Outros níveis não recebem sessão ou perfil mobile. A lista `permissions` mantém as autoridades da API; a autorização de cada operação continua no servidor. O mesmo mapeamento é usado pelo claim de papel da IA, inclusive para Supervisor (60), sem mudar suas autoridades de API.

Cadastros legados com níveis como 1, 2 ou 3 não são convertidos automaticamente pelo nome do cargo. A equipe responsável pelo cadastro deve atribuir um papel operacional com nível aprovado. Um cargo chamado Estoquista não concede acesso por si só.

Não é necessário criar documentos em `users` no Firestore, configurar custom claims de cargo/loja no Firebase nem sincronizar senhas com `app_user.password_hash`. O UID validado deve corresponder a `app_user.firebase_uid`, vinculado a um `employee` com `retail_store`, `company` e `employee_role` ativos e não excluídos. A verificação de contexto usa `mottainai.fn_bootstrap_staff_context(uid, 'FIREBASE_UID')`, preservando RLS. Resultado falso não identifica sozinho qual elo está ausente ou inativo.

O vínculo é um dado de cadastro, não uma migration. A equipe responsável deve identificar a conta/funcionário corretos e usar o fluxo administrativo de cadastro/ativação existente. Não se deve associar o UID a uma loja arbitrária, reutilizar uma conta de outra pessoa, contornar RLS ou vincular automaticamente por e-mail. Nenhuma conta ou permissão do banco real foi alterada nesta PR.

Implantação: usar a role de aplicação prevista pelo banco e suas credenciais próprias. Uma conexão administrativa com `BYPASSRLS` não valida o isolamento por RLS. Verificar a role efetiva e a configuração de criptografia dos dados de funcionário no ambiente de destino, sem publicar chaves ou credenciais.

É necessário publicar a API e instalar o APK atualizado juntos. A compilação do código não significa que o contêiner em execução já foi atualizado. Uma conta sem vínculo operacional válido continuará sendo recusada, mesmo autenticada no Firebase. O contrato legado `/auth/profile` permanece inalterado.

Escopo: autenticação e perfil operacional. Esta API não possui integração com Firestore. Fluxos legados de aprovações PDV e cadastro FCM no aplicativo ainda precisam de contratos próprios para substituir seu acesso direto ao Firestore; esta PR não cria tabelas nem inventa esses contratos.

Validação automatizada: `AuthenticationServiceTest`, `AuthenticationControllerTest`, `MobileProfileServiceTest`, `ApiSecurityTest` e testes existentes de JWT/RLS. O CI executa `mvn -B -ntp clean verify -Dsurefire.runOrder=random`. No aplicativo, testar uma conta vinculada e ativa sem custom claims de cargo/loja, senha Firebase incorreta, conta sem vínculo e restauração de sessão.
