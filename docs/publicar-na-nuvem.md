# Publicar o site na nuvem (Render + Neon)

Este guia coloca o site no ar com **um link público**, para qualquer pessoa abrir só com o navegador.
Usa dois serviços gratuitos: o **Neon** guarda o banco de dados PostgreSQL e o **Render** hospeda o site
(telas + back-end, montados pelo `Dockerfile` da raiz do projeto).

> Os nomes de botões podem mudar um pouco com o tempo. Os conceitos são os mesmos.

## Limites do plano gratuito (importante para a apresentação)

- **Render:** depois de 15 minutos sem acesso, o site "dorme". O primeiro acesso seguinte demora cerca de 1 minuto
  para acordar. Abra o link uns 2 minutos antes de apresentar.
- **Neon:** o banco dorme após 5 minutos sem uso, mas acorda em frações de segundo e os dados continuam salvos.
  O plano gratuito inclui 1 GB por projeto.
- O site fica **público**: qualquer pessoa com o link pode criar conta. Os dados são fictícios; divulgue o link
  só para o grupo e o professor.

## Passo 0: deixar a branch certa como padrão no GitHub

No fork, o GitHub abre a branch `main`, que ainda tem o projeto original. Para o grupo ver o trabalho final:

1. No fork, abra **Settings** > **Branches**.
2. Em **Default branch**, clique no ícone de troca e escolha `central_pets_leticia`.

## Passo 1: criar o banco no Neon

1. Entre em **neon.com** e crie a conta (dá para entrar com o GitHub).
2. Crie um projeto com o nome `central-pets`. Escolha uma região do leste dos EUA (por exemplo, *US East*), perto do Render.
3. No painel do projeto, clique em **Connect** e copie a *connection string*. Use a versão **direta**
   (sem `-pooler` no endereço). Ela tem este formato:

   ```
   postgresql://USUARIO:SENHA@ep-exemplo-123456.us-east-2.aws.neon.tech/neondb?sslmode=require
   ```

4. Separe em **três valores** (o Java precisa do formato `jdbc:`):

   | Variável | Valor |
   | --- | --- |
   | `DB_URL` | `jdbc:postgresql://ep-exemplo-123456.us-east-2.aws.neon.tech/neondb?sslmode=require` |
   | `DB_USER` | `USUARIO` |
   | `DB_PASSWORD` | `SENHA` |

   Se aparecer `&channel_binding=require` no final do endereço, apague essa parte.

## Passo 2: criar o site no Render

1. Entre em **render.com**, crie a conta com o GitHub e autorize o acesso ao repositório do fork.
2. Clique em **New** > **Web Service** e escolha o repositório `central_pets_leticia`.
3. Em **Branch**, escolha `central_pets_leticia`. Em **Language** (ou *Runtime*), escolha **Docker**.
4. Em **Instance Type**, escolha **Free**. Escolha uma região do leste dos EUA (por exemplo, *Ohio* ou *Virginia*).
5. Em **Environment Variables**, adicione:

   | Nome | Valor |
   | --- | --- |
   | `DB_URL` | o valor do passo anterior |
   | `DB_USER` | o valor do passo anterior |
   | `DB_PASSWORD` | o valor do passo anterior |
   | `SHOW_SQL` | `false` |

6. Em **Advanced**, preencha **Health Check Path** com `/api/status`.
7. Clique em **Create Web Service** e espere. O primeiro *build* leva alguns minutos.
8. Quando aparecer *Live*, abra o endereço do serviço (algo como `https://central-pets.onrender.com`).

Na primeira abertura, o sistema cria as tabelas e os dados de exemplo sozinho.
As contas de exemplo (senha `exemplo123`) aparecem na tela de entrada.

## Se algo der errado

- Abra a aba **Logs** do serviço no Render e procure a palavra `ERROR`.
- **`DB_URL` precisa começar com `jdbc:postgresql://`.** Se começar com `postgresql://`, o back-end não sobe.
- Mudou algo no código? É só enviar para a branch (`git push`): o Render publica de novo sozinho.
- Para trocar um valor (por exemplo, a senha do banco), altere em **Environment** no Render e salve.
