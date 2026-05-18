export const SYSTEM_NAME = '降水相态网格编辑系统'

export const TOP_MENUS = ['网格编辑', '版本审核', '复盘中心', '历史分析', '系统管理'] as const

export const TOP_NAV_ITEMS = [
  { label: '网格编辑', path: '/' },
  { label: '版本审核', path: '/approval' },
  { label: '复盘中心', path: '/review' },
  { label: '历史分析', path: '/analysis/operations' },
] as const

export const ADMIN_NAV_ITEMS = [
  { label: '用户管理', path: '/admin/users' },
  { label: '配置管理', path: '/admin/config' },
  { label: '模板管理', path: '/admin/templates' },
  { label: '审计日志', path: '/admin/audit' },
  { label: '任务监控', path: '/admin/tasks' },
  { label: '存储监控', path: '/admin/storage' },
] as const
