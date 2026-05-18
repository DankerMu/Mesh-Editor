# Mesh-Editor E2E 测试清单

> 基于 M0-M6 全部功能点梳理，覆盖 12 个前端页面、50+ API 端点、6 个状态机、4 种角色。
> 已有后端 E2E（test_e2e_case1/2/3）覆盖核心 happy path，本清单补全前端交互、边界、权限、异常场景。

---

## 一、认证与权限（Auth & RBAC）

### 1.1 登录流程

| # | 用例 | 操作 | 预期结果 |
|---|------|------|----------|
| A-01 | 正常登录 | 输入正确用户名/密码，点击登录 | 跳转到首页，AppHeader 显示用户名和角色 |
| A-02 | 错误密码 | 输入错误密码 | 表单显示错误提示，不跳转 |
| A-03 | 空字段提交 | 不填用户名或密码 | 字段校验提示，按钮不可用 |
| A-04 | 禁用账户登录 | 使用 is_active=false 的账户 | 显示"账户已禁用"专用提示 |
| A-05 | Token 过期 | 等待 Token 过期后发起请求 | 自动跳转到登录页 |
| A-06 | 已登录访问 /login | 已登录状态直接访问 /login | 自动重定向到首页 |
| A-07 | 刷新保持会话 | 登录后刷新页面 | localStorage 恢复 token 和 user，保持登录态 |
| A-08 | 退出登录 | 点击 AppHeader 退出按钮 | 清除 token，跳转到登录页 |

### 1.2 角色权限守卫

| # | 用例 | 操作 | 预期结果 |
|---|------|------|----------|
| A-09 | viewer 访问编辑器 | viewer 角色访问 /editor | 跳转到 /forbidden |
| A-10 | forecaster 访问管理页 | forecaster 访问 /admin/users | 跳转到 /forbidden |
| A-11 | reviewer 访问管理页 | reviewer 访问 /admin/config | 跳转到 /forbidden |
| A-12 | admin 访问全部页面 | admin 遍历所有路由 | 全部正常访问 |
| A-13 | 未登录访问受保护页 | 未登录访问 /editor | 跳转到 /login |
| A-14 | AppHeader 菜单可见性 | 各角色登录 | admin 看到管理菜单；其他角色不显示 |
| A-15 | forecaster 版本列表过滤 | forecaster 查看版本列表 | 仅看到自己创建的版本 |

---

## 二、数据扫描与窗口管理（Data Scan & Windows）

### 2.1 数据扫描

| # | 用例 | 操作 | 预期结果 |
|---|------|------|----------|
| B-01 | 正常扫描 | 输入有效 case_id (YYYYMMDDHH, HH=08/20)，触发扫描 | 显示扫描进度，完成后窗口列表刷新 |
| B-02 | 无效 case_id 格式 | 输入 "abc" 或 "2026011100" (HH=00) | CaseIdInput 组件校验失败，不触发扫描 |
| B-03 | case_id HH 校验 | 输入 HH=12 | 拒绝，仅允许 08/20 |
| B-04 | 扫描轮询 | 扫描进行中 | ScanProgress 每 2 秒轮询状态，显示实时进度 |
| B-05 | 扫描完成 | 扫描结束 | 停止轮询，显示完成状态，窗口列表自动加载 |
| B-06 | 扫描失败 | 后端扫描出错 | 显示 scanErrorMessage，停止轮询 |
| B-07 | 重复扫描 | 正在扫描时再次点击扫描 | 检测到运行中扫描，拒绝并提示 |
| B-08 | viewer 触发扫描 | viewer/forecaster 角色触发扫描 | 权限不足，接口返回 403 |

### 2.2 窗口选择器

