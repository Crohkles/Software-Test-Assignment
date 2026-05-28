# DEVELOPMENT_LOG — Assignment2 AutoTestDesign

> 文档版本：`2026-05-28` · 生成管线 `generation-pipeline-v1` · 规则引擎 `autotestdesign-engine-v3` · Prompt `autotestdesign-v6-fr-complete`  
> 目标应用：**FitnessAI**（智能健身辅助系统）。风险报告、测试计划、详细测试设计等 **PDF 交付物面向 FitnessAI**，而非本工具本身。  
> 管线重构详见 [GENERATION_PIPELINE_REFACTOR_SUMMARY.md](GENERATION_PIPELINE_REFACTOR_SUMMARY.md)；FitnessAI Prompt 见 [FitnessAI_PROMPT_EXAMPLES.md](FitnessAI_PROMPT_EXAMPLES.md)。

---

## 1. 项目概述

### 1.1 作业定位

| 维度 | 说明 |
| --- | --- |
| **工具** | AI 驱动的 AutoTestDesign：多源输入 → **QRA**（结构化需求 + 风险）→ **按技术独立生成**黑盒/白盒用例 → 汇总审查 → 预言/优化（可选）→ 导出 |
| **目标应用** | FitnessAI：姿态分析、状态机计数、训练计划、记录过滤、仪表盘统计 |
| **必做 FR** | FR 1.0、1.1、2.0、3.0、6.0 + **交互式审查**（Assignment2「主要内容」） |
| **加分 FR** | FR 4.0（`WhiteBoxJava` CFG + `StateTransition`）、5.0、7.0 |

### 1.2 技术栈

```
┌─────────────┐     HTTP      ┌─────────────┐     HTTP      ┌─────────────┐
│  Vue 3 +    │ ────────────► │  Express    │ ────────────► │  FastAPI    │
│  Vite 前端   │               │  backend    │               │  ai-service │
│  :5173      │               │  :3000      │               │  :8000      │
└─────────────┘               └──────┬──────┘               └──────┬──────┘
                                     │                              │
                                     │    generation_pipeline       │
                                     ▼                              ▼
                              ┌─────────────┐              OpenAI 兼容 API
                              │ PostgreSQL  │              （黑盒 LLM / 白盒增强）
                              │  :5432      │
                              └─────────────┘
```

### 1.3 仓库结构

| 路径 | 职责 |
| --- | --- |
| `frontend/src/App.vue` | 四主 Tab：QRA、黑盒技术、白盒、结果汇总；按技术生成；Technical Prompt |
| `backend/src/index.js` | `/api/qra`、`/api/testcases/generate`、符合性、导出、历史 |
| `backend/src/db.js` | 生成记录持久化（JSONB 列） |
| `ai-service/app/main.py` | QRA、生成、`GlobalContext`、响应封装 |
| `ai-service/app/engines/generation_pipeline.py` | **主入口** `run_generation_pipeline`（Map-Reduce / Worker 路由） |
| `ai-service/app/engines/blackbox_workers.py` | 黑盒 5 技术（LLM 优先 + fallback） |
| `ai-service/app/engines/whitebox_java_*.py` | Java CFG、覆盖项、序列、LLM 增强层 |
| `FitnessAI_LLM_CONTEXT.md` | 目标应用需求/接口上下文 |
| `fitnessai-java-tests/` | FitnessAI 相关 Java 测试样例（交付物 4 参考） |
| `GENERATION_PIPELINE_REFACTOR_SUMMARY.md` | 重构目标、模块边界、验证说明 |
| `FitnessAI_PROMPT_EXAMPLES.md` | Overall + 各黑盒 Technical Prompt |
| `README.md` | 安装、工作流、API 速查 |

---

## 2. Assignment2 符合性审查

### 2.1 工具功能性需求（FR）

