package com.cupk.games.service;

import com.cupk.games.dto.LadderJumpQuestionResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 提供平台跳跃小游戏使用的内置题库。
 */
@Service
public class LadderJumpQuestionService {
    private final List<LadderJumpQuestionResponse> questions = List.of(
            new LadderJumpQuestionResponse(
                    1L,
                    "Java 中用于声明类继承关系的关键字是？",
                    List.of("extends", "implements", "instanceof"),
                    0,
                    "extends 表示一个类继承另一个类。"
            ),
            new LadderJumpQuestionResponse(
                    2L,
                    "Vue 3 组合式 API 中用于创建响应式引用的是？",
                    List.of("ref", "map", "bind"),
                    0,
                    "ref 可以创建一个响应式引用值。"
            ),
            new LadderJumpQuestionResponse(
                    3L,
                    "HTTP 状态码 404 通常表示？",
                    List.of("请求成功", "资源不存在", "服务器重启"),
                    1,
                    "404 表示客户端请求的资源没有找到。"
            ),
            new LadderJumpQuestionResponse(
                    4L,
                    "数据库主键的主要作用是？",
                    List.of("唯一标识记录", "压缩表空间", "自动备份数据"),
                    0,
                    "主键用于唯一标识表中的一条记录。"
            ),
            new LadderJumpQuestionResponse(
                    5L,
                    "CSS 中控制元素层叠顺序的属性是？",
                    List.of("display", "z-index", "overflow"),
                    1,
                    "z-index 用于控制定位元素的层叠顺序。"
            ),
            new LadderJumpQuestionResponse(
                    6L,
                    "算法复杂度 O(log n) 常见于哪类操作？",
                    List.of("二分查找", "顺序遍历", "冒泡排序"),
                    0,
                    "二分查找每次缩小一半搜索范围。"
            ),
            new LadderJumpQuestionResponse(
                    7L,
                    "Spring Boot 中声明 REST 控制器常用注解是？",
                    List.of("@Service", "@Repository", "@RestController"),
                    2,
                    "@RestController 用于声明返回数据的控制器。"
            ),
            new LadderJumpQuestionResponse(
                    8L,
                    "JavaScript 中数组末尾追加元素的方法是？",
                    List.of("push", "shift", "slice"),
                    0,
                    "push 会把元素追加到数组末尾。"
            )
    );

    public List<LadderJumpQuestionResponse> listQuestions() {
        return questions;
    }
}
