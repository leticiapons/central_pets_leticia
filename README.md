# Sistema de Apoio às ONGs e Protetores de Animais

Projeto desenvolvido em grupo na **Residência Full Stack 5.0**. A aplicação apoia ONGs, abrigos e
protetores independentes na divulgação de animais, na captação de doações e no relacionamento com
padrinhos.

O repositório é dividido em dois projetos independentes, executados em processos separados e
conectados por HTTP:

- **`backend/`** — API REST com Spring Boot, Spring Data JPA e PostgreSQL;
- **`frontend/`** — SPA em HTML, CSS e JavaScript puro (ES Modules, sem framework e sem build).

---

## Visão geral

O MVP trabalha com **uma ONG** e **três perfis de uso**, sem cadastro e sem login. O perfil é
escolhido na tela inicial e define quais telas ficam disponíveis.

| Perfil | O que pode fazer |
| --- | --- |
| **Administrador (ONG)** | Cadastrar animais e necessidades, definir a meta de arrecadação, acompanhar o percentual atingido, consultar as doações recebidas, inativar animais quando a meta for atingida e editar os dados da ONG (incluindo a chave PIX). |
| **Doador** | Ver os animais ativos, suas necessidades e os dados da ONG, doar qualquer valor via PIX e registrar a doação como realizada. |
| **Padrinho** | Ver seus afilhados, apadrinhar novos animais e contribuir com a mensalidade de cada um. |

O fluxo de doação é baseado em confiança: o usuário copia a chave PIX, faz o pagamento e clica em
**“Já fiz o PIX • Marcar como doado”**. Não existe processamento real de pagamento. O perfil ativo
fica salvo no `localStorage` do navegador.

---

## Arquitetura

Front-end e back-end são projetos autônomos, conectados apenas pelo contrato da API REST:

```text
frontend/  (SPA estática, servida em :3000)
     │
     │  fetch http://localhost:8080/api/...
     ▼
backend/
  Controllers  →  Services + DTOs  →  Repositories (Spring Data JPA)  →  PostgreSQL / Hibernate
```

Essa divisão permite subir, testar e evoluir cada lado separadamente. Como em desenvolvimento os
dois rodam em origens diferentes, o back-end libera CORS para `http://localhost:3000` e
`http://127.0.0.1:3000` em `config/CorsConfig.java`. Se o front for servido em outra porta, basta
ajustar essa classe e o `BASE_URL` do front.

---

## Tecnologias

| Camada | Stack |
| --- | --- |
| Back-end | Java 21, Spring Boot 4.1.2, Spring Web, Spring Data JPA, Hibernate 7.4.13, Maven Wrapper |
| Banco | PostgreSQL 18 (driver 42.7.14) |
| Front-end | HTML5, CSS3, JavaScript puro (ES Modules), `npx serve` para desenvolvimento |

---

## Estrutura do projeto

```text
atividade_ongs/
├── backend/
│   ├── pom.xml, mvnw, mvnw.cmd
│   └── src/main/
│       ├── java/br/com/ongs/atividadeongs/
│       │   ├── config/       # CORS e seed inicial
│       │   ├── controller/   # endpoints REST
│       │   ├── dto/          # contratos de entrada e saída
│       │   ├── exception/    # erros e tratamento global
│       │   ├── models/       # entidades JPA e enums
│       │   ├── repository/   # Spring Data JPA
│       │   └── service/      # regras de negócio
│       └── resources/application.properties
└── frontend/
    ├── index.html, css/styles.css
    ├── data/                 # JSONs do antigo modo mock (desativado)
    └── js/                   # app.js, api.js, config.js, ids.js, components.js,
                              # doacao-modal.js, state.js e views/ (uma por perfil)
```

---

## Modelo de dados

As entidades ficam em `backend/src/main/java/br/com/ongs/atividadeongs/models/`.

| Entidade | Tabela | Campos principais |
| --- | --- | --- |
| `Ong` | `ongs` | `nome`, `descricao`, `chavePix`, `tipoChavePix`, `telefone`, `endereco` |
| `Usuario` | `usuarios` | `nome`, `email`, `perfil` |
| `Animal` | `animais` | `nome`, `especie`, `idade`, `descricao`, `status`, `meta`, `mensalidadeSugerida` |
| `Doacao` | `doacoes` | `animal`, `perfil`, `padrinho`, `doadorNome`, `valor`, `data`, `status` |

Tabelas auxiliares criadas pelas relações:

- `animal_necessidades` — `@ElementCollection` da lista de necessidades do animal, com ordem;
- `animal_padrinhos` — `@ManyToMany` entre `Animal` e `Usuario`.

Relações principais:

- `Doacao.animal` → `@ManyToOne` obrigatório;
- `Doacao.padrinho` → `@ManyToOne` opcional (apenas doações de padrinho);
- `Animal.padrinhos` ↔ `Usuario.afilhados` → N:N, o que permite apadrinhar vários animais;
- `Usuario.doacoes` → lado inverso das doações feitas como padrinho.

Enums são gravados como texto (`@Enumerated(EnumType.STRING)`): `StatusAnimal`
(`ATIVO`, `INATIVO`), `StatusDoacao` (`DOADO`, `CANCELADA`), `PerfilUsuario`
(`ADM`, `USUARIO`, `PADRINHO`) e `TipoChavePix` (`CPF_CNPJ`, `EMAIL`, `TELEFONE`,
`CHAVE_ALEATORIA`). O `TipoChavePix` serializa os rótulos exibidos no front (`"CPF/CNPJ"`,
`"E-mail"`, `"Telefone"`, `"Chave aleatória"`) via `@JsonValue`/`@JsonCreator`.

Decisões de mapeamento:

- valores monetários usam `BigDecimal(12,2)`;
- `Doacao.data` usa `Instant`, definido no servidor;
- as relações usam `fetch = LAZY`, e os controllers expõem **DTOs** em vez das entidades, evitando
  `LazyInitializationException` e vazamento de detalhes do banco.

---

## Back-end: como foi implementado

### Camadas

O front chama um **controller**; o controller delega a regra de negócio ao **service** e devolve um
**DTO**. Os services usam os **repositories** (Spring Data JPA) para acessar o banco.

- `AnimalService` — criação/validação de animais, cálculo de arrecadação, inativação e
  apadrinhamento.
- `DoacaoService` — validação da doação, definição de data/status e vínculo automático de padrinho.
- `OngController` e `UsuarioController` — operações simples, resolvidas diretamente com o
  repositório, com validação no próprio controller.
- `DataInitializer` — garante ONG e padrinho padrão em banco vazio.

Os DTOs são `records` (`AnimalRequest/Response`, `DoacaoRequest/Response`, `OngRequest/Response`,
`PadrinhoRequest`), o que mantém o contrato da API explícito e separado das entidades.

### Endpoints

| Método | Endpoint | Retorno |
| --- | --- | --- |
| `GET` | `/api/status` | mensagem de status da API |
| `GET` | `/api/ong` | dados da ONG (objeto vazio se ainda não existir) |
| `PUT` | `/api/ong` | ONG criada/atualizada (upsert com campos parciais) |
| `GET` | `/api/usuarios` | `{"padrinhoAtual": {...}}` |
| `GET` | `/api/usuarios/todos` | lista de usuários |
| `GET` | `/api/usuarios/{id}` | usuário ou `404` |
| `POST` | `/api/usuarios` | usuário criado (`201`) |
| `GET` | `/api/animais` | lista de animais com progresso da meta |
| `GET` | `/api/animais/{id}` | animal ou `404` |
| `POST` | `/api/animais` | animal criado (`201`) |
| `PATCH` | `/api/animais/{id}/status` | animal com novo status |
| `POST` | `/api/animais/{id}/padrinhos` | vincula padrinho (`{"usuarioId": 1}`) |
| `DELETE` | `/api/animais/{id}/padrinhos/{usuarioId}` | remove o vínculo |
| `GET` | `/api/doacoes` | lista de doações |
| `POST` | `/api/doacoes` | doação criada (`201`) |