| FR | 作业要求 | 实现状态 | 实现位置与证据 | 残余风险 |
| --- | --- | --- | --- | --- |
| **FR 1.0** 输入/解析 | CSV、纯文本、直接输入 | **已满足** | 前端：文件上传、`manualRequirementText`、`csvRequirementText`；`buildManualContent()`；后端 `content` + `documents`；白盒 `sourceType: codebase` + Java 片段/文件 | 极复杂 CSV 未单独测尽 |
| **FR 1.1** 需求结构化 | 字段、范围、条件、预期动作 | **已满足** | `requirement_parser.parse_content_blocks`；**QRA** 输出 `requirementsStructured`；QRA Review 可编辑 | 非 CSV/编号句式依赖默认模板或 LLM 补充 |
| **FR 2.0** 风险与优先级 | 风险分 + H/M/L | **已满足** | `risk_engine.score_requirements`；`POST /api/qra`；Risk Items Review 可保存后再生成 | 初值来自特性权重表 |
| **FR 3.0** 黑盒设计 | ≥3 种 ISO 29119-4 技术 | **已满足** | `blackbox_workers`：EP、BVA、DecisionTable、Combinatorial、StateTransition **可独立** `selectedTechniques` 调度；`blackbox_fallbacks` 降级；`techniquePrompts`  per 技术 | LLM 不可用时 fallback 用例较模板化 |
| **FR 4.0** 白盒建模 | 状态图/覆盖/序列 | **已满足（加分）** | **`WhiteBoxJava`**：`whitebox_java_analyzer` 方法级 CFG + `whitebox_coverage` + `whitebox_sequence_generator`；**StateTransition** 仍经 `state_transition_worker` + `state_model_engine` | Java 分析为简化 CFG；非通用图编辑器 |
| **FR 5.0** 测试预言 | 合成预期结果 | **已满足（加分）** | `Oracle` worker → `oracle_engine.attach_oracles`；用例 `oracle` / `oracleHints` | 非符号执行 |
| **FR 6.0** 输出导出 | JSON / Excel / CSV | **已满足** | openpyxl 四表；Markdown（含 LLM 白盒增强段）；`POST /api/export/artifacts` | — |
| **FR 7.0** 套件优化 | 风险优先或最小化 | **已满足（加分）** | `Optimization` worker → `suite_optimizer`；`testSuiteOptimization` | 非理论最小覆盖 |
| **交互式审查** | 可改覆盖项、策略、用例 | **已满足** | QRA 风险分 Tab 保存；Summary 五子 Tab 分节保存；白盒 coverage 勾选与 manual item | — |

**生成流水线（当前）：**

```
POST /api/qra（确定性解析 + 风险）
    → 用户审查 requirementsStructured / riskItems
    → POST /api/testcases/generate
    → main.py 构造 GlobalContext
    → run_generation_pipeline(selectedTechniques)  // Worker 并发 + Reduce
    → artifacts + testcases + engineMetadata + timingMetrics
    → backend 质量分析、assignmentCompliance、落库
```

旧 `pipeline.py`、`blackbox_engine.py`（主黑盒）、`whitebox_engine.py`（主白盒）已移除或改名为 fallback/helper，**不再**作为 FastAPI 主流程入口。

### 2.2 作业「主要内容」流程支持

| 步骤 | 工具支持 | 建议留证 |
| --- | --- | --- |
| 概念 / 覆盖项 | QRA → `requirementsStructured`；生成后 `coverageItems`（含白盒 statement/branch） | QRA 与 Summary 截图 |
| 策略与方法 | `testStrategies`；`designMethod`（EP/BVA/…/WhiteBoxJava） | 按技术单独生成前后对比 |
| 用例与追溯 | `testcases`、`traceability`；白盒 `testSequences` | 审查前后 JSON |
| 提示设计 | Overall Input Prompt + 各技术 **Technical Prompt**（见 [FitnessAI_PROMPT_EXAMPLES.md](FitnessAI_PROMPT_EXAMPLES.md)） | Prompt 文本入报告附录 |
| 结果分析 | `quality`（黑盒方法覆盖）；白盒 `needsReview`、`constraintConflicts` | Metrics + LLM Enhancements Tab |
| 交互改进 | QRA 改风险；Summary 分节 Save；再导出 Markdown | 两个 `.md` diff |

### 2.3 非功能性需求（NFR）

