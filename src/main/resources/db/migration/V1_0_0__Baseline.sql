create table LOAD
(
    id                    BIGINT PRIMARY KEY,
    customer_id           VARCHAR(255),
    timestamp             TIMESTAMP WITH TIME ZONE,
    load_amount           NUMERIC(20, 2),
    daily_count_accepted  BOOLEAN,
    daily_limit_accepted  BOOLEAN,
    weekly_limit_accepted BOOLEAN
);
