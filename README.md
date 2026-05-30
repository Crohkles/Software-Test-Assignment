# AutoTestDesign — AI 驱动测试设计工具

面向 **Software Testing Assignment 2** 的全栈 AutoTestDesign 实现：在 **generation_pipeline**（Map-Reduce / 多 Worker）与可选 LLM 增强下，完成需求解析、风险分析（QRA）、黑盒/白盒测试设计、测试预言、套件优化与多格式导出，并以 **FitnessAI**（智能健身辅助系统）作为目标应用验证工具有效性。

| 元数据 | 值 |
| --- | --- |
| 生成管线 | `generation-pipeline-v1`（`run_generation_pipeline`） |
| 规则引擎版本 | `autotestdesign-engine-v3`（生成响应 `engineMetadata`） |
| Prompt 版本 | `autotestdesign-v6-fr-complete` |
| 目标应用 | FitnessAI |

---

## 目录

1. [项目简介](#1-项目简介)
2. [系统架构与生成管线](#2-系统架构与生成管线)
3. [功能与 Assignment2 符合性](#3-功能与-assignment2-符合性)
4. [仓库结构](#4-仓库结构)
5. [环境准备](#5-环境准备)
6. [快速启动](#6-快速启动)
7. [使用指南（QRA → 黑盒 → 白盒 → 汇总）](#7-使用指南qra--黑盒--白盒--汇总)
8. [FitnessAI Prompt 示例](#8-fitnessai-prompt-示例)
9. [API 参考](#9-api-参考)
10. [引擎与 Worker 模块](#10-引擎与-worker-模块)
11. [导出与历史](#11-导出与历史)
12. [测试与验证](#12-测试与验证)
14. [相关文档](#14-相关文档)

---

## 1. 项目简介

### 1.1 做什么

本仓库实现的是 **AutoTestDesign 工具本身**（被测对象不是本工具）。推荐工作流对齐 ISTQB / ISO/IEC/IEEE 29119-4 思路：

```
多源需求输入 → QRA（结构化需求 + 风险评分）→ 按技术独立生成黑盒/白盒用例
    → 结果汇总与交互审查 → 测试预言 / 套件优化（可选）→ JSON / CSV / Markdown / Excel 导出
```

生成结果可写入 PostgreSQL，支持历史回看、实验指标统计与答辩演示。

### 1.2 测谁（目标应用）

**FitnessAI** 为固定目标应用，核心范围包括：

| 模块 | 测试关注点 |
| --- | --- |
| 姿态分析 `POST /api/analytics/pose` | `exerciseType` 等价类；MediaPipe `landmarks` 32/33/34 边界 |
| 状态机计数 | UP → DESCENDING → DOWN → ASCENDING → UP 完整循环；非法短循环不计数 |
| 训练记录过滤 | `count < 3` 且 `durationSeconds < 30` 不入库 |
| 训练计划 | 难度、组数、休息、`skipRest` 组合 |
| 仪表盘 | 趋势、分布、卡路里（MET × 体重 × 时长） |

导入 Markdown 或点击前端 **填入示例** 即可快速演示。

### 1.3 设计原则

- **Worker 可独立调度**：黑盒 5 种技术、白盒 `WhiteBoxJava`、Oracle、Optimization 通过 `selectedTechniques` 按需激活。
- **确定性优先**：黑盒 LLM 不可用时降级到 `blackbox_fallbacks.py`；白盒 CFG、coverage item、path 由 Java 分析器确定性产出。
- **白盒 LLM 边界**：`whitebox_llm_enhancer.py` 仅增强自然语言标题、输入/前置/oracle 建议与审查问题，**不得**修改 CFG、coverageItems、coverageTargets、path。
- **可审查**：QRA 风险、覆盖项、策略、用例、白盒 coverage 勾选与 manual item 均可编辑后保存/应用。
- **可追踪**：`engineMetadata`、`timingMetrics`、`pipelineVersion` 随响应与历史记录保存。

---

## 2. 系统架构与生成管线

```
┌──────────────────────────────────────────────────────────────────────────┐
│                         浏览器  http://localhost:5173                     │
│   Vue 3：QRA / 黑盒技术 Tab / White-Box Tab / Generated Results Summary │
└─────────────────────────────────┬────────────────────────────────────────┘
                                  │ HTTP
                                  ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                    Express Backend  :3000                                 │
│   POST /api/qra · POST /api/testcases/generate · 符合性 · 落库 · 导出    │
└───────────────┬──────────────────────────────────────┬───────────────────┘
                │                                      │
                ▼                                      ▼
┌───────────────────────────────┐      ┌───────────────────────────────────┐
│  FastAPI ai-service  :8000    │      │  PostgreSQL  :5432                 │
│  generation_pipeline          │      │  generation_records（JSONB）       │
│  blackbox_workers /           │      └───────────────────────────────────┘
│  whitebox_java_worker         │
│  LLM（OpenAI 兼容，可选）      │
└───────────────────────────────┘
```

### 2.1 主生成流程（当前）

1. `frontend/src/App.vue` → `POST /api/testcases/generate`
2. `backend/src/index.js` → `ai-service` `POST /generate-testcases`
3. `ai-service/app/main.py` 构造 `GlobalContext`（含 QRA 阶段确认的 `requirementsStructured`、`riskItems`）
4. `ai-service/app/engines/generation_pipeline.py` 按 `selectedTechniques` 路由到对应 Worker，Reduce 阶段合并工件
5. 返回 `testcases`、`artifacts`（含 `coverageItems`、`testSequences`、`llmEnhancedTestcases` 等）、`engineMetadata`、`timingMetrics`

### 2.2 支持的 `selectedTechniques`

| ID | 类型 | 说明 |
| --- | --- | --- |
| `EP` | 黑盒 | 等价类划分 |
| `BVA` | 黑盒 | 边界值分析 |
| `DecisionTable` | 黑盒 | 决策表 |
| `Combinatorial` | 黑盒 | Pairwise 组合 |
| `StateTransition` | 黑盒 | 状态迁移（LLM + `state_model_engine` 降级） |
| `WhiteBoxJava` | 白盒 | Java 方法级 CFG、statement/branch 覆盖项与 test sequences |
| `Oracle` | 后处理 | 为用例附加 oracle |
| `Optimization` | 后处理 | `risk-first` / `minimize` 套件优化 |

### 2.3 运行模式

| 模式 | 条件 | 行为 |
| --- | --- | --- |
| 离线 / 确定性 | `OPENAI_API_KEY` 为空或 LLM 失败 | 黑盒 fallback、白盒 CFG 与序列仍可用；白盒增强区显示 `promptPreview` |
| 在线增强 | 配置 `OPENAI_API_KEY`、`OPENAI_BASE_URL`、`OPENAI_MODEL` | 黑盒用例更贴近 prompt；白盒输出 `llmEnhancedTestcases` 自然语言设计说明 |

---

## 3. 功能与 Assignment2 符合性

### 3.1 功能性需求（FR）

| FR | 描述 | 实现状态 | 主要实现 |
| --- | --- | --- | --- |
| **FR 1.0** | CSV / 纯文本 / 文件导入 | ✅ | 前端多源输入；`requirement_parser` |
| **FR 1.1** | 需求结构化 | ✅ | QRA → `requirementsStructured` |
| **FR 2.0** | 风险分与 H/M/L 优先级 | ✅ | `riskScore = impact × likelihood`；`POST /api/qra` |
| **FR 3.0** | ≥3 种黑盒技术 | ✅ | EP、BVA、DecisionTable、Combinatorial、StateTransition（5 种可独立生成） |
| **FR 4.0** | 白盒覆盖与序列 | ✅ 加分 | `WhiteBoxJava`（CFG）；`StateTransition` 仍可用状态模型 |
| **FR 5.0** | 测试预言 | ✅ 加分 | `Oracle` worker；用例 `oracle` 字段 |
| **FR 6.0** | JSON / CSV / Excel 导出 | ✅ | `.xlsx` 四表；Markdown（含 LLM 白盒增强段） |
| **FR 7.0** | 套件优化 | ✅ 加分 | `Optimization` worker；`testSuiteOptimization` |
| **交互式审查** | 编辑并应用变更 | ✅ | QRA 风险、汇总区覆盖/策略/用例/追溯分 Tab 保存 |

### 3.2 非功能性需求（NFR）

| NFR | 说明 |
| --- | --- |
| 性能 | 管线记录 `engineMs`、`engineMeetsNfr`、`totalMs` |
| 可用性 | 四主 Tab 工作流、FitnessAI 示例、按技术单独生成 |
| 安全 | API Key 仅环境变量；生产需收紧 CORS |
| 可维护性 | Worker 分文件；`generation_pipeline` 单一编排入口；Docker Compose 部署 |

---

## 4. 仓库结构

```
Software-Test-Assignment/
├── frontend/
│   └── src/App.vue              # QRA / 黑盒 / 白盒 / 结果汇总 Tab
├── backend/
│   └── src/
│       ├── index.js             # /api/qra、/api/testcases/generate、导出、历史
│       └── db.js
├── ai-service/
│   ├── app/
│   │   ├── main.py              # FastAPI：QRA、生成、导出
│   │   ├── export_xlsx.py
│   │   └── engines/
│   │       ├── generation_pipeline.py   # 主入口 run_generation_pipeline
│   │       ├── blackbox_workers.py      # EP/BVA/DecisionTable/Combinatorial
│   │       ├── blackbox_fallbacks.py    # 黑盒 LLM 降级
│   │       ├── state_transition_worker.py
│   │       ├── state_model_engine.py    # StateTransition 确定性状态模型
│   │       ├── whitebox_java_analyzer.py
│   │       ├── whitebox_coverage.py
│   │       ├── whitebox_sequence_generator.py
│   │       ├── whitebox_java_worker.py
│   │       ├── whitebox_llm_enhancer.py
│   │       ├── input_hint_generator.py
│   │       ├── requirement_parser.py / risk_engine.py
│   │       ├── oracle_engine.py / suite_optimizer.py / strategy_builder.py
│   │       └── schema_validator.py
│   └── tests/                   # python -m unittest discover -s tests
├── fitnessai-java-tests/        # FitnessAI 相关 Java 测试样例
├── infra/postgres/init.sql
├── docker-compose.yml
├── GENERATION_PIPELINE_REFACTOR_SUMMARY.md
├── FitnessAI_PROMPT_EXAMPLES.md
├── FitnessAI_LLM_CONTEXT.md
├── Assignment2.md
├── DEVELOPMENT_LOG.md
└── README.md
```

> 前端默认 **本地** `npm run dev`，不打包进 Docker；`postgres`、`ai-service`、`backend` 由 Compose 构建。

---

## 5. 环境准备

### 5.1 先决条件

- **Windows / macOS / Linux**
- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- **Node.js 20+** 与 **npm 10+**（前端）
- （可选）Python 3.11+（本地跑 `ai-service/tests`）

### 5.2 环境变量

在项目根目录创建 `.env`（可参考 Compose 中 `postgres` / `ai-service` 所用变量）：

| 变量 | 说明 |
| --- | --- |
| `POSTGRES_*` | 数据库连接 |
| `BACKEND_PORT` | 默认 `3000` |
| `AI_SERVICE_PORT` | 默认 `8000` |
| `OPENAI_API_KEY` | 留空则黑盒/白盒增强走 fallback 或 preview |
| `OPENAI_MODEL` | 如 `deepseek-chat`、`gpt-4o-mini` |
| `OPENAI_BASE_URL` | OpenAI 兼容网关 |
| `TEST_TECHNIQUE` | 默认 `black-box` |

---

## 6. 快速启动

### 6.1 启动后端栈（Docker）

```powershell
cd Software-Test-Assignment
docker compose up -d --build
docker compose ps
```

预期容器 **Up**（`ai-service` 为 **healthy**）：

| 容器名 | 端口 |
| --- | --- |
| `aitest-postgres` | 5432 |
| `aitest-ai-service` | 8000 |
| `aitest-backend` | 3000 |

健康检查：

```powershell
Invoke-RestMethod http://localhost:3000/health
Invoke-RestMethod http://localhost:8000/health
Invoke-RestMethod http://localhost:3000/api/engines/info
```

### 6.2 启动前端（本地）

```powershell
cd frontend
npm install
npm run dev
```

浏览器访问：**http://localhost:5173**

### 6.3 修改代码后

| 改动位置 | 操作 |
| --- | --- |
| `backend/` 或 `ai-service/` | `docker compose up -d --build` |
| `frontend/` | 重启 `npm run dev`，浏览器 **Ctrl+F5** |

### 6.4 停止

```powershell
docker compose down          # 保留数据卷
docker compose down -v       # 清空数据库（慎用）
```

---

## 7. 使用指南（QRA → 黑盒 → 白盒 → 汇总）

### 7.1 推荐演示流程

1. 打开前端，点击 **填入示例** 加载 FitnessAI 需求与全局 Prompt 草稿。
2. 在 **Input and Generate** 侧栏填写/确认 **Overall Input Prompt**（项目范围、风险关注点、输出风格）。
3. 主 Tab **QRA** → **Run QRA**，在子 Tab 中审查 **Structured Requirements** 与 **Risk Items**，保存风险编辑。
4. 主 Tab **Black-Box Technique Test Design**：
   - 勾选要运行的技术（EP、BVA 等）；
   - 在各技术的 **Technical Prompt** 中填写技术专用说明（见 [§8](#8-fitnessai-prompt-示例)）；
   - 按技术 **Generate**（可一次一种，便于审查）。
5. 主 Tab **White-Box Technique Test Design**：
   - 选择 coverage criterion（如 `statement+branch`）；
   - 粘贴 Java 片段或上传 `.java` 文件；
   - 勾选 coverage items、可添加 manual coverage item；
   - **Generate White-Box** → 查看 Java Analysis Result 与 **LLM Enhanced Test Design**。
6. 主 Tab **Generated Results Summary**：浏览 **Coverage Items / Test Strategies / Test Cases / LLM Enhancements / Traceability**，分节保存编辑。
7. 导出 **Markdown / JSON / CSV / Excel**；**History** 可回填记录。

### 7.2 输入方式

| 方式 | 操作 |
| --- | --- |
| 纯文本 | Manual requirements 文本框 |
| CSV | CSV requirements（含 `id,feature,input,condition,expected` 表头） |
| 文件 | 导入 `.md` / `.txt` 等至 `documents[]` |
| 示例 | **填入示例** |
| 白盒 | Java 手动粘贴或文件上传（`sourceType: codebase`） |

### 7.3 Prompt 分工

- **Overall Input Prompt**（侧栏）：描述 FitnessAI 项目、测试范围、风险与期望的输出风格（用例标题、输入、oracle、优先级、追溯）。
- **Technical Prompt**（各黑盒技术）：仅写该技术相关约束（等价类域、边界簇、决策表规则、因子水平、状态迁移路径等）。

---

## 8. FitnessAI Prompt 示例

以下与仓库内示例文档一致，可直接复制到前端对应输入框。

### 8.1 Overall Input Prompt

```text
FitnessAI is an intelligent fitness assistant with pose analysis, repetition counting, training plans, workout record filtering, and dashboard analytics.

Please generate test cases from the reviewed QRA requirements and risk items. Focus on API-level and business-flow behavior that can reveal validation errors, incorrect state counting, record filtering mistakes, invalid plan handling, and dashboard calculation defects.

Use clear test case titles, explicit input data, expected results/oracles, priority, and traceability to requirement or risk IDs. Keep the output suitable for manual review and later automation.
```

### 8.2 各黑盒 Technical Prompt（摘要）

| 技术 | 要点 |
| --- | --- |
| **EP** | `exerciseType`、`landmarks`、难度、`skipRest`、`count`、`durationSeconds`、`weightKg`、`durationHours` 等等价类；正负代表用例各一；链接 REQ id |
| **BVA** | `landmarks.length` 32/33/34；`count` 2/3/4；`durationSeconds` 29/30/31；`weightKg` 边界；`durationHours` 0 与小正数 |
| **Decision Table** | 记录保存：`count`/`durationSeconds` 与 saved/not saved；计划难度与 `skipRest` |
| **Combinatorial** | `exerciseType`、`difficulty`、`skipRest`、记录分类、输入有效性等因素的 pairwise |
| **State Transition** | UP/DESCENDING/DOWN/ASCENDING；合法完整循环、非法短路径、重复帧、cooldown 与 count 变化 |

---

## 9. API 参考

### 9.1 Backend（`:3000`）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/health` | 健康检查 |
| GET | `/api/target-application` | FitnessAI 元数据 |
| GET | `/api/engines/info` | FR 模块与管线能力说明 |
| POST | `/api/qra` | **QRA**：结构化需求 + 风险评分（确定性引擎） |
| POST | `/api/testcases/generate` | **主生成接口**（转发 ai-service） |
| GET | `/api/history?limit=20` | 历史记录 |
| DELETE | `/api/history/:id` | 删除历史 |
| GET | `/api/risk-matrix` | 风险矩阵 |
| POST | `/api/export/artifacts` | JSON / CSV / xlsx |
| GET | `/api/analysis/experiment?limit=200` | 实验统计 |

#### `POST /api/qra`

请求体：`sourceType`、`content`、`documents`（与生成接口输入段一致）。

响应：`requirementsStructured`、`riskItems`、`engineMetadata`、`timingMetrics`。

#### `POST /api/testcases/generate`

黑盒示例（需先完成 QRA 并将审查后的结构传入）：

```json
{
  "sourceType": "requirements",
  "content": "[Plain-text requirements]\n...",
  "requirementsStructured": [],
  "riskItems": [],
  "selectedTechniques": ["EP", "BVA"],
  "techniquePrompts": {
    "EP": "Use Equivalence Partitioning for FitnessAI...",
    "BVA": "Use Boundary Value Analysis for FitnessAI..."
  },
  "customPrompt": "FitnessAI overall scope...",
  "includeOracle": true,
  "includeOptimization": true
}
```

白盒 `WhiteBoxJava` 示例：

```json
{
  "sourceType": "codebase",
  "content": "public class LoginService { ... }",
  "selectedTechniques": ["WhiteBoxJava"],
  "coverageCriterion": "statement+branch",
  "reviewerOverrides": {
    "coverageItemSelection": {},
    "manualCoverageItems": []
  }
}
```

预期（白盒）：

- `testcases[*].designMethod` 为 `WhiteBoxJava`，`technique` 为 `white-box`
- `artifacts.coverageItems` 含 statement / branch 项
- `artifacts.testSequences` 含 `path`、`pathConstraints`、`inputHints`、`setupHints` 等
- `artifacts.llmEnhancedTestcases` 存在；配置 LLM 后含自然语言增强

响应常见字段：`artifacts`、`testcases`、`assignmentCompliance`、`engineMetadata`、`timingMetrics`、`quality`。

### 9.2 AI Service（`:8000`）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/generate-testcases` | generation_pipeline 生成 |
| POST | `/qra`（若直接调用） | 同 QRA 逻辑 |
| GET | `/api/risk-matrix` | 风险矩阵 |
| POST | `/export-artifacts` | xlsx 二进制 |
| GET | `/prompt-template` | Prompt 模板预览 |

---

## 10. 引擎与 Worker 模块

路径：`ai-service/app/engines/`

| 模块 | 职责 |
| --- | --- |
| `generation_pipeline.py` | 主入口：`GlobalContext`、路由、Reduce、`PIPELINE_VERSION` |
| `blackbox_workers.py` | 黑盒 5 技术；优先 LLM，失败则 fallback |
| `blackbox_fallbacks.py` | 确定性黑盒用例与覆盖项辅助 |
| `state_transition_worker.py` / `state_model_engine.py` | StateTransition |
| `whitebox_java_*.py` + `input_hint_generator.py` | Java CFG、覆盖项、序列、输入提示 |
| `whitebox_llm_enhancer.py` | 白盒序列的自然语言增强（不改 CFG/path） |
| `requirement_parser.py` / `risk_engine.py` | FR 1.x / 2.0 |
| `oracle_engine.py` / `suite_optimizer.py` | Oracle / Optimization worker |
| `strategy_builder.py` / `schema_validator.py` | 策略映射、LLM JSON 校验 |

`engines/__init__.py` 仅导出 `run_generation_pipeline`。

本地测试：

```powershell
cd ai-service
python -m unittest discover -s tests
```

---

## 11. 导出与历史

| 格式 | 说明 |
| --- | --- |
| Markdown | 含结构化用例与 **LLM enhanced white-box design** 段 |
| JSON | 完整 `artifacts` + `testcases` |
| CSV | 扁平化用例表 |
| Excel `.xlsx` | Requirements / Risks / Strategies / TestCases 四表 |

每次成功生成写入 `generation_records`；**History → View** 可恢复 QRA 与生成结果至审查区。

---

## 12. 测试与验证

```powershell
# 容器重建
docker compose up -d --build

# 前端构建
cd frontend && npm run build

# ai-service 单元测试
cd ai-service && python -m unittest discover -s tests

# 后端语法检查
cd backend && node --check src/index.js
```

冒烟清单：

- [ ] `http://localhost:3000/health` 与 `http://localhost:8000/health` 正常
- [ ] Run QRA → 有风险项与结构化需求
- [ ] 单选 `EP` 生成 → 用例 `designMethod` 为 `EP`
- [ ] `WhiteBoxJava` 生成 → `testSequences` 与 `coverageItems` 非空
- [ ] Summary 中 **LLM Enhancements** 可查看（或 preview 警告）
- [ ] xlsx 四表可打开；History 可 View / Delete

---

## 13. 相关文档

| 文档 | 用途 |
| --- | --- |
| [GENERATION_PIPELINE_REFACTOR_SUMMARY.md](GENERATION_PIPELINE_REFACTOR_SUMMARY.md) | 管线重构目标、Worker 划分、白盒 LLM 边界、验证步骤 |
| [FitnessAI_PROMPT_EXAMPLES.md](FitnessAI_PROMPT_EXAMPLES.md) | Overall + 各黑盒 Technical Prompt 全文 |
| [FitnessAI_LLM_CONTEXT.md](FitnessAI_LLM_CONTEXT.md) | 目标应用需求与接口 |
| [DEVELOPMENT_LOG.md](DEVELOPMENT_LOG.md) | FR 深度对照与测试流程 |
| [Assignment2.md](Assignment2.md) | 课程作业原文 |

---

## 许可证与课程说明

本项目为课程作业实现，目标应用 **FitnessAI** 仅用于验证 AutoTestDesign 工具有效性。使用 LLM 时请遵守校方与 API 服务商规定，勿将真实 API Key 提交至公开仓库。