| NFR | 状态 | 说明 |
| --- | --- | --- |
| **性能** | **引擎可追踪** | `timingMetrics.engineMs`、`engineMeetsNfr`、`totalMs`；白盒 CFG 为确定性本地分析 |
| **可用性** | 已满足 | 四主 Tab；FitnessAI 填入示例；按技术单独 Generate；白盒 Java 粘贴/上传 |
| **安全性** | 基本满足 | API Key 仅环境变量；CORS 开发态开放 |
| **可维护性** | 已满足 | Worker 分文件；`generation_pipeline` 单一编排；`PIPELINE_VERSION` / `ENGINE_VERSION` 可追踪 |

### 2.4 交付物完成度（Assignment2 §1.2）

| 交付物 | 权重 | 仓库内状态 | 待完成（小组文档/演示） |
| --- | --- | --- | --- |
| 1. AutoTestDesign 工具 + README + 演示视频 | 20% | **代码已完成**；README / 本日志已更新；**视频待录制** | 演示 QRA → 黑盒 EP → WhiteBoxJava |
| 2. FitnessAI 风险分析报告 PDF | 10% | **未在仓库** | 用 QRA 导出 `riskItems` |
| 3. FitnessAI 测试计划 PDF | 40% | **未在仓库** | 引用管线能力与追溯 |
| 4. 详细测试设计与执行 PDF | 30% | **未在仓库** | 可参考 `fitnessai-java-tests/` + 工具导出用例 |
| 演示 PPT | — | **未在仓库** | 15 分钟：QRA、分技术黑盒、Java 白盒、LLM 边界 |

---

## 2.5 各 FR 实现原理、验证流程与对应论证

### FR 1.0 输入/解析

| 维度 | 说明 |
| --- | --- |
| **实现原理** | 前端合并 `[Plain-text]`、`[CSV requirements]`、`documents[]`；白盒 Tab 支持 Java 手动输入或 `.java` 文件（`sourceType: codebase`）。 |
| **对应** | 三种需求输入 + 代码片段输入均已覆盖。 |
| **验证流程** | ① 仅纯文本 → Run QRA；② 仅 CSV（§6.5）；③ 导入 `FitnessAI_LLM_CONTEXT.md`；④ 白盒粘贴 `LoginService` 类片段生成。 |

### FR 1.1 需求结构化

| 维度 | 说明 |
| --- | --- |
| **实现原理** | `requirement_parser.parse_content_blocks`：CSV 表头、`REQ-ID` 文本、FitnessAI 默认五条；QRA 仅跑解析+风险，不生成用例。 |
| **对应** | `inputFields`、`ranges`、`conditions`、`expectedAction` 可在 QRA → Structured Requirements 审查。 |
| **验证流程** | QRA 后检查 `requirementsStructured`；`engineMetadata.parseChannel` 含 `csv` 等。 |

### FR 2.0 风险分析与优先级

| 维度 | 说明 |
| --- | --- |
| **实现原理** | `risk_config.FEATURE_WEIGHTS` → `riskScore = impact × likelihood` → H/M/L；`POST /api/qra` → ai-service `POST /qra`。 |
| **对应** | 生成前必须（建议）完成 QRA 并保存风险编辑。 |
| **验证流程** | Run QRA → Risk Items Review 改 priority → Save → 再生成；`GET /api/risk-matrix`。 |

### FR 3.0 黑盒测试设计

| 维度 | 说明 |
| --- | --- |
| **实现原理** | `generation_pipeline` 路由至 `blackbox_workers`：各 worker **优先 LLM**（结合 `customPrompt` + `techniquePrompts[技术ID]`），失败则 `blackbox_fallbacks` 确定性生成；Reduce 合并 `equivalencePartitions`、`boundaryValues`、`decisionTableRules` 等工件。 |
| **对应** | 5 种技术 ≥ 作业 3 种要求；前端可一次只选 EP 验证单技术输出。 |
| **验证流程** | 勾选 EP/BVA/DecisionTable 等分别 Generate；Metrics 中 `designMethod` 覆盖；`analyzeBlackBoxQuality`（纯白盒 run 时跳过黑盒评分）。 |

### FR 4.0 白盒测试建模（加分）

