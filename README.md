<p align="center">
  <img src="./infra/assets/notcountry-readme-header.svg" alt="S02-26-Equipo 15 - Web App Development" width="100%" />
</p>

# NoCountry Growth Observability Platform

Plataforma para attribution y conversion server-side: captura trafico con contexto de campana, correlaciona pagos Stripe con `eventId` y expone trazabilidad operativa en un panel admin con metricas de negocio.

## Estado actual / MVP

- [x] Landing (`frontend/landing`): captura UTM y dispara eventos de funnel.
- [x] Admin panel (`frontend/admin`): consulta sesiones, eventos, ordenes y estado de integraciones.
- [x] Backend API (`backend`): tracking, Stripe webhook idempotente, auth admin Basic.
- [x] Base de datos (`BDD` + Flyway): esquema versionado para `tracking_session`, `tracking_event`, `orders`, `stripe_webhook_event`, `integrations_log`.

## Componentes

- Landing: [`frontend/landing`](./frontend/landing)
- Admin panel: [`frontend/admin`](./frontend/admin)
- Backend API: [`backend`](./backend)
- Base de datos + esquema: [`BDD`](./BDD)

## Arquitectura de alto nivel

```mermaid
flowchart LR
  U[Usuario] --> L[Landing React/Vite]
  O[Operador] --> AD[Admin React/Vite]

  L -->|POST /api/track| API[Backend Spring Boot]
  L -->|Checkout| ST[Stripe]
  ST -->|Webhooks firmados| API

  L --> GAC[GA4 client]
  L --> MPC[Meta Pixel client]

  API --> DB[(PostgreSQL + Flyway)]
  API --> GASS[GA4 MP]
  API --> MCAPI[Meta CAPI]

  AD -->|GET /api/admin/*| API
```

## Flujo de negocio y datos

```mermaid
sequenceDiagram
  autonumber
  participant U as Usuario
  participant L as Landing
  participant API as Backend
  participant ST as Stripe
  participant DB as PostgreSQL
  participant GA as GA4 MP
  participant META as Meta CAPI
  participant AD as Admin

  U->>L: Visita landing
  L->>API: /api/track (landing_view)
  API->>DB: upsert tracking_session + insert tracking_event
  API-->>L: eventId

  U->>L: Click CTA / begin checkout
  L->>ST: Redireccion con client_reference_id=eventId
  ST->>API: webhook de estado de pago
  API->>DB: upsert orders + log webhook (idempotente)

  alt pago exitoso
    API->>GA: purchase server-side
    API->>META: purchase server-side
    API->>DB: integrations_log = SENT
  else pago pendiente/intermedio
    API->>DB: business_status = PENDING
  else pago fallido
    API->>DB: business_status = FAILED
  end

  AD->>API: consulta sesiones/eventos/metricas
  API->>DB: lectura consolidada
  API-->>AD: trazabilidad completa por eventId
```

## Links

| Recurso | URL |
|---|---|
| Landing deploy | https://s02-26-equipo-15-web-app-developmen.vercel.app/ |
| Admin deploy | https://s02-26-equipo-15-web-app-admin.vercel.app/admin/login |
| API base URL | https://s02-26-equipo-15-web-app-development-desarrollo.up.railway.app |
| Health check | https://s02-26-equipo-15-web-app-development-desarrollo.up.railway.app/actuator/health |
| Swagger | No habilitado en deploy actual |

## Levantar en local

```bash
# Backend (http://localhost:8080)
cd backend
mvn spring-boot:run

# Landing (http://localhost:5173)
cd ../frontend/landing
npm install
npm run dev

# Admin (http://localhost:5174 o puerto libre de Vite)
cd ../admin
npm install
npm run dev
```

## Integraciones

- Stripe webhook: procesamiento idempotente por `stripeEventId` y actualizacion de `orders.business_status`.
- GA4 Measurement Protocol: envio server-side de `purchase` cuando corresponde.
- Meta CAPI: envio server-side de conversion para consistencia de atribucion.
- Pipedrive: integracion existente por feature flag (`PIPEDRIVE_ENABLED`), deshabilitada por defecto hasta contar con token y definicion final de pipeline comercial.

## KPIs / metricas del dashboard

- Sesiones y eventos por rango.
- Ordenes y revenue.
- Distribucion de `orders.business_status` (`SUCCESS`, `PENDING`, `FAILED`, `UNKNOWN`).
- Conversion del funnel (`landing_view`, `click_cta`, `begin_checkout`, `purchase`).
- Estado de `Integrations log` por `eventId`.

## Roadmap

- Endurecer observabilidad operativa y alertas de webhooks/integraciones.
- Incorporar pruebas E2E de checkout y trazabilidad completa.
- Cerrar rollout de Pipedrive en produccion.

## Documentacion por modulo

- [`backend/README.md`](./backend/README.md)
- [`frontend/landing/README.md`](./frontend/landing/README.md)
- [`frontend/admin/README.md`](./frontend/admin/README.md)
- [`BDD/README.md`](./BDD/README.md)
- [`infra/arquitectura_end-to-end.md`](./infra/arquitectura_end-to-end.md)
