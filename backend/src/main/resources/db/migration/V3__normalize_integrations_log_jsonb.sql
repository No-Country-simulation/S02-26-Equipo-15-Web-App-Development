DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'integrations_log'
          AND column_name = 'request_payload'
          AND data_type <> 'jsonb'
    ) THEN
        ALTER TABLE integrations_log
            ALTER COLUMN request_payload TYPE jsonb USING to_jsonb(request_payload);
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'integrations_log'
          AND column_name = 'response_payload'
          AND data_type <> 'jsonb'
    ) THEN
        ALTER TABLE integrations_log
            ALTER COLUMN response_payload TYPE jsonb USING to_jsonb(response_payload);
    END IF;
END $$;