| 维度 | 说明 |
| --- | --- |
| **实现原理（WhiteBoxJava）** | `whitebox_java_analyzer` 解析 Java → 方法级 CFG；`whitebox_coverage` 生成 statement/branch coverage items；`whitebox_sequence_generator` 路径搜索 → `testSequences`（`path`、`pathConstraints`、`inputHints`、`setupHints` 等）；`whitebox_llm_enhancer` **仅**增强自然语言字段，**禁止**改 CFG/coverageItems/path。 |
| **实现原理（StateTransition）** | 黑盒技术 `StateTransition`：`state_transition_worker` + `state_model_engine`（深蹲默认或 `whiteboxDescription` 自定义箭头/JSON）。 |
| **对应** | FR 4.0 双路径：Java 代码级白盒 + 业务状态机黑盒技术。 |
| **验证流程** | 白盒 Tab：`coverageCriterion`=`statement+branch` → Generate → 检查 `artifacts.coverageItems`、`testSequences`、`llmEnhancedTestcases`；Summary → LLM Enhancements。 |

### FR 5.0 测试预言（加分）

| 维度 | 说明 |
| --- | --- |
| **实现原理** | `selectedTechniques` 含 `Oracle` 时，`oracle_engine` 对已聚合用例附加 `oracle`；白盒序列另含 `oracleHints`。 |
| **验证流程** | 黑盒生成时勾选 Oracle worker 或 `includeOracle: true`；导出 CSV 检查 oracle 列。 |

### FR 6.0 输出与导出

| 维度 | 说明 |
| --- | --- |
| **实现原理** | Markdown 含 **LLM enhanced white-box design**；JSON/CSV；xlsx 四表。 |
| **验证流程** | 白盒生成后导出 Markdown，确认增强段存在（或 `promptPreview` 警告）。 |

### FR 7.0 测试套件优化（加分）

| 维度 | 说明 |
| --- | --- |
| **实现原理** | `Optimization` worker：`risk-first` / `minimize` → `testSuiteOptimization`。 |
| **验证流程** | `selectedTechniques` 含 `Optimization` 或请求 `includeOptimization: true`。 |

### 交互式审查（作业「主要内容」必做）

| 维度 | 说明 |
| --- | --- |
| **实现原理** | QRA：需求草稿删除确认、风险表编辑与 Save；Summary：Coverage / Strategies / Cases / LLM Enhancements / Traceability 分 Tab 保存；白盒：coverage item 勾选、`manualCoverageItems`。 |
| **验证流程** | 场景 E（§6.4）：QRA 改风险 + Summary 改用例 → 再导出对比。 |

---

## 3. 核心实现说明

### 3.1 生成管线 `generation_pipeline.py`

| 阶段 | 行为 |
| --- | --- |
| **GlobalContext** | 不可变快照：`requirements_structured`、`risk_items`、`coverage_criterion`、`whitebox_description`、`optimization_mode`、`technique_prompts`、`reviewer_overrides` |
| **Route** | `selectedTechniques` → Worker 映射；`asyncio.gather` 并发 |
| **Reduce** | 合并 `testcases`、`coverageItems`、`testStrategies`、`traceability`、`testSequences`、`llmEnhancedTestcases`、`engineMetadata`、`warnings` |

**Worker 映射（节选）：**

| Technique ID | 模块 |
| --- | --- |
| EP / BVA / DecisionTable / Combinatorial | `blackbox_workers` |
| StateTransition | `state_transition_worker`（降级 `state_model_engine`） |
| WhiteBoxJava | `whitebox_java_worker` |
| Oracle | `oracle_engine`（后处理） |
| Optimization | `suite_optimizer`（后处理） |

### 3.2 引擎目录 `ai-service/app/engines/`

