create table vehicle.sys_dept
(
    id          int auto_increment
        primary key,
    name        varchar(50)                         not null,
    parent_id   int                                 null,
    sort        int                                 null,
    description varchar(200)                        null,
    status      tinyint   default 1                 null,
    create_time timestamp default CURRENT_TIMESTAMP null,
    create_by   bigint                              null,
    constraint name
        unique (name),
    constraint sys_dept_sys_user_id_fk
        foreign key (create_by) references vehicle.sys_user (id)
);

