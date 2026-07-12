package com.cupk.production.service;

import com.cupk.production.dto.ProductionPage;
import com.cupk.production.dto.SavePumpRecordRequest;
import com.cupk.production.dto.SaveReservoirRecordRequest;
import com.cupk.production.dto.SaveStimulationRecordRequest;
import com.cupk.production.dto.SaveWaterfloodRecordRequest;
import com.cupk.production.model.ProductionPumpRecord;
import com.cupk.production.model.ProductionReservoirRecord;
import com.cupk.production.model.ProductionStimulationRecord;
import com.cupk.production.model.ProductionWaterfloodRecord;
import com.cupk.production.repository.ProductionRecordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 采油生产仿真记录服务层。
 * 仅负责持久化流程、JSON 校验和分页边界控制，不执行仿真计算。
 */
@Service
public class ProductionRecordService {
    private static final String PUMP_TABLE = "production_pump_record";
    private static final String RESERVOIR_TABLE = "production_reservoir_record";
    private static final String WATERFLOOD_TABLE = "production_waterflood_record";
    private static final String STIMULATION_TABLE = "production_stimulation_record";
    private static final Set<String> STIMULATION_TYPES = Set.of("fracture", "acid");

    private final ProductionRecordRepository recordRepository;
    private final ObjectMapper objectMapper;