| 文件 | 功能 |
| --- | --- |
| `generation_pipeline.py` | 主入口、`PIPELINE_VERSION` |
| `blackbox_workers.py` | 黑盒 LLM worker |
| `blackbox_fallbacks.py` | 确定性黑盒 fallback（原 blackbox_engine 职责） |
| `state_transition_worker.py` | StateTransition LLM worker |
| `state_model_engine.py` | 状态模型 fallback（原 whitebox_engine 职责） |
| `whitebox_java_analyzer.py` | Java AST/CFG |
| `whitebox_coverage.py` | statement / branch coverage items |
| `whitebox_sequence_generator.py` | CFG 路径 → test sequences |
| `input_hint_generator.py` | 条件 → input/setup hints |
| `whitebox_java_worker.py` | `worker_whitebox_java` 对外入口 |
| `whitebox_llm_enhancer.py` | LLM 自然语言增强（有边界） |
| `requirement_parser.py` / `risk_engine.py` | FR 1.x / 2.0 |
| `oracle_engine.py` / `suite_optimizer.py` | Oracle / Optimization |
| `strategy_builder.py` / `schema_validator.py` | 策略、LLM JSON 校验 |
| `risk_config.py` | 风险矩阵权重 |

`engines/__init__.py` **仅**导出 `run_generation_pipeline`。

### 3.3 AI 服务 `main.py`

- `ENGINE_VERSION = autotestdesign-engine-v3`
- `PIPELINE_VERSION`（来自 `generation_pipeline`）写入 `engineMetadata.pipelineVersion`
- `POST /qra`：确定性解析 + `score_requirements`，**不**调用用例 Worker
- `POST /generate-testcases`：`_normalize_selected_techniques`；默认激活除 `WhiteBoxJava` 外的黑盒+后处理（见 `_normalize_selected_techniques` 逻辑）
- `reviewerOverrides`：`coverageItemSelection`、`manualCoverageItems`（白盒）
- 无 `OPENAI_API_KEY`：黑盒 fallback、白盒 CFG 仍可用；`llmEnhancedTestcases` 可为 `promptPreview`

### 3.4 后端 API

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/health` | 含 `targetApplication: FitnessAI` |
| GET | `/api/target-application` | 目标应用元数据 |
| GET | `/api/engines/info` | FR 模块映射（含 generation_pipeline、WhiteBoxJava） |
| POST | `/api/qra` | **QRA**：结构化需求 + 风险 |
| POST | `/api/testcases/generate` | 生成；`selectedTechniques`、`techniquePrompts`、`reviewerOverrides` |
| GET | `/api/history` | 历史列表 |
| DELETE | `/api/history/:id` | 删除记录 |
| GET | `/api/export` | 历史元数据 JSON/CSV |
| POST | `/api/export/artifacts` | JSON/CSV/xlsx |
| GET | `/api/risk-matrix` | 风险矩阵 |
| GET | `/api/analysis/experiment` | 实验统计 |

ai-service：`POST /qra`、`POST /generate-testcases`、`POST /export-artifacts`。

### 3.5 前端能力摘要（`App.vue`）

| 主 Tab | 能力 |
| --- | --- |
| **QRA** | Run QRA；Structured Requirements / Risk Items Review；保存风险后再生成 |
| **Black-Box** | 5 技术勾选；每技术 Technical Prompt；按技术 Generate；`techniqueResults` 缓存 |
| **White-Box** | `WhiteBoxJava`；`statement+branch`；Java 粘贴/文件；coverage 勾选；manual item；LLM Enhanced 区 |
| **Summary** | Coverage / Strategies / Cases / **LLM Enhancements** / Traceability |

其他：填入示例（含 `FITNESS_TECHNIQUE_PROMPT_SAMPLES`）、四种导出、History 回填 QRA+生成结果、Assignment 检查清单。

### 3.6 数据库

`generation_records` 保存：用例、结构化需求、覆盖项、风险、`testSequences`、`llmEnhancedTestcases`、`engine_metadata`、`pipelineVersion` 等。

---

## 4. FitnessAI 测试范围（目标应用）

与 `FitnessAI_LLM_CONTEXT.md`、`FitnessAI_PROMPT_EXAMPLES.md` 一致：

| 模块 | 测试要点 | 建议设计技术 | Technical Prompt 要点 |
| --- | --- | --- | --- |
| 姿态分析 `/api/analytics/pose` | exerciseType；landmarks 32/33/34 | EP、BVA | 等价类域；边界簇 |
| 状态机计数 | UP→…→UP；非法短循环 | StateTransition | 五状态 + count 变化 |
| 记录过滤 | count&lt;3 ∧ duration&lt;30 | DecisionTable | 四规则 saved/not saved |
| 训练计划 | 难度、skipRest | Combinatorial | pairwise 因子 |
| 仪表盘 | MET×体重×时长 | EP、oracle | 卡路里计算缺陷 |
| Java 服务类（若提供源码） | 分支/语句覆盖 | WhiteBoxJava | 粘贴被测类；不依赖 FitnessAI 全仓库 |

**Overall Input Prompt**（侧栏）应描述项目范围与追溯要求，**不要**把 AutoTestDesign 工具能力写成 FitnessAI 需求。全文见 [FitnessAI_PROMPT_EXAMPLES.md](FitnessAI_PROMPT_EXAMPLES.md)。

---

## 5. 环境与启动

### 5.1 先决条件

Docker Desktop、Node.js 20+、npm 10+；可选 Python 3.11+（`ai-service/tests`）。

### 5.2 配置与启动

```powershell
# 项目根目录
# 配置 .env（POSTGRES_*、OPENAI_* 等）
docker compose up -d --build
docker compose ps

