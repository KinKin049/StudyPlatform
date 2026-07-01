package com.cupk.welllog.controller;

import com.cupk.welllog.model.WellLogTemplate;
import com.cupk.welllog.service.WellLogTemplateService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测井基础模板接口。
 * 提供前端可加载的深度序列和基础 GR 曲线数据。
 */
@RestController
@RequestMapping("/api/well-log/template")
public class WellLogTemplateController {
    private final WellLogTemplateService templateService;

    public WellLogTemplateController(WellLogTemplateService templateService) {
        this.templateService = templateService;
    }

    /**
     * 查询全部测井模板，供前端模板选择列表使用。
     */
    @GetMapping("/list")
    public List<WellLogTemplate> listTemplates() {
        return templateService.listTemplates();
    }

    /**
     * 根据模板主键查询模板详情。
     */
    @GetMapping("/{id}")
    public WellLogTemplate getTemplate(@PathVariable Long id) {
        return templateService.getTemplate(id);
    }
}
