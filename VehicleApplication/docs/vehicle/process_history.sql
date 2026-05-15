create table vehicle.process_history
(
    id           bigint auto_increment
        primary key,
    apply_id     bigint                              not null,
    node_order   int                                 not null,
    node_name    varchar(50)                         not null,
    process_by   bigint                              not null,
    action       int                                 null comment '驳回、同意、转接',
    opinion      varchar(200)                        null comment '驳回原因或同意意见',
    transfer_to  text                                null comment '转审人',
    process_time timestamp default CURRENT_TIMESTAMP null,
    constraint process_history_ibfk_1
        foreign key (apply_id) references vehicle.process_apply (id),
    constraint process_history_ibfk_2
        foreign key (process_by) references vehicle.sys_user (id)
);

create index apply_id
    on vehicle.process_history (apply_id);

create index process_by
    on vehicle.process_history (process_by);

