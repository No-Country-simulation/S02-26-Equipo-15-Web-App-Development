# BDD

Documentacion y apoyo del modelo de base de datos usado por el backend.

## Que contiene esta carpeta

- `descripcion_bdd.md`: descripcion funcional y tecnica del modelo.
- `pgcrypto.sql`: script de apoyo para bootstrap/local.

## Tablas principales del dominio

- `tracking_session`
- `tracking_event`
- `orders` (incluye `orders.business_status`)
- `stripe_webhook_event`
- `integrations_log`

## Versionado del esquema (Flyway)

La fuente de verdad del esquema es Flyway en:

- `backend/src/main/resources/db/migration`

Versiones actuales:

- `V1` a `V9` (inicial + ajustes de `orders`, `stripe_webhook_event` e `integrations_log`).

## Relacion con backend

- Backend README: `backend/README.md`
- Resumen tecnico: `infra/modelo_bdd.md`

Nota: para ambientes reales, aplicar migraciones Flyway; `pgcrypto.sql` se usa como apoyo en escenarios de bootstrap/local.

## Conexion de referencia (Render)

- Motor: PostgreSQL
- Puerto: `5432`
- DB de produccion: `nocountry_rvoc`
- Conexion backend recomendada:
  - `SPRING_DATASOURCE_URL=jdbc:postgresql://<host-render>:5432/nocountry_rvoc?sslmode=require`
  - `SPRING_DATASOURCE_USERNAME=<render_user>`
  - `SPRING_DATASOURCE_PASSWORD=<render_password>`
