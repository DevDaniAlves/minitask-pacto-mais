# Backend — Mini Task

API Java 21 + Spring Boot. Postgres, JWT, JPA.

## Rodar

```bash
cp .env.example .env
# DB_URL=jdbc:postgresql://localhost:5435/minitask
./mvnw spring-boot:run
```

Health: http://localhost:8080/api/health

Seed padrão: `admin@demo.com` / `admin123` e `func@demo.com` / `func123`.

Stack Docker (API + front + Postgres + Evolution + n8n) fica no **monorepo** (`docker compose` na raiz), não neste repositório.

## O que a API cobre

- Auth: login, register, OTP/2FA, reset/set password
- Times, boards, membros
- Tasks: CRUD, status, filtros, paginação, comentários, avaliação (admin)
- Perfil: senha, 2FA
- Admin users: convite, telefone (remover desliga 2FA)
- WhatsApp (Evolution) e rotas `/api/bot/**` pro n8n

## Testes

```bash
./mvnw test
```

Cobertura focada nas regras pedidas no desafio:

- cadastro de usuário (`AuthServiceRegisterTest`, `UserServiceTest`)
- criação de task com/sem responsável (`TaskServiceTest`)
- movimentação de status / transição inválida / concluída com responsável (`TaskServiceTest`, `TaskStatusTest`)
