create table if not exists notifications (
                                             id bigserial primary key,
                                             user_id bigint not null,
                                             type varchar(50) not null,
    title varchar(150) not null,
    message varchar(800) not null,
    entity_type varchar(50),
    entity_id varchar(80),
    created_at timestamp with time zone not null default now(),
    read_at timestamp with time zone null
                             );

create index if not exists idx_notifications_user_created
    on notifications (user_id, created_at desc);

create index if not exists idx_notifications_user_read
    on notifications (user_id, read_at);