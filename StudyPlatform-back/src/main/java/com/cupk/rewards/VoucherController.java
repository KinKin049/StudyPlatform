package com.cupk.rewards;

import com.cupk.rewards.dto.UserVoucherResponse;
import com.cupk.rewards.dto.VoucherItemResponse;
import com.cupk.rewards.dto.VoucherExchangeRequest;
import com.cupk.rewards.dto.VoucherUseRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 卡券控制器。
 * 提供卡券列表查询、兑换和使用的 REST API。
 */
@RestController
@RequestMapping("/api/rewards/vouchers")
public class VoucherController {
    private static final long DEFAULT_USER_ID = 1L;

    private final VoucherService voucherService;

    public VoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @GetMapping
    public List<UserVoucherResponse> list(@RequestHeader(value = "X-Auth-User-Id", required = false) Long userId) {
        return voucherService.findUserVouchers(resolveUserId(userId));
    }

    /**
     * 查询可兑换的卡券商品列表。
     * @return 可兑换卡券列表
     */
    @GetMapping("/items")
    public List<VoucherItemResponse> listItems() {
        return voucherService.findAvailableItems();
    }

    /**
     * 使用金币兑换卡券。
     * @param userId 用户ID（从请求头获取，可选）
     * @param request 兑换请求，包含卡券标识
     * @return 兑换后用户的卡券列表
     */
    @PostMapping("/exchange")
    public List<UserVoucherResponse> exchange(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId,
            @RequestBody VoucherExchangeRequest request
    ) {
        return voucherService.exchange(resolveUserId(userId), request == null ? null : request.voucherKey());
    }

    /**
     * 使用卡券。
     * @param userId 用户ID（从请求头获取，可选）
     * @param request 使用请求，包含卡券标识
     * @return 使用后用户的卡券列表
     */
    @PostMapping("/use")
    public List<UserVoucherResponse> use(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId,
            @RequestBody VoucherUseRequest request
    ) {
        return voucherService.use(resolveUserId(userId), request == null ? null : request.voucherKey());
    }

    /**
     * 解析用户ID，为空时使用默认用户ID。
     * @param userId 用户ID
     * @return 解析后的用户ID
     */
    private long resolveUserId(Long userId) {
        return userId == null || userId <= 0 ? DEFAULT_USER_ID : userId;
    }
}
