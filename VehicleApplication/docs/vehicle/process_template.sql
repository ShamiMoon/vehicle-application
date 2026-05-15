create table vehicle.process_template
(
    template_id int auto_increment
        primary key,
    name        varchar(50)                         not null,
    description varchar(200)                        null,
    type        tinyint                             null comment '1内部用车 2跨部门用车 3长途用车',
    node_config json                                null comment '审批节点配置',
    status      tinyint                             null,
    create_by   bigint                              null,
    create_time timestamp default CURRENT_TIMESTAMP null,
    update_time timestamp default CURRENT_TIMESTAMP null,
    constraint name
        unique (name),
    constraint process_template_sys_user_id_fk
        foreign key (create_by) references vehicle.sys_user (id)
);

