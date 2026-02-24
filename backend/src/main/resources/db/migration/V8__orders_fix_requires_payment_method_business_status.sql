UPDATE orders
SET business_status = 'FAILED'
WHERE UPPER(COALESCE(status, '')) = 'REQUIRES_PAYMENT_METHOD'
  AND UPPER(COALESCE(business_status, 'UNKNOWN')) <> 'FAILED';
