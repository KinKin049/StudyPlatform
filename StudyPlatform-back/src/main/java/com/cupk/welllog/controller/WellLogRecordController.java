package com.cupk.welllog.controller;

import com.cupk.welllog.dto.SaveWellLogRecordRequest;
import com.cupk.welllog.dto.WellLogRecordPage;
import com.cupk.welllog.model.WellLogRecord;
import com.cupk.welllog.service.WellLogRecordService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测井仿真记录接口。
 * 接收前端生成的仿真参数与解释报告，仅负责记录的保存、查询和删除。
 */
@RestController
@RequestMapping("/api/well-log/record")
public class WellLogRecordController {
    private final WellLogRecordService recordService;

    /**
     * 构造函数。
     * @param recordService 测井记录服务
     */
    public WellLogRecordController(WellLogRecordService recordService) {
        this.recordService = recordService;
    }

    /**
     * 保存一次测井仿真报告记录。
     * @param request 保存请求
     * @return 保存后的记录
     */
    @PostMapping("/save")
    @ResponseStatus(HttpStatus.CREATED)
    public WellLogRecord saveRecord(@Valid @RequestBody SaveWellLogRecordRequest request) {
        return recordService.saveRecord(request);
    }

    /**
     * 分页查询测井仿真历史；userId为空时查询匿名记录。
     * @param userId 用户ID（可选）
     * @param page 页码（默认1）
     * @param size 每页数量（默认10）
     * @return 分页结果
     */
    @GetMapping("/page")
    public WellLogRecordPage pageRecords(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return recordService.pageRecords(userId, page, size);
    }

    /**
     * 根据记录主键查询单条报告详情。
     * @param id 记录ID
     * @return 记录详情
     */
    @GetMapping("/{id}")
    public WellLogRecord getRecord(@PathVariable Long id) {
        return recordService.getRecord(id);
    }

    /**
     * 删除指定测井仿真记录。
     * @param id 记录ID
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRecord(@PathVariable Long id) {
        recordService.deleteRecord(id);
    }
}
