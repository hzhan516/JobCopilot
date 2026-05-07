ALTER TABLE application_trackings
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

UPDATE application_trackings
SET created_at = COALESCE(updated_at, CURRENT_TIMESTAMP)
WHERE created_at IS NULL;

ALTER TABLE application_trackings
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
