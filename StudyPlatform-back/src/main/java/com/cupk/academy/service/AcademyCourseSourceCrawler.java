package com.cupk.academy.service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AcademyCourseSourceCrawler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcademyCourseSourceCrawler.class);
    private static final int REQUEST_TIMEOUT_MILLIS = 12_000;
    private static final Pattern OUTLINE_ITEM_PATTERN = Pattern.compile(
            "name\\s*:\\s*\"([^\"]*)\"\\s*,\\s*goals\\s*:\\s*\"([^\"]*)\"\\s*,\\s*plan\\s*:\\s*\"([^\"]*)\"",
            Pattern.DOTALL
    );

    public Optional<AcademyCourseSourceContent> crawl(String sourceUrl) {
        if (!hasText(sourceUrl) || !sourceUrl.startsWith("http")) {
            return Optional.empty();
        }

        try {
            Document document = Jsoup.connect(sourceUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                            + "(KHTML, like Gecko) Chrome/126.0 Safari/537.36")
                    .referrer("https://www.icourse163.org/")
                    .timeout(REQUEST_TIMEOUT_MILLIS)
                    .followRedirects(true)
                    .get();
            return Optional.of(parse(document));
        } catch (IOException ex) {
            LOGGER.warn("Failed to crawl online course source: {}", sourceUrl, ex);
            return Optional.empty();
        }
    }

    private AcademyCourseSourceContent parse(Document document) {
        String introduction = extractIntroduction(document);
        Map<String, String> detailSections = new LinkedHashMap<>();
        putIfPresent(detailSections, "课程概述", extractSectionByTitle(document, "课程概述"));
        putIfPresent(detailSections, "授课目标", extractSectionByTitle(document, "授课目标"));

        String htmlOutline = extractSectionByTitle(document, "课程大纲");
        String scriptOutline = extractOutlineFromScripts(document);
        putIfPresent(detailSections, "课程大纲", hasText(htmlOutline) ? htmlOutline : scriptOutline);

        putIfPresent(detailSections, "预备知识", extractSectionByTitle(document, "预备知识"));
        putIfPresent(detailSections, "参考资料", extractSectionByTitle(document, "参考资料"));

        String overview = detailSections.getOrDefault("课程概述", introduction);
        String detail = buildDetail(introduction, detailSections);
        return new AcademyCourseSourceContent(introduction, overview, detail);
    }

    private String extractIntroduction(Document document) {
        Element introductionElement = document.selectFirst("#j-rectxt");
        String introduction = introductionElement == null ? "" : introductionElement.text();
        if (!hasText(introduction)) {
            Element descriptionMeta = document.selectFirst("meta[name=description]");
            introduction = descriptionMeta == null ? "" : descriptionMeta.attr("content");
        }
        introduction = stripBefore(introduction, "spContent=");
        introduction = stripAfter(introduction, ",中国大学MOOC");
        return normalizeText(introduction);
    }

    private String extractSectionByTitle(Document document, String sectionTitle) {
        for (Element titleElement : document.select(".category-title")) {
            if (!normalizeText(titleElement.text()).contains(sectionTitle)) {
                continue;
            }

            Element sectionElement = titleElement.nextElementSibling();
            while (sectionElement != null && !sectionElement.hasClass("category-title")) {
                if (sectionElement.hasClass("category-content")) {
                    return extractRichText(sectionElement);
                }
                sectionElement = sectionElement.nextElementSibling();
            }
        }
        return "";
    }

    private String extractRichText(Element element) {
        Element content = element.clone();
        content.select("script, style").remove();

        StringBuilder builder = new StringBuilder();
        for (Element textElement : content.select("p, li")) {
            String text = normalizeText(textElement.text());
            if (hasText(text)) {
                appendLine(builder, text);
            }
        }

        if (builder.isEmpty()) {
            appendLine(builder, normalizeText(content.text()));
        }
        return builder.toString().trim();
    }

    private String extractOutlineFromScripts(Document document) {
        Matcher matcher = OUTLINE_ITEM_PATTERN.matcher(document.html());
        StringBuilder builder = new StringBuilder();
        int chapterIndex = 1;
        while (matcher.find()) {
            String chapterName = normalizeText(unescapeScriptValue(matcher.group(1)));
            String goals = normalizeText(unescapeScriptValue(matcher.group(2)));
            String plan = normalizeText(unescapeScriptValue(matcher.group(3)).replace(";;;", "\n"));
            if (!hasText(chapterName) && !hasText(plan)) {
                continue;
            }

            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(chapterIndex++).append(". ").append(chapterName);
            if (hasText(goals)) {
                builder.append('\n').append("目标：").append(goals);
            }
            for (String item : plan.split("\\n+")) {
                String normalizedItem = normalizeText(item);
                if (hasText(normalizedItem)) {
                    builder.append('\n').append("- ").append(normalizedItem);
                }
            }
        }
        return builder.toString().trim();
    }

    private String buildDetail(String introduction, Map<String, String> detailSections) {
        StringBuilder builder = new StringBuilder();
        if (hasText(introduction)) {
            appendSection(builder, "课程简介", introduction);
        }
        detailSections.forEach((sectionTitle, sectionText) -> appendSection(builder, sectionTitle, sectionText));
        return builder.toString().trim();
    }

    private void putIfPresent(Map<String, String> sections, String title, String text) {
        String normalizedText = normalizeText(text);
        if (hasText(normalizedText)) {
            sections.put(title, normalizedText);
        }
    }

    private void appendSection(StringBuilder builder, String title, String text) {
        if (!hasText(text)) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append("\n\n");
        }
        builder.append(title).append('\n').append(text.trim());
    }

    private void appendLine(StringBuilder builder, String text) {
        if (!hasText(text)) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append('\n');
        }
        builder.append(text);
    }

    private String stripBefore(String text, String marker) {
        if (!hasText(text)) {
            return "";
        }
        int markerIndex = text.indexOf(marker);
        return markerIndex >= 0 ? text.substring(markerIndex + marker.length()) : text;
    }

    private String stripAfter(String text, String marker) {
        if (!hasText(text)) {
            return "";
        }
        int markerIndex = text.indexOf(marker);
        return markerIndex >= 0 ? text.substring(0, markerIndex) : text;
    }

    private String unescapeScriptValue(String value) {
        if (value == null) {
            return "";
        }
        String unescaped = value
                .replace("\\\"", "\"")
                .replace("\\/", "/")
                .replace("\\n", "\n")
                .replace("\\r", "\n")
                .replace("\\t", " ");
        return Parser.unescapeEntities(unescaped, false);
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace('\u00a0', ' ')
                .replace('\u3000', ' ')
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll(" *\\n+ *", "\n")
                .trim();
    }

    private boolean hasText(String text) {
        return text != null && !text.isBlank();
    }

    public record AcademyCourseSourceContent(
            String introduction,
            String overview,
            String detail
    ) {
    }
}
