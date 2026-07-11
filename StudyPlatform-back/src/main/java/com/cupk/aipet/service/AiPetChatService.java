package com.cupk.aipet.service;

import com.cupk.aipet.dto.AiPetChatMessage;
import com.cupk.aipet.dto.AiPetChatRequest;
import com.cupk.aipet.dto.AiPetPageContext;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/**
 * AI 宠物聊天服务类
 * 负责与 AI 大模型进行交互，处理用户与 AI 宠物的对话逻辑
 */
@Service
public class AiPetChatService {

    /**
     * 最大历史消息条数
     */
    private static final int MAX_HISTORY_MESSAGES = 8;

    /**
     * 单条消息最大长度
     */
    private static final int MAX_MESSAGE_LENGTH = 1200;

    /**
     * 页面上下文最大长度
     */
    private static final int MAX_CONTEXT_LENGTH = 5200;

    /**
     * HTTP 客户端实例
     */
    private final RestClient restClient;

    /**
     * AI 服务基础地址
     */
    private final String baseUrl;

    /**
     * API 密钥
     */
    private final String apiKey;

    /**
     * 使用的模型名称
     */
    private final String model;

    /**
     * 构造函数，初始化 AI 宠物聊天服务
     *
     * @param restClientBuilder RestClient 构建器
     * @param baseUrl AI 服务基础地址，默认为 https://yunwu.ai
     * @param apiKey API 密钥，从配置文件读取
     * @param model 模型名称，默认为 deepseek-v4-flash
     */
    public AiPetChatService(
            RestClient.Builder restClientBuilder,
            @Value("${ai.pet.base-url:https://yunwu.ai}") String baseUrl,
            @Value("${ai.pet.api-key:}") String apiKey,
            @Value("${ai.pet.model:deepseek-v4-flash}") String model
    ) {
        this.restClient = restClientBuilder.build();
        this.baseUrl = trimTrailingSlash(cleanConfigValue(baseUrl));
        this.apiKey = cleanConfigValue(apiKey);
        String configuredModel = cleanConfigValue(model);
        this.model = configuredModel.isBlank() ? "deepseek-v4-flash" : configuredModel;
    }

