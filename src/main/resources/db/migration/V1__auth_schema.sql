create table users (
    id uuid primary key,
    email varchar(255) not null unique,
    password_hash varchar(255) not null,
    display_name varchar(255) not null,
    role varchar(32) not null,
    enabled boolean not null default true,
    created_at timestamptz not null default now()
);

create table refresh_tokens (
    id uuid primary key,
    token varchar(255) not null unique,
    user_id uuid not null references users(id),
    expires_at timestamptz not null,
    revoked boolean not null default false
);

create index idx_refresh_tokens_user_id on refresh_tokens(user_id);
