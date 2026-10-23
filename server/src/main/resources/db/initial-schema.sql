-- Run once against a new, empty PostgreSQL database before starting the deploy profile.
-- Existing local H2 data is not migrated by this file.
CREATE TABLE pickup (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    owner_hash VARCHAR(255),
    invite_hash VARCHAR(255),
    invite_expires_at TIMESTAMP(6) WITH TIME ZONE,
    expires_at TIMESTAMP(6) WITH TIME ZONE,
    created_at TIMESTAMP(6) WITH TIME ZONE,
    ended_at TIMESTAMP(6) WITH TIME ZONE,
    status VARCHAR(255),
    place_label VARCHAR(255),
    place_latitude DOUBLE PRECISION NOT NULL,
    place_longitude DOUBLE PRECISION NOT NULL,
    participant_id UUID,
    participant_name VARCHAR(255),
    participant_hash VARCHAR(255),
    readiness VARCHAR(255),
    readiness_sequence BIGINT NOT NULL,
    vehicle_sequence BIGINT NOT NULL,
    vehicle_latitude DOUBLE PRECISION,
    vehicle_longitude DOUBLE PRECISION,
    duration_seconds BIGINT,
    vehicle_source_time TIMESTAMP(6) WITH TIME ZONE,
    vehicle_received_at TIMESTAMP(6) WITH TIME ZONE
);
CREATE INDEX pickup_retention_ended ON pickup (ended_at);
CREATE INDEX pickup_retention_expires ON pickup (expires_at);
