CREATE TABLE commute_profile (
    id SMALLINT PRIMARY KEY CHECK (id = 1),
    departure_postcode VARCHAR(16) NOT NULL,
    max_one_way_minutes INTEGER NULL CHECK (max_one_way_minutes IS NULL OR max_one_way_minutes > 0),
    arrival_buffer_minutes INTEGER NOT NULL DEFAULT 30 CHECK (arrival_buffer_minutes >= 0),
    selected_concessions TEXT[] NOT NULL DEFAULT '{}',
    preferred_payment_method VARCHAR(40) NULL,
    other_payment_method_label VARCHAR(120) NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