| # | 用例 | 操作 | 预期结果 |
|---|------|------|----------|
| B-09 | 窗口分组显示 | 扫描完成后查看窗口 | 按 24h/48h/168h 三个 Tab 分组 |
| B-10 | 窗口状态标签 | 查看各状态窗口 | available=绿, partial=橙, invalid=红, pending=灰 |
| B-11 | 可用窗口点击 | 点击 status=available 窗口 | 路由跳转到 /editor/{windowId} |
| B-12 | partial 窗口点击 | 点击 partial 窗口 | 可选择（有警告提示），进入编辑器 |
| B-13 | invalid 窗口点击 | 点击 invalid 窗口 | 不可选择，显示 Tooltip 说明原因 |
| B-14 | pending 窗口 | 查看 pending 状态窗口 | 不可选择，灰色显示 |
| B-15 | 窗口 QC 信息 | 查看有 QC 问题的窗口 | WindowQcInfo 显示 negative_count, negative_min, ptype 缺失时效 |
| B-16 | 空窗口 Tab | 某 accum_hours 无窗口 | 显示空状态提示 |
| B-17 | 窗口计数 | WindowSelector 标题 | 显示可用窗口总数 |

---

## 三、编辑器工作台（Editor Workspace）

### 3.1 会话管理

| # | 用例 | 操作 | 预期结果 |
|---|------|------|----------|
| C-01 | 开始会话 | 从窗口选择器进入 /editor/{windowId} | 自动调用 session/start，加载会话数据 |
| C-02 | 加载会话字段 | 会话启动 | 加载 qpf_before, ptype_before, qpf_after, ptype_after, invalid_mask |
| C-03 | 字段二进制校验 | 加载字段数据 | Float32Array 长度 = 501×821×4, Uint8Array = 501×821 |
| C-04 | 窗口锁定冲突 | 两个用户同时打开同一窗口 | 第二个用户收到 WINDOW_LOCKED 错误提示 |
| C-05 | invalid 窗口开始会话 | 直接访问 invalid 窗口的 URL | 收到 WINDOW_NOT_EDITABLE 错误 |
| C-06 | 会话超时 | 会话闲置超过 240 分钟 | 操作时提示 SESSION_EXPIRED |
| C-07 | 会话上下文显示 | 查看顶部栏 | 显示起报时间、累积时效、预报时效、会话状态 |

### 3.2 地图交互

| # | 用例 | 操作 | 预期结果 |
|---|------|------|----------|
| C-08 | 底图渲染 | 进入编辑器 | OpenLayers 地图正常显示，OSM 底图 + EPSG:4326 |
| C-09 | 网格悬浮提示 | 鼠标在地图上移动 | GridTooltip 显示经纬度、格点行列号、当前值 |
| C-10 | 数据图层渲染 | 加载会话后 | 降水量色斑图（WebGL DataTile）正常着色 |
| C-11 | 七种视图模式 | 切换 before/after/delta/change_ptype/touched/changed/review | 各图层正确显示对应字段数据 |
| C-12 | 视图模式默认 | 进入编辑器 | 默认显示 "before" 视图 |
| C-13 | 区域范围 | 缩放/平移 | 网格范围 70-111°E, 25-50°N 正确框定 |

### 3.3 绘图工具

| # | 用例 | 操作 | 预期结果 |
|---|------|------|----------|
| C-14 | 多边形工具 | 选择 polygon 工具，在地图上画多边形 | 至少 3 个点后可闭合，生成 PolygonGeometry |
| C-15 | 多边形最少点数 | 画不到 3 个点尝试闭合 | 无法闭合，继续等待输入 |
| C-16 | 多边形撤销点 | 画点过程中按 Backspace | 移除最后一个点 |
| C-17 | 多边形取消 | 画点过程中按 Escape 或右键 | 取消当前绘制 |
| C-18 | 线缓冲工具 | 选择 line_buffer，画线并设置宽度 | 至少 2 点，宽度范围 1-50 格点，生成 LineBufferGeometry |
| C-19 | 线缓冲宽度调节 | 修改宽度输入框 | 实时更新缓冲区预览 |
| C-20 | 画笔工具 | 选择 brush_path，在地图上拖拽 | 鼠标按下拖动绘制路径，释放完成 |
| C-21 | 画笔半径调节 | 按 [ / ] 键或滚轮 | 半径增减 (1-30 范围)，光标圆圈同步变化 |
| C-22 | 画笔光标 | 激活 brush_path | 显示圆形覆盖层（半径 × 2） |
| C-23 | 画笔采样节流 | 快速拖拽 | 16ms 或 4px 最小间隔采样，不卡顿 |
| C-24 | 清除选区 | 点击清除按钮或 Ctrl+Shift+A | 清除当前 mask，恢复无选区状态 |
| C-25 | 工具切换 | 从 polygon 切换到 brush_path | 中断当前绘制，切换工具模式 |
| C-26 | 禁用状态 | disabled=true 时 | 所有工具按钮不可点击 |

