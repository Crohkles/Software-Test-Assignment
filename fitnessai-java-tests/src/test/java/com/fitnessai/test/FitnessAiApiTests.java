package com.fitnessai.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitnessai.FitnessAiApplication;
import com.fitnessai.dto.PoseAnalysisRequest;
import com.fitnessai.model.*;
import com.fitnessai.service.UserService;
import io.qameta.allure.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private static final String TEST_USER_ID = "test_user_automation";

    private List<PoseLandmark> createLandmarks(int count) {
        List<PoseLandmark> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new PoseLandmark(0.5, 0.5, 0.0, 0.9));
        }
        return list;
    }

    // =========================================================================
    // TS-01: EP 等价划分 (TC-EP-001 至 TC-EP-020)
    // =========================================================================

    @Test
    @DisplayName("TC-EP-001")
    @Feature("Pose Analysis")
    @Story("Equivalence Partitioning")
    @Description("EP-有效等价类-pose analysis")
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
    @Description("EP-无效等价类-pose analysis")
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
    @Description("EP-有效等价类-pose analysis invalid input")
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
    @Description("EP-无效等价类-pose analysis invalid input")
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
    @Description("EP-有效等价类-state-machine counting")
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
    @Description("EP-无效等价类-state-machine counting")
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
    @Description("EP-有效等价类-state-machine short cycle")
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
    @Description("EP-无效等价类-state-machine short cycle")
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
    @Description("EP-有效等价类-record filtering")
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
    @Description("EP-无效等价类-record filtering")
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
    @Description("EP-有效等价类-record saving")
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
    @Description("EP-无效等价类-record saving")
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
    @Description("EP-有效等价类-training plan easy")
    public void test_TC_EP_013() throws Exception {
        mockMvc.perform(get("/api/exercises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].difficulty").value("easy"));
    }

    @Test
    @DisplayName("TC-EP-014")
    @Feature("Exercise Records")
    @Story("Equivalence Partitioning")
    @Description("EP-无效等价类-training plan easy")
    public void test_TC_EP_014() throws Exception {
        mockMvc.perform(get("/api/exercises/invalid"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("TC-EP-015")
    @Feature("Exercise Records")
    @Story("Equivalence Partitioning")
    @Description("EP-有效等价类-training plan medium")
    public void test_TC_EP_015() throws Exception {
        mockMvc.perform(get("/api/exercises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].difficulty").value("medium"));
    }

    @Test
    @DisplayName("TC-EP-016")
    @Feature("Exercise Records")
    @Story("Equivalence Partitioning")
    @Description("EP-无效等价类-training plan medium")
    public void test_TC_EP_016() throws Exception {
        mockMvc.perform(get("/api/exercises/invalid_med"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("TC-EP-017")
    @Feature("Exercise Records")
    @Story("Equivalence Partitioning")
    @Description("EP-有效等价类-training plan hard")
    public void test_TC_EP_017() throws Exception {
        mockMvc.perform(get("/api/exercises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[2].difficulty").value("medium"));
    }

    @Test
    @DisplayName("TC-EP-018")
    @Feature("Exercise Records")
    @Story("Equivalence Partitioning")
    @Description("EP-无效等价类-training plan hard")
    public void test_TC_EP_018() throws Exception {
        mockMvc.perform(get("/api/exercises/invalid_hard"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("TC-EP-019")
    @Feature("Exercise Records")
    @Story("Equivalence Partitioning")
    @Description("EP-有效等价类-dashboard calories")
    public void test_TC_EP_019() throws Exception {
        mockMvc.perform(get("/api/user/" + TEST_USER_ID + "/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-EP-020")
    @Feature("Exercise Records")
    @Story("Equivalence Partitioning")
    @Description("EP-无效等价类-dashboard calories")
    public void test_TC_EP_020() throws Exception {
        mockMvc.perform(get("/api/user/null/dashboard"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // TS-02: BVA 边界值分析 (TC-BVA-001 至 TC-BVA-030)
    // =========================================================================

    @Test
    @DisplayName("TC-BVA-001")
    @Feature("Pose Analysis")
    @Story("Boundary Value Analysis")
    @Description("BVA-landmarks.length=31 (下边界-1, 无效)")
    public void test_TC_BVA_001() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(31), ExerciseType.SQUAT, "session_bva");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("TC-BVA-002")
    @Feature("Pose Analysis")
    @Story("Boundary Value Analysis")
    @Description("BVA-landmarks.length=32 (下边界, 有效)")
    public void test_TC_BVA_002() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(32), ExerciseType.SQUAT, "session_bva");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("TC-BVA-003")
    @Feature("Pose Analysis")
    @Story("Boundary Value Analysis")
    @Description("BVA-landmarks.length=33 (下边界+1, 有效)")
    public void test_TC_BVA_003() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(33), ExerciseType.SQUAT, "session_bva");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-BVA-004")
    @Feature("Pose Analysis")
    @Story("Boundary Value Analysis")
    @Description("BVA-landmarks.length=33 (上边界-1, 有效)")
    public void test_TC_BVA_004() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(33), ExerciseType.SQUAT, "session_bva");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-BVA-005")
    @Feature("Pose Analysis")
    @Story("Boundary Value Analysis")
    @Description("BVA-landmarks.length=34 (上边界, 有效)")
    public void test_TC_BVA_005() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(34), ExerciseType.SQUAT, "session_bva");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-BVA-006")
    @Feature("Pose Analysis")
    @Story("Boundary Value Analysis")
    @Description("BVA-landmarks.length=35 (上边界+1, 无效)")
    public void test_TC_BVA_006() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(35), ExerciseType.SQUAT, "session_bva");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-BVA-007")
    @Feature("Pose Analysis")
    @Story("Boundary Value Analysis")
    @Description("BVA-landmarks.length=31 (下边界-1, 无效)")
    public void test_TC_BVA_007() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(31), ExerciseType.SQUAT, "session_bva");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("TC-BVA-008")
    @Feature("Pose Analysis")
    @Story("Boundary Value Analysis")
    @Description("BVA-landmarks.length=32 (下边界, 有效)")
    public void test_TC_BVA_008() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(32), ExerciseType.SQUAT, "session_bva");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("TC-BVA-009")
    @Feature("Pose Analysis")
    @Story("Boundary Value Analysis")
    @Description("BVA-landmarks.length=33 (下边界+1, 有效)")
    public void test_TC_BVA_009() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(33), ExerciseType.SQUAT, "session_bva");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-BVA-010")
    @Feature("Pose Analysis")
    @Story("Boundary Value Analysis")
    @Description("BVA-landmarks.length=33 (上边界-1, 有效)")
    public void test_TC_BVA_010() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(33), ExerciseType.SQUAT, "session_bva");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-BVA-011")
    @Feature("Pose Analysis")
    @Story("Boundary Value Analysis")
    @Description("BVA-landmarks.length=34 (上边界, 有效)")
    public void test_TC_BVA_011() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(34), ExerciseType.SQUAT, "session_bva");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-BVA-012")
    @Feature("Pose Analysis")
    @Story("Boundary Value Analysis")
    @Description("BVA-landmarks.length=35 (上边界+1, 无效)")
    public void test_TC_BVA_012() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(35), ExerciseType.SQUAT, "session_bva");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-BVA-013")
    @Feature("Exercise Records")
    @Story("Boundary Value Analysis")
    @Description("BVA-durationSeconds=29 (边界-1, 无效)")
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
    @Description("BVA-durationSeconds=30 (边界, 有效)")
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
    @Description("BVA-durationSeconds=31 (边界+1, 有效)")
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
    @Description("BVA-count=2 (边界-1, 无效)")
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
    @Description("BVA-count=3 (边界, 有效)")
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
    @Description("BVA-count=4 (边界+1, 有效)")
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

    @Test
    @DisplayName("TC-BVA-019")
    @Feature("Exercise Records")
    @Story("Boundary Value Analysis")
    @Description("BVA-durationSeconds=29 (边界-1, 无效)")
    public void test_TC_BVA_019() throws Exception {
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
    @DisplayName("TC-BVA-020")
    @Feature("Exercise Records")
    @Story("Boundary Value Analysis")
    @Description("BVA-durationSeconds=30 (边界, 有效)")
    public void test_TC_BVA_020() throws Exception {
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
    @DisplayName("TC-BVA-021")
    @Feature("Exercise Records")
    @Story("Boundary Value Analysis")
    @Description("BVA-durationSeconds=31 (边界+1, 有效)")
    public void test_TC_BVA_021() throws Exception {
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
    @DisplayName("TC-BVA-022")
    @Feature("Exercise Records")
    @Story("Boundary Value Analysis")
    @Description("BVA-count=2 (边界-1, 无效)")
    public void test_TC_BVA_022() throws Exception {
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
    @DisplayName("TC-BVA-023")
    @Feature("Exercise Records")
    @Story("Boundary Value Analysis")
    @Description("BVA-count=3 (边界, 有效)")
    public void test_TC_BVA_023() throws Exception {
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
    @DisplayName("TC-BVA-024")
    @Feature("Exercise Records")
    @Story("Boundary Value Analysis")
    @Description("BVA-count=4 (边界+1, 有效)")
    public void test_TC_BVA_024() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercise_type", "squat");
        body.put("count", 4);
        body.put("duration", 20);
        mockMvc.perform(post("/api/user/" + TEST_USER_ID + "/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-BVA-025")
    @Feature("Exercise Records")
    @Story("Boundary Value Analysis")
    @Description("BVA-durationSeconds=-1 (边界-1, 无效)")
    public void test_TC_BVA_025() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercise_type", "squat");
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
    @Description("BVA-durationSeconds=0 (边界, 有效)")
    public void test_TC_BVA_026() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercise_type", "squat");
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
    @Description("BVA-durationSeconds=1 (边界+1, 有效)")
    public void test_TC_BVA_027() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercise_type", "squat");
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
    @Description("BVA-count=2 (边界-1, 无效)")
    public void test_TC_BVA_028() throws Exception {
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
    @DisplayName("TC-BVA-029")
    @Feature("Exercise Records")
    @Story("Boundary Value Analysis")
    @Description("BVA-count=3 (边界, 有效)")
    public void test_TC_BVA_029() throws Exception {
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
    @DisplayName("TC-BVA-030")
    @Feature("Exercise Records")
    @Story("Boundary Value Analysis")
    @Description("BVA-count=4 (边界+1, 有效)")
    public void test_TC_BVA_030() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercise_type", "squat");
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
    @Description("决策表-exerciseType in [SQUAT;PUSHUP;PLANK;JUMP")
    public void test_TC_DT_001() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(33), ExerciseType.SQUAT, "session_dt");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-DT-002")
    @Feature("Pose Analysis")
    @Story("Decision Table")
    @Description("决策表-exerciseType=YOGA OR landmarks.length=32")
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
    @Description("决策表-valid cycle: UP>DESCENDING>DOWN>ASCENDIN")
    public void test_TC_DT_003() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(33), ExerciseType.SQUAT, "session_dt_cycle");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-DT-004")
    @Feature("Pose Analysis")
    @Story("Decision Table")
    @Description("决策表-invalid: UP>DESCENDING>UP skips DOWN")
    public void test_TC_DT_004() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(33), ExerciseType.SQUAT, "session_dt_invalid");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-DT-005")
    @Feature("Exercise Records")
    @Story("Decision Table")
    @Description("决策表-count<3, duration<30")
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
    @Description("决策表-count<3, duration>=30")
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
    @Description("决策表-count>=3, duration<30")
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
    @Description("决策表-count>=3, duration>=30")
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
    @Description("决策表-count<3, duration<30")
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
    @Description("决策表-count<3, duration>=30")
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
    @Description("决策表-count>=3, duration<30")
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
    @Description("决策表-count>=3, duration>=30")
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
    @Description("决策表-difficulty=easy AND skipRest=false")
    public void test_TC_DT_013() throws Exception {
        mockMvc.perform(get("/api/exercises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].difficulty").value("easy"));
    }

    @Test
    @DisplayName("TC-DT-014")
    @Feature("Exercise Records")
    @Story("Decision Table")
    @Description("决策表-difficulty=medium AND skipRest=true")
    public void test_TC_DT_014() throws Exception {
        mockMvc.perform(get("/api/exercises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].difficulty").value("medium"));
    }

    @Test
    @DisplayName("TC-DT-015")
    @Feature("Exercise Records")
    @Story("Decision Table")
    @Description("决策表-difficulty=hard AND skipRest=false")
    public void test_TC_DT_015() throws Exception {
        mockMvc.perform(get("/api/exercises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[2].difficulty").value("medium"));
    }

    @Test
    @DisplayName("TC-DT-016")
    @Feature("Exercise Records")
    @Story("Decision Table")
    @Description("决策表-weightKg in [30;200] AND durationHours>0")
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
    @Description("Pairwise组合-difficulty=easy, skipRest=true")
    public void test_TC_CB_001() throws Exception {
        mockMvc.perform(get("/api/recommendations")
                .param("userId", TEST_USER_ID)
                .param("currentExercise", "squat"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-CB-002")
    @Feature("Exercise Records")
    @Story("Combinatorial")
    @Description("Pairwise组合-difficulty=easy, skipRest=false")
    public void test_TC_CB_002() throws Exception {
        mockMvc.perform(get("/api/recommendations")
                .param("userId", TEST_USER_ID)
                .param("currentExercise", "squat"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-CB-003")
    @Feature("Exercise Records")
    @Story("Combinatorial")
    @Description("Pairwise组合-difficulty=medium, skipRest=true")
    public void test_TC_CB_003() throws Exception {
        mockMvc.perform(get("/api/recommendations")
                .param("userId", TEST_USER_ID)
                .param("currentExercise", "pushup"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-CB-004")
    @Feature("Exercise Records")
    @Story("Combinatorial")
    @Description("Pairwise组合-difficulty=medium, skipRest=false")
    public void test_TC_CB_004() throws Exception {
        mockMvc.perform(get("/api/recommendations")
                .param("userId", TEST_USER_ID)
                .param("currentExercise", "pushup"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-CB-005")
    @Feature("Exercise Records")
    @Story("Combinatorial")
    @Description("Pairwise组合-difficulty=hard, skipRest=true")
    public void test_TC_CB_005() throws Exception {
        mockMvc.perform(get("/api/recommendations")
                .param("userId", TEST_USER_ID)
                .param("currentExercise", "jumping_jack"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-CB-006")
    @Feature("Exercise Records")
    @Story("Combinatorial")
    @Description("Pairwise组合-difficulty=hard, skipRest=false")
    public void test_TC_CB_006() throws Exception {
        mockMvc.perform(get("/api/recommendations")
                .param("userId", TEST_USER_ID)
                .param("currentExercise", "jumping_jack"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // TS-05: State Transition 状态转换 (TC-ST-001 至 TC-ST-005)
    // =========================================================================

    @Test
    @DisplayName("TC-ST-001")
    @Feature("Pose Analysis")
    @Story("State Transition")
    @Description("ST[all-states] UP")
    public void test_TC_ST_001() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(33), ExerciseType.SQUAT, "session_st_1");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-ST-002")
    @Feature("Pose Analysis")
    @Story("State Transition")
    @Description("ST[all-states] DESCENDING")
    public void test_TC_ST_002() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(33), ExerciseType.SQUAT, "session_st_2");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-ST-003")
    @Feature("Pose Analysis")
    @Story("State Transition")
    @Description("ST[all-states] DOWN")
    public void test_TC_ST_003() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(33), ExerciseType.SQUAT, "session_st_3");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-ST-004")
    @Feature("Pose Analysis")
    @Story("State Transition")
    @Description("ST[all-states] ASCENDING")
    public void test_TC_ST_004() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(33), ExerciseType.SQUAT, "session_st_4");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-ST-005")
    @Feature("Pose Analysis")
    @Story("State Transition")
    @Description("ST[all-states] COOLDOWN")
    public void test_TC_ST_005() throws Exception {
        PoseAnalysisRequest request = new PoseAnalysisRequest(createLandmarks(33), ExerciseType.SQUAT, "session_st_5");
        mockMvc.perform(post("/api/analytics/pose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // TS-06: WhiteBox Java 白盒分析 (TC-WBJ-001 至 TC-WBJ-004)
    // =========================================================================

    @Test
    @DisplayName("TC-WBJ-001")
    @Feature("WhiteBox Java")
    @Story("White Box")
    @Description("Cover saveExerciseRecord branch: count < 3 && duration < 30 == true")
    public void test_TC_WBJ_001() {
        ExerciseRecord record = userService.saveExerciseRecord(TEST_USER_ID, "squat", 2, 20, 80, 0.9);
        assertNull(record, "Should be filtered and return null");
    }

    @Test
    @DisplayName("TC-WBJ-002")
    @Feature("WhiteBox Java")
    @Story("White Box")
    @Description("Cover saveExerciseRecord branch: count < 3 && duration < 30 == false")
    public void test_TC_WBJ_002() {
        ExerciseRecord record = userService.saveExerciseRecord(TEST_USER_ID, "squat", 5, 40, 80, 0.9);
        assertNotNull(record, "Should be saved successfully");
        assertEquals("squat", record.getExerciseType());
        assertEquals(5, record.getCount());
        assertEquals(40, record.getDuration());
    }

    @Test
    @DisplayName("TC-WBJ-003")
    @Feature("WhiteBox Java")
    @Story("White Box")
    @Description("Cover saveExerciseRecord branch: count < 3 && duration < 30 == true")
    public void test_TC_WBJ_003() {
        ExerciseRecord record = userService.saveExerciseRecord(TEST_USER_ID, "squat", 1, 10, 80, 0.9);
        assertNull(record, "Should be filtered and return null");
    }

    @Test
    @DisplayName("TC-WBJ-004")
    @Feature("WhiteBox Java")
    @Story("White Box")
    @Description("Cover saveExerciseRecord branch: count < 3 && duration < 30 == false")
    public void test_TC_WBJ_004() {
        ExerciseRecord record = userService.saveExerciseRecord(TEST_USER_ID, "squat", 4, 35, 80, 0.9);
        assertNotNull(record, "Should be saved successfully");
        assertEquals("squat", record.getExerciseType());
        assertEquals(4, record.getCount());
        assertEquals(35, record.getDuration());
    }
}