cd frontend
npm install
npm run dev
```

访问：`http://localhost:5173`、`http://localhost:3000/health`、`http://localhost:8000/health`。

### 5.3 冒烟检查

```powershell
Invoke-RestMethod http://localhost:3000/health
Invoke-RestMethod http://localhost:8000/health
Invoke-RestMethod http://localhost:3000/api/engines/info
```

---

## 6. 完整工具测试流程

### 6.1 测试模式

| 模式 | 条件 | 表现 |
| --- | --- | --- |
| 离线 / 确定性为主 | `OPENAI_API_KEY` 为空 | 黑盒 fallback；白盒 CFG + sequences；LLM Enhancements 为 preview |
| 在线 | Key + Base URL + Model | 黑盒更贴 prompt；`llmEnhancedTestcases` 含自然语言设计 |

### 6.2 场景 A — 端到端回归（发版必做，约 8 分钟）

1. 打开 `http://localhost:5173`，**填入示例**。
2. **Run QRA** → 检查 Structured Requirements 与 Risk Items → **Save** 风险（若有修改）。
3. Black-Box Tab：仅选 **EP** → 填写 EP Technical Prompt（或示例）→ **Generate** → 用例 `designMethod=EP`。
4. 再选 **BVA**、**DecisionTable** 各生成一次（或一次多选）。
5. White-Box Tab：粘贴 §6.5 `LoginService` Java → `statement+branch` → **Generate White-Box** → 检查 `coverageItems`、`testSequences`。
6. Summary Tab：浏览 **LLM Enhancements**；导出 Markdown / JSON / CSV / xlsx。
7. History → View 回填。

验证点：`engineMetadata.engineVersion` 或 `pipelineVersion`；`timingMetrics`；白盒 `testcases[*].technique=white-box`。

### 6.3 场景 B / C / D — 分输入通道

| 场景 | 操作 | 验证点 |
| --- | --- | --- |
| **B 纯文本** | 仅 Plain-text → QRA → 生成 EP | `requirementsStructured` 多 feature |
| **C CSV** | 仅 CSV（§6.5）→ QRA → 生成 | REQ-ID 一致；`parseChannel` 含 csv |
| **D 文件** | 导入 `FitnessAI_LLM_CONTEXT.md` → QRA | documents 并入解析 |

### 6.4 场景 E — 交互式审查留证（作业重点）

1. QRA 后导出 JSON（或截图风险表）。
2. 修改 `riskItems[].priority` → Save QRA risks。
3. 生成 EP 后，在 Summary 修改一条用例 → Save Cases。
4. 再导出 JSON/Markdown，附 `riskScore = impact × likelihood` 示例。

### 6.5 测试数据样例

**纯文本：**

```text
FitnessAI 通过 MediaPipe 提供 33 个关键点，支持 SQUAT/PUSHUP/PLANK/JUMPING_JACK。
深蹲需完整状态循环才计数；count<3 且 durationSeconds<30 的记录应过滤；仪表盘展示趋势与卡路里。
```

**CSV：**

```csv
id,feature,input,condition,expected
REQ-POSE-001,姿态分析,exerciseType+landmarks,合法类型且33点,返回count/score/feedback
REQ-POSE-002,状态机计数,帧序列,完整UP-DOWN循环,count+1
REQ-REC-001,记录过滤,count+durationSeconds,count<3且duration<30,不入库
```