---

## 四、编辑操作（Edit Operations）

### 4.1 QPF 编辑预览

| # | 用例 | 操作 | 预期结果 |
|---|------|------|----------|
| D-01 | set_value 预览 | 选区 + qpf + set_value=5.0 | PreviewStatsPanel 显示统计：before/after min/max/mean/sum |
| D-02 | increase 预览 | 选区 + qpf + increase=3.0 | 影响格点的 qpf 均增加 3.0mm |
| D-03 | decrease 预览 | 选区 + qpf + decrease=2.0 | 减少但不低于 0，floor=0 |
| D-04 | multiply 预览 | 选区 + qpf + multiply=1.5 | 乘以系数 |
| D-05 | clear 预览 | 选区 + qpf + clear | qpf=0 且同步 ptype=0 |
| D-06 | screen_clear 预览 | qpf + screen_clear + threshold=0.5 | 选区内 qpf ≤ 0.5 的格点清零 |
| D-07 | 负值自动修正 | decrease 导致负值 | 预览显示 min=0（不为负） |
| D-08 | 空选区预览 | 无 mask 时请求预览 | 提示需要先选区 |
| D-09 | 预览统计面板 | 预览返回后 | 显示影响格点数、面积 km²、before/after 对比表 |
| D-10 | 预览警告 | 产生新降水区域 | 显示 new_precip_needs_ptype 警告 + 格点计数 |

### 4.2 Ptype 编辑预览

| # | 用例 | 操作 | 预期结果 |
|---|------|------|----------|
| D-11 | set_ptype=1(雨) | 选区 + ptype + set_ptype=1 | 仅 qpf > 0.1mm 的格点变为雨 |
| D-12 | set_ptype=2(雪) | 选区 + ptype + set_ptype=2 | 仅有效降水区变雪 |
| D-13 | set_ptype=3(雨夹雪) | 选区 + ptype + set_ptype=3 | 仅有效降水区变混合 |
| D-14 | 无降水区设 ptype | 选区全部 qpf ≤ 0.1mm | ptype 强制为 0，无实际变化 |
| D-15 | ptype 转换矩阵 | 预览 ptype 修改 | PreviewStatsPanel 显示 4×4 转换矩阵 |

### 4.3 编辑应用

| # | 用例 | 操作 | 预期结果 |
|---|------|------|----------|
| D-16 | 正常 apply | 预览后点击"应用" | qpf_after/ptype_after 更新，操作列表新增记录 |
| D-17 | 新降水 apply | 新产生降水且无 target_ptype | 弹出相态选择对话框（雨/雪/雨夹雪），选择后 apply |
| D-18 | apply 不选 ptype | 新降水弹窗中不选 ptype 直接提交 | 返回 NEW_PRECIP_NEEDS_PTYPE 错误 |
| D-19 | 预览过期后 apply | 等待预览过期（>10min） | 返回 PREVIEW_EXPIRED，提示重新预览 |
| D-20 | 连续 apply | 不预览直接 apply | 无法操作，必须先预览 |
| D-21 | apply 后视图刷新 | apply 成功 | "after" 视图自动更新显示编辑结果 |

### 4.4 Undo/Redo

