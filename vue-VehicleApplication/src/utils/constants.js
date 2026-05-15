export const ROLE_MAP = {
  1: '超级管理员',
  2: '部门管理员',
  3: '用车管理员',
  4: '普通员工',
  5: '审批人',
  6: '实习生'
}

export const APPROVAL_STATUS_MAP = {
  0: { label: '待提交', type: 'info' },
  1: { label: '待审批', type: 'warning' },
  2: { label: '审批中', type: 'primary' },
  3: { label: '已通过', type: 'success' },
  4: { label: '已驳回', type: 'danger' },
  5: { label: '已撤销', type: 'info' },
  6: { label: '已驳回(不可提交)', type: 'danger' }
}

export const TEMPLATE_TYPE_MAP = {
  1: '内部用车',
  2: '跨部门用车',
  3: '长途用车'
}

export const VEHICLE_TYPE_MAP = {
  1: '轿车',
  2: '商务车',
  3: '大巴',
  4: '小巴',
  5: '其他'
}

export const APPROVAL_ACTION_MAP = {
  1: '同意',
  2: '驳回',
  3: '转审'
}

export const APPROVAL_RULE_MAP = {
  single: '或签',
  all: '会签'
}
