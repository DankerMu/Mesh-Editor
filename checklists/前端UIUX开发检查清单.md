# 前端 UI/UX 开发检查清单

## 全局检查

- [ ] 已阅读 `docs/22-前端UIUX视觉与布局规范.md`
- [ ] 已阅读 `docs/23-前端效果图索引与页面级规格.md`
- [ ] 已阅读 `docs/24-前端交互与组件状态规范.md`
- [ ] 已阅读 `docs/25-前端Mock数据与接口联调规范.md`
- [ ] 已阅读 `docs/26-前端实现验收与走查清单.md`
- [ ] 已使用 `schemas/frontend_ui_tokens.json`
- [ ] 已使用 `schemas/frontend_effect_screens.json`

## M0

- [ ] Vite + Vue 3 + TypeScript + Pinia + Vue Router 初始化
- [ ] TDesign zh-CN 生效
- [ ] axios/http.ts 统一封装
- [ ] 错误码与枚举生成 TS 类型
- [ ] LoginView 与权限路由可用

## M2

- [ ] WindowSelector 可用
- [ ] BaseMap 可用
- [ ] PrecipPhaseGridLayer 可渲染 qpf/ptype
- [ ] Binary field API 可加载 Float32Array / Uint8Array
- [ ] DrawTools 三种 geometry 可输出
- [ ] F01/F02/F03 截图走查通过

## M3

- [ ] preview/apply 交互完整
- [ ] 新增降水相态弹窗完整
- [ ] OperationHistory 支持详情、撤销、重做
- [ ] F04/F11/F12/F13 截图走查通过

## M4

- [ ] ApprovalView 完整
- [ ] before/after/delta/change/mask 切换完整
- [ ] 审核意见、通过、退回交互完整
- [ ] F05/F14 截图走查通过

## M5

- [ ] ReviewCenterView 完整
- [ ] success/partial_success/failed 状态完整
- [ ] missing_fields 可见
- [ ] 复盘包导出与 manifest 查看完整
- [ ] F06/F15 截图走查通过

## M6

- [ ] 历史分析页面完整
- [ ] 系统管理页面完整
- [ ] 任务/存储/审计监控完整
- [ ] F16/F17/F18 截图走查通过
