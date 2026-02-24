UPDATE orders
SET business_status = 'PENDING'
WHERE UPPER(COALESCE(status, '')) = 'UNPAID'
  AND UPPER(COALESCE(business_status, 'UNKNOWN')) = 'FAILED';
