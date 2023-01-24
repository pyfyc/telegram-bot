-- liquibase formatted sql

-- changeset alexeym75:1
CREATE TABLE notification_task
(
    id        SERIAL,
    chat_id   INTEGER,
    message   VARCHAR,
    date_time TIMESTAMP
)
