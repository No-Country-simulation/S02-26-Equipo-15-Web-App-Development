```mermaid
flowchart LR
  %% ===== Usuarios =====
  U[Usuario / Cliente]
  A[Operador / Admin]

  %% ===== Frontend =====
  L[Landing\nReact + Vite\nVercel]
  AD[Admin Dashboard\nReact + Vite\nVercel]

  %% ===== Backend =====
  API[Backend API\nSpring Boot\nRailway]

  %% ===== Pagos =====
  ST[Stripe Checkout]

  %% ===== Datos =====
  DB[(PostgreSQL\nRailway)]

  %% ===== Tracking =====
  GA[Google Analytics 4]
  MP[Meta Pixel]
  CAPI[Meta CAPI]
  GMP[GA4 Server-side]

  %% ===== CRM =====
  PD[Pipedrive CRM]

  %% ===== Flujos =====
  U --> L
  A --> AD

  L -->|API calls| API
  AD -->|API calls| API

  L -->|Checkout / Payment Link| ST
  ST -->|Webhook: payment success| API

  API -->|Guardar transacción| DB
  AD <-->|Consultar datos| API

  L -->|Eventos client-side| GA
  L -->|Eventos client-side| MP

  API -->|Eventos server-side| CAPI
  API -->|Eventos server-side| GMP

  API -->|Crear / actualizar deal| PD
