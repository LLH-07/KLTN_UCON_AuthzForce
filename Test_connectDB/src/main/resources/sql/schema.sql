drop table if exists xacml_policies;
drop table if exists xacml_root_pointer;
drop table if exists users_demo;
drop table if exists books;
drop table if exists reading_sessions;
drop function if exists touch_updated_at();

create table xacml_policies (
    policy_id        varchar(200) not null,
    version_no       varchar(30)  not null,
    xml_body         text         not null,
    is_active        boolean      not null default true,
    -- Auto reload uses this lightweight timestamp instead of reading full XML every time.
    updated_at       timestamptz  not null default now(),
    primary key (policy_id, version_no)
);

create function touch_updated_at()
returns trigger
language plpgsql
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

create trigger touch_xacml_policies_updated_at
before update on xacml_policies
for each row
execute function touch_updated_at();

create table xacml_root_pointer (
    root_policy_id      varchar(200) not null,
    root_policy_version varchar(30)  not null
);

create table users_demo (
    user_id serial primary key,
    username varchar(100) not null,
    status varchar(20) not null,
    is_visitor boolean not null
);

create table books (
    book_id serial primary key,
    title varchar(200) not null,
    status varchar(20) not null,
    preview_chunk_limit int not null
);

create table reading_sessions (
    session_id serial primary key,
    user_id int not null references users_demo(user_id),
    book_id int not null references books(book_id),
    access_path varchar(20) not null
);
