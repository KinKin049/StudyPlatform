package com.cupk.academy.service;

import com.cupk.academy.dto.CourseQuestionBankCategoryResponse;
import com.cupk.academy.dto.CourseQuestionBankDetailResponse;
import com.cupk.academy.dto.CourseQuestionBankQuestionPageResponse;
import com.cupk.academy.dto.CourseQuestionBankQuestionResponse;
import com.cupk.academy.dto.CourseQuestionBankSetResponse;
import com.cupk.academy.dto.QuestionBankFavoritePageResponse;
import com.cupk.academy.dto.QuestionBankFavoriteRequest;
import com.cupk.academy.dto.QuestionBankFavoriteSummaryResponse;
import com.cupk.academy.dto.QuestionBankFavoriteToggleResponse;
import com.cupk.academy.dto.QuestionBankMistakeAnswerRequest;
import com.cupk.academy.dto.QuestionBankMistakeAnswerResponse;
import com.cupk.academy.dto.QuestionBankMistakePageResponse;
import com.cupk.academy.dto.QuestionBankMistakeSummaryResponse;
import com.cupk.academy.dto.QuestionBankImportResponse;
import com.cupk.academy.dto.QuestionBankProblemPageResponse;
import com.cupk.academy.dto.QuestionBankProblemResponse;
import com.cupk.academy.dto.QuestionBankSubjectResponse;
import com.cupk.academy.dto.TypeWarriorWordPoolResponse;
import com.cupk.academy.dto.TypeWarriorWordResponse;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 题库服务，提供题库管理、题目查询、错题本、收藏夹和类型战士等功能。
 */
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

    /**
     * 构造函数，注入依赖组件并初始化HTTP客户端。
     *
     * @param questionBankRepository 题库数据访问层
     * @param objectMapper JSON映射器
     */
    public QuestionBankService(QuestionBankRepository questionBankRepository, ObjectMapper objectMapper) {
        this.questionBankRepository = questionBankRepository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /**
     * 获取科目列表。
     *
     * @return 科目列表
     */
    public List<QuestionBankSubjectResponse> listSubjects() {
        return questionBankRepository.findSubjects();
    }

    /**
     * 分页查询题目列表。支持科目、关键词和难度筛选。
     *
     * @param subject 科目
     * @param keyword 关键词
     * @param difficulty 难度
     * @param page 页码
     * @param size 每页数量
     * @return 题目分页响应
     */
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

    /**
     * 获取题目详情。
     *
     * @param id 题目ID
     * @return 题目详情
     */
    public QuestionBankProblemResponse getProblem(long id) {
        return questionBankRepository.findProblemById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "题目不存在"));
    }

    /**
     * 获取课程题库分类目录。包含分类信息和所属题库集合。
     *
     * @return 题库分类目录列表
     */
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

    /**
     * 获取课程题库详情。包含题库信息和分页题目列表。
     *
     * @param code 题库编号
     * @param page 页码
     * @param size 每页数量
     * @param keyword 关键词
     * @param userId 用户ID
     * @return 题库详情
     */
    public CourseQuestionBankDetailResponse getCourseQuestionBank(String code, int page, int size, String keyword, long userId) {
        CourseQuestionBankSetResponse bank = questionBankRepository.findCourseQuestionBankSet(code)
                .map(this::withBankCover)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "课程题库不存在"));
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.max(1, Math.min(size, 100));
        CourseQuestionBankQuestionPageResponse questionPage =
                questionBankRepository.findCourseQuestionBankQuestions(
                        code,
                        keyword,
                        normalizedPage,
                        normalizedSize,
                        userId
                );
        return new CourseQuestionBankDetailResponse(
                bank,
                questionPage.items(),
                questionPage.page(),
                questionPage.size(),
                questionPage.total(),
                questionPage.totalPages()
        );
    }

    /**
     * 获取类型战士词库。聚合用户的词汇题目，根据熟悉度和难度分级。
     *
     * @param userId 用户ID
     * @return 词库响应
     */
    public TypeWarriorWordPoolResponse getTypeWarriorWordPool(long userId) {
        Map<String, AggregatedTypeWarriorWord> wordsByKeyword = new LinkedHashMap<>();
        for (QuestionBankRepository.TypeWarriorVocabularyRow row : questionBankRepository.findTypeWarriorVocabularyRows(userId)) {
            String normalizedWord = normalizeTypeWarriorWord(row.stem());
            if (normalizedWord.isBlank()) {
                continue;
            }

            String familiarity = normalizeFamiliarity(row.familiarity());
            AggregatedTypeWarriorWord existing = wordsByKeyword.get(normalizedWord);
            if (existing == null) {
                wordsByKeyword.put(normalizedWord, new AggregatedTypeWarriorWord(
                        row.questionId(),
                        row.setCode(),
                        normalizedWord,
                        summarizeTranslation(row.answer()),
                        familiarity,
                        inferTypeWarriorTier(normalizedWord, row.setCode())
                ));
                continue;
            }

            String mergedFamiliarity = familiarityRank(familiarity) > familiarityRank(existing.familiarity())
                    ? familiarity
                    : existing.familiarity();
            String mergedText = existing.text().isBlank() ? summarizeTranslation(row.answer()) : existing.text();
            wordsByKeyword.put(normalizedWord, new AggregatedTypeWarriorWord(
                    existing.questionId(),
                    existing.setCode(),
                    existing.word(),
                    mergedText,
                    mergedFamiliarity,
                    Math.max(existing.tier(), inferTypeWarriorTier(normalizedWord, row.setCode()))
            ));
        }

        List<TypeWarriorWordResponse> words = wordsByKeyword.values().stream()
                .map(word -> new TypeWarriorWordResponse(
                        word.questionId(),
                        word.setCode(),
                        word.word(),
                        word.text(),
                        word.familiarity(),
                        word.tier()
                ))
                .toList();
        return new TypeWarriorWordPoolResponse(words);
    }

    /**
     * 获取错题本摘要。
     *
     * @param userId 用户ID
     * @return 错题本摘要
     */
    public QuestionBankMistakeSummaryResponse getMistakeSummary(long userId) {
        return questionBankRepository.findMistakeSummary(userId);
    }

    /**
     * 分页查询错题列表。支持题库编号、状态和关键词筛选。
     *
     * @param userId 用户ID
     * @param setCode 题库编号
     * @param status 状态
     * @param keyword 关键词
     * @param page 页码
     * @param size 每页数量
     * @return 错题分页响应
     */
    public QuestionBankMistakePageResponse listMistakes(
            long userId,
            String setCode,
            String status,
            String keyword,
            int page,
            int size
    ) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.max(1, Math.min(size, 100));
        return questionBankRepository.findMistakes(
                userId,
                setCode,
                status,
                keyword,
                normalizedPage,
                normalizedSize
        );
    }

    /**
     * 记录错题回答。验证答案正确性，更新错题状态，连续答对达到阈值后标记掌握。
     *
     * @param userId 用户ID
     * @param request 错题回答请求
     * @return 错题回答响应
     */
    public QuestionBankMistakeAnswerResponse recordMistakeAnswer(long userId, QuestionBankMistakeAnswerRequest request) {
        if (request == null || request.questionId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "题目编号无效");
        }
        String selectedAnswer = request.selectedAnswer() == null ? "" : request.selectedAnswer().trim();
        if (selectedAnswer.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "答案不能为空");
        }

        QuestionBankRepository.CourseQuestionAnswerReference reference =
                questionBankRepository.findCourseQuestionAnswerReference(request.questionId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "题目不存在"));
        String correctAnswer = reference.answer() == null ? "" : reference.answer();
        boolean correct = isCourseQuestionAnswerCorrect(reference.questionType(), selectedAnswer, correctAnswer);
        int masteredThreshold = 2;

        if (correct) {
            questionBankRepository.applyCorrectMistakeReview(
                    userId,
                    reference.questionId(),
                    selectedAnswer,
                    correctAnswer,
                    masteredThreshold
            );
        } else {
            questionBankRepository.upsertWrongMistake(
                    userId,
                    reference.questionId(),
                    selectedAnswer,
                    correctAnswer
            );
        }

        QuestionBankRepository.QuestionBankMistakeState state =
                questionBankRepository.findMistakeState(userId, reference.questionId())
                        .orElse(new QuestionBankRepository.QuestionBankMistakeState(0, 0, false));
        boolean inMistakeBook = state.wrongCount() > 0 && !state.mastered();
        String message;
        if (!correct) {
            message = "已加入错题本";
        } else if (state.mastered()) {
            message = "连续答对，已标记掌握";
        } else if (state.wrongCount() > 0) {
            message = "回答正确，再答对一次即可掌握";
        } else {
            message = "回答正确";
        }
        return new QuestionBankMistakeAnswerResponse(
                reference.questionId(),
                correct,
                inMistakeBook,
                state.wrongCount(),
                state.correctStreak(),
                state.mastered(),
                message
        );
    }

    /**
     * 获取收藏夹摘要。
     *
     * @param userId 用户ID
     * @return 收藏夹摘要
     */
    public QuestionBankFavoriteSummaryResponse getFavoriteSummary(long userId) {
        return questionBankRepository.findFavoriteSummary(userId);
    }

    /**
     * 分页查询收藏题目列表。支持题库编号和关键词筛选。
     *
     * @param userId 用户ID
     * @param setCode 题库编号
     * @param keyword 关键词
     * @param page 页码
     * @param size 每页数量
     * @return 收藏分页响应
     */
    public QuestionBankFavoritePageResponse listFavorites(long userId, String setCode, String keyword, int page, int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.max(1, Math.min(size, 100));
        return questionBankRepository.findFavorites(userId, setCode, keyword, normalizedPage, normalizedSize);
    }

    /**
     * 添加题目到收藏夹。验证题目存在后添加。
     *
     * @param userId 用户ID
     * @param request 收藏请求
     * @return 收藏切换响应
     */
    public QuestionBankFavoriteToggleResponse addFavorite(long userId, QuestionBankFavoriteRequest request) {
        long questionId = validateFavoriteQuestion(request);
        questionBankRepository.addFavorite(userId, questionId);
        return new QuestionBankFavoriteToggleResponse(
                questionId,
                true,
                questionBankRepository.countFavorites(userId),
                "已收藏题目"
        );
    }

    /**
     * 取消题目收藏。
     *
     * @param userId 用户ID
     * @param questionId 题目ID
     * @return 收藏切换响应
     */
    public QuestionBankFavoriteToggleResponse removeFavorite(long userId, long questionId) {
        if (questionId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "题目编号无效");
        }
        questionBankRepository.removeFavorite(userId, questionId);
        return new QuestionBankFavoriteToggleResponse(
                questionId,
                false,
                questionBankRepository.countFavorites(userId),
                "已取消收藏"
        );
    }

    /**
     * 从洛谷平台导入题目。先导入标签，再分页抓取题目详情并存储到数据库，自动挂载到科目。
     *
     * @param pages 导入页数
     * @param limit 最大导入数量
     * @return 导入结果响应
     */
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

    /**
     * 导入洛谷标签数据。获取标签列表并存储到数据库。
     *
     * @return 标签ID到名称的映射
     */
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

    /**
     * 验证收藏题目是否存在。
     *
     * @param request 收藏请求
     * @return 题目ID
     */
    private long validateFavoriteQuestion(QuestionBankFavoriteRequest request) {
        if (request == null || request.questionId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "题目编号无效");
        }
        return questionBankRepository.findCourseQuestionAnswerReference(request.questionId())
                .map(QuestionBankRepository.CourseQuestionAnswerReference::questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "题目不存在"));
    }

    /**
     * 判断课程题目的答案是否正确。支持词汇类题目和普通选择题。
     *
     * @param type 题目类型
     * @param selectedAnswer 用户选择的答案
     * @param correctAnswer 正确答案
     * @return 是否正确
     */
    private boolean isCourseQuestionAnswerCorrect(String type, String selectedAnswer, String correctAnswer) {
        if ("vocabulary".equalsIgnoreCase(type)) {
            return "known".equalsIgnoreCase(selectedAnswer);
        }
        List<String> selectedKeys = normalizeAnswerKeys(selectedAnswer);
        List<String> correctKeys = normalizeAnswerKeys(correctAnswer);
        return !selectedKeys.isEmpty()
                && !correctKeys.isEmpty()
                && selectedKeys.size() == correctKeys.size()
                && selectedKeys.containsAll(correctKeys)
                && correctKeys.containsAll(selectedKeys);
    }

    /**
     * 标准化答案键。支持多种分隔符，将连续字母拆分为单个字母。
     *
     * @param answer 答案字符串
     * @return 标准化的答案键列表
     */
    private List<String> normalizeAnswerKeys(String answer) {
        if (answer == null || answer.isBlank()) {
            return List.of();
        }
        String[] parts = answer.trim().toUpperCase().split("[,，、\\s]+");
        List<String> keys = new ArrayList<>();
        for (String part : parts) {
            String key = part.trim();
            if (!key.isBlank()) {
                keys.add(key);
            }
        }
        if (keys.size() == 1 && keys.get(0).matches("[A-Z]{2,}")) {
            String compact = keys.get(0);
            List<String> splitKeys = new ArrayList<>();
            for (int index = 0; index < compact.length(); index += 1) {
                splitKeys.add(String.valueOf(compact.charAt(index)));
            }
            return splitKeys;
        }
        return keys;
    }

    /**
     * 获取洛谷题目列表页面。解析HTML提取题目基本信息。
     *
     * @param page 页码
     * @param tagNameMap 标签映射
     * @return 题目列表
     */
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

    /**
     * 获取洛谷题目详情。解析HTML提取题目描述、输入输出格式和提示。
     *
     * @param pid 题目ID
     * @return 题目详情
     */
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

    /**
     * 从HTML文档中提取指定标题的章节内容。
     *
     * @param document HTML文档
     * @param heading 章节标题
     * @return 章节文本内容
     */
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

    /**
     * 标准化类型战士词汇。转小写并移除非字母字符。
     *
     * @param word 原始词汇
     * @return 标准化后的词汇
     */
    private String normalizeTypeWarriorWord(String word) {
        if (word == null) {
            return "";
        }
        return word.toLowerCase().replaceAll("[^a-z]", "");
    }

    /**
     * 摘要翻译内容。截取第一个逗号前的内容作为摘要。
     *
     * @param answer 答案/翻译内容
     * @return 摘要内容
     */
    private String summarizeTranslation(String answer) {
        if (answer == null || answer.isBlank()) {
            return "";
        }
        String normalized = answer.replace("；", "，").replace(";", "，");
        int separatorIndex = normalized.indexOf('，');
        return (separatorIndex >= 0 ? normalized.substring(0, separatorIndex) : normalized).trim();
    }

    /**
     * 标准化熟悉度标签。将输入转换为标准值：known、fuzzy、unknown、unmarked。
     *
     * @param familiarity 原始熟悉度
     * @return 标准化后的熟悉度
     */
    private String normalizeFamiliarity(String familiarity) {
        if (familiarity == null || familiarity.isBlank()) {
            return "unmarked";
        }
        String normalized = familiarity.trim().toLowerCase();
        return switch (normalized) {
            case "known", "fuzzy", "unknown" -> normalized;
            default -> "unmarked";
        };
    }

    /**
     * 获取熟悉度等级。用于比较熟悉度高低，数值越大越不熟悉。
     *
     * @param familiarity 熟悉度
     * @return 等级数值
     */
    private int familiarityRank(String familiarity) {
        return switch (normalizeFamiliarity(familiarity)) {
            case "unknown" -> 4;
            case "fuzzy" -> 3;
            case "known" -> 2;
            default -> 1;
        };
    }

    /**
     * 推断类型战士词汇等级。根据词汇长度和题库编号计算难度等级。
     *
     * @param normalizedWord 标准化后的词汇
     * @param setCode 题库编号
     * @return 等级(1-4)
     */
    private int inferTypeWarriorTier(String normalizedWord, String setCode) {
        int length = normalizedWord.length();
        int tier;
        if (length <= 4) {
            tier = 1;
        } else if (length <= 7) {
            tier = 2;
        } else if (length <= 10) {
            tier = 3;
        } else {
            tier = 4;
        }
        if ("cet6".equalsIgnoreCase(setCode) && tier < 4) {
            tier += 1;
        }
        return Math.max(1, Math.min(4, tier));
    }

    /**
     * 发送HTTP GET请求。支持重定向和Cookie管理。
     *
     * @param url 请求URL
     * @return 响应体内容
     */
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

    /**
     * 保存HTTP响应中的Cookie。从Set-Cookie头中提取键值对。
     *
     * @param response HTTP响应
     */
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

    /**
     * 构建Cookie请求头。将存储的Cookie转换为请求头格式。
     *
     * @return Cookie头字符串
     */
    private String cookieHeader() {
        return luoguCookies.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + "; " + right)
                .orElse("");
    }

    /**
     * 将难度数值转换为中文标签。
     *
     * @param difficulty 难度数值
     * @return 中文难度标签
     */
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

    /**
     * 安静地休眠指定毫秒数。封装Thread.sleep以提高代码可读性。
     *
     * @param millis 休眠毫秒数
     */
    private void sleepQuietly(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    /**
     * 为题库集合设置封面URL。优先使用本地文件路径，否则使用远程URL。
     *
     * @param bank 题库集合响应
     * @return 更新封面URL后的响应
     */
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

    private record AggregatedTypeWarriorWord(
            long questionId,
            String setCode,
            String word,
            String text,
            String familiarity,
            int tier
    ) {
    }
}