| # | 用例 | 操作 | 预期结果 |
|---|------|------|----------|
| D-22 | 单次 undo | apply 后点击撤销 | 恢复到上一状态，canRedo=true |
| D-23 | 单次 redo | undo 后点击重做 | 恢复撤销的操作 |
| D-24 | 连续多次 undo | 执行 3 次操作后连续 undo 3 次 | 恢复到初始状态，canUndo=false |
| D-25 | undo 后新操作 | undo 后执行新编辑 | redo 历史被清除，canRedo=false |
| D-26 | 操作历史显示 | 多次操作后 | OperationHistory 按序显示所有操作，撤销的加删除线 + "已撤销" 标签 |
| D-27 | 无操作时 undo | 无任何操作时 | undo 按钮禁用 |
| D-28 | undo/redo 字段同步 | undo/redo 后 | qpf_after/ptype_after/touched_mask/changed_mask 全部正确重算 |

### 4.5 一致性规则

| # | 用例 | 操作 | 预期结果 |
|---|------|------|----------|
| D-29 | qpf>threshold+ptype=0 | 增加 qpf 到 >0.1mm 区域但不设 ptype | 保存时 CONSISTENCY_VIOLATION |
| D-30 | clear 联动 | qpf clear | ptype 同步清零 |
| D-31 | ptype 在无降水区 | 降水区设 ptype 后 decrease 到 0 | ptype 自动变 0 |
| D-32 | 负 qpf 修正 | decrease 超出原值 | 自动 clamp 到 0 |

---

## 五、版本管理（Version Lifecycle）

### 5.1 保存版本

| # | 用例 | 操作 | 预期结果 |
|---|------|------|----------|
| E-01 | 正常保存 | 编辑后点击"保存版本" | 创建 draft 版本，会话状态→saved |
| E-02 | 无编辑保存 | 无任何操作直接保存 | 可保存（与原始相同的快照） |
| E-03 | 保存字段完整性 | 保存后查看版本详情 | qpf_before/after, ptype_before/after, delta_qpf, change_ptype, touched_mask, changed_mask 全部有值 |
| E-04 | 版本号递增 | 同窗口保存多个版本 | version_no 自增 (1, 2, 3...) |
| E-05 | 审计日志 | 保存版本 | audit_log 记录 version_save |

### 5.2 提交审核

| # | 用例 | 操作 | 预期结果 |
|---|------|------|----------|
| E-06 | draft 提交 | 点击"提交审核" | 状态 draft → submitted |
| E-07 | 非 draft 提交 | 对 approved 版本提交 | 返回 VERSION_STATUS_CONFLICT |
| E-08 | forecaster 提交 | forecaster 保存并提交 | 成功，进入待审核 |

### 5.3 审核操作

| # | 用例 | 操作 | 预期结果 |
|---|------|------|----------|
| E-09 | 批准版本 | reviewer 对 submitted 版本点"批准" | 状态 submitted → approved |
| E-10 | 驳回版本 | reviewer 点"驳回"并填写意见 | 状态 submitted → rejected，记录 comment |
| E-11 | 驳回无意见 | 驳回但不填 comment | 前端校验提示必填 |
| E-12 | forecaster 审核 | forecaster 尝试审核 | 无审核按钮 / API 返回 403 |
| E-13 | 审核历史 | 查看版本详情 | 显示完整审批时间线（审核人、动作、时间、意见） |

### 5.4 发布

| # | 用例 | 操作 | 预期结果 |
|---|------|------|----------|
| E-14 | 正常发布 | 对 approved 版本点"发布" | 状态 approved → released，创建 ReleaseProduct(active) |
| E-15 | 发布替代旧版 | 同窗口已有 released 版本时发布新版 | 旧版 released → superseded，新版 released |
| E-16 | 非 approved 发布 | 对 draft/submitted 版本发布 | 返回 VERSION_STATUS_CONFLICT |
| E-17 | 发布确认弹窗 | 点击"发布" | 弹出确认对话框，确认后才执行 |
| E-18 | 审计日志 | 发布版本 | audit_log 记录 version_release |

### 5.5 版本列表与详情（ApprovalView）

