ALTER TABLE absences
    ADD COLUMN IF NOT EXISTS start_date DATE,
    ADD COLUMN IF NOT EXISTS end_date DATE;

UPDATE absences
SET start_date = COALESCE(start_date, date, CURRENT_DATE),
    end_date = COALESCE(end_date, date, CURRENT_DATE)
WHERE start_date IS NULL
   OR end_date IS NULL;

ALTER TABLE absences
    ALTER COLUMN schedule_id DROP NOT NULL,
    ALTER COLUMN start_date SET NOT NULL,
    ALTER COLUMN end_date SET NOT NULL,
    ALTER COLUMN reason TYPE VARCHAR(500),
    DROP CONSTRAINT IF EXISTS chk_absences_date_range,
    ADD CONSTRAINT chk_absences_date_range CHECK (end_date >= start_date);

CREATE INDEX IF NOT EXISTS idx_absences_user_start_date
    ON absences (user_id, start_date DESC);
