create table vehicle.process_approver
(
    template_id int    not null,
    node_order  int    not null,
    user_id     bigint not null,
    primary key (template_id, node_order, user_id),
    constraint process_approver_ibfk_1
        foreign key (template_id) references vehicle.process_template (template_id),
    constraint process_approver_ibfk_2
        foreign key (user_id) references vehicle.sys_user (id)
);

