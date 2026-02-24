<p align="center">
  <img src="./infra/assets/notcountry-readme-header.svg" alt="S02-26-Equipo 15 - Web App Development" width="100%" />
</p>

# NoCountry Growth Observability Platform

Plataforma end-to-end para convertir trafico en ventas medibles, uniendo marketing attribution, pagos en Stripe y trazabilidad operativa en un panel admin.

## Resumen ejecutivo

Este proyecto resuelve un problema real de growth y operacion:

- Captura demanda (Google/Meta) con contexto de campana.
- Correlaciona todo con un `eventId` unico desde landing hasta pago.
- Confirma conversiones con datos de servidor (GA4 MP y Meta CAPI), no solo con pixel cliente.
- Expone trazabilidad auditable para negocio, producto y operaciones.

En una frase: pasamos de "clicks sin contexto" a "ingresos explicables por sesion".

## Problema de negocio

En funnels tradicionales, los equipos suelen tener:

- Datos fragmentados entre landing, Stripe y plataformas de anuncios.
- Dudas sobre que conversion es real vs estimada por pixel.
- Poco contexto para explicar por que sube o cae la conversion.

Esta plataforma cierra esa brecha con una capa de observabilidad de conversion orientada a decisiones de negocio.

## Propuesta de valor para NotCountry

- Menor friccion comercial: medicion clara del journey completo.
- Mayor confianza: deduplicacion e idempotencia en pagos/webhooks.
- Mejor operacion: panel para auditar sesiones, eventos, estado de pago e integraciones.
- Escalabilidad: arquitectura modular con frontends separados y backend desacoplado.

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

## Indicadores clave (KPI)

- Funnel principal: `landing_view -> click_cta -> begin_checkout -> purchase`
- Tasa de conversion por fuente de campana
- Revenue confirmado por rango de fechas
- Distribucion de estado de negocio: `SUCCESS | PENDING | FAILED | UNKNOWN`
- Estado de integraciones: `GA4_MP` y `META_CAPI`

## Estado del MVP

Completado y operativo:

- Landing con tracking de atribucion.
- Backend con Stripe webhook e idempotencia.
- Integracion server-side con GA4 MP y Meta CAPI.
- Admin con sesiones, eventos, metricas y trazabilidad.
- Modelo de datos versionado con Flyway.

Siguiente foco recomendado:

- Hardening de auth/admin.
- Alertas y observabilidad operativa.
- Pruebas E2E automatizadas de checkout y webhooks.

## Estructura del repositorio

```text
/
|-- backend/                 # API Spring Boot (Java 17)
|-- frontend/
|   |-- landing/             # Landing de conversion
|   `-- admin/               # Panel observability
|-- infra/                   # Arquitectura y diagramas
|-- BDD/                     # Modelo de datos y scripts SQL
`-- README.md
```

## Levantar en local

```bash
# Backend
cd backend
mvn spring-boot:run

# Landing
cd ../frontend/landing
npm install
npm run dev

# Admin
cd ../admin
npm install
npm run dev
```

## Documentacion por modulo

- `backend/README.md`
- `frontend/landing/README.md`
- `frontend/admin/README.md`
- `infra/arquitectura_end-to-end.md`
- `infra/modelo_bdd.md`
- `infra/resumen_end_to_end.md`
- `BDD/README.md`
- `BDD/descripcion_bdd.md`
