package com.cupk.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 记录所有后端业务接口的访问日志，便于开发调试和项目报告截图展示。
 */
@Component
public class ApiAccessLogFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiAccessLogFilter.class);
    private static final Map<String, String> MODULE_NAMES = new LinkedHashMap<>();

    static {
        MODULE_NAMES.put("/api/auth", "用户认证与注册");
        MODULE_NAMES.put("/api/profile", "个人中心与学习画像");
        MODULE_NAMES.put("/api/academy/question-bank", "课程题库与错题收藏");
        MODULE_NAMES.put("/api/academy/assignments", "课程作业");
        MODULE_NAMES.put("/api/academy/exams", "课程考试");
        MODULE_NAMES.put("/api/academy/textbooks", "精品教材");
        MODULE_NAMES.put("/api/academy/textbook-cart", "教材购物车");
        MODULE_NAMES.put("/api/academy/textbook-orders", "教材订单");
        MODULE_NAMES.put("/api/academy/teacher", "教师工作台");
        MODULE_NAMES.put("/api/academy", "在线学堂");
        MODULE_NAMES.put("/api/oj/submissions", "OJ代码提交");
        MODULE_NAMES.put("/api/oj/problems", "OJ题目管理");
        MODULE_NAMES.put("/api/games/ladder-jump", "万题天梯跳游戏");
        MODULE_NAMES.put("/api/games/type-warrior", "Type Warrior游戏");
        MODULE_NAMES.put("/api/rewards/vouchers", "金币兑换与卡券");
        MODULE_NAMES.put("/api/well-log", "测井解释实验");
        MODULE_NAMES.put("/api/production", "采油仿真实验");
        MODULE_NAMES.put("/api/ai-pet", "AI宠物助手");
        MODULE_NAMES.put("/api/admin", "后台管理");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || !uri.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = sanitizeQuery(request.getQueryString());
        String userId = defaultValue(request.getHeader("X-Auth-User-Id"), "未登录/默认用户");
        String module = resolveModule(uri);
        String operation = resolveOperation(method, uri);

        LOGGER.info("【业务请求开始】模块={}，操作={}，方法={}，路径={}{}，用户ID={}",
                module, operation, method, uri, query, userId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            LOGGER.info("【业务请求结束】模块={}，操作={}，状态码={}，耗时={}ms",
                    module, operation, response.getStatus(), duration);
        }
    }

    private String resolveModule(String uri) {
        for (Map.Entry<String, String> entry : MODULE_NAMES.entrySet()) {
            if (uri.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "其他业务接口";
    }

    private String resolveOperation(String method, String uri) {
        if (uri.contains("/login")) {
            return "用户登录";
        }
        if (uri.contains("/register")) {
            return "用户注册";
        }
        if (uri.contains("/password-reset/code")) {
            return "发送密码重置验证码";
        }
        if (uri.contains("/password-reset/confirm")) {
            return "确认密码重置";
        }
        if (uri.contains("/onboarding")) {
            return "保存入驻引导信息";
        }
        if (uri.endsWith("/home")) {
            return "获取首页聚合数据";
        }
        if (uri.contains("/my-courses")) {
            return "查询我的课程";
        }
        if (uri.contains("/assignments") && uri.contains("/submit")) {
            return "提交课程作业";
        }
        if (uri.contains("/assignments") && uri.contains("/draft")) {
            return "保存课程作业草稿";
        }
        if (uri.contains("/assignments")) {
            return "查询课程作业";
        }
        if (uri.contains("/exams") && uri.contains("/start")) {
            return "开始课程考试";
        }
        if (uri.contains("/exams") && uri.contains("/submit")) {
            return "提交课程考试";
        }
        if (uri.contains("/exams") && uri.contains("/draft")) {
            return "保存课程考试草稿";
        }
        if (uri.contains("/exams")) {
            return "查询课程考试";
        }
        if (uri.contains("/enroll") && "POST".equals(method)) {
            return "加入课程";
        }
        if (uri.contains("/enroll") && "DELETE".equals(method)) {
            return "退出课程";
        }
        if (uri.contains("/reviews") && "POST".equals(method)) {
            return "发布课程或教材评价";
        }
        if (uri.contains("/reviews") && "GET".equals(method)) {
            return "查询课程或教材评价";
        }
        if (uri.contains("/reply")) {
            return "回复用户评价";
        }
        if (uri.contains("/online-open-courses") && "POST".equals(method)) {
            return "教师发布在线课程";
        }
        if (uri.contains("/online-open-courses")) {
            return "查询在线开放课程";
        }
        if (uri.contains("/general-courses")) {
            return "查询通识课程";
        }
        if (uri.contains("/micro-major-courses")) {
            return "查询微专业课程";
        }
        if (uri.contains("/textbook-cart")) {
            return "维护教材购物车";
        }
        if (uri.contains("/textbook-orders")) {
            return "创建或查询教材订单";
        }
        if (uri.contains("/textbooks")) {
            return "查询精品教材";
        }
        if (uri.contains("/mistakes/answers")) {
            return "记录题库作答与错题";
        }
        if (uri.contains("/mistakes")) {
            return "查询错题本";
        }
        if (uri.contains("/favorites") && "POST".equals(method)) {
            return "收藏题目";
        }
        if (uri.contains("/favorites") && "DELETE".equals(method)) {
            return "取消收藏题目";
        }
        if (uri.contains("/favorites")) {
            return "查询收藏题目";
        }
        if (uri.contains("/course-catalog")) {
            return "查询课程题库目录";
        }
        if (uri.contains("/question-bank/courses")) {
            return "查询课程题库详情";
        }
        if (uri.contains("/question-bank/problems")) {
            return "查询通用题源题目";
        }
        if (uri.contains("/question-bank/subjects")) {
            return "查询题库学科";
        }
        if (uri.contains("/question-bank/import")) {
            return "导入外部题库";
        }
        if (uri.contains("/oj/submissions") && "POST".equals(method)) {
            return "提交OJ代码";
        }
        if (uri.contains("/oj/submissions")) {
            return "查询OJ提交结果";
        }
        if (uri.contains("/oj/problems") && "POST".equals(method)) {
            return "创建OJ题目";
        }
        if (uri.contains("/oj/problems") && "PUT".equals(method)) {
            return "更新OJ题目";
        }
        if (uri.contains("/oj/problems")) {
            return "查询OJ题目";
        }
        if (uri.contains("/ladder-jump") && uri.contains("/records")) {
            return "保存万题天梯跳成绩";
        }
        if (uri.contains("/type-warrior") && uri.contains("/records")) {
            return "保存Type Warrior成绩";
        }
        if (uri.contains("/vouchers/exchange")) {
            return "使用金币兑换卡券";
        }
        if (uri.contains("/vouchers/use")) {
            return "使用卡券";
        }
        if (uri.contains("/vouchers/items")) {
            return "查询可兑换卡券";
        }
        if (uri.contains("/vouchers")) {
            return "兑换或查询卡券";
        }
        if (uri.contains("/well-log/template")) {
            return "查询测井模板";
        }
        if (uri.contains("/well-log/record") && "POST".equals(method)) {
            return "保存测井解释记录";
        }
        if (uri.contains("/well-log/record")) {
            return "查询测井解释记录";
        }
        if (uri.contains("/production/pump/save")) {
            return "保存抽油泵仿真记录";
        }
        if (uri.contains("/production/reservoir/save")) {
            return "保存油藏动态仿真记录";
        }
        if (uri.contains("/production/waterflood/save")) {
            return "保存注水开发仿真记录";
        }
        if (uri.contains("/production/stimulation/save")) {
            return "保存压裂酸化仿真记录";
        }
        if (uri.contains("/production")) {
            return "查询或维护采油仿真记录";
        }
        if (uri.contains("/ai-pet")) {
            return "调用AI宠物助手";
        }
        if (uri.contains("/record")) {
            return "保存或查询实验/游戏记录";
        }
        if (uri.contains("/admin")) {
            return "执行后台管理操作";
        }
        return switch (method) {
            case "GET" -> "查询业务数据";
            case "POST" -> "提交业务数据";
            case "PUT", "PATCH" -> "更新业务数据";
            case "DELETE" -> "删除业务数据";
            default -> "访问业务接口";
        };
    }

    private String sanitizeQuery(String queryString) {
        if (queryString == null || queryString.isBlank()) {
            return "";
        }
        return "?" + queryString
                .replaceAll("(?i)(password|code|token|apiKey|api-key)=([^&]*)", "$1=***");
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