| # | 用例 | 操作 | 预期结果 |
|---|------|------|----------|
| E-19 | 状态 Tab 过滤 | 切换"全部/待审核/已批准/已驳回/已发布" | 列表按状态正确过滤 |
| E-20 | 窗口过滤 | 选择特定窗口 | 仅显示该窗口的版本 |
| E-21 | 版本详情 | 点击版本列表项 | 右侧面板显示详情：版本号、创建者、时间、操作数、影响格点 |
| E-22 | 前后对比图 | 查看版本详情 | 地图对比面板显示 before/after |
| E-23 | 派生字段 Tab | 切换 delta_qpf/change_ptype/touched_mask/changed_mask | 各字段图正确渲染 |
| E-24 | 审核图片画廊 | 查看详情中的图片 | 6 种图片（before/after/delta_qpf/change_ptype/touched/changed）缩略图 |
| E-25 | 图片全屏预览 | 点击缩略图 | 全屏遮罩显示大图，可关闭 |

---

## 六、复盘中心（Review Center）

### 6.1 复盘生成

| # | 用例 | 操作 | 预期结果 |
|---|------|------|----------|
| F-01 | 正常生成 | 选择版本 + 模板，点"生成复盘" | 创建 ReviewProduct，状态 pending → running → success |
| F-02 | 生成轮询 | 生成请求后 | 每 3 秒自动轮询状态，UI 实时更新 |
| F-03 | 生成成功 | 绘图完成 | 显示合成图、面板统计、耗时信息 |
| F-04 | 部分成功 | 缺少 IFS 字段 | plot_status=partial_success，显示缺失字段表 |
| F-05 | 生成失败 | 绘图出错 | plot_status=failed，显示错误日志链接 + 重试按钮 |
| F-06 | 重新生成 | 对已完成的复盘点"重新生成" | 创建新复盘记录，旧记录保留 |
| F-07 | 模板不存在 | 使用无效 template_id | 返回 TEMPLATE_NOT_FOUND |

### 6.2 复盘浏览与导出

| # | 用例 | 操作 | 预期结果 |
|---|------|------|----------|
| F-08 | 复盘列表过滤 | 按 case_id / window_id / plot_status 筛选 | 列表正确过滤 |
| F-09 | 复盘详情 | 点击复盘列表项 | 显示合成图、面板统计、缺失字段、耗时 |
| F-10 | ZIP 导出 | 点击"导出" | 下载 ZIP 包含：图片 + 配置 + manifest + npz 文件 |
| F-11 | 未完成导出 | 对 pending/running 状态导出 | 拒绝导出，提示等待完成 |
| F-12 | 轮询终止 | 状态进入 success/failed/permanently_failed | 自动停止轮询 |
| F-13 | 缺失字段表 | partial_success 详情 | 显示 variable_name / level / lead_hour / reason |

---

## 七、统计分析（Statistics）

| # | 用例 | 操作 | 预期结果 |
|---|------|------|----------|
| G-01 | 默认加载 | 进入统计页 | 默认最近 30 天，显示 4 个汇总卡 + 3 个图表 |
| G-02 | 日期过滤 | 修改日期范围 | 图表和汇总数据刷新 |
| G-03 | 用户过滤 | 输入 user_id | 仅显示该用户的统计 |
| G-04 | 窗口过滤 | 输入 window_id | 仅显示该窗口的统计 |
| G-05 | 累积时效过滤 | 选择 24h/48h/168h | 按时效过滤统计 |
| G-06 | 工具柱状图 | 查看操作统计 | 按 polygon/line_buffer/brush_path 分组柱状图 |
| G-07 | 操作类型柱状图 | 查看操作类型统计 | 按 set_value/increase/decrease... 分组 |
| G-08 | ptype 转换热力图 | 查看相态统计 | 4×4 转换矩阵热力图 |
| G-09 | CSV 导出 | 点击导出 | 下载 CSV，包含操作统计 + ptype 转换 + 版本汇总 |
| G-10 | 日期范围上限 | 设置超过 365 天 | 接口拒绝，提示最大 365 天 |
| G-11 | 空数据 | 过滤结果为空 | 图表显示空状态，汇总卡为 0 |

---

## 八、管理功能（Admin）

### 8.1 用户管理

