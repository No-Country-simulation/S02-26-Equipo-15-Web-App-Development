# BDD (operacion DEV)

Esta carpeta se mantiene como apoyo operativo para desarrollo local.

## Alcance

- Scripts SQL para entornos de desarrollo y pruebas manuales.
- No reemplaza Flyway en produccion.
- Fuente de verdad productiva: `backend/src/main/resources/db/migration`.

## Archivos

- `schema.sql`: crea esquema de trabajo DEV (alineado al modelo actual).
- `reset.sql`: limpia tablas de dominio para reinicio controlado en DEV.
- `pgcrypto.sql`: script de bootstrap historico y referencia.
- `descripcion_bdd.md`: explicacion funcional del modelo.

## Como exportar schema

Desde una base local PostgreSQL:

```bash
pg_dump --schema-only --no-owner --no-privileges \
  -h localhost -p 5432 -U postgres app_db > BDD/schema_export.sql
```

## Como limpiar / resetear esquema en DEV

```bash
psql -h localhost -p 5432 -U postgres -d app_db -f BDD/reset.sql
```

## Como restaurar en DEV

```bash
psql -h localhost -p 5432 -U postgres -d app_db -f BDD/schema.sql
```

## Notas

- Ejecutar estos scripts solo en desarrollo.
- En ambientes reales, usar Flyway y no aplicar SQL manual.
