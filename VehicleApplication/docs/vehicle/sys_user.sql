create table vehicle.sys_user
(
    id                   bigint auto_increment
        primary key,
    username             varchar(50)                         not null,
    password             varchar(200)                        not null,
    realname             varchar(50)                         not null,
    phone                varchar(20)                         not null,
    email                varchar(50)                         not null,
    dept_id              int                                 null,
    role_id              int                                 null,
    status               tinyint   default 1                 null,
    email_notify         tinyint   default 0                 null comment '邮箱是否提醒',
    create_time          timestamp default CURRENT_TIMESTAMP null,
    last_login_time      timestamp                           null,
    is_temp_password     tinyint   default 0                 null comment '是否为临时密码(0否 1是)',
    temp_password_expire datetime                            null comment '临时密码有效期',
    constraint email
        unique (email),
    constraint phone
        unique (phone),
    constraint username
        unique (username),
    constraint sys_user_dept_fk
        foreign key (dept_id) references vehicle.sys_dept (id),
    constraint sys_user_role_fk
        foreign key (role_id) references vehicle.sys_role (id)
);

