# Leitura Dinamica

Plataforma full stack de leitura dinâmica com Angular + Ionic + Capacitor no frontend, Spring Boot 3 no backend, MySQL 8 e Docker Compose.

## Stack

- Frontend: Angular 22, Ionic, Capacitor, RxJS, Signals, Bootstrap
- Backend: Java 21, Spring Boot 3.5, Spring Security, JWT, JPA, Validation, Lombok, MapStruct, OpenAPI
- Banco: MySQL 8
- Containers: Docker Compose

## Estrutura

- `frontend`: aplicação web responsiva e base mobile com Capacitor
- `backend`: API REST com autenticação JWT, estatísticas, biblioteca e histórico
- `docker-compose.yml`: sobe frontend, backend e MySQL

## Subir tudo com Docker

```bash
docker compose up -d --build
```

## Acessos

- Frontend: http://localhost:8081
- Backend: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html

## Usuários demo

- Admin: `admin@leituradinamica.dev` / `Admin123!`
- Usuário: `demo@leituradinamica.dev` / `Demo12345`

## Frontend local

```bash
cd frontend
npm install
npm start
```

## Backend local

```bash
cd backend
./mvnw spring-boot:run
```

## Mobile

```bash
cd frontend
npm run build
npm run mobile:sync
```

Depois disso você pode abrir `android` ou `ios` com os scripts do Capacitor.