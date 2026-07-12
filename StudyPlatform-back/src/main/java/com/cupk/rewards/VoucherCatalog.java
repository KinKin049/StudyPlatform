package com.cupk.rewards;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 卡券目录，定义系统支持的卡券类型和常量。
 */
public final class VoucherCatalog {
    public static final String TYPE_WARRIOR_SKILL_REFRESH = "type-warrior-skill-refresh";
    public static final String GAME_REVIVE = "game-revive";
    public static final String TEXTBOOK_80_15 = "coupon-textbook-80-15";

    private static final List<VoucherItem> ITEMS = List.of(
            new VoucherItem(
                    TYPE_WARRIOR_SKILL_REFRESH,
                    "GAME_ITEM",
                    "Type Warrior 技能刷新券",
                    "技能选择弹窗出现时可刷新一次候选技能。",
                    260
            ),
            new VoucherItem(
                    GAME_REVIVE,
                    "GAME_ITEM",
                    "游戏复活券",
                    "在 Type Warrior 或万题天梯跳失败后可立即复活一次。",
                    360
            ),
            new VoucherItem(
                    "coupon-course-30-5",
                    "DISCOUNT",
                    "满 30 元减 5 元优惠券",
                    "课程资料与学习权益展示券，后续可接入真实抵扣。",
                    300
            ),
            new VoucherItem(
                    TEXTBOOK_80_15,
                    "DISCOUNT",
                    "满 80 元减 15 元优惠券",
                    "教材购买展示券，后续可接入真实抵扣。",
                    700
            ),
            new VoucherItem(
                    "coupon-study-90",
                    "DISCOUNT",
                    "课程资料 9 折券",
                    "课程资料折扣展示券，后续可接入真实抵扣。",
                    500
            )
    );

    private VoucherCatalog() {
    }

    public static Optional<VoucherItem> find(String voucherKey) {
        String normalized = normalize(voucherKey);
        return ITEMS.stream()
                .filter(item -> item.key().equals(normalized))
                .findFirst();
    }

    public static List<VoucherItem> items() {
        return ITEMS;
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isTextbookVoucher(String voucherKey) {
        return TEXTBOOK_80_15.equals(normalize(voucherKey));
    }

    public record VoucherItem(
            String key,
            String type,
            String name,
            String description,
            int price
    ) {
    }
}
