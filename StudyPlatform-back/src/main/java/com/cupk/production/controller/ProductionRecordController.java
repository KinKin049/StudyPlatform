package com.cupk.production.controller;

import com.cupk.production.dto.ProductionPage;
import com.cupk.production.dto.SavePumpRecordRequest;
import com.cupk.production.dto.SaveReservoirRecordRequest;
import com.cupk.production.dto.SaveStimulationRecordRequest;
import com.cupk.production.dto.SaveWaterfloodRecordRequest;
import com.cupk.production.model.ProductionPumpRecord;
import com.cupk.production.model.ProductionReservoirRecord;
import com.cupk.production.model.ProductionStimulationRecord;
import com.cupk.production.model.ProductionWaterfloodRecord;
import com.cupk.production.service.ProductionRecordService;
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
 * 采油生产仿真记录接口。
 * 四个子模块统一在 /api/production 下暴露 CRUD 接口。
 */
@RestController
@RequestMapping("/api/production")
public class ProductionRecordController {
    private final ProductionRecordService recordService;

    /**
     * 构造函数。
     * @param recordService 生产记录服务
     */
    public ProductionRecordController(ProductionRecordService recordService) {
        this.recordService = recordService;
    }

    /**
     * 保存抽油机仿真记录。
     * @param request 保存请求
     * @return 保存后的记录
     */
    @PostMapping("/pump/save")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductionPumpRecord savePumpRecord(@Valid @RequestBody SavePumpRecordRequest request) {
        return recordService.savePumpRecord(request);
    }

    /**
     * 分页查询抽油机仿真记录。
     * @param userId 用户ID（可选）
     * @param page 页码（默认1）
     * @param size 每页数量（默认10）
     * @return 分页结果
     */
    @GetMapping("/pump/page")
    public ProductionPage<ProductionPumpRecord> pagePumpRecords(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return recordService.pagePumpRecords(userId, page, size);
    }

    /**
     * 查询单条抽油机仿真记录。
     * @param id 记录ID
     * @return 记录详情
     */
    @GetMapping("/pump/{id}")
    public ProductionPumpRecord getPumpRecord(@PathVariable Long id) {
        return recordService.getPumpRecord(id);
    }

    @DeleteMapping("/pump/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePumpRecord(@PathVariable Long id) {
        recordService.deletePumpRecord(id);
    }

    /**
     * 保存油藏仿真记录。
     * @param request 保存请求
     * @return 保存后的记录
     */
    @PostMapping("/reservoir/save")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductionReservoirRecord saveReservoirRecord(@Valid @RequestBody SaveReservoirRecordRequest request) {
        return recordService.saveReservoirRecord(request);
    }

    /**
     * 分页查询油藏仿真记录。
     * @param userId 用户ID（可选）
     * @param page 页码（默认1）
     * @param size 每页数量（默认10）
     * @return 分页结果
     */
    @GetMapping("/reservoir/page")
    public ProductionPage<ProductionReservoirRecord> pageReservoirRecords(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return recordService.pageReservoirRecords(userId, page, size);
    }

    /**
     * 查询单条油藏仿真记录。
     * @param id 记录ID
     * @return 记录详情
     */
    @GetMapping("/reservoir/{id}")
    public ProductionReservoirRecord getReservoirRecord(@PathVariable Long id) {
        return recordService.getReservoirRecord(id);
    }

    /**
     * 删除油藏仿真记录。
     * @param id 记录ID
     */
    @DeleteMapping("/reservoir/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReservoirRecord(@PathVariable Long id) {
        recordService.deleteReservoirRecord(id);
    }

    /**
     * 保存注水开发仿真记录。
     * @param request 保存请求
     * @return 保存后的记录
     */
    @PostMapping("/waterflood/save")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductionWaterfloodRecord saveWaterfloodRecord(@Valid @RequestBody SaveWaterfloodRecordRequest request) {
        return recordService.saveWaterfloodRecord(request);
    }

    /**
     * 分页查询注水开发仿真记录。
     * @param userId 用户ID（可选）
     * @param page 页码（默认1）
     * @param size 每页数量（默认10）
     * @return 分页结果
     */
    @GetMapping("/waterflood/page")
    public ProductionPage<ProductionWaterfloodRecord> pageWaterfloodRecords(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return recordService.pageWaterfloodRecords(userId, page, size);
    }

    /**
     * 查询单条注水开发仿真记录。
     * @param id 记录ID
     * @return 记录详情
     */
    @GetMapping("/waterflood/{id}")
    public ProductionWaterfloodRecord getWaterfloodRecord(@PathVariable Long id) {
        return recordService.getWaterfloodRecord(id);
    }

    @DeleteMapping("/waterflood/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWaterfloodRecord(@PathVariable Long id) {
        recordService.deleteWaterfloodRecord(id);
    }

    /**
     * 保存增产措施仿真记录。
     * @param request 保存请求
     * @return 保存后的记录
     */
    @PostMapping("/stimulation/save")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductionStimulationRecord saveStimulationRecord(
            @Valid @RequestBody SaveStimulationRecordRequest request
    ) {
        return recordService.saveStimulationRecord(request);
    }

    /**
     * 分页查询增产措施仿真记录。
     * @param userId 用户ID（可选）
     * @param page 页码（默认1）
     * @param size 每页数量（默认10）
     * @return 分页结果
     */
    @GetMapping("/stimulation/page")
    public ProductionPage<ProductionStimulationRecord> pageStimulationRecords(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return recordService.pageStimulationRecords(userId, page, size);
    }

    /**
     * 查询单条增产措施仿真记录。
     * @param id 记录ID
     * @return 记录详情
     */
    @GetMapping("/stimulation/{id}")
    public ProductionStimulationRecord getStimulationRecord(@PathVariable Long id) {
        return recordService.getStimulationRecord(id);
    }

    /**
     * 删除增产措施仿真记录。
     * @param id 记录ID
     */
    @DeleteMapping("/stimulation/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStimulationRecord(@PathVariable Long id) {
        recordService.deleteStimulationRecord(id);
    }
}
