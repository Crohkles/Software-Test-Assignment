# AutoTestDesign — AI-Driven Test Design Tool

Full-stack AutoTestDesign implementation for **Software Testing Assignment 2**: requirement parsing, risk analysis (QRA), black-box/white-box test design, test oracles, suite optimization, and multi-format export under **generation_pipeline** (Map-Reduce / multi-Worker) with optional LLM enhancement, validated against the target application **FitnessAI** (intelligent fitness assistant system).

| Metadata | Value |
| --- | --- |
| Generation Pipeline | `generation-pipeline-v1` (`run_generation_pipeline`) |
| Rule Engine Version | `autotestdesign-engine-v3` (generation response `engineMetadata`) |
| Prompt Version | `autotestdesign-v6-fr-complete` |
| Target Application | FitnessAI |

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [System Architecture and Generation Pipeline](#2-system-architecture-and-generation-pipeline)
3. [Features and Assignment 2 Compliance](#3-features-and-assignment-2-compliance)
4. [Repository Structure](#4-repository-structure)
5. [Environment Setup](#5-environment-setup)
6. [Quick Start](#6-quick-start)
7. [User Guide (QRA → Black-Box → White-Box → Summary)](#7-user-guide-qra--black-box--white-box--summary)
8. [FitnessAI Prompt Examples](#8-fitnessai-prompt-examples)
9. [API Reference](#9-api-reference)
10. [Engine and Worker Modules](#10-engine-and-worker-modules)
11. [Export and History](#11-export-and-history)
12. [Testing and Verification](#12-testing-and-verification)
14. [Related Documentation](#14-related-documentation)

---

## 1. Project Overview

### 1.1 What It Does

This repository implements **the AutoTestDesign tool itself** (the system under test is not this tool). The recommended workflow aligns with ISTQB / ISO/IEC/IEEE 29119-4 thinking:

```
Multi-source requirement input → QRA (structured requirements + risk scoring) → Independent black-box/white-box case generation by technique
    → Result aggregation and interactive review → Test oracle / suite optimization (optional) → JSON / CSV / Markdown / Excel export
```

Generation results can be persisted to PostgreSQL, supporting history review, experiment metrics, and demo presentations.

### 1.2 What to Test (Target Application)

**FitnessAI** is the fixed target application. Core scope includes:

| Module | Testing Focus |
| --- | --- |
| Pose analysis `POST /api/analytics/pose` | `exerciseType` equivalence classes; MediaPipe `landmarks` 32/33/34 boundaries |
| State machine counting | UP → DESCENDING → DOWN → ASCENDING → UP full cycle; illegal short cycles do not count |
| Workout record filtering | Records with `count < 3` AND `durationSeconds < 30` are not persisted |
| Training plans | Difficulty, sets, rest, `skipRest` combinations |
| Dashboard | Trends, distribution, calories (MET × weight × duration) |

Import Markdown or click **Load Sample** in the frontend for a quick demo.

### 1.3 Design Principles

- **Workers independently schedulable**: Black-box 5 techniques, white-box `WhiteBoxJava`, Oracle, Optimization activated on demand via `selectedTechniques`.
- **Determinism first**: When black-box LLM is unavailable, fallback to `blackbox_fallbacks.py`; white-box CFG, coverage items, and paths are produced deterministically by the Java analyzer.
- **White-box LLM boundary**: `whitebox_llm_enhancer.py` only enhances natural-language titles, input/precondition/oracle suggestions, and review questions; it **must not** modify CFG, `coverageItems`, `coverageTargets`, or `path`.
- **Reviewable**: QRA risks, coverage items, strategies, cases, white-box coverage checkboxes, and manual items can all be edited then saved/applied.
- **Traceable**: `engineMetadata`, `timingMetrics`, `pipelineVersion` are saved with responses and history records.

---

## 2. System Architecture and Generation Pipeline

```
┌──────────────────────────────────────────────────────────────────────────┐
│                         Browser  http://localhost:5173                     │
│   Vue 3: QRA / Black-Box Technique Tab / White-Box Tab / Generated Results Summary │
└─────────────────────────────────┬────────────────────────────────────────┘
                                  │ HTTP
                                  ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                    Express Backend  :3000                                 │
│   POST /api/qra · POST /api/testcases/generate · Compliance · Persist · Export    │
└───────────────┬──────────────────────────────────────┬───────────────────┘
                │                                      │
                ▼                                      ▼
┌───────────────────────────────┐      ┌───────────────────────────────────┐
│  FastAPI ai-service  :8000    │      │  PostgreSQL  :5432                 │
│  generation_pipeline          │      │  generation_records (JSONB)       │
│  blackbox_workers /           │      └───────────────────────────────────┘
│  whitebox_java_worker         │
│  LLM (OpenAI-compatible, optional)      │
└───────────────────────────────┘
```

### 2.1 Main Generation Flow (Current)

1. `frontend/src/App.vue` → `POST /api/testcases/generate`
2. `backend/src/index.js` → `ai-service` `POST /generate-testcases`
3. `ai-service/app/main.py` constructs `GlobalContext` (including QRA-confirmed `requirementsStructured`, `riskItems`)
4. `ai-service/app/engines/generation_pipeline.py` routes to Workers by `selectedTechniques`, merges artifacts in Reduce phase
5. Returns `testcases`, `artifacts` (including `coverageItems`, `testSequences`, `llmEnhancedTestcases`, etc.), `engineMetadata`, `timingMetrics`

### 2.2 Supported `selectedTechniques`

| ID | Type | Description |
| --- | --- | --- |
| `EP` | Black-box | Equivalence Partitioning |
| `BVA` | Black-box | Boundary Value Analysis |
| `DecisionTable` | Black-box | Decision Table |
| `Combinatorial` | Black-box | Pairwise combinatorial |
| `StateTransition` | Black-box | State transition (LLM + `state_model_engine` fallback) |
| `WhiteBoxJava` | White-box | Java method-level CFG, statement/branch coverage items and test sequences |
| `Oracle` | Post-processing | Attach oracle to test cases |
| `Optimization` | Post-processing | `risk-first` / `minimize` suite optimization |

### 2.3 Operating Modes

| Mode | Condition | Behavior |
| --- | --- | --- |
| Offline / deterministic | `OPENAI_API_KEY` empty or LLM failure | Black-box fallback, white-box CFG and sequences still available; white-box enhancement area shows `promptPreview` |
| Online enhancement | Configure `OPENAI_API_KEY`, `OPENAI_BASE_URL`, `OPENAI_MODEL` | Black-box cases closer to prompt; white-box outputs `llmEnhancedTestcases` natural-language design notes |

---

## 3. Features and Assignment 2 Compliance

### 3.1 Functional Requirements (FR)

| FR | Description | Implementation Status | Primary Implementation |
| --- | --- | --- | --- |
| **FR 1.0** | CSV / plain text / file import | ✅ | Frontend multi-source input; `requirement_parser` |
| **FR 1.1** | Requirement structuring | ✅ | QRA → `requirementsStructured` |
| **FR 2.0** | Risk score and H/M/L priority | ✅ | `riskScore = impact × likelihood`; `POST /api/qra` |
| **FR 3.0** | ≥3 black-box techniques | ✅ | EP, BVA, DecisionTable, Combinatorial, StateTransition (5 techniques, independently generatable) |
| **FR 4.0** | White-box coverage and sequences | ✅ Bonus | `WhiteBoxJava` (CFG); `StateTransition` still uses state model |
| **FR 5.0** | Test oracle | ✅ Bonus | `Oracle` worker; test case `oracle` field |
| **FR 6.0** | JSON / CSV / Excel export | ✅ | `.xlsx` four sheets; Markdown (including LLM white-box enhancement section) |
| **FR 7.0** | Suite optimization | ✅ Bonus | `Optimization` worker; `testSuiteOptimization` |
| **Interactive review** | Edit and apply changes | ✅ | QRA risks, summary coverage/strategy/cases/traceability saved per tab |

### 3.2 Non-Functional Requirements (NFR)

| NFR | Description |
| --- | --- |
| Performance | Pipeline records `engineMs`, `engineMeetsNfr`, `totalMs` |
| Usability | Four main-tab workflow, FitnessAI sample, per-technique generation |
| Security | API Key via environment variables only; tighten CORS in production |
| Maintainability | Workers in separate files; `generation_pipeline` single orchestration entry; Docker Compose deployment |

---

## 4. Repository Structure

```
Software-Test-Assignment/
├── frontend/
│   └── src/App.vue              # QRA / Black-box / White-box / Results Summary tabs
├── backend/
│   └── src/
│       ├── index.js             # /api/qra, /api/testcases/generate, export, history
│       └── db.js
├── ai-service/
│   ├── app/
│   │   ├── main.py              # FastAPI: QRA, generation, export
│   │   ├── export_xlsx.py
│   │   └── engines/
│   │       ├── generation_pipeline.py   # Main entry run_generation_pipeline
│   │       ├── blackbox_workers.py      # EP/BVA/DecisionTable/Combinatorial
│   │       ├── blackbox_fallbacks.py    # Black-box LLM fallback
│   │       ├── state_transition_worker.py
│   │       ├── state_model_engine.py    # StateTransition deterministic state model
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
├── fitnessai-java-tests/        # FitnessAI-related Java test samples
├── infra/postgres/init.sql
├── docker-compose.yml
├── GENERATION_PIPELINE_REFACTOR_SUMMARY.md
├── FitnessAI_PROMPT_EXAMPLES.md
├── FitnessAI_LLM_CONTEXT.md
├── Assignment2.md
├── DEVELOPMENT_LOG.md
└── README.md
```

> Frontend runs locally by default via **npm run dev**, not packaged in Docker; `postgres`, `ai-service`, `backend` are built by Compose.

---

## 5. Environment Setup

### 5.1 Prerequisites

- **Windows / macOS / Linux**
- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- **Node.js 20+** and **npm 10+** (frontend)
- (Optional) Python 3.11+ (run `ai-service/tests` locally)

### 5.2 Environment Variables

Create `.env` in the project root (refer to variables used by `postgres` / `ai-service` in Compose):

| Variable | Description |
| --- | --- |
| `POSTGRES_*` | Database connection |
| `BACKEND_PORT` | Default `3000` |
| `AI_SERVICE_PORT` | Default `8000` |
| `OPENAI_API_KEY` | Leave empty for black-box/white-box enhancement fallback or preview |
| `OPENAI_MODEL` | e.g. `deepseek-chat`, `gpt-4o-mini` |
| `OPENAI_BASE_URL` | OpenAI-compatible gateway |
| `TEST_TECHNIQUE` | Default `black-box` |

---

## 6. Quick Start

### 6.1 Start Backend Stack (Docker)

```powershell
cd Software-Test-Assignment
docker compose up -d --build
docker compose ps
```

Expected containers **Up** (`ai-service` **healthy**):

| Container Name | Port |
| --- | --- |
| `aitest-postgres` | 5432 |
| `aitest-ai-service` | 8000 |
| `aitest-backend` | 3000 |

Health checks:

```powershell
Invoke-RestMethod http://localhost:3000/health
Invoke-RestMethod http://localhost:8000/health
Invoke-RestMethod http://localhost:3000/api/engines/info
```

### 6.2 Start Frontend (Local)

```powershell
cd frontend
npm install
npm run dev
```

Open in browser: **http://localhost:5173**

### 6.3 After Code Changes

| Change Location | Action |
| --- | --- |
| `backend/` or `ai-service/` | `docker compose up -d --build` |
| `frontend/` | Restart `npm run dev`, browser **Ctrl+F5** |

### 6.4 Stop

```powershell
docker compose down          # Keep data volumes
docker compose down -v       # Clear database (use with caution)
```

---

## 7. User Guide (QRA → Black-Box → White-Box → Summary)

### 7.1 Recommended Demo Flow

1. Open frontend, click **Load Sample** to load FitnessAI requirements and global Prompt draft.
2. In **Input and Generate** sidebar, fill/confirm **Overall Input Prompt** (project scope, risk focus, output style).
3. Main tab **QRA** → **Run QRA**, review **Structured Requirements** and **Risk Items** in sub-tabs, save risk edits.
4. Main tab **Black-Box Technique Test Design**:
   - Check techniques to run (EP, BVA, etc.);
   - Fill **Technical Prompt** for each technique (see [§8](#8-fitnessai-prompt-examples));
   - **Generate** per technique (one at a time for easier review).
5. Main tab **White-Box Technique Test Design**:
   - Select coverage criterion (e.g. `statement+branch`);
   - Paste Java snippet or upload `.java` file;
   - Check coverage items, add manual coverage items if needed;
   - **Generate White-Box** → view Java Analysis Result and **LLM Enhanced Test Design**.
6. Main tab **Generated Results Summary**: browse **Coverage Items / Test Strategies / Test Cases / LLM Enhancements / Traceability**, save edits per section.
7. Export **Markdown / JSON / CSV / Excel**; **History** can restore records.

### 7.2 Input Methods

| Method | Action |
| --- | --- |
| Plain text | Manual requirements text box |
| CSV | CSV requirements (with `id,feature,input,condition,expected` header) |
| File | Import `.md` / `.txt` etc. into `documents[]` |
| Sample | **Load Sample** |
| White-box | Java manual paste or file upload (`sourceType: codebase`) |

### 7.3 Prompt Responsibilities

- **Overall Input Prompt** (sidebar): Describe FitnessAI project, test scope, risks, and expected output style (case titles, inputs, oracle, priority, traceability).
- **Technical Prompt** (each black-box technique): Write only technique-specific constraints (equivalence domains, boundary clusters, decision table rules, factor levels, state transition paths, etc.).

---

## 8. FitnessAI Prompt Examples

The following matches in-repo sample docs; copy directly into the corresponding frontend input fields.

### 8.1 Overall Input Prompt

```text
FitnessAI is an intelligent fitness assistant with pose analysis, repetition counting, training plans, workout record filtering, and dashboard analytics.

Please generate test cases from the reviewed QRA requirements and risk items. Focus on API-level and business-flow behavior that can reveal validation errors, incorrect state counting, record filtering mistakes, invalid plan handling, and dashboard calculation defects.

Use clear test case titles, explicit input data, expected results/oracles, priority, and traceability to requirement or risk IDs. Keep the output suitable for manual review and later automation.
```

### 8.2 Black-Box Technical Prompts (Summary)

| Technique | Key Points |
| --- | --- |
| **EP** | Equivalence classes for `exerciseType`, `landmarks`, difficulty, `skipRest`, `count`, `durationSeconds`, `weightKg`, `durationHours`; one positive and one negative representative case each; link REQ ids |
| **BVA** | `landmarks.length` 32/33/34; `count` 2/3/4; `durationSeconds` 29/30/31; `weightKg` boundaries; `durationHours` 0 and small positive values |
| **Decision Table** | Record save: `count`/`durationSeconds` vs saved/not saved; plan difficulty and `skipRest` |
| **Combinatorial** | Pairwise of `exerciseType`, `difficulty`, `skipRest`, record classification, input validity factors |
| **State Transition** | UP/DESCENDING/DOWN/ASCENDING; valid full cycle, illegal short paths, duplicate frames, cooldown and count changes |

---

## 9. API Reference

### 9.1 Backend (`:3000`)

| Method | Path | Description |
| --- | --- | --- |
| GET | `/health` | Health check |
| GET | `/api/target-application` | FitnessAI metadata |
| GET | `/api/engines/info` | FR modules and pipeline capability description |
| POST | `/api/qra` | **QRA**: structured requirements + risk scoring (deterministic engine) |
| POST | `/api/testcases/generate` | **Main generation endpoint** (forwards to ai-service) |
| GET | `/api/history?limit=20` | History records |
| DELETE | `/api/history/:id` | Delete history |
| GET | `/api/risk-matrix` | Risk matrix |
| POST | `/api/export/artifacts` | JSON / CSV / xlsx |
| GET | `/api/analysis/experiment?limit=200` | Experiment statistics |

#### `POST /api/qra`

Request body: `sourceType`, `content`, `documents` (same input segment as generation endpoint).

Response: `requirementsStructured`, `riskItems`, `engineMetadata`, `timingMetrics`.

#### `POST /api/testcases/generate`

Black-box example (complete QRA first and pass reviewed structure):

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

White-box `WhiteBoxJava` example:

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

Expected (white-box):

- `testcases[*].designMethod` is `WhiteBoxJava`, `technique` is `white-box`
- `artifacts.coverageItems` includes statement / branch items
- `artifacts.testSequences` includes `path`, `pathConstraints`, `inputHints`, `setupHints`, etc.
- `artifacts.llmEnhancedTestcases` exists; with LLM configured, includes natural-language enhancement

Common response fields: `artifacts`, `testcases`, `assignmentCompliance`, `engineMetadata`, `timingMetrics`, `quality`.

### 9.2 AI Service (`:8000`)

| Method | Path | Description |
| --- | --- | --- |
| POST | `/generate-testcases` | generation_pipeline generation |
| POST | `/qra` (if called directly) | Same QRA logic |
| GET | `/api/risk-matrix` | Risk matrix |
| POST | `/export-artifacts` | xlsx binary |
| GET | `/prompt-template` | Prompt template preview |

---

## 10. Engine and Worker Modules

Path: `ai-service/app/engines/`

| Module | Responsibility |
| --- | --- |
| `generation_pipeline.py` | Main entry: `GlobalContext`, routing, Reduce, `PIPELINE_VERSION` |
| `blackbox_workers.py` | Black-box 5 techniques; LLM first, fallback on failure |
| `blackbox_fallbacks.py` | Deterministic black-box cases and coverage item helpers |
| `state_transition_worker.py` / `state_model_engine.py` | StateTransition |
| `whitebox_java_*.py` + `input_hint_generator.py` | Java CFG, coverage items, sequences, input hints |
| `whitebox_llm_enhancer.py` | Natural-language enhancement of white-box sequences (does not modify CFG/path) |
| `requirement_parser.py` / `risk_engine.py` | FR 1.x / 2.0 |
| `oracle_engine.py` / `suite_optimizer.py` | Oracle / Optimization worker |
| `strategy_builder.py` / `schema_validator.py` | Strategy mapping, LLM JSON validation |

`engines/__init__.py` exports only `run_generation_pipeline`.

Local testing:

```powershell
cd ai-service
python -m unittest discover -s tests
```

---

## 11. Export and History

| Format | Description |
| --- | --- |
| Markdown | Structured cases and **LLM enhanced white-box design** section |
| JSON | Full `artifacts` + `testcases` |
| CSV | Flattened case table |
| Excel `.xlsx` | Requirements / Risks / Strategies / TestCases four sheets |

Each successful generation writes to `generation_records`; **History → View** restores QRA and generation results to the review area.

---

## 12. Testing and Verification

```powershell
# Rebuild containers
docker compose up -d --build

# Frontend build
cd frontend && npm run build

# ai-service unit tests
cd ai-service && python -m unittest discover -s tests

# Backend syntax check
cd backend && node --check src/index.js
```

Smoke checklist:

- [ ] `http://localhost:3000/health` and `http://localhost:8000/health` OK
- [ ] Run QRA → risk items and structured requirements present
- [ ] Select `EP` only → cases have `designMethod` `EP`
- [ ] `WhiteBoxJava` generation → `testSequences` and `coverageItems` non-empty
- [ ] Summary **LLM Enhancements** viewable (or preview warning)
- [ ] xlsx four sheets openable; History View / Delete works

---

## 13. Related Documentation

| Document | Purpose |
| --- | --- |
| [FitnessAI_PROMPT_EXAMPLES.md](FitnessAI_PROMPT_EXAMPLES.md) | Full Overall + black-box Technical Prompts |

---

## License and Course Notice

This project is a course assignment implementation. Target application **FitnessAI** is used only to validate AutoTestDesign tool effectiveness. When using LLM, comply with university and API provider policies; do not commit real API Keys to public repositories.
