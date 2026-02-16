-- Deduplicate historical rows created by parallel Stripe events
-- and enforce single order row per payment_intent_id.

WITH ranked AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY payment_intent_id
            ORDER BY
                (event_id IS NOT NULL) DESC,
                created_at DESC,
                id DESC
        ) AS rn
    FROM orders
    WHERE payment_intent_id IS NOT NULL
)
DELETE FROM orders o
USING ranked r
WHERE o.id = r.id
  AND r.rn > 1;

CREATE UNIQUE INDEX IF NOT EXISTS ux_orders_payment_intent_id
    ON orders(payment_intent_id)
    WHERE payment_intent_id IS NOT NULL;
