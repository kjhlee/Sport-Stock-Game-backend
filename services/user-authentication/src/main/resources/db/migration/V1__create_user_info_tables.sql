CREATE SCHEMA IF NOT EXISTS users;
-- SET search_path TO users;

CREATE TABLE userinfo (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(255) NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    password VARCHAR(255) NOT NULL,
    creation_date TIMESTAMPTZ,
    update_date TIMESTAMPTZ
);