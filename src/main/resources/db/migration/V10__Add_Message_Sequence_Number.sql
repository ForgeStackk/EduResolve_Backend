-- Add a BIGSERIAL sequence number to every message so homework_submission
-- can reference it with a plain BIGINT instead of the UUID message_id.
ALTER TABLE message ADD COLUMN msg_num BIGSERIAL;
ALTER TABLE message ADD CONSTRAINT uq_message_msg_num UNIQUE (msg_num);

-- Now that msg_num exists, wire the FK from homework_submission.
ALTER TABLE homework_submission
    ADD CONSTRAINT fk_hw_sub_assignment
    FOREIGN KEY (assignment_id) REFERENCES message(msg_num) ON DELETE CASCADE;