| # | 用例 | 操作 | 预期结果 |
|---|------|------|----------|
| H-01 | 用户列表 | admin 进入用户管理 | 表格显示用户名、显示名、角色、状态、最后登录 |
| H-02 | 创建用户 | 点击创建，填写完整信息 | 成功创建，列表刷新 |
| H-03 | 创建重复用户名 | 使用已存在的用户名 | 返回错误提示 |
| H-04 | 创建校验 | 不填必填字段 | 前端表单校验提示 |
| H-05 | 编辑用户 | 修改显示名和角色 | 保存成功，列表更新 |
| H-06 | 禁用用户 | 切换 is_active 为 false | 用户被禁用，再次登录失败 |
| H-07 | 启用用户 | 切换 is_active 为 true | 用户恢复，可再次登录 |
| H-08 | 审计日志 | 创建/编辑/禁用用户 | audit_log 记录 user_manage |
| H-09 | 分页 | 用户量超过 page_size | 分页正常工作 |
| H-10 | 按角色/状态过滤 | 选择过滤条件 | 列表正确过滤 |

### 8.2 配置管理

| # | 用例 | 操作 | 预期结果 |
|---|------|------|----------|
| H-11 | 查看配置 | 切换 product_config/plot_config/template_config Tab | 显示当前 JSON 配置 |
| H-12 | 修改配置 | 编辑 JSON 并保存 | 创建 ConfigSnapshot，配置生效 |
| H-13 | 无效 JSON | 输入非法 JSON | 前端校验失败，提示格式错误 |
| H-14 | 配置历史 | 查看历史列表 | 显示 snapshot_id、修改人、时间 |
| H-15 | 审计日志 | 修改配置 | audit_log 记录 config_change |

### 8.3 模板管理

| # | 用例 | 操作 | 预期结果 |
|---|------|------|----------|
| H-16 | 模板列表 | 进入模板管理 | 左侧面板显示模板名称 + 面板数量 |
| H-17 | 模板详情 | 选择模板 | 显示必需字段、可选字段、面板结构 |
| H-18 | 编辑模板 | admin 修改模板 JSON | 保存成功，创建 ConfigSnapshot |
| H-19 | 非 admin 编辑 | reviewer 尝试编辑模板 | 编辑按钮不可见 / API 403 |

### 8.4 任务监控

| # | 用例 | 操作 | 预期结果 |
|---|------|------|----------|
| H-20 | 任务概览 | 进入任务监控 | 4 卡片显示 pending/running/success/failed 计数 |
| H-21 | 自动刷新 | 页面停留 | 每 10 秒自动刷新数据 |
| H-22 | 失败任务表 | 有失败任务 | 表格显示 task_id, window, error, 时间, 重试按钮 |
| H-23 | 手动重试 | 点击失败任务的重试按钮 | 任务重新排队，状态变 pending |
| H-24 | permanently_failed | 超过重试次数 | 仅 admin 可手动重试 |
| H-25 | 审计日志 | 手动重试 | audit_log 记录 manual_retry |

### 8.5 存储监控

| # | 用例 | 操作 | 预期结果 |
|---|------|------|----------|
| H-26 | 存储概览 | 进入存储监控 | 进度条显示使用率 + GB 数值 |
| H-27 | 目录明细 | 查看明细表 | 显示各类型目录的大小和文件数 |
| H-28 | 最后扫描时间 | 查看页面 | 显示上次存储扫描时间戳 |

### 8.6 审计日志

| # | 用例 | 操作 | 预期结果 |
|---|------|------|----------|
| H-29 | 日志列表 | 进入审计日志 | 表格显示时间、用户、动作、资源类型、摘要 |
| H-30 | 按用户过滤 | 输入 user_id | 仅显示该用户操作 |
| H-31 | 按动作过滤 | 输入 action | 仅显示该类型操作 |
| H-32 | 按资源类型过滤 | 输入 resource_type | 正确过滤 |
| H-33 | 日期范围过滤 | 选择日期 | 仅显示范围内记录 |
| H-34 | 展开详情 | 点击展开行 | 显示 detail_json 完整内容 |
| H-35 | 分页 | 大量日志 | 分页控件正常工作 |