    /**
     * 发送聊天请求，获取 AI 宠物回复
     *
     * @param request 聊天请求，包含用户消息和上下文信息
     * @return AI 宠物的回复内容
     */
    public String chat(AiPetChatRequest request) {
        if (apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 宠物接口还没有配置 api key");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("messages", buildMessages(request));
        payload.put("temperature", 0.7);
        payload.put("max_tokens", 900);

        try {
            JsonNode response = restClient.post()
                    .uri(baseUrl + "/v1/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);

            String reply = extractReply(response);
            if (reply.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 宠物没有返回有效内容");
            }
            return reply;
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 宠物连接中转站失败：" + exception.getMessage());
        }
    }

    /**
     * 构建发送给 AI 模型的消息列表
     * 包含系统提示、历史消息和当前用户消息
     *
     * @param request 聊天请求
     * @return 格式化后的消息列表
     */
    private List<Map<String, String>> buildMessages(AiPetChatRequest request) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(message("system", systemPrompt(request)));

        List<AiPetChatMessage> history = request.history() == null ? List.of() : request.history();
        int fromIndex = Math.max(0, history.size() - MAX_HISTORY_MESSAGES);
        for (AiPetChatMessage item : history.subList(fromIndex, history.size())) {
            String role = "user".equals(item.role()) ? "user" : "assistant";
            String text = limit(clean(item.text()), MAX_MESSAGE_LENGTH);
            if (!text.isBlank()) {
                messages.add(message(role, text));
            }
        }

        messages.add(message("user", buildUserPrompt(request)));
        return messages;
    }

    /**
     * 获取系统提示词
     * 定义 AI 宠物的角色、语气和行为准则
     *
     * @return 系统提示词文本
     */
    private String systemPrompt(AiPetChatRequest request) {
        String petName = resolvePetName(request);
        String petShortName = resolvePetShortName(request, petName);
        return """
                你是 StudyPlatform 的 AI 宠物“%s”，简称“%s”。
                当用户问你叫什么、是什么形象或当前身份时，必须回答当前名称，不要自称星云学习猫，除非当前名称确实是星云学习猫。
                你的语气温暖、简洁、可爱，但不要过度卖萌。
                你要优先结合当前页面内容回答学习问题，可以解释页面、总结重点、给出下一步操作建议。
                当前页面上下文来自前端实时读取的可见文本、标题、选中文本和安全表单摘要；用户问“本页”“这里”“这个按钮/内容”时，必须优先依据这些上下文回答。
                前端已经能真实执行这些动作：打开页面、搜索课程、创建待办、启动番茄专注。
                如果用户要求你执行动作，但你没有收到工具执行结果，不要说“已完成”“已创建”“已启动”。
                不能真实执行的动作，请明确说需要用户确认或手动操作，并给出下一步。
                如果用户问到作业、考试、题库、番茄钟、待办事项，要给出明确可执行的步骤，不要假装已经代替用户操作。
                不确定时请说明不确定，不要编造项目中不存在的按钮或数据。
                回复尽量控制在 180 字以内，复杂问题可以用短列表。
                """.formatted(petName, petShortName);
    }

    private String resolvePetName(AiPetChatRequest request) {
        String petName = request == null ? "" : clean(request.petName());
        if (!petName.isBlank()) {
            return limit(petName, 80);
        }
        String petShortName = request == null ? "" : clean(request.petShortName());
        return petShortName.isBlank() ? "AI 学习伙伴" : limit(petShortName, 80);
    }

    private String resolvePetShortName(AiPetChatRequest request, String fallbackName) {
        String petShortName = request == null ? "" : clean(request.petShortName());
        return petShortName.isBlank() ? fallbackName : limit(petShortName, 60);
    }

    /**
     * 构建用户提示词
     * 将页面上下文和用户问题组合成完整的提示内容
     *
     * @param request 聊天请求
     * @return 组合后的用户提示词
     */
    private String buildUserPrompt(AiPetChatRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append("当前页面信息：\n");
        builder.append(buildContextText(request.pageContext()));
        builder.append("\n\n用户问题：\n");
        builder.append(limit(clean(request.message()), MAX_MESSAGE_LENGTH));
        return builder.toString();
    }

    /**
     * 构建页面上下文文本
     * 将页面上下文对象转换为可读的文本格式
     *
     * @param context 页面上下文对象
     * @return 格式化后的上下文文本
     */
    private String buildContextText(AiPetPageContext context) {
        if (context == null) {
            return "暂无页面上下文。";
        }
        String headings = context.headings() == null || context.headings().isEmpty()
                ? "暂无标题"
                : String.join(" / ", context.headings());
        String formSnapshot = context.formSnapshot() == null || context.formSnapshot().isEmpty()
                ? "暂无表单状态"
                : String.join("；", context.formSnapshot());
        String selectedText = clean(context.selectedText()).isBlank() ? "暂无选中文本" : context.selectedText();
        String contentLength = context.contentLength() == null ? "未知" : context.contentLength().toString();
        return """
                路径：%s
                路由：%s
                标题：%s
                页面标题摘要：%s
                页面可见文本长度：约 %s 字
                用户选中文本：%s
                表单/输入状态：%s
                页面可见内容摘录：%s
                """.formatted(
                limit(clean(context.path()), 220),
                limit(clean(context.routeName()), 120),
                limit(clean(context.title()), 180),
                limit(clean(headings), 700),
                contentLength,
                limit(clean(selectedText), 900),
                limit(clean(formSnapshot), 1200),
                limit(clean(context.textSnippet()), MAX_CONTEXT_LENGTH)
        );
    }

    /**
     * 从 AI 模型响应中提取回复内容
     * 支持 content 和 text 两种字段格式
     *
     * @param response AI 模型的响应 JSON
     * @return 提取的回复文本
     */
    private String extractReply(JsonNode response) {
        if (response == null) {
            return "";
        }
        JsonNode choices = response.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return "";
        }
        JsonNode message = choices.get(0).path("message");
        String content = message.path("content").asText("");
        if (!content.isBlank()) {
            return content.trim();
        }
        return choices.get(0).path("text").asText("").trim();
    }

    /**
     * 创建格式化的消息对象
     *
     * @param role 消息角色（system/user/assistant）
     * @param content 消息内容
     * @return 消息键值对映射
     */
    private Map<String, String> message(String role, String content) {
        Map<String, String> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    /**
     * 清理字符串，去除首尾空白
     *
     * @param value 待清理的字符串
     * @return 清理后的字符串，null 返回空字符串
     */
    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 清理配置值，去除首尾空白和可能的引号包裹
     *
     * @param value 配置值
     * @return 清理后的配置值
     */
    private String cleanConfigValue(String value) {
        String cleaned = clean(value);
        if (cleaned.length() >= 2) {
            char firstChar = cleaned.charAt(0);
            char lastChar = cleaned.charAt(cleaned.length() - 1);
            if ((firstChar == '"' && lastChar == '"') || (firstChar == '\'' && lastChar == '\'')) {
                return cleaned.substring(1, cleaned.length() - 1).trim();
            }
        }
        return cleaned;
    }

    /**
     * 限制字符串长度，超出部分用省略号替换
     *
     * @param value 待限制的字符串
     * @param maxLength 最大长度
     * @return 截断后的字符串
     */
    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, maxLength) + "…";
    }

    /**
     * 去除 URL 末尾的斜杠
     *
     * @param value URL 字符串
     * @return 去除末尾斜杠后的 URL，空值返回默认地址
     */
    private String trimTrailingSlash(String value) {
        String cleaned = clean(value);
        while (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned.isBlank() ? "https://yunwu.ai" : cleaned;
    }
}
