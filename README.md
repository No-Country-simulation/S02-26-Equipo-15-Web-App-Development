# S02-26-Equipo-15 | Web App Development

Monorepo del equipo 15 para conversion tracking y pagos con Stripe.

## Objetivo del proyecto

Construir una plataforma con:

- Landing de conversion (React + Vite)
- Backend de tracking/pagos (Spring Boot + PostgreSQL)
- Integraciones server-side (GA4 MP, Meta CAPI, Pipedrive opcional)
- Panel administrador para consulta de datos (pendiente de desarrollo frontend)

## Estado actual de modulos

- `backend/`: implementado y operativo
- `frontend/landing/`: implementado y operativo
- `frontend/admin/`: pendiente de desarrollo (planificado)
- `infra/`: documentacion tecnica actualizada

## Arquitectura del proyecto

```text
/
|-- backend/                 # API - Spring Boot 3 / Java 17
|-- frontend/
|   |-- landing/             # Landing + Checkout (React + Vite)
|   `-- admin/               # Panel Administrador (pendiente)
|-- infra/                   # Documentacion de arquitectura y modelo de datos
|-- BDD/                     # Material de apoyo historico
`-- README.md
```

## Arquitectura end-to-end

```mermaid
flowchart LR
  U[Usuario] --> L[Landing React/Vite]
  A[Operador/Admin] --> AD[Admin Dashboard React/Vite\npendiente]

  L -->|POST /api/track| API[Backend API Spring Boot]
  L -->|Stripe Checkout| ST[Stripe]
  ST -->|Webhooks firmados| API

  AD -.->|GET /api/admin/*| API

  API --> DB[(PostgreSQL + Flyway)]
  L --> GAC[GA4 client]
  L --> MPC[Meta Pixel client]
  API --> GASS[GA4 MP server-side]
  API --> MCAPI[Meta CAPI server-side]
  API --> PD[Pipedrive opcional]
```

## Flujo transaccional

```mermaid
sequenceDiagram
  autonumber
  participant U as Usuario
  participant L as Landing
  participant API as Backend API
  participant ST as Stripe
  participant DB as Postgres

  U->>L: Entra con UTM/gclid/fbclid
  L->>API: POST /api/track (landing_view + attribution)
  API->>DB: upsert tracking_session + insert tracking_event
  API-->>L: { "eventId": "<uuid>" }

  U->>ST: Completa pago
  ST->>API: payment_intent.succeeded
  ST->>API: checkout.session.completed

  API->>DB: stripe_webhook_event (idempotencia)
  API->>DB: upsert orders (sin duplicados)
  API->>DB: tracking_event purchase
  API->>DB: integrations_log (GA4_MP / META_CAPI / PIPEDRIVE)
```

## Endpoints backend

- `POST /api/track`
- `POST /api/stripe/webhook`
- `GET /api/admin/sessions`
- `GET /api/admin/sessions/{eventId}`
- `GET /api/admin/events`
- `GET /api/admin/metrics`
- `GET /api/health/db`
- `GET /actuator/health`

## Modelo de datos (resumen)

```mermaid
erDiagram
  TRACKING_SESSION ||--o{ TRACKING_EVENT : has
  TRACKING_SESSION ||--o{ ORDERS : links
  TRACKING_SESSION ||--o{ STRIPE_WEBHOOK_EVENT : correlates
```

Tablas activas:

- `tracking_session`
- `tracking_event`
- `orders`
- `stripe_webhook_event`
- `integrations_log`

## Flujo de ramas Git

```mermaid
gitGraph
  commit id: "init"
  branch develop
  checkout develop
  commit id: "integracion"
  branch feature/tracking
  checkout feature/tracking
  commit id: "feat"
  checkout develop
  merge feature/tracking
  branch feature/webhook
  checkout feature/webhook
  commit id: "feat"
  checkout develop
  merge feature/webhook
  checkout main
  merge develop tag: "release"
```

## Variables de entorno clave

### Backend (`backend/.env.example`)

- `SPRING_PROFILES_ACTIVE`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `STRIPE_WEBHOOK_SECRET`
- `TRACKING_ENABLED`
- `META_CAPI_ENABLED`
- `META_PIXEL_ID`
- `META_ACCESS_TOKEN`
- `GA4_MP_ENABLED`
- `GA4_MEASUREMENT_ID`
- `GA4_API_SECRET`
- `GA4_MP_DEBUG_VALIDATION_ENABLED`
- `PIPEDRIVE_ENABLED`
- `PIPEDRIVE_API_TOKEN`
- `CORS_ALLOWED_ORIGINS`

### Landing (`frontend/landing/.env.example`)

- `VITE_STRIPE_PAYMENT_LINK`
- `VITE_API_URL`
- `VITE_GA_MEASUREMENT_ID`
- `VITE_META_PIXEL_ID`

## Ejecucion local

### Backend

```bash
cd backend
mvn spring-boot:run
```

### Landing

```bash
cd frontend/landing
npm install
npm run dev
```

## Documentacion complementaria

- `backend/README.md`
- `frontend/landing/README.md`
- `infra/arquitectura_end-to-end.md`
- `infra/modelo_bdd.md`
- `infra/resumen_end_to_end.md`
