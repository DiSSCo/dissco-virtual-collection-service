create type collection_type as enum ('REFERENCE_COLLECTION', 'COMMUNITY_COLLECTION');

create table virtual_collection
(
    id              text                     not null
        primary key,
    version         integer default 1        not null,
    name            text                     not null,
    collection_type collection_type          not null,
    created         timestamp with time zone not null,
    modified        timestamp with time zone not null,
    tombstoned      timestamp with time zone,
    creator         text                     not null,
    data            jsonb                    not null
);