create table vehicle.sys_role
(
    id          int auto_increment
        primary key,
    name        varchar(50)                         not null,
    description varchar(200)                        null,
    status      tinyint   default 1                 null,
    create_time timestamp default CURRENT_TIMESTAMP null,
    constraint name
        unique (name)
);