**白盒 Java（冒烟）：**

```java
public class LoginService {
    public String login(String username, String password) {
        if (username == null || password == null) {
            return "missing";
        }
        if (username.equals("admin") && password.equals("123456")) {
            return "success";
        }
        return "invalid";
    }
}
```

**Overall Input Prompt：** 见 [FitnessAI_PROMPT_EXAMPLES.md](FitnessAI_PROMPT_EXAMPLES.md) §Overall Input Prompt。

### 6.6 API 验收（PowerShell）

**QRA：**

```powershell
$qra = @{
  sourceType = "requirements"
  content = "[CSV requirements]`nid,feature,input,condition,expected`nREQ-POSE-002,状态机计数,frames,full cycle,count+1"
} | ConvertTo-Json -Depth 4

Invoke-RestMethod -Uri http://localhost:3000/api/qra -Method Post -ContentType "application/json; charset=utf-8" -Body ([System.Text.Encoding]::UTF8.GetBytes($qra))
```

**黑盒生成（带 QRA 结果）：**

```powershell
$body = @{
  sourceType = "requirements"
  content = "[CSV requirements]`nid,feature,input,condition,expected`nREQ-POSE-002,状态机计数,frames,full cycle,count+1"
  selectedTechniques = @("EP", "BVA")
  techniquePrompts = @{ EP = "Use Equivalence Partitioning for FitnessAI..." }
  requirementsStructured = @()
  riskItems = @()
} | ConvertTo-Json -Depth 6

$res = Invoke-RestMethod -Uri http://localhost:3000/api/testcases/generate -Method Post -ContentType "application/json; charset=utf-8" -Body ([System.Text.Encoding]::UTF8.GetBytes($body))
$res.quality.caseCount
$res.engineMetadata
```

**白盒：**

```powershell
$wb = @{
  sourceType = "codebase"
  content = "public class LoginService { public String login(String u, String p) { if (u == null) return \"missing\"; return \"ok\"; } }"
  selectedTechniques = @("WhiteBoxJava")
  coverageCriterion = "statement+branch"
} | ConvertTo-Json -Depth 4

Invoke-RestMethod -Uri http://localhost:3000/api/testcases/generate -Method Post -ContentType "application/json; charset=utf-8" -Body ([System.Text.Encoding]::UTF8.GetBytes($wb))
```

### 6.7 静态检查

```powershell
cd frontend; npm run build
cd ..\backend; node --check src/index.js
cd ..\ai-service; python -m unittest discover -s tests
```

---

## 7. 15 分钟演示建议

| 时间 | 内容 |
| --- | --- |
| 0–1 min | 工具 vs FitnessAI 边界；Prompt 分工（Overall vs Technical） |
| 1–3 min | 填入示例 → **Run QRA** → 展示风险表 |
| 3–6 min | Black-Box：EP + BVA 分技术生成；展示 `techniquePrompts` |
| 6–9 min | White-Box：`WhiteBoxJava` + coverage items + **LLM Enhancements**（说明 LLM 不改 path） |
| 9–12 min | Summary 追溯 + 四种导出 |
| 12–15 min | PDF 交付物计划 + Q&A |

---

## 8. 提交前检查清单

### 8.1 工具（20%）

- [ ] `docker compose up -d --build` 成功
- [ ] 场景 A（QRA → 黑盒 → 白盒 → Summary）+ 场景 E 留证
- [ ] 四种导出可用；Markdown 含白盒增强段（或说明 offline preview）
- [ ] README + DEVELOPMENT_LOG + 演示视频

### 8.2 文档（80%，FitnessAI）

- [ ] 风险分析报告 PDF（来自 QRA `riskItems`）
- [ ] 测试计划 PDF
- [ ] 详细测试设计与执行 PDF（可结合 `fitnessai-java-tests/`）
- [ ] 演示 PPT

### 8.3 FR 与交互

- [ ] FR 1.0–3.0、6.0 + QRA + Interactive Review 可演示
- [ ] FR 4.0：`WhiteBoxJava` +（可选）`StateTransition`
- [ ] FR 5/7：`Oracle` / `Optimization` worker
- [ ] 能说明白盒 **LLM 边界**（不改 CFG/coverageItems/path）

