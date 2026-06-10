CREATE TABLE IF NOT EXISTS event_rsvp (
    id         BIGSERIAL PRIMARY KEY,
    event_id   BIGINT NOT NULL,
    user_id    BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (event_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_event_rsvp_event ON event_rsvp(event_id);
CREATE INDEX IF NOT EXISTS idx_event_rsvp_user  ON event_rsvp(user_id);
