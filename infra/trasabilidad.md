
| Color | Interpretación Visual|
|---|---|
|🔵 Azul|Adquisición (dinero invertido + captura de origen)|
|🟠 Amarillo|Pago real|
|🟣 Morado|Backend (seguridad + idempotencia)|
|🟢 Verde|Integraciones server-side|
|🔷 Celeste|Observabilidad y revenue|

```mermaid
%%{init: {
  "theme": "default",
  "themeVariables": {
    "fontSize": "22px"
  }
}}%%

flowchart LR
  A["💰 Inversión en Ads(Meta / Google)"] --> B["Landing Captura origen: UTMs / gclid / fbclid+ genera eventId"]
  B --> C["Stripe Checkout Pago del cliente"]
  C --> D["TrackSure Backend Webhook firmado (validación) + idempotencia (anti-duplicado)"]
  D --> E["📤 Envío server-side Meta CAPI + GA4 MP"]
  D --> F["📊 Dashboard Revenue + Trazabilidad (Trace View)"]

%% Colores + tipografía
classDef acquisition fill:#3B82F6,color:#fff,stroke:#1E40AF,stroke-width:2px,font-size:22px;
classDef payment fill:#F59E0B,color:#fff,stroke:#B45309,stroke-width:2px,font-size:22px;
classDef backend fill:#8B5CF6,color:#fff,stroke:#5B21B6,stroke-width:2px,font-size:22px;
classDef integration fill:#10B981,color:#fff,stroke:#065F46,stroke-width:2px,font-size:22px;
classDef observability fill:#0EA5E9,color:#fff,stroke:#0C4A6E,stroke-width:2px,font-size:22px;

class A,B acquisition;
class C payment;
class D,D1,D2,D3 backend;
class E integration;
class F observability;

```