`AnimalResponse` já devolve `arrecadado`, `percentual`, `metaAtingida` e `padrinhoIds`, calculados
no servidor, para que qualquer cliente receba o estado pronto do animal.

### Regras de negócio

- `arrecadado` = soma das doações do animal com status diferente de `CANCELADA`
  (`DoacaoRepository.somarValorPorAnimal`).
- `percentual` = `arrecadado / meta`, limitado a 100%; `metaAtingida` = `arrecadado >= meta`.
- Um animal só pode ser **inativado** quando a meta for atingida; caso contrário a API responde
  `409`.
- Doações exigem animal existente e **ativo**, valor positivo com até duas casas decimais e perfil
  válido.
- Doação com `perfil = PADRINHO` exige `padrinhoId` de um usuário com esse perfil. Doações de
  outros perfis não podem enviar `padrinhoId`.
- Textos, tamanhos máximos e nomes são validados no back-end, não apenas no front.
- `POST /api/usuarios` não permite criar `ADM` nem e-mail duplicado.

### Fluxo de apadrinhamento

Como não há autenticação, o MVP considera **um único padrinho**:

1. Em banco vazio, o `DataInitializer` cria a ONG padrão (se não houver) e o padrinho
   **Carlos Andrade** (`carlos.andrade@email.com`).
2. `GET /api/usuarios` devolve esse usuário em `padrinhoAtual`.
3. A área do padrinho lista em **“Animais para apadrinhar”** os animais ativos ainda não
   vinculados. O botão **Apadrinhar** chama `POST /api/animais/{id}/padrinhos`.
4. A operação é idempotente: apadrinhar novamente não duplica o vínculo. O modelo é N:N, então o
   padrinho pode ter vários afilhados.
5. Doar a mensalidade de um animal com `perfil: "PADRINHO"` também cria o vínculo automaticamente,
   caso ele ainda não exista.

### Erros

`ApiExceptionHandler` centraliza as respostas de erro, sempre no formato
`{"mensagem": "..."}` — contrato lido pelo `api.js`:

| Situação | Status |
| --- | --- |
| Validação de entrada | `400` |
| Recurso inexistente | `404` |
| Regra de negócio (ex.: inativar sem meta) | `409` |
| JSON inválido ou enum desconhecido | `400` |
| Violação de integridade no banco | `409` |

### Banco de dados

O Hibernate **não cria o banco**, apenas as tabelas. Crie o banco antes de subir a aplicação:

```powershell
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -c "CREATE DATABASE atividade_ongs;"
```

As credenciais e a URL ficam em `backend/src/main/resources/application.properties` e podem ser
sobrescritas por `DB_URL`, `DB_USER` e `DB_PASSWORD`:

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/atividade_ongs}
spring.datasource.username=${DB_USER:postgres}
spring.datasource.password=${DB_PASSWORD:admin}
spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false
```

Com `ddl-auto=update`, o Hibernate cria/atualiza as tabelas `ongs`, `usuarios`, `animais`,
`animal_necessidades`, `animal_padrinhos` e `doacoes` a partir das entidades.

---

## Front-end: como foi implementado

A SPA não tem build: o navegador carrega os ES Modules diretamente. O **`app.js`** controla as
rotas por hash e redireciona para o perfil ativo; cada tela fica em **`views/`**. O **`api.js`**
centraliza as chamadas HTTP e o tratamento de erros; **`components.js`** e **`doacao-modal.js`**
reúnem cards, barra de progresso, modal, toasts e o formulário de doação com PIX. O **`ids.js`**
expõe `mesmoId(a, b)`, que compara ids numéricos da API com strings de atributos `data-*` do DOM
usando `String(a) === String(b)` — necessário porque o DOM sempre devolve texto.

Rotas:

| Rota | Tela |
| --- | --- |
| `#/` | Seleção de perfil |
| `#/adm` | Painel da ONG |
| `#/usuario` | Área do doador |
| `#/padrinho` | Área do padrinho |

