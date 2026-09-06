CREATE UNIQUE INDEX IF NOT EXISTS uk_users_telegram_id
    ON users (telegram_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_teacher_uuid
    ON users (teacher_uuid);

CREATE UNIQUE INDEX IF NOT EXISTS pk_teacher_groups
    ON teacher_groups (teacher_id, group_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_schedule_occurrence
    ON schedule (occurrence_id);

CREATE INDEX IF NOT EXISTS idx_schedule_group_series_date
    ON schedule (group_id, series_id, lesson_date);

CREATE UNIQUE INDEX IF NOT EXISTS uk_schedule_change_group_request
    ON schedule_changes (group_id, client_request_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_schedule_change_occurrence_version
    ON schedule_changes (occurrence_id, change_version);

CREATE UNIQUE INDEX IF NOT EXISTS uk_schedule_change_batch_occurrence
    ON schedule_changes (group_id, batch_request_id, occurrence_id);

CREATE INDEX IF NOT EXISTS idx_notification_history_user_created
    ON notification_history (user_id, created_at DESC);