---

## 九、全链路场景（End-to-End Scenarios）

### 9.1 Happy Path：完整编辑发布流程

| # | 用例 | 步骤 | 预期 |
|---|------|------|------|
| E2E-01 | 正常编辑→发布→复盘 | 1. admin 登录<br>2. 输入 case_id 扫描<br>3. 选择 24h available 窗口<br>4. 进入编辑器，polygon 工具选区<br>5. qpf increase 3.0mm<br>6. 预览→应用<br>7. brush 工具，ptype set=2(雪)<br>8. 预览→应用<br>9. 保存版本<br>10. 提交审核<br>11. reviewer 登录，批准<br>12. 发布<br>13. 生成复盘<br>14. 导出 ZIP | 全链路成功，审计日志完整记录所有动作 |

### 9.2 驳回→修改→重新发布

| # | 用例 | 步骤 | 预期 |
|---|------|------|------|
| E2E-02 | 驳回后修订 | 1. forecaster 编辑并提交 v001<br>2. reviewer 驳回（填意见"降水量过大"）<br>3. forecaster 开新会话（base=v001）<br>4. 修改 decrease 降水<br>5. 保存 v002 并提交<br>6. reviewer 批准并发布 | v001=rejected, v002=released, v002.base_version_id=v001 |

### 9.3 部分数据场景

| # | 用例 | 步骤 | 预期 |
|---|------|------|------|
| E2E-03 | partial 窗口编辑 | 1. 扫描得到 partial 状态窗口<br>2. 进入编辑器（有警告提示）<br>3. 正常编辑保存发布<br>4. 复盘生成 partial_success<br>5. 导出包含 missing_fields | 全流程可完成，missing_fields 正确记录 |

### 9.4 多工具混合编辑 + Undo/Redo

| # | 用例 | 步骤 | 预期 |
|---|------|------|------|
| E2E-04 | 复杂编辑链 | 1. polygon qpf set_value=10<br>2. line_buffer qpf increase=2<br>3. brush ptype set=1(雨)<br>4. undo（撤销 brush）<br>5. undo（撤销 line_buffer）<br>6. redo（恢复 line_buffer）<br>7. 新操作 polygon ptype set=2(雪)<br>8. 此时 redo 不可用<br>9. 保存版本 | 最终结果 = op1 + op2(恢复) + op4(新)，redo 链在新操作后断裂 |

### 9.5 并发冲突场景

| # | 用例 | 步骤 | 预期 |
|---|------|------|------|
| E2E-05 | 窗口锁竞争 | 1. userA 开始会话 windowX<br>2. userB 尝试开始会话 windowX | userB 收到 WINDOW_LOCKED 错误 |
| E2E-06 | 会话过期后再编辑 | 1. 开始会话<br>2. 等待超过 TTL<br>3. 尝试 preview | 收到 SESSION_EXPIRED |

### 9.6 角色权限全覆盖

| # | 用例 | 步骤 | 预期 |
|---|------|------|------|
| E2E-07 | viewer 全链路 | viewer 登录 → 只能看首页/统计/审计 | 编辑器/管理页不可访问，操作按钮不可见 |
| E2E-08 | forecaster 全链路 | 编辑+保存+提交 → 无法审核/发布 | 审核/发布按钮不存在 |
| E2E-09 | reviewer 全链路 | 可编辑+审核+发布 → 无法管理用户/配置 | 管理页不可访问 |
| E2E-10 | admin 全链路 | 遍历所有功能 | 全部可用 |

### 9.7 版本替代链

| # | 用例 | 步骤 | 预期 |
|---|------|------|------|
| E2E-11 | 发布替代 | 1. 发布 v001（active）<br>2. 编辑保存 v002 → 审核 → 发布 | v001=superseded, v002=released(active) |

### 9.8 清除与边界

