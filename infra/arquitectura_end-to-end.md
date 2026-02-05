flowchart LR
  %% ===== Users =====
  U[Usuario / Cliente] --> L[Frontend Landing (Vercel)\nReact + Vite]
  A[Operador / Admin] --> AD[Frontend Admin (Vercel)\nReact + Vite]

  %% ===== Tracking (client-side) =====
  L -->|pageview / events| GA[Google Analytics 4]
  L -->|pixel events| MP[Meta Pixel]

  %% ===== Backend API =====
  L -->|API calls| API[Backend API (Railway)\nSpring Boot]
  AD -->|API calls| API

  %% ===== Payments =====
  L -->|Checkout / Payment Link| ST[Stripe Checkout]
  ST -->|Webhook: payment_intent.succeeded, checkout.session.completed| API

  %% ===== Data =====
  API --> DB[(PostgreSQL (Railway))]
  API -->|Guardar transacción, customer, order| DB
  AD <-->|Listar / filtrar / ver detalle| API

  %% ===== Server-side tracking (recommended) =====
  API -->|Conversion API event| CAPI[Meta CAPI]
  API -->|GA4 Measurement Protocol| GMP[GA4 Server-side]

  %% ===== CRM / Ops (optional) =====
  API -->|Crear/Actualizar Deal| PD[Pipedrive CRM]

  %% ===== Notes =====
  GA -.reporting.-> MK[Marketing / Ads Optimization]
  MP -.attribution.-> MK
  CAPI -.improves match quality.-> MK
  GMP -.reduces ad blockers impact.-> MK