    /**
     * 构造函数。
     * @param recordRepository 生产记录数据访问层
     * @param objectMapper JSON对象映射器
     */
    public ProductionRecordService(ProductionRecordRepository recordRepository, ObjectMapper objectMapper) {
        this.recordRepository = recordRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 保存抽油机仿真记录。
     * @param request 保存请求
     * @return 保存后的记录
     */
    @Transactional
    public ProductionPumpRecord savePumpRecord(SavePumpRecordRequest request) {
        validateJson(request.indicatorChartData(), "indicatorChartData");
        Long id = recordRepository.createPumpRecord(request);
        return getPumpRecord(id);
    }

    /**
     * 保存油藏仿真记录。
     * @param request 保存请求
     * @return 保存后的记录
     */
    @Transactional
    public ProductionReservoirRecord saveReservoirRecord(SaveReservoirRecordRequest request) {
        Long id = recordRepository.createReservoirRecord(request);
        return getReservoirRecord(id);
    }

    /**
     * 保存注水开发仿真记录。
     * @param request 保存请求
     * @return 保存后的记录
     */
    @Transactional
    public ProductionWaterfloodRecord saveWaterfloodRecord(SaveWaterfloodRecordRequest request) {
        validateJson(request.productionCurve(), "productionCurve");
        Long id = recordRepository.createWaterfloodRecord(request);
        return getWaterfloodRecord(id);
    }

    /**
     * 保存增产措施仿真记录。
     * @param request 保存请求
     * @return 保存后的记录
     */
    @Transactional
    public ProductionStimulationRecord saveStimulationRecord(SaveStimulationRecordRequest request) {
        if (!STIMULATION_TYPES.contains(request.type())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type must be fracture or acid");
        }
        Long id = recordRepository.createStimulationRecord(request);
        return getStimulationRecord(id);
    }

    /**
     * 分页查询抽油机仿真记录。
     * @param userId 用户ID（可选）
     * @param page 页码
     * @param size 每页数量
     * @return 分页结果
     */
    public ProductionPage<ProductionPumpRecord> pagePumpRecords(Long userId, int page, int size) {
        int safePage = safePage(page);
        int safeSize = safeSize(size);
        return new ProductionPage<>(
                recordRepository.count(PUMP_TABLE, userId),
                safePage,
                safeSize,
                recordRepository.findPumpRecords(userId, safePage, safeSize)
        );
    }

    /**
     * 分页查询油藏仿真记录。
     * @param userId 用户ID（可选）
     * @param page 页码
     * @param size 每页数量
     * @return 分页结果
     */
    public ProductionPage<ProductionReservoirRecord> pageReservoirRecords(Long userId, int page, int size) {
        int safePage = safePage(page);
        int safeSize = safeSize(size);
        return new ProductionPage<>(
                recordRepository.count(RESERVOIR_TABLE, userId),
                safePage,
                safeSize,
                recordRepository.findReservoirRecords(userId, safePage, safeSize)
        );
    }

    /**
     * 分页查询注水开发仿真记录。
     * @param userId 用户ID（可选）
     * @param page 页码
     * @param size 每页数量
     * @return 分页结果
     */
    public ProductionPage<ProductionWaterfloodRecord> pageWaterfloodRecords(Long userId, int page, int size) {
        int safePage = safePage(page);
        int safeSize = safeSize(size);
        return new ProductionPage<>(
                recordRepository.count(WATERFLOOD_TABLE, userId),
                safePage,
                safeSize,
                recordRepository.findWaterfloodRecords(userId, safePage, safeSize)
        );
    }

    /**
     * 分页查询增产措施仿真记录。
     * @param userId 用户ID（可选）
     * @param page 页码
     * @param size 每页数量
     * @return 分页结果
     */
    public ProductionPage<ProductionStimulationRecord> pageStimulationRecords(Long userId, int page, int size) {
        int safePage = safePage(page);
        int safeSize = safeSize(size);
        return new ProductionPage<>(
                recordRepository.count(STIMULATION_TABLE, userId),
                safePage,
                safeSize,
                recordRepository.findStimulationRecords(userId, safePage, safeSize)
        );
    }

    /**
     * 根据ID获取抽油机仿真记录。
     * @param id 记录ID
     * @return 抽油机仿真记录
     */
    public ProductionPumpRecord getPumpRecord(Long id) {
        return recordRepository.findPumpById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pump record not found"));
    }

    /**
     * 根据ID获取油藏仿真记录。
     * @param id 记录ID
     * @return 油藏仿真记录
     */
    public ProductionReservoirRecord getReservoirRecord(Long id) {
        return recordRepository.findReservoirById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservoir record not found"));
    }

    /**
     * 根据ID获取注水开发仿真记录。
     * @param id 记录ID
     * @return 注水开发仿真记录
     */
    public ProductionWaterfloodRecord getWaterfloodRecord(Long id) {
        return recordRepository.findWaterfloodById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Waterflood record not found"));
    }

    /**
     * 根据ID获取增产措施仿真记录。
     * @param id 记录ID
     * @return 增产措施仿真记录
     */
    public ProductionStimulationRecord getStimulationRecord(Long id) {
        return recordRepository.findStimulationById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stimulation record not found"));
    }

    /**
     * 删除抽油机仿真记录。
     * @param id 记录ID
     */
    @Transactional
    public void deletePumpRecord(Long id) {
        deleteRecord(PUMP_TABLE, id, "Pump record not found");
    }

    /**
     * 删除油藏仿真记录。
     * @param id 记录ID
     */
    @Transactional
    public void deleteReservoirRecord(Long id) {
        deleteRecord(RESERVOIR_TABLE, id, "Reservoir record not found");
    }

    /**
     * 删除注水开发仿真记录。
     * @param id 记录ID
     */
    @Transactional
    public void deleteWaterfloodRecord(Long id) {
        deleteRecord(WATERFLOOD_TABLE, id, "Waterflood record not found");
    }

    /**
     * 删除增产措施仿真记录。
     * @param id 记录ID
     */
    @Transactional
    public void deleteStimulationRecord(Long id) {
        deleteRecord(STIMULATION_TABLE, id, "Stimulation record not found");
    }

    /**
     * 根据表名和ID删除记录，不存在时抛出异常。
     * @param tableName 表名
     * @param id 记录ID
     * @param notFoundMessage 记录不存在时的异常消息
     */
    private void deleteRecord(String tableName, Long id, String notFoundMessage) {
        int deleted = recordRepository.deleteById(tableName, id);
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, notFoundMessage);
        }
    }

    /**
     * 安全页码，确保页码不小于1。
     * @param page 原始页码
     * @return 安全页码
     */
    private int safePage(int page) {
        return Math.max(page, 1);
    }

    /**
     * 安全每页数量，确保数量在1到100之间。
     * @param size 原始每页数量
     * @return 安全每页数量
     */
    private int safeSize(int size) {
        return Math.min(Math.max(size, 1), 100);
    }

    /**
     * 校验字符串是否为合法JSON，非法时抛出异常。
     * @param json 待校验的JSON字符串
     * @param fieldName 字段名称
     */
    private void validateJson(String json, String fieldName) {
        try {
            objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be valid JSON");
        }
    }
}
