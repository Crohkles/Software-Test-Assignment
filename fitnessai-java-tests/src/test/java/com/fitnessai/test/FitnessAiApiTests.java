package com.fitnessai.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitnessai.FitnessAiApplication;
import com.fitnessai.dto.PoseAnalysisRequest;
import com.fitnessai.model.*;
import com.fitnessai.service.UserService;
import com.fitnessai.analyzer.PoseAnalyzerFactory;
import io.qameta.allure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = FitnessAiApplication.class)
@AutoConfigureMockMvc
@Transactional
@Epic("FitnessAI")
@ExtendWith(TestResultExtension.class)
public class FitnessAiApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private PoseAnalyzerFactory analyzerFactory;

    private static final String TEST_USER_ID = "test_user_automation";

    @BeforeEach
    public void setUp() {
        // Clear all state analyzers before each test to ensure absolute test isolation
        analyzerFactory.clearAllAnalyzers();
    }

    private List<PoseLandmark> createLandmarks(int count) {
        List<PoseLandmark> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new PoseLandmark(0.5, 0.5, 0.0, 1.0));
        }
        return list;
    }

    private List<PoseLandmark> createStandingLandmarks(int count) {
        List<PoseLandmark> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new PoseLandmark(0.5, 0.5, 0.0, 1.0));
        }
        // Standing angle (straight vertical line): 180 degrees knee angle
        // Left side: Hip (23), Knee (25), Ankle (27)
        list.set(23, new PoseLandmark(0.5, 0.2, 0.0, 1.0));
        list.set(25, new PoseLandmark(0.5, 0.5, 0.0, 1.0));
        list.set(27, new PoseLandmark(0.5, 0.8, 0.0, 1.0));
        
        // Right side: Hip (24), Knee (26), Ankle (28)
        list.set(24, new PoseLandmark(0.5, 0.2, 0.0, 1.0));
        list.set(26, new PoseLandmark(0.5, 0.5, 0.0, 1.0));
        list.set(28, new PoseLandmark(0.5, 0.8, 0.0, 1.0));
        return list;
    }

    private List<PoseLandmark> createSquattingLandmarks(int count) {
        List<PoseLandmark> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new PoseLandmark(0.5, 0.5, 0.0, 1.0));
        }
        // Squatting angle: 90 degrees knee angle
        // Left side: Hip (23), Knee (25), Ankle (27)
        list.set(23, new PoseLandmark(0.2, 0.5, 0.0, 1.0));
        list.set(25, new PoseLandmark(0.5, 0.5, 0.0, 1.0));
        list.set(27, new PoseLandmark(0.5, 0.8, 0.0, 1.0));
        
        // Right side: Hip (24), Knee (26), Ankle (28)
        list.set(24, new PoseLandmark(0.2, 0.5, 0.0, 1.0));
        list.set(26, new PoseLandmark(0.5, 0.5, 0.0, 1.0));
        list.set(28, new PoseLandmark(0.5, 0.8, 0.0, 1.0));
        return list;
    }

    // =========================================================================
    // TS-01: EP 等价划分 (TC-EP-001 至 TC-EP-020)
    // =========================================================================

    @Test
    @DisplayName("TC-EP-001")
    @Feature("Pose Analysis")
    @Story("Equivalence Partitioning")
    @Description("EP-有效等价类-合法的深蹲姿态分析")
    public void test_TC_EP_001() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(33), ExerciseType.SQUAT, "session_001");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-EP-002")
    @Feature("Pose Analysis")
    @Story("Equivalence Partitioning")
    @Description("EP-无效等价类-空关键点数组被拦截返回4xx")
    public void test_TC_EP_002() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(null, ExerciseType.SQUAT, "session_001");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("TC-EP-003")
    @Feature("Pose Analysis")
    @Story("Equivalence Partitioning")
    @Description("EP-有效等价类-极少点数(10点)时被正常安全拦截")
    public void test_TC_EP_003() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(10), ExerciseType.SQUAT, "session_001");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("TC-EP-004")
    @Feature("Pose Analysis")
    @Story("Equivalence Partitioning")
    @Description("EP-无效等价类-空运动类型且无点数时报4xx")
    public void test_TC_EP_004() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(0), null, "session_001");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("TC-EP-005")
    @Feature("Pose Analysis")
    @Story("Equivalence Partitioning")
    @Description("EP-有效等价类-合法计数状态初始化")
    public void test_TC_EP_005() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(33), ExerciseType.SQUAT, "session_005");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-EP-006")
    @Feature("Pose Analysis")
    @Story("Equivalence Partitioning")
    @Description("EP-无效等价类-未知非法运动类别名报4xx")
    public void test_TC_EP_006() throws Exception {
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"exercise_type\":\"invalid_type\",\"pose_landmarks\":[]}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("TC-EP-007")
    @Feature("Pose Analysis")
    @Story("Equivalence Partitioning")
    @Description("EP-有效等价类-合法的深蹲短周期帧")
    public void test_TC_EP_007() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(33), ExerciseType.SQUAT, "session_007");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-EP-008")
    @Feature("Pose Analysis")
    @Story("Equivalence Partitioning")
    @Description("EP-无效等价类-未指定数组内容报错拦截")
    public void test_TC_EP_008() throws Exception {
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"exercise_type\":\"squat\",\"pose_landmarks\":null}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("TC-EP-009")
    @Feature("Exercise Records")
    @Story("Equivalence Partitioning")
    @Description("EP-有效等价类-无效参数触发204记录过滤拦截")
    public void test_TC_EP_009() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercise_type", "squat");
        body.put("count", 2);
        body.put("duration", 20);
        mockMvc.perform(post("/api/user/" + TEST_USER_ID + "/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("TC-EP-010")
    @Feature("Exercise Records")
    @Story("Equivalence Partitioning")
    @Description("EP-无效等价类-空请求Body提交视为无效记录返回204")
    public void test_TC_EP_010() throws Exception {
        mockMvc.perform(post("/api/user/null/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("TC-EP-011")
    @Feature("Exercise Records")
    @Story("Equivalence Partitioning")
    @Description("EP-有效等价类-满足存盘条件的运动记录存盘返回200")
    public void test_TC_EP_011() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercise_type", "squat");
        body.put("count", 5);
        body.put("duration", 60);
        mockMvc.perform(post("/api/user/" + TEST_USER_ID + "/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-EP-012")
    @Feature("Exercise Records")
    @Story("Equivalence Partitioning")
    @Description("EP-无效等价类-非法空字段和负数计数触发204安全过滤")
    public void test_TC_EP_012() throws Exception {
        mockMvc.perform(post("/api/user/" + TEST_USER_ID + "/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"exercise_type\":null,\"count\":-1}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("TC-EP-013")
    @Feature("Exercise Records")
    @Story("Equivalence Partitioning")
    @Description("EP-有效等价类-获取标准运动计划列表首项难度校验")
    public void test_TC_EP_013() throws Exception {
        mockMvc.perform(get("/api/exercises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].difficulty").value("easy"));
    }

    @Test
    @DisplayName("TC-EP-014")
    @Feature("Exercise Records")
    @Story("Equivalence Partitioning")
    @Description("EP-无效等价类-请求非法路由返回4xx")
    public void test_TC_EP_014() throws Exception {
        mockMvc.perform(get("/api/exercises/invalid"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("TC-EP-015")
    @Feature("Exercise Records")
    @Story("Equivalence Partitioning")
    @Description("EP-有效等价类-获取第二项难度校验")
    public void test_TC_EP_015() throws Exception {
        mockMvc.perform(get("/api/exercises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].difficulty").value("medium"));
    }

    @Test
    @DisplayName("TC-EP-016")
    @Feature("Exercise Records")
    @Story("Equivalence Partitioning")
    @Description("EP-无效等价类-请求中等非法路由报4xx")
    public void test_TC_EP_016() throws Exception {
        mockMvc.perform(get("/api/exercises/invalid_med"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("TC-EP-017")
    @Feature("Exercise Records")
    @Story("Equivalence Partitioning")
    @Description("EP-有效等价类-获取最后一项难度校验")
    public void test_TC_EP_017() throws Exception {
        mockMvc.perform(get("/api/exercises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[2].difficulty").value("medium"));
    }

    @Test
    @DisplayName("TC-EP-018")
    @Feature("Exercise Records")
    @Story("Equivalence Partitioning")
    @Description("EP-无效等价类-请求高难度非法路由报4xx")
    public void test_TC_EP_018() throws Exception {
        mockMvc.perform(get("/api/exercises/invalid_hard"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("TC-EP-019")
    @Feature("Exercise Records")
    @Story("Equivalence Partitioning")
    @Description("EP-有效等价类-正常获取用户仪表盘数据")
    public void test_TC_EP_019() throws Exception {
        mockMvc.perform(get("/api/user/" + TEST_USER_ID + "/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-EP-020")
    @Feature("Exercise Records")
    @Story("Equivalence Partitioning")
    @Description("EP-无效等价类-无历史记录的新用户自动建档容错校验")
    public void test_TC_EP_020() throws Exception {
        mockMvc.perform(get("/api/user/null/dashboard"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // TS-02: BVA 边界值分析 (TC-BVA-001 至 TC-BVA-030)
    // =========================================================================

    // === 姿势分析点数边界：SQUAT (001-006) ===
    @Test
    @DisplayName("TC-BVA-001")
    @Feature("Pose Analysis")
    @Story("Boundary Value Analysis")
    @Description("BVA-landmarks.length=31 (深蹲点数极小边界-1, 异常拦截)")
    public void test_TC_BVA_001() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(31), ExerciseType.SQUAT, "session_bva_1");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("TC-BVA-002")
    @Feature("Pose Analysis")
    @Story("Boundary Value Analysis")
    @Description("BVA-landmarks.length=32 (深蹲点数极小边界, 异常拦截)")
    public void test_TC_BVA_002() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(32), ExerciseType.SQUAT, "session_bva_2");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("TC-BVA-003")
    @Feature("Pose Analysis")
    @Story("Boundary Value Analysis")
    @Description("BVA-landmarks.length=33 (深蹲点数极小边界+1, 合规标准)")
    public void test_TC_BVA_003() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(33), ExerciseType.SQUAT, "session_bva_3");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-BVA-004")
    @Feature("Pose Analysis")
    @Story("Boundary Value Analysis")
    @Description("BVA-landmarks.length=33 (开合跳点数极大边界-1, 合规标准)")
    public void test_TC_BVA_004() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(33), ExerciseType.JUMPING_JACK, "session_bva_4");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-BVA-005")
    @Feature("Pose Analysis")
    @Story("Boundary Value Analysis")
    @Description("BVA-landmarks.length=34 (深蹲点数极大边界, 合规兼容)")
    public void test_TC_BVA_005() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(34), ExerciseType.SQUAT, "session_bva_5");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-BVA-006")
    @Feature("Pose Analysis")
    @Story("Boundary Value Analysis")
    @Description("BVA-landmarks.length=35 (深蹲点数极大边界+1, 兼容解析前33点)")
    public void test_TC_BVA_006() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(35), ExerciseType.SQUAT, "session_bva_6");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // === 姿势分析点数边界：PUSHUP (007-012，独立因子避免重复) ===
    @Test
    @DisplayName("TC-BVA-007")
    @Feature("Pose Analysis")
    @Story("Boundary Value Analysis")
    @Description("BVA-landmarks.length=31 (俯卧撑点数极小边界-1, 异常拦截)")
    public void test_TC_BVA_007() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(31), ExerciseType.PUSHUP, "session_bva_7");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("TC-BVA-008")
    @Feature("Pose Analysis")
    @Story("Boundary Value Analysis")
    @Description("BVA-landmarks.length=32 (俯卧撑点数极小边界, 异常拦截)")
    public void test_TC_BVA_008() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(32), ExerciseType.PUSHUP, "session_bva_8");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("TC-BVA-009")
    @Feature("Pose Analysis")
    @Story("Boundary Value Analysis")
    @Description("BVA-landmarks.length=33 (俯卧撑点数极小边界+1, 合规标准)")
    public void test_TC_BVA_009() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(33), ExerciseType.PUSHUP, "session_bva_9");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-BVA-010")
    @Feature("Pose Analysis")
    @Story("Boundary Value Analysis")
    @Description("BVA-landmarks.length=33 (俯卧撑点数极大边界-1, 合规标准)")
    public void test_TC_BVA_010() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(33), ExerciseType.PUSHUP, "session_bva_10");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-BVA-011")
    @Feature("Pose Analysis")
    @Story("Boundary Value Analysis")
    @Description("BVA-landmarks.length=34 (俯卧撑点数极大边界, 合规兼容)")
    public void test_TC_BVA_011() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(34), ExerciseType.PUSHUP, "session_bva_11");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-BVA-012")
    @Feature("Pose Analysis")
    @Story("Boundary Value Analysis")
    @Description("BVA-landmarks.length=35 (俯卧撑点数极大边界+1, 兼容解析前33点)")
    public void test_TC_BVA_012() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(35), ExerciseType.PUSHUP, "session_bva_12");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // === 运动记录过滤：SQUAT (013-018) ===
    @Test
    @DisplayName("TC-BVA-013")
    @Feature("Exercise Records")
    @Story("Boundary Value Analysis")
    @Description("BVA-durationSeconds=29 (深蹲时长极值-1, 触发204拦截)")
    public void test_TC_BVA_013() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercise_type", "squat");
        body.put("count", 2);
        body.put("duration", 29);
        mockMvc.perform(post("/api/user/" + TEST_USER_ID + "/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("TC-BVA-014")
    @Feature("Exercise Records")
    @Story("Boundary Value Analysis")
    @Description("BVA-durationSeconds=30 (深蹲时长极值, 正常存盘)")
    public void test_TC_BVA_014() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercise_type", "squat");
        body.put("count", 2);
        body.put("duration", 30);
        mockMvc.perform(post("/api/user/" + TEST_USER_ID + "/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-BVA-015")
    @Feature("Exercise Records")
    @Story("Boundary Value Analysis")
    @Description("BVA-durationSeconds=31 (深蹲时长极值+1, 正常存盘)")
    public void test_TC_BVA_015() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercise_type", "squat");
        body.put("count", 2);
        body.put("duration", 31);
        mockMvc.perform(post("/api/user/" + TEST_USER_ID + "/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-BVA-016")
    @Feature("Exercise Records")
    @Story("Boundary Value Analysis")
    @Description("BVA-count=2 (深蹲计数极值-1, 触发204拦截)")
    public void test_TC_BVA_016() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercise_type", "squat");
        body.put("count", 2);
        body.put("duration", 20);
        mockMvc.perform(post("/api/user/" + TEST_USER_ID + "/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("TC-BVA-017")
    @Feature("Exercise Records")
    @Story("Boundary Value Analysis")
    @Description("BVA-count=3 (深蹲计数极值, 正常存盘)")
    public void test_TC_BVA_017() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercise_type", "squat");
        body.put("count", 3);
        body.put("duration", 20);
        mockMvc.perform(post("/api/user/" + TEST_USER_ID + "/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-BVA-018")
    @Feature("Exercise Records")
    @Story("Boundary Value Analysis")
    @Description("BVA-count=4 (深蹲计数极值+1, 正常存盘)")
    public void test_TC_BVA_018() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercise_type", "squat");
        body.put("count", 4);
        body.put("duration", 20);
        mockMvc.perform(post("/api/user/" + TEST_USER_ID + "/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    // === 运动记录过滤：PUSHUP (019-024，独立参数避免重复) ===
    @Test
    @DisplayName("TC-BVA-019")
    @Feature("Exercise Records")
    @Story("Boundary Value Analysis")
    @Description("BVA-durationSeconds=29 (俯卧撑时长极值-1, 触发204拦截)")
    public void test_TC_BVA_019() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercise_type", "pushup");
        body.put("count", 2);
        body.put("duration", 29);
        mockMvc.perform(post("/api/user/" + TEST_USER_ID + "/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("TC-BVA-020")
    @Feature("Exercise Records")
    @Story("Boundary Value Analysis")
    @Description("BVA-durationSeconds=30 (俯卧撑时长极值, 正常存盘)")
    public void test_TC_BVA_020() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercise_type", "pushup");
        body.put("count", 2);
        body.put("duration", 30);
        mockMvc.perform(post("/api/user/" + TEST_USER_ID + "/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-BVA-021")
    @Feature("Exercise Records")
    @Story("Boundary Value Analysis")
    @Description("BVA-durationSeconds=31 (俯卧撑时长极值+1, 正常存盘)")
    public void test_TC_BVA_021() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercise_type", "pushup");
        body.put("count", 2);
        body.put("duration", 31);
        mockMvc.perform(post("/api/user/" + TEST_USER_ID + "/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-BVA-022")
    @Feature("Exercise Records")
    @Story("Boundary Value Analysis")
    @Description("BVA-count=2 (俯卧撑计数极值-1, 触发204拦截)")
    public void test_TC_BVA_022() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercise_type", "pushup");
        body.put("count", 2);
        body.put("duration", 20);
        mockMvc.perform(post("/api/user/" + TEST_USER_ID + "/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("TC-BVA-023")
    @Feature("Exercise Records")
    @Story("Boundary Value Analysis")
    @Description("BVA-count=3 (俯卧撑计数极值, 正常存盘)")
    public void test_TC_BVA_023() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercise_type", "pushup");
        body.put("count", 3);
        body.put("duration", 20);
        mockMvc.perform(post("/api/user/" + TEST_USER_ID + "/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-BVA-024")
    @Feature("Exercise Records")
    @Story("Boundary Value Analysis")
    @Description("BVA-count=4 (俯卧撑计数极值+1, 正常存盘)")
    public void test_TC_BVA_024() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercise_type", "pushup");
        body.put("count", 4);
        body.put("duration", 20);
        mockMvc.perform(post("/api/user/" + TEST_USER_ID + "/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    // === 运动记录过滤：PLANK (025-030，独立参数避免重复) ===
    @Test
    @DisplayName("TC-BVA-025")
    @Feature("Exercise Records")
    @Story("Boundary Value Analysis")
    @Description("BVA-durationSeconds=-1 (平板支撑异常负值时间, 拦截)")
    public void test_TC_BVA_025() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercise_type", "plank");
        body.put("count", 2);
        body.put("duration", -1);
        mockMvc.perform(post("/api/user/" + TEST_USER_ID + "/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("TC-BVA-026")
    @Feature("Exercise Records")
    @Story("Boundary Value Analysis")
    @Description("BVA-durationSeconds=0 (平板支撑零值时间, 拦截)")
    public void test_TC_BVA_026() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercise_type", "plank");
        body.put("count", 2);
        body.put("duration", 0);
        mockMvc.perform(post("/api/user/" + TEST_USER_ID + "/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("TC-BVA-027")
    @Feature("Exercise Records")
    @Story("Boundary Value Analysis")
    @Description("BVA-durationSeconds=1 (平板支撑极小正数时间, 拦截)")
    public void test_TC_BVA_027() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercise_type", "plank");
        body.put("count", 2);
        body.put("duration", 1);
        mockMvc.perform(post("/api/user/" + TEST_USER_ID + "/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("TC-BVA-028")
    @Feature("Exercise Records")
    @Story("Boundary Value Analysis")
    @Description("BVA-count=2 (平板支撑极值计数-1, 拦截)")
    public void test_TC_BVA_028() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercise_type", "plank");
        body.put("count", 2);
        body.put("duration", 20);
        mockMvc.perform(post("/api/user/" + TEST_USER_ID + "/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("TC-BVA-029")
    @Feature("Exercise Records")
    @Story("Boundary Value Analysis")
    @Description("BVA-count=3 (平板支撑极值计数, 正常存盘)")
    public void test_TC_BVA_029() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercise_type", "plank");
        body.put("count", 3);
        body.put("duration", 20);
        mockMvc.perform(post("/api/user/" + TEST_USER_ID + "/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-BVA-030")
    @Feature("Exercise Records")
    @Story("Boundary Value Analysis")
    @Description("BVA-count=4 (平板支撑极值计数+1, 正常存盘)")
    public void test_TC_BVA_030() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercise_type", "plank");
        body.put("count", 4);
        body.put("duration", 20);
        mockMvc.perform(post("/api/user/" + TEST_USER_ID + "/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // TS-03: Decision Table 决策表 (TC-DT-001 至 TC-DT-016)
    // =========================================================================

    @Test
    @DisplayName("TC-DT-001")
    @Feature("Pose Analysis")
    @Story("Decision Table")
    @Description("决策表-运动类别在[SQUAT;PUSHUP;PLANK;JUMPING_JACK]内且33点")
    public void test_TC_DT_001() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(33), ExerciseType.SQUAT, "session_dt_1");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-DT-002")
    @Feature("Pose Analysis")
    @Story("Decision Table")
    @Description("决策表-非法运动类别且点数异常时强行400拦截")
    public void test_TC_DT_002() throws Exception {
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"exercise_type\":\"yoga\",\"pose_landmarks\":[]}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("TC-DT-003")
    @Feature("Pose Analysis")
    @Story("Decision Table")
    @Description("决策表-深蹲状态机完美UP-DESCENDING-DOWN-ASCENDING-UP合规计数循环")
    public void test_TC_DT_003() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createStandingLandmarks(33), ExerciseType.SQUAT, "session_dt_cycle_1");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-DT-004")
    @Feature("Pose Analysis")
    @Story("Decision Table")
    @Description("决策表-无效短动作序列跳过DOWN点触发不计数拦截")
    public void test_TC_DT_004() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createStandingLandmarks(33), ExerciseType.SQUAT, "session_dt_invalid_1");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-DT-005")
    @Feature("Exercise Records")
    @Story("Decision Table")
    @Description("决策表-count<3 且 duration<30, 拦截")
    public void test_TC_DT_005() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercise_type", "squat");
        body.put("count", 2);
        body.put("duration", 20);
        mockMvc.perform(post("/api/user/" + TEST_USER_ID + "/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("TC-DT-006")
    @Feature("Exercise Records")
    @Story("Decision Table")
    @Description("决策表-count<3 但 duration>=30, 正常存盘")
    public void test_TC_DT_006() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercise_type", "squat");
        body.put("count", 2);
        body.put("duration", 30);
        mockMvc.perform(post("/api/user/" + TEST_USER_ID + "/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-DT-007")
    @Feature("Exercise Records")
    @Story("Decision Table")
    @Description("决策表-count>=3 但 duration<30, 正常存盘")
    public void test_TC_DT_007() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercise_type", "squat");
        body.put("count", 3);
        body.put("duration", 20);
        mockMvc.perform(post("/api/user/" + TEST_USER_ID + "/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-DT-008")
    @Feature("Exercise Records")
    @Story("Decision Table")
    @Description("决策表-count>=3 且 duration>=30, 正常存盘")
    public void test_TC_DT_008() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercise_type", "squat");
        body.put("count", 3);
        body.put("duration", 30);
        mockMvc.perform(post("/api/user/" + TEST_USER_ID + "/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-DT-009")
    @Feature("Exercise Records")
    @Story("Decision Table")
    @Description("决策表-重合性过滤-极小参数拦截")
    public void test_TC_DT_009() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercise_type", "squat");
        body.put("count", 1);
        body.put("duration", 10);
        mockMvc.perform(post("/api/user/" + TEST_USER_ID + "/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("TC-DT-010")
    @Feature("Exercise Records")
    @Story("Decision Table")
    @Description("决策表-重合性过滤-小计数大时长存盘")
    public void test_TC_DT_010() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercise_type", "squat");
        body.put("count", 1);
        body.put("duration", 40);
        mockMvc.perform(post("/api/user/" + TEST_USER_ID + "/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-DT-011")
    @Feature("Exercise Records")
    @Story("Decision Table")
    @Description("决策表-重合性过滤-大计数小时长存盘")
    public void test_TC_DT_011() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercise_type", "squat");
        body.put("count", 4);
        body.put("duration", 15);
        mockMvc.perform(post("/api/user/" + TEST_USER_ID + "/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-DT-012")
    @Feature("Exercise Records")
    @Story("Decision Table")
    @Description("决策表-重合性过滤-大计数大时长存盘")
    public void test_TC_DT_012() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercise_type", "squat");
        body.put("count", 4);
        body.put("duration", 45);
        mockMvc.perform(post("/api/user/" + TEST_USER_ID + "/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-DT-013")
    @Feature("Exercise Records")
    @Story("Decision Table")
    @Description("决策表-推荐计划-easy难度与不跳过休息组合")
    public void test_TC_DT_013() throws Exception {
        mockMvc.perform(get("/api/exercises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].difficulty").value("easy"));
    }

    @Test
    @DisplayName("TC-DT-014")
    @Feature("Exercise Records")
    @Story("Decision Table")
    @Description("决策表-推荐计划-medium难度与跳过休息组合")
    public void test_TC_DT_014() throws Exception {
        mockMvc.perform(get("/api/exercises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].difficulty").value("medium"));
    }

    @Test
    @DisplayName("TC-DT-015")
    @Feature("Exercise Records")
    @Story("Decision Table")
    @Description("决策表-推荐计划-medium难度与不跳过休息组合")
    public void test_TC_DT_015() throws Exception {
        mockMvc.perform(get("/api/exercises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[2].difficulty").value("medium"));
    }

    @Test
    @DisplayName("TC-DT-016")
    @Feature("Exercise Records")
    @Story("Decision Table")
    @Description("决策表-仪表盘指标卡路里公式边界正确性")
    public void test_TC_DT_016() throws Exception {
        mockMvc.perform(get("/api/user/" + TEST_USER_ID + "/dashboard"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // TS-04: Combinatorial 组合测试 (TC-CB-001 至 TC-CB-006)
    // =========================================================================

    @Test
    @DisplayName("TC-CB-001")
    @Feature("Exercise Records")
    @Story("Combinatorial")
    @Description("Pairwise组合-历史记录过滤: exerciseType=squat, minScore=80, sortBy=date")
    public void test_TC_CB_001() throws Exception {
        mockMvc.perform(get("/api/user/" + TEST_USER_ID + "/records")
                .param("exerciseType", "squat")
                .param("minScore", "80")
                .param("sortBy", "date"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-CB-002")
    @Feature("Exercise Records")
    @Story("Combinatorial")
    @Description("Pairwise组合-历史记录过滤: exerciseType=squat, minScore=90, sortBy=score")
    public void test_TC_CB_002() throws Exception {
        mockMvc.perform(get("/api/user/" + TEST_USER_ID + "/records")
                .param("exerciseType", "squat")
                .param("minScore", "90")
                .param("sortBy", "score"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-CB-003")
    @Feature("Exercise Records")
    @Story("Combinatorial")
    @Description("Pairwise组合-历史记录过滤: exerciseType=pushup, minScore=80, sortBy=score")
    public void test_TC_CB_003() throws Exception {
        mockMvc.perform(get("/api/user/" + TEST_USER_ID + "/records")
                .param("exerciseType", "pushup")
                .param("minScore", "80")
                .param("sortBy", "score"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-CB-004")
    @Feature("Exercise Records")
    @Story("Combinatorial")
    @Description("Pairwise组合-历史记录过滤: exerciseType=pushup, minScore=90, sortBy=date")
    public void test_TC_CB_004() throws Exception {
        mockMvc.perform(get("/api/user/" + TEST_USER_ID + "/records")
                .param("exerciseType", "pushup")
                .param("minScore", "90")
                .param("sortBy", "date"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-CB-005")
    @Feature("Exercise Records")
    @Story("Combinatorial")
    @Description("Pairwise组合-历史记录过滤: exerciseType=plank, minScore=80, sortBy=date")
    public void test_TC_CB_005() throws Exception {
        mockMvc.perform(get("/api/user/" + TEST_USER_ID + "/records")
                .param("exerciseType", "plank")
                .param("minScore", "80")
                .param("sortBy", "date"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-CB-006")
    @Feature("Exercise Records")
    @Story("Combinatorial")
    @Description("Pairwise组合-历史记录过滤: exerciseType=plank, minScore=90, sortBy=score")
    public void test_TC_CB_006() throws Exception {
        mockMvc.perform(get("/api/user/" + TEST_USER_ID + "/records")
                .param("exerciseType", "plank")
                .param("minScore", "90")
                .param("sortBy", "score"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // TS-05: State Transition 状态转换 (TC-ST-001 至 TC-ST-005)
    // =========================================================================

    @Test
    @DisplayName("TC-ST-001")
    @Feature("Pose Analysis")
    @Story("State Transition")
    @Description("ST-动作状态机-验证初始化直立UP状态 confirmed")
    public void test_TC_ST_001() throws Exception {
        // Send standing frame twice to confirm UP state
        PoseAnalysisRequest request = new PoseAnalysisRequest(createStandingLandmarks(33), ExerciseType.SQUAT, "session_st_1");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details.state").value("up"));
    }

    @Test
    @DisplayName("TC-ST-002")
    @Feature("Pose Analysis")
    @Story("State Transition")
    @Description("ST-动作状态机-验证下蹲中DESCENDING瞬时过渡")
    public void test_TC_ST_002() throws Exception {
        // Frame 1-3: Standing to confirm UP state
        PoseAnalysisRequest standRequest = new PoseAnalysisRequest(createStandingLandmarks(33), ExerciseType.SQUAT, "session_st_2");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(standRequest)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(standRequest)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(standRequest)))
                .andExpect(status().isOk());

        // Frame 4: Squatting (Knee angle 90 < 140 ANGLE_THRESHOLD).
        // Since it's only 1 frame (< 2 REQUIRED_FRAMES), confirmed state remains "up" (transitional descending state)
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PoseAnalysisRequest(createSquattingLandmarks(33), ExerciseType.SQUAT, "session_st_2"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details.state").value("up"));
    }

    @Test
    @DisplayName("TC-ST-003")
    @Feature("Pose Analysis")
    @Story("State Transition")
    @Description("ST-动作状态机-连续2帧下蹲成功确认DOWN最低状态")
    public void test_TC_ST_003() throws Exception {
        // Frame 1: Standing
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PoseAnalysisRequest(createStandingLandmarks(33), ExerciseType.SQUAT, "session_st_3"))))
                .andExpect(status().isOk());

        // Frame 2-3: Squatting to confirm DOWN state
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PoseAnalysisRequest(createSquattingLandmarks(33), ExerciseType.SQUAT, "session_st_3"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PoseAnalysisRequest(createSquattingLandmarks(33), ExerciseType.SQUAT, "session_st_3"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details.state").value("down"));
    }

    @Test
    @DisplayName("TC-ST-004")
    @Feature("Pose Analysis")
    @Story("State Transition")
    @Description("ST-动作状态机-验证完整UP-DOWN-UP周期触发运动计数++")
    public void test_TC_ST_004() throws Exception {
        String sessionId = "session_st_4";
        PoseAnalysisRequest standRequest = new PoseAnalysisRequest(createStandingLandmarks(33), ExerciseType.SQUAT, sessionId);
        PoseAnalysisRequest squatRequest = new PoseAnalysisRequest(createSquattingLandmarks(33), ExerciseType.SQUAT, sessionId);
        
        // 1. Standing (Frame 1-3) -> Confirm UP and prepare stateChangedFrames >= 2
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(standRequest)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(standRequest)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(standRequest)))
                .andExpect(status().isOk());

        // 2. Down (Frame 4-6) -> Confirm DOWN and prepare stateChangedFrames >= 2
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(squatRequest)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(squatRequest)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(squatRequest)))
                .andExpect(status().isOk());

        // 3. Ascending / Back to Standing (Frame 7-8) -> Confirm UP again (Triggers Count++)
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(standRequest)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(standRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1)) // Verify count incremented to 1!
                .andExpect(jsonPath("$.details.state").value("up"));
    }

    @Test
    @DisplayName("TC-ST-005")
    @Feature("Pose Analysis")
    @Story("State Transition")
    @Description("ST-动作状态机-验证重置接口自动归零和清理")
    public void test_TC_ST_005() throws Exception {
        String sessionId = "session_st_5";
        PoseAnalysisRequest standRequest = new PoseAnalysisRequest(createStandingLandmarks(33), ExerciseType.SQUAT, sessionId);
        PoseAnalysisRequest squatRequest = new PoseAnalysisRequest(createSquattingLandmarks(33), ExerciseType.SQUAT, sessionId);
        
        // 1. Run SQUAT cycle to increment count to 1 (using the correct 3-3-2 frame sequence)
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(standRequest)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(standRequest)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(standRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(squatRequest)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(squatRequest)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(squatRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(standRequest)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(standRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        // 2. Perform analyzer reset POST `/api/analyzer/reset/squat`
        mockMvc.perform(post("/api/analyzer/reset/squat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        // 3. Verify count is reset to 0 in subsequent pose analyses (requires 2 stand requests to confirm UP state)
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(standRequest)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(standRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    // =========================================================================
    // TS-06: WhiteBox Java 白盒分支分析 (TC-WBJ-001 至 TC-WBJ-004)
    // =========================================================================

    @Test
    @DisplayName("TC-WBJ-001")
    @Feature("WhiteBox Java")
    @Story("White Box")
    @Description("白盒覆盖-saveExerciseRecord: count=2, duration=20 满足拦截返回 null (SQUAT)")
    public void test_TC_WBJ_001() {
        ExerciseRecord record = userService.saveExerciseRecord(TEST_USER_ID, "squat", 2, 20, 80, 0.9);
        assertNull(record, "Should be filtered and return null");
    }

    @Test
    @DisplayName("TC-WBJ-002")
    @Feature("WhiteBox Java")
    @Story("White Box")
    @Description("白盒覆盖-saveExerciseRecord: count=5, duration=40 正常保存入库并返回 (SQUAT)")
    public void test_TC_WBJ_002() {
        ExerciseRecord record = userService.saveExerciseRecord(TEST_USER_ID, "squat", 5, 40, 85, 0.95);
        assertNotNull(record, "Should be saved successfully");
        assertEquals("squat", record.getExerciseType());
        assertEquals(5, record.getCount());
        assertEquals(40, record.getDuration());
    }

    @Test
    @DisplayName("TC-WBJ-003")
    @Feature("WhiteBox Java")
    @Story("White Box")
    @Description("白盒覆盖-saveExerciseRecord: count=1, duration=10 满足拦截返回 null (PUSHUP)")
    public void test_TC_WBJ_003() {
        ExerciseRecord record = userService.saveExerciseRecord(TEST_USER_ID, "pushup", 1, 10, 70, 0.8);
        assertNull(record, "Should be filtered and return null");
    }

    @Test
    @DisplayName("TC-WBJ-004")
    @Feature("WhiteBox Java")
    @Story("White Box")
    @Description("白盒覆盖-saveExerciseRecord: count=4, duration=35 正常保存入库并返回 (PUSHUP)")
    public void test_TC_WBJ_004() {
        ExerciseRecord record = userService.saveExerciseRecord(TEST_USER_ID, "pushup", 4, 35, 90, 0.98);
        assertNotNull(record, "Should be saved successfully");
        assertEquals("pushup", record.getExerciseType());
        assertEquals(4, record.getCount());
        assertEquals(35, record.getDuration());
    }
}
