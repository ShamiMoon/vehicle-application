-- 审批人动态调整区分新旧申请：添加快照列
ALTER TABLE vehicle.process_apply
    ADD COLUMN node_config_snapshot text null comment '提交时的模板节点配置快照，后续节点流转使用快照不受模板编辑影响' AFTER is_urgent;
