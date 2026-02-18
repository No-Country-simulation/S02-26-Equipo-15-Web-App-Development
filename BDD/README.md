# BDD

Documentacion y scripts de apoyo para base de datos.

## Contenido

- `BDD/descripcion_bdd.md`: descripcion funcional y tecnica del modelo de datos actual.
- `BDD/pgcrypto.sql`: script bootstrap del esquema actual (tablas + indices + constraints operativas).

## Fuente de verdad del esquema actual

El esquema vigente se define en:

- `backend/src/main/resources/db/migration/V1__init.sql`
- `backend/src/main/resources/db/migration/V2__integrations_log.sql`
- `backend/src/main/resources/db/migration/V3__normalize_integrations_log_jsonb.sql`
- `backend/src/main/resources/db/migration/V4__drop_legacy_tables.sql`
- `backend/src/main/resources/db/migration/V5__orders_payment_intent_unique.sql`
- `backend/src/main/resources/db/migration/V6__stripe_webhook_event_add_event_id.sql`
- `backend/src/main/resources/db/migration/V7__orders_add_business_status.sql`

## Uso rapido

- Crear esquema actual en una base vacia:
  - `psql -h <host> -U <user> -d <db> -f BDD/pgcrypto.sql`
- Revisar descripcion funcional del modelo:
  - `BDD/descripcion_bdd.md`
- Revisar resumen tecnico del modelo:
  - `infra/modelo_bdd.md`

Nota: para ambientes reales, aplicar siempre migraciones Flyway; `BDD/pgcrypto.sql` es apoyo para bootstrap/local.

## Convencion de terminologia

- `GA4`: Google Analytics 4 (client-side).
- `GA4_MP`: Google Analytics 4 Measurement Protocol (server-side).
- `META_CAPI`: Meta Conversions API (server-side).
