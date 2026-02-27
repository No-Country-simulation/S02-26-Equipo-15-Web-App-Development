# BDD - TrackSure

Documentacion del esquema de base de datos para tracking, ordenes e integraciones.

## Objetivo

Esta carpeta centraliza la referencia del modelo SQL y su uso operativo para ambientes locales y produccion.

## Contenido

- `descripcion_bdd.md`: descripcion funcional del modelo.
- `pgcrypto.sql`: script de bootstrap/local para pruebas manuales.
- `README.md`: guia de uso y conexion.

## Fuente de verdad del esquema

El esquema oficial se versiona con Flyway en:

- `backend/src/main/resources/db/migration`

Versiones actuales:

- `V1` a `V9`.

## Tablas core

- `tracking_session`
- `tracking_event`
- `orders`
- `stripe_webhook_event`
- `integrations_log`

## Conexion local vs Render

### Local (desarrollo)

Opciones recomendadas:

1. `SPRING_DATASOURCE_URL` + `SPRING_DATASOURCE_USERNAME` + `SPRING_DATASOURCE_PASSWORD`
2. Fallback con `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`

Ejemplo local:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/app_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
```

### Render (produccion)

Ejemplo:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://<host-render>:5432/<db>?sslmode=require
SPRING_DATASOURCE_USERNAME=<render_user>
SPRING_DATASOURCE_PASSWORD=<render_password>
```

## Ejemplo `application.yml`

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://${PGHOST:localhost}:${PGPORT:5432}/${PGDATABASE:app_db}}
    username: ${SPRING_DATASOURCE_USERNAME:${PGUSER:postgres}}
    password: ${SPRING_DATASOURCE_PASSWORD:${PGPASSWORD:postgres}}
  flyway:
    enabled: true
```

## Referencias cruzadas

- Modelo y diccionario: [`../infra/modelo_bdd.md`](../infra/modelo_bdd.md)
- Arquitectura E2E: [`../infra/arquitectura_end-to-end.md`](../infra/arquitectura_end-to-end.md)
- Backend: [`../backend/README.md`](../backend/README.md)

`pgcrypto.sql` se usa como apoyo en bootstrap/local. En ambientes reales, el esquema debe aplicarse via Flyway.
