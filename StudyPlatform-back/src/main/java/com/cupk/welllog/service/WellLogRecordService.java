package com.cupk.welllog.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cupk.welllog.dto.SaveWellLogRecordRequest;
import com.cupk.welllog.dto.WellLogRecordPage;
import com.cupk.welllog.model.WellLogRecord;
import com.cupk.welllog.repository.WellLogRecordRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 测井仿真记录服务层。
 * 仅处理记录 CRUD、分页边界和报告 JSON 校验，不执行曲线计算。
 */
@Service
public class WellLogRecordService {
    private final WellLogRecordRepository recordRepository;
    private final ObjectMapper objectMapper;

    public WellLogRecordService(WellLogRecordRepository recordRepository, ObjectMapper objectMapper) {
        this.recordRepository = recordRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 校验报告 JSON 后保存记录，并返回数据库中的完整记录。
     */
    @Transactional
    public WellLogRecord saveRecord(SaveWellLogRecordRequest request) {
        validateReportJson(request.reportJson());
        Long id = recordRepository.create(request);
        return getRecord(id);
    }

    /**
     * 查询分页历史记录，并对页码和每页数量做边界保护。
     */
    public WellLogRecordPage pageRecords(Long userId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return new WellLogRecordPage(
                recordRepository.countByUserId(userId),
                safePage,
                safeSize,
                recordRepository.findByUserId(userId, safePage, safeSize)
        );
    }

    /**
     * 查询单条记录；不存在时返回 404。
     */
    public WellLogRecord getRecord(Long id) {
        return recordRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Well log record not found"));
    }

    /**
     * 删除单条记录；不存在时返回 404。
     */
    @Transactional
    public void deleteRecord(Long id) {
        int deleted = recordRepository.deleteById(id);
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Well log record not found");
        }
    }

    /**
     * 确保前端提交的解释报告是合法 JSON，便于后续检索和展示。
     */
    private void validateReportJson(String reportJson) {
        try {
            objectMapper.readTree(reportJson);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reportJson must be valid JSON");
        }
    }
}
