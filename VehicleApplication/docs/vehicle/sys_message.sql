create table vehicle.sys_message
(
    id           bigint auto_increment comment '消息ID'
        primary key,
    user_id      bigint                             not null comment '接收用户ID',
    title        varchar(200)                       not null comment '消息标题',
    content      text                               null comment '消息内容',
    message_type tinyint                            not null comment '消息类型：1-待审批提醒 2-审批通过 3-审批驳回 4-转审通知 5-审批超时',
    apply_id     bigint                             null comment '关联的申请ID',
    is_read      tinyint  default 0                 not null comment '是否已读：0-未读 1-已读',
    read_time    datetime                           null comment '阅读时间',
    create_time  datetime default CURRENT_TIMESTAMP not null comment '创建时间'
)
    comment '系统消息表';

create index idx_apply_id
    on vehicle.sys_message (apply_id);

create index idx_create_time
    on vehicle.sys_message (create_time);

create index idx_is_read
    on vehicle.sys_message (is_read);

create index idx_user_id
    on vehicle.sys_message (user_id);