---

## 9. 后续优化建议（按优先级）

### 已完成（generation-pipeline / engine v3）

- `generation_pipeline` Map-Reduce + 可独立 `selectedTechniques`
- 黑盒 `blackbox_workers` + `blackbox_fallbacks` 降级
- **`WhiteBoxJava`**：CFG、coverage items、test sequences、`whitebox_llm_enhancer`
- 前端四主 Tab + QRA API + Summary **LLM Enhancements**
- `StateTransition` 独立 worker + `state_model_engine`
- 扩展 `tests/test_engines.py`（含白盒 Java、pipeline、LLM enhancer 等）

### P0 — 提交前建议完成（文档/演示）

1. **补齐三份 PDF + PPT + 演示视频**（占分 80%）。
2. **场景 E 审查前后对比**（§6.4）。
3. 演示脚本对齐 [FitnessAI_PROMPT_EXAMPLES.md](FitnessAI_PROMPT_EXAMPLES.md)。

### P2 — 目标应用验证（交付物 4）

| 项 | 说明 |
| --- | --- |
| `fitnessai-java-tests/` | 已有 Java 测试骨架；与工具导出 `TC-*` / 白盒 sequence 对齐 |
| PyTest（Python API） | 针对 `/api/analytics/pose` 的脚本与工具 traceability 映射 |
| 成本估算 | 纯手工 vs AutoTestDesign + QRA/审查人时 |

### P3 — 算法与泛化

| 项 | 状态 | 说明 |
| --- | --- | --- |
| Java CFG 完整语义 | 部分 | 已支持 if/switch/循环/try；复杂泛型/反射待加强 |
| 自定义状态图 | 已完成 | `whiteboxDescription` + StateTransition worker |
| 风险矩阵 API | 已完成 | `GET /api/risk-matrix` |
| LLM JSON 校验 | 已完成 | `schema_validator`（含 `WhiteBoxJava`） |
| `/api/engines/info` 版本号 | 待统一 | 与 `main.py` ENGINE_VERSION v3 对齐 |
| 生产 CORS / 限流 | 待做 | — |

### P4 — 工程化

| 项 | 状态 | 说明 |
| --- | --- | --- |
| 单元测试 | 已扩展 | `python -m unittest discover -s tests` |
| CI | 待做 | build + unittest + 冒烟 |
| E2E | 待做 | Playwright：QRA → EP Generate → xlsx |

---

## 10. 版本变更记录

| 版本 | 日期 | 摘要 |
| --- | --- | --- |
| v4 | 早期 | FitnessAI Prompt、符合性面板、交互审查 |
| v5 + engine v1 | 2026-05-20 | `engines/` 确定性 FR；Excel；`engineMetadata` |
| v6 + engine v2 | 2026-05-20 | xlsx、计时 NFR、testStrategies、Pairwise、状态图、schema 校验 |
| **v7 + pipeline v1 + engine v3** | **2026-05-28** | **`generation_pipeline` 主流程**；黑盒/白盒 Worker 拆分；**`WhiteBoxJava`**；**`POST /api/qra`**；前端四 Tab；`blackbox_fallbacks` / `state_model_engine` 改名；移除 `pipeline.py` 主入口；LLM 白盒增强层与边界；README / DEVELOPMENT_LOG 同步 |

---

## 附录：相关文档

| 文档 | 用途 |
| --- | --- |
| [README.md](README.md) | 快速上手、API、Prompt 示例摘要 |
| [GENERATION_PIPELINE_REFACTOR_SUMMARY.md](GENERATION_PIPELINE_REFACTOR_SUMMARY.md) | 重构设计与验证 |
| [FitnessAI_PROMPT_EXAMPLES.md](FitnessAI_PROMPT_EXAMPLES.md) | Overall + Technical Prompt 全文 |
| [FitnessAI_LLM_CONTEXT.md](FitnessAI_LLM_CONTEXT.md) | 目标应用需求与接口 |
| [Assignment2.md](Assignment2.md) | 作业原文与评分 |
