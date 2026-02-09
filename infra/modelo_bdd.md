```mermaid
erDiagram
  USERS {
    uuid id PK
    string email
    string full_name
    string country
    timestamp created_at
  }

  ATTRIBUTIONS {
    uuid id PK
    string event_id
    string utm_source
    string utm_medium
    string utm_campaign
    string utm_term
    string utm_content
    string gclid
    string fbclid
    string landing_path
    timestamp created_at
  }

  LANDING_EVENTS {
    uuid id PK
    uuid user_id FK
    uuid attribution_id FK
    string event_type
    timestamp created_at
  }

  PAYMENTS {
    uuid id PK
    uuid user_id FK
    uuid attribution_id FK
    string stripe_session_id
    string stripe_payment_intent
    numeric amount
    string currency
    string status
    timestamp created_at
  }

  INTEGRATIONS_LOG {
    uuid id PK
    string integration
    string reference_id
    string status
    int http_status
    int latency_ms
    json request_payload
    json response_payload
    string error_message
    timestamp created_at
  }

  USERS ||--o{ LANDING_EVENTS : has
  USERS ||--o{ PAYMENTS : makes
  ATTRIBUTIONS ||--o{ LANDING_EVENTS : links
  ATTRIBUTIONS ||--o{ PAYMENTS : links