Integração com a API:

- `frontend/js/config.js` define `BASE_URL: 'http://localhost:8080/api'` e `USE_MOCK: false`;
- os dados vêm sempre do back-end; `mock.js` e `frontend/data/*.json` permanecem apenas como
  referência histórica;
- o back-end devolve `arrecadado`/`percentual`/`metaAtingida`, e o front recalcula esses valores
  localmente após uma doação para atualizar a tela imediatamente;
- a área do padrinho combina `padrinhoAtual` com `padrinhoIds` de cada animal para montar
  “meus afilhados” e “animais para apadrinhar”.

### Roteiro de demonstração

1. Acesse `#/adm`, cadastre um animal com meta e necessidades.
2. Ainda no painel, confira o card, o percentual e a lista de doações.
3. Troque para `#/usuario`, abra o modal de doação, copie a chave PIX e registre uma doação.
4. Troque para `#/padrinho`, clique em **Apadrinhar** em um animal e depois doe a mensalidade.
5. Volte ao painel: o valor arrecadado sobe, o percentual é recalculado e, ao atingir a meta, o
   botão **Inativar** é liberado.

---

## Como rodar

Pré-requisitos: JDK 21+, PostgreSQL em execução com o banco `atividade_ongs` criado, credenciais
configuradas e Node.js para servir o front-end.

### 1. Back-end

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

A API sobe em **http://localhost:8080**. Se a porta estiver ocupada, use outra:

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

> Ao mudar a porta, ajuste também `BASE_URL` em `frontend/js/config.js`.

### 2. Front-end

```powershell
npx serve frontend -l 3000
```

Acesse **http://localhost:3000**. Se o PowerShell bloquear `npx.ps1`, use
`npx.cmd serve frontend -l 3000`.

> O front precisa ser servido via HTTP: abrir `frontend/index.html` direto (`file://`) não funciona
> porque o navegador bloqueia ES Modules.

### Conferindo a API

```powershell
Invoke-RestMethod http://localhost:8080/api/status
# mensagem: API do Sistema de ONGs funcionando!
```

### Pelo IntelliJ IDEA

Importe o Maven a partir de `backend/pom.xml`, execute `AtividadeOngsApplication` e suba o
front-end em outro terminal.

### Gerando o JAR

```powershell
cd backend
.\mvnw.cmd -DskipTests package
java -jar target/atividade_ongs-0.0.1-SNAPSHOT.jar
```

O front-end não é empacotado no JAR; ele deve ser publicado em um servidor estático próprio.

### Problemas comuns

| Sintoma | Causa provável | Solução |
| --- | --- | --- |
| `404` nos endpoints | Instância antiga do back-end rodando | Pare e suba novamente |
| `28P01` / `3D000` | Credenciais erradas ou banco inexistente | Ajuste usuário/senha e crie `atividade_ongs` |
| `Connection refused` | PostgreSQL parado | Inicie o serviço do PostgreSQL |
| `CORS policy` no console | Origem não liberada | Use `http://localhost:3000` ou ajuste `CorsConfig` |
| Tela em branco | HTML aberto via `file://` | Sirva o front com `npx serve frontend -l 3000` |
| Dados desatualizados | Cache do navegador | Recarregue com `Ctrl + F5` |

---

## Próximos passos

- Adicionar testes automatizados de service e controller.
- Versionar o schema com Flyway ou Liquibase e trocar `ddl-auto=update` por `validate`.
- Evoluir para autenticação/autorização com perfis reais.
- Integrar um meio de pagamento real.
- Adicionar paginação e filtros no servidor para animais e doações.
- Remover o código legado do modo mock (`mock.js` e `frontend/data/*.json`).
