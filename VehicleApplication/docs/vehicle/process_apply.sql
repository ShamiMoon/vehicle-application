create table vehicle.process_apply
(
    id                   bigint auto_increment
        primary key,
    title                varchar(50)                         not null,
    start_time           date                                null comment '用车开始日期',
    end_time             date                                null comment '用车结束日期',
    reason               varchar(200)                        null,
    passengers           int                                 null,
    destination          varchar(200)                        null,
    vehicle_type         tinyint                             null,
    attachment           text                                null,
    apply_by             bigint                              null,
    create_time          timestamp default CURRENT_TIMESTAMP null,
    dept_id              int                                 null,
    template_id          int                                 null,
    current_node         int                                 null comment '当前审批节点',
    current_approver_ids text                                null comment '当前审批人ids',
    status               tinyint   default 0                 null comment '审批状态0~5',
    is_urgent            tinyint   default 0                 null comment '是否紧急用车 0否 1是',
    target_dept_id       int                                 null,
    update_time          datetime                            null comment '更新时间',
    constraint process_apply_ibfk_1
        foreign key (apply_by) references vehicle.sys_user (id),
    constraint process_apply_ibfk_2
        foreign key (dept_id) references vehicle.sys_dept (id),
    constraint process_apply_ibfk_3
        foreign key (template_id) references vehicle.process_template (template_id),
    constraint process_apply_ibfk_4
        foreign key (target_dept_id) references vehicle.sys_dept (id)
);

create index apply_by
    on vehicle.process_apply (apply_by);

create index dept_id
    on vehicle.process_apply (dept_id);

create index template_id
    on vehicle.process_apply (template_id);

