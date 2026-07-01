package com.cupk.welllog.service;

import com.cupk.welllog.model.WellLogTemplate;
import com.cupk.welllog.repository.WellLogTemplateRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 测井模板服务层。
 * 封装模板查询与未找到处理，不解析或计算模板中的曲线数据。
 */
@Service
public class WellLogTemplateService {
    private final WellLogTemplateRepository templateRepository;

    public WellLogTemplateService(WellLogTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    /**
     * 查询所有测井基础模板。
     */
    public List<WellLogTemplate> listTemplates() {
        return templateRepository.findAll();
    }

    /**
     * 查询单个测井基础模板；不存在时返回 404。
     */
    public WellLogTemplate getTemplate(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Well log template not found"));
    }
}
