create table vehicle.sys_dept_role
(
    dept_id    int                        not null comment '部门ID',
    role_id    int                        not null comment '角色ID',
    data_scope varchar(20) default 'self' null comment '数据权限范围：self仅个人，dept本部门，dept_and_sub本部门及下级，all全部',
    primary key (dept_id, role_id),
    constraint sys_dept_role_ibfk_1
        foreign key (dept_id) references vehicle.sys_dept (id),
    constraint sys_dept_role_ibfk_2
        foreign key (role_id) references vehicle.sys_role (id)
);

create index role_id
    on vehicle.sys_dept_role (role_id);

