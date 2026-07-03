package com.cupk.academy.service;

import com.cupk.academy.dto.CourseQuestionBankCategoryResponse;
import com.cupk.academy.dto.CourseQuestionBankDetailResponse;
import com.cupk.academy.dto.CourseQuestionBankQuestionResponse;
import com.cupk.academy.dto.CourseQuestionBankSetResponse;
import com.cupk.academy.dto.QuestionBankImportResponse;
import com.cupk.academy.dto.QuestionBankProblemPageResponse;
import com.cupk.academy.dto.QuestionBankProblemResponse;
import com.cupk.academy.dto.QuestionBankSubjectResponse;
import com.cupk.academy.repository.QuestionBankRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QuestionBankService {
    private static final String LUOGU_SOURCE = "luogu";
    private static final String LUOGU_BASE_URL = "https://www.luogu.com.cn";
    private static final int MAX_IMPORT_PAGES = 3;
    private static final int MAX_IMPORT_PROBLEMS = 30;

    private final QuestionBankRepository questionBankRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Map<String, String> luoguCookies = new ConcurrentHashMap<>();

    public QuestionBankService(QuestionBankRepository questionBankRepository, ObjectMapper objectMapper) {
        this.questionBankRepository = questionBankRepository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public List<QuestionBankSubjectResponse> listSubjects() {
        return questionBankRepository.findSubjects();
    }

    public QuestionBankProblemPageResponse listProblems(
            String subject,
            String keyword,
            Integer difficulty,
            int page,
            int size
    ) {
        int normalizedSize = Math.max(1, Math.min(size, 50));
        return questionBankRepository.findProblems(subject, keyword, difficulty, Math.max(page, 0), normalizedSize);
    }

    public QuestionBankProblemResponse getProblem(long id) {
        return questionBankRepository.findProblemById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "题目不存在"));
    }

    public List<CourseQuestionBankCategoryResponse> listCourseQuestionBankCatalog() {
        return questionBankRepository.findCourseQuestionBankCatalog().stream()
                .map(category -> new CourseQuestionBankCategoryResponse(
                        category.code(),
                        category.name(),
                        category.description(),
                        category.sets().stream().map(this::withBankCover).toList()
                ))
                .toList();
    }

    public CourseQuestionBankDetailResponse getCourseQuestionBank(String code) {
        CourseQuestionBankSetResponse bank = questionBankRepository.findCourseQuestionBankSet(code)
                .map(this::withBankCover)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "课程题库不存在"));
        List<CourseQuestionBankQuestionResponse> questions = questionBankRepository.findCourseQuestionBankQuestions(code);
        return new CourseQuestionBankDetailResponse(bank, questions);
    }

    public QuestionBankImportResponse importLuoguProblems(int pages, int limit) {
        int normalizedPages = Math.max(1, Math.min(pages, MAX_IMPORT_PAGES));
        int normalizedLimit = Math.max(1, Math.min(limit, MAX_IMPORT_PROBLEMS));

        try {
            Map<Integer, String> tagNameMap = importLuoguTags();
            int imported = 0;
            for (int page = 1; page <= normalizedPages && imported < normalizedLimit; page += 1) {
                List<LuoguListProblem> problems = fetchLuoguProblemList(page, tagNameMap);
                for (LuoguListProblem problem : problems) {
                    if (imported >= normalizedLimit) {
                        break;
                    }
                    LuoguProblemDetail detail = fetchLuoguProblemDetail(problem.pid());
                    List<String> tagNames = problem.tagIds().stream()
                            .map(tagNameMap::get)
                            .filter(name -> name != null && !name.isBlank())
                            .toList();
                    long problemId = questionBankRepository.upsertProblem(
                            LUOGU_SOURCE,
                            problem.pid(),
                            problem.title(),
                            problem.difficulty(),
                            difficultyLabel(problem.difficulty()),
                            problem.tagIds(),
                            tagNames,
                            detail.description(),
                            detail.inputDescription(),
                            detail.outputDescription(),
                            detail.hint(),
                            problem.totalSubmit(),
                            problem.totalAccepted(),
                            LUOGU_BASE_URL + "/problem/" + problem.pid()
                    );
                    questionBankRepository.attachProblemToAllSubjects(problemId);
                    imported += 1;
                    sleepQuietly(120);
                }
            }

            return new QuestionBankImportResponse(
                    true,
                    imported,
                    tagNameMap.size(),
                    "已从洛谷导入 " + imported + " 道题，并挂载到 C语言、Java、Python 三个课程题库"
            );
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "访问洛谷失败：" + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "导入任务被中断", ex);
        }
    }

    private Map<Integer, String> importLuoguTags() throws IOException, InterruptedException {
        String body = get(LUOGU_BASE_URL + "/_lfe/tags/zh-CN");
        JsonNode root = objectMapper.readTree(body);
        Map<Integer, String> tagNameMap = new HashMap<>();
        JsonNode tags = root.path("tags");
        if (!tags.isArray()) {
            return tagNameMap;
        }

        for (JsonNode tag : tags) {
            int id = tag.path("id").asInt();
            String name = tag.path("name").asText("");
            Integer type = tag.hasNonNull("type") ? tag.path("type").asInt() : null;
            Integer parent = tag.hasNonNull("parent") ? tag.path("parent").asInt() : null;
            if (!name.isBlank()) {
                tagNameMap.put(id, name);
                questionBankRepository.upsertTag(LUOGU_SOURCE, id, name, type, parent);
            }
        }
        return tagNameMap;
    }

    private List<LuoguListProblem> fetchLuoguProblemList(int page, Map<Integer, String> tagNameMap)
            throws IOException, InterruptedException {
        String url = LUOGU_BASE_URL + "/problem/list?page=" + page;
        String body = get(url);
        Document document = Jsoup.parse(body);
        Element context = document.getElementById("lentille-context");
        if (context == null) {
            return List.of();
        }

        JsonNode result = objectMapper.readTree(context.html())
                .path("data")
                .path("problems")
                .path("result");
        if (!result.isArray()) {
            return List.of();
        }

        List<LuoguListProblem> problems = new ArrayList<>();
        for (JsonNode node : result) {
            String pid = node.path("pid").asText("");
            String title = node.path("name").asText("");
            if (pid.isBlank() || title.isBlank()) {
                continue;
            }
            List<Integer> tagIds = new ArrayList<>();
            JsonNode tags = node.path("tags");
            if (tags.isArray()) {
                for (JsonNode tag : tags) {
                    int tagId = tag.asInt();
                    if (tagNameMap.containsKey(tagId)) {
                        tagIds.add(tagId);
                    }
                }
            }
            problems.add(new LuoguListProblem(
                    pid,
                    title,
                    node.path("difficulty").isMissingNode() ? null : node.path("difficulty").asInt(),
                    tagIds,
                    node.path("totalSubmit").isMissingNode() ? null : node.path("totalSubmit").asInt(),
                    node.path("totalAccepted").isMissingNode() ? null : node.path("totalAccepted").asInt()
            ));
        }
        return problems;
    }

    private LuoguProblemDetail fetchLuoguProblemDetail(String pid) throws IOException, InterruptedException {
        String body = get(LUOGU_BASE_URL + "/problem/" + URLEncoder.encode(pid, StandardCharsets.UTF_8));
        Document document = Jsoup.parse(body);
        return new LuoguProblemDetail(
                sectionText(document, "题目描述"),
                sectionText(document, "输入格式"),
                sectionText(document, "输出格式"),
                sectionText(document, "说明/提示")
        );
    }

    private String sectionText(Document document, String heading) {
        for (Element section : document.select("article section")) {
            Element h2 = section.selectFirst("h2");
            if (h2 != null && heading.equals(h2.text().trim())) {
                Element div = section.selectFirst("div");
                return div == null ? "" : div.text().trim();
            }
        }
        return "";
    }

    private String get(String url) throws IOException, InterruptedException {
        String currentUrl = url;
        for (int redirectCount = 0; redirectCount < 5; redirectCount += 1) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(currentUrl))
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,application/json;q=0.8,*/*;q=0.7")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.7")
                    .header("Referer", LUOGU_BASE_URL + "/problem/list")
                    .GET();
            if (!luoguCookies.isEmpty()) {
                builder.header("Cookie", cookieHeader());
            }
            HttpRequest request = builder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            rememberCookies(response);
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }
            if (response.statusCode() >= 300 && response.statusCode() < 400) {
                currentUrl = response.headers()
                        .firstValue("Location")
                        .orElse(currentUrl);
                continue;
            }
            throw new IOException("HTTP " + response.statusCode());
        }
        throw new IOException("redirect limit exceeded");
    }

    private void rememberCookies(HttpResponse<?> response) {
        response.headers().allValues("Set-Cookie").forEach(cookie -> {
            String pair = cookie.split(";", 2)[0];
            int equalsIndex = pair.indexOf('=');
            if (equalsIndex <= 0) {
                return;
            }
            luoguCookies.put(pair.substring(0, equalsIndex), pair.substring(equalsIndex + 1));
        });
    }

    private String cookieHeader() {
        return luoguCookies.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + "; " + right)
                .orElse("");
    }

    private String difficultyLabel(Integer difficulty) {
        if (difficulty == null) {
            return "未知";
        }
        return switch (difficulty) {
            case 0 -> "暂无评定";
            case 1 -> "入门";
            case 2 -> "普及-";
            case 3 -> "普及/提高-";
            case 4 -> "提高+/省选-";
            case 5 -> "省选/NOI-";
            case 6 -> "NOI/NOI+/CTSC";
            case 7 -> "高难";
            default -> "未知";
        };
    }

    private void sleepQuietly(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    private CourseQuestionBankSetResponse withBankCover(CourseQuestionBankSetResponse bank) {
        String localCoverUrl = fileUrl(bank.coverFilePath());
        String resolvedCoverUrl = localCoverUrl.isBlank() ? bank.coverUrl() : localCoverUrl;
        return new CourseQuestionBankSetResponse(
                bank.id(),
                bank.code(),
                bank.title(),
                bank.subtitle(),
                bank.description(),
                bank.categoryCode(),
                bank.categoryName(),
                resolvedCoverUrl,
                bank.fallbackCoverUrl(),
                bank.coverFilePath(),
                bank.questionCount(),
                bank.difficultyLabel(),
                bank.statusLabel(),
                bank.sourceName(),
                bank.sourceUrl(),
                bank.sourceRefs(),
                bank.routePath()
        );
    }

    private String fileUrl(String coverFilePath) {
        if (coverFilePath == null || coverFilePath.isBlank()) {
            return "";
        }
        String normalizedPath = coverFilePath.replace("\\", "/");
        if (normalizedPath.startsWith("storage/")) {
            normalizedPath = normalizedPath.substring("storage/".length());
        }
        String encodedPath = java.util.Arrays.stream(normalizedPath.split("/"))
                .map(part -> URLEncoder.encode(part, StandardCharsets.UTF_8).replace("+", "%20"))
                .reduce((left, right) -> left + "/" + right)
                .orElse("");
        return "/files/" + encodedPath;
    }

    private record LuoguListProblem(
            String pid,
            String title,
            Integer difficulty,
            List<Integer> tagIds,
            Integer totalSubmit,
            Integer totalAccepted
    ) {
    }

    private record LuoguProblemDetail(
            String description,
            String inputDescription,
            String outputDescription,
            String hint
    ) {
    }
}