| # | 用例 | 步骤 | 预期 |
|---|------|------|------|
| E2E-12 | screen_clear 联动 | 1. 有大面积微量降水<br>2. screen_clear threshold=0.5<br>3. 预览确认<br>4. 应用 | qpf ≤ 0.5 区域清零，ptype 同步清零 |
| E2E-13 | 新降水区对话框 | 1. 在 qpf=0 区域 set_value=5.0<br>2. 预览→apply | 弹出 target_ptype 选择框，选择后成功 |
| E2E-14 | 选区完全在 invalid_mask | 1. 在全 invalid 区域画选区<br>2. 预览 | 返回 MASK_EMPTY |
| E2E-15 | 选区超出边界 | 在 70-111°E, 25-50°N 之外画选区 | 返回 MASK_OUT_OF_BOUNDS 或 clamp 到边界 |

---

## 十、错误处理与异常恢复

| # | 用例 | 操作 | 预期结果 |
|---|------|------|----------|
| X-01 | 网络中断 | 操作中断网 | 显示网络错误提示，不丢失已有状态 |
| X-02 | 500 错误 | 后端返回 500 | 前端显示通用错误信息 |
| X-03 | 401 自动跳转 | Token 失效后操作 | 自动跳转到登录页 |
| X-04 | 字段加载失败 | 二进制字段请求失败 | 显示 fieldLoadError，不崩溃 |
| X-05 | 预览错误恢复 | preview 请求失败 | 显示 previewError，可重试 |
| X-06 | 保存失败恢复 | save 请求失败 | 显示错误提示，会话保持编辑状态可重试 |
| X-07 | 并发修改检测 | 版本 base 过期 | 返回 VERSION_BASE_OUTDATED，提示刷新 |

---

## 十一、性能与用户体验

| # | 用例 | 验收标准 |
|---|------|----------|
| P-01 | 窗口列表加载 | ≤ 1s |
| P-02 | polygon 预览响应 | ≤ 3s |
| P-03 | brush apply 响应 | ≤ 1s |
| P-04 | undo/redo 50 步 | ≤ 5s |
| P-05 | 版本保存 | ≤ 10s |
| P-06 | 地图渲染流畅 | 缩放/平移无卡顿 |
| P-07 | 画笔绘制流畅 | 拖拽采样无明显延迟 |
| P-08 | 大数据图表渲染 | ECharts 图表 ≤ 2s |

---

## 十二、数据一致性验证

| # | 用例 | 验证点 |
|---|------|--------|
| V-01 | 二进制字段头校验 | X-Grid-Rows=501, X-Grid-Cols=821, dtype 正确 |
| V-02 | QPF 差分计算 | delta_qpf = qpf_after - qpf_before，逐格点验证 |
| V-03 | touched_mask 完整性 | 所有操作 mask 的并集 = touched_mask |
| V-04 | changed_mask 正确性 | qpf_after ≠ qpf_before OR ptype_after ≠ ptype_before 的格点 |
| V-05 | ptype 一致性 | 所有 qpf ≤ 0.1mm 格点的 ptype 必须为 0 |
| V-06 | 操作重放一致性 | 从 base 重放所有操作 = 当前 after 字段 |
| V-07 | 版本快照不可变 | 版本保存后字段文件内容不变 |
| V-08 | 审计日志完整性 | 每个状态变更均有对应 audit_log 记录 |

---

## 附录：测试数据需求

| 数据 | 说明 |
|------|------|
| case_id | 2026051808 / 2026051820（valid），2026051812（invalid HH） |
| 用户 | admin_test / reviewer_test / forecaster_test / viewer_test |
| 窗口 | 24h/48h/168h 各至少 1 个 available + 1 个 partial + 1 个 invalid |
| 网格数据 | 501×821 float32 (qpf) / uint8 (ptype) 标准测试数组 |
| 模板 | 默认复盘模板（含 required + optional 字段） |
| 配置 | product_config（含 ptype_qpf_threshold_mm=0.1, allow_partial_window_edit=true） |

---

> 共计 **12 大类、~120 个测试用例**，覆盖认证/权限/扫描/窗口/编辑器/编辑操作/版本生命周期/复盘/统计/管理/全链路/异常/性能/数据一致性。
