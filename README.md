# bellatrix-auth

Authentication and authorization service for Orion Platform V1.

## Endpoints

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`
- `GET /auth/me`

## Local

```bash
mvn test
mvn spring-boot:run
```

The service expects PostgreSQL and Redis. See `../m42-infra/docker-compose.yml`.
