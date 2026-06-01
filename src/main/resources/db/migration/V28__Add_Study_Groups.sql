-- V28__Add_Study_Groups.sql
-- Student-created peer study groups within a classroom.

CREATE TABLE IF NOT EXISTS classroom_study_groups (
    id              BIGSERIAL    PRIMARY KEY,
    classroom_id    BIGINT       NOT NULL REFERENCES student_classrooms(id),
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    owner_user_id   BIGINT       NOT NULL REFERENCES user_login(id),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_study_groups_classroom ON classroom_study_groups(classroom_id, owner_user_id);

CREATE TABLE IF NOT EXISTS classroom_study_group_members (
    id          BIGSERIAL  PRIMARY KEY,
    group_id    BIGINT     NOT NULL REFERENCES classroom_study_groups(id) ON DELETE CASCADE,
    user_id     BIGINT     NOT NULL REFERENCES user_login(id),
    joined_at   TIMESTAMP  NOT NULL DEFAULT NOW(),
    UNIQUE (group_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_study_group_members ON classroom_study_group_members(group_id, user_id);
