# Infra - TrackSure

Indice de documentacion tecnica de arquitectura, modelo de datos y consultas operativas.

## Documentos principales

- [`arquitectura_end-to-end.md`](./arquitectura_end-to-end.md)
  - Flujo funcional completo (landing -> track -> stripe -> integraciones -> admin).
  - Endpoints activos y controles operativos.

- [`modelo_bdd.md`](./modelo_bdd.md)
  - ERD, diccionario de tablas, constraints e indices.
  - Evolucion del esquema con Flyway (`V1` a `V9`).

- [`resumen_end_to_end.md`](./resumen_end_to_end.md)
  - Checklist operativo y consultas de verificacion rapida.

- [`../BDD/README.md`](../BDD/README.md)
  - Guia de conexion local/Render y relacion con migraciones.

## Diagramas

- Arquitectura de alto nivel y secuencia de negocio: `arquitectura_end-to-end.md`.
- Modelo relacional ERD: `modelo_bdd.md`.

## Queries operativas

Para monitoreo y troubleshooting:

- Estado de webhooks Stripe (`stripe_webhook_event`)
- Estado de envios a integraciones (`integrations_log`)
- Correlacion por `eventId` entre sesiones, eventos y ordenes

Las queries estan documentadas en:

- `resumen_end_to_end.md`
- `modelo_bdd.md`
