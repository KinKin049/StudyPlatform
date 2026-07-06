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

@Service
public class AiPetChatService {
    private static final int MAX_HISTORY_MESSAGES = 8;
    private static final int MAX_MESSAGE_LENGTH = 1200;
    private static final int MAX_CONTEXT_LENGTH = 1800;

    private final RestClient restClient;
    private final String baseUrl;
    private final String apiKey;
    private final String model;

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

    private List<Map<String, String>> buildMessages(AiPetChatRequest request) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(message("system", systemPrompt()));

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

    private String systemPrompt() {
        return """
                你是 StudyPlatform 的 AI 宠物“星云学习猫”。
                你的语气温暖、简洁、可爱，但不要过度卖萌。
                你要优先结合当前页面内容回答学习问题，可以解释页面、总结重点、给出下一步操作建议。
                前端已经能真实执行这些动作：打开页面、搜索课程、创建待办、启动番茄专注。
                如果用户要求你执行动作，但你没有收到工具执行结果，不要说“已完成”“已创建”“已启动”。
                不能真实执行的动作，请明确说需要用户确认或手动操作，并给出下一步。
                如果用户问到作业、考试、题库、番茄钟、待办事项，要给出明确可执行的步骤，不要假装已经代替用户操作。
                不确定时请说明不确定，不要编造项目中不存在的按钮或数据。
                回复尽量控制在 180 字以内，复杂问题可以用短列表。
                """;
    }

    private String buildUserPrompt(AiPetChatRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append("当前页面信息：\n");
        builder.append(buildContextText(request.pageContext()));
        builder.append("\n\n用户问题：\n");
        builder.append(limit(clean(request.message()), MAX_MESSAGE_LENGTH));
        return builder.toString();
    }

    private String buildContextText(AiPetPageContext context) {
        if (context == null) {
            return "暂无页面上下文。";
        }
        String headings = context.headings() == null || context.headings().isEmpty()
                ? "暂无标题"
                : String.join(" / ", context.headings());
        return """
                路径：%s
                标题：%s
                页面标题摘要：%s
                页面可见内容摘录：%s
                """.formatted(
                limit(clean(context.path()), 180),
                limit(clean(context.title()), 180),
                limit(clean(headings), 500),
                limit(clean(context.textSnippet()), MAX_CONTEXT_LENGTH)
        );
    }

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

    private Map<String, String> message(String role, String content) {
        Map<String, String> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

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

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, maxLength) + "…";
    }

    private String trimTrailingSlash(String value) {
        String cleaned = clean(value);
        while (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned.isBlank() ? "https://yunwu.ai" : cleaned;
    }
}
