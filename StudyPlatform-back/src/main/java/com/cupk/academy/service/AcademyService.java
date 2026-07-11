package com.cupk.academy.service;

import com.cupk.academy.dto.AcademyCategoryResponse;
import com.cupk.academy.dto.AcademyCourseEnrollmentResponse;
import com.cupk.academy.dto.AcademyCourseReviewRequest;
import com.cupk.academy.dto.AcademyCourseReviewResponse;
import com.cupk.academy.dto.AcademyCourseResponse;
import com.cupk.academy.dto.AcademyEnrolledCourseResponse;
import com.cupk.academy.dto.AcademyHomeItemResponse;
import com.cupk.academy.dto.AcademyHomeSectionResponse;
import com.cupk.academy.dto.AcademyTextbookCartItemResponse;
import com.cupk.academy.dto.AcademyTextbookCartRequest;
import com.cupk.academy.dto.AcademyTextbookDetailResponse;
import com.cupk.academy.dto.AcademyTextbookOrderRequest;
import com.cupk.academy.dto.AcademyTextbookOrderResponse;
import com.cupk.academy.dto.AcademyTextbookPaymentRequest;
import com.cupk.academy.dto.AcademyTextbookPaymentResponse;
import com.cupk.academy.dto.AcademyTextbookPaymentStatusResponse;
import com.cupk.academy.dto.AcademyTextbookCommentResponse;
import com.cupk.academy.dto.AcademyTextbookReviewRequest;
import com.cupk.academy.dto.AcademyTextbookResponse;
import com.cupk.academy.dto.TeacherWorkbenchMetricResponse;
import com.cupk.academy.dto.TeacherWorkbenchResponse;
import com.cupk.academy.repository.AcademyRepository.AcademyTextbookOrderResponseData;
import com.cupk.academy.repository.AcademyRepository.TextbookPaymentSessionData;
import com.cupk.academy.repository.AcademyRepository;
import com.cupk.auth.dto.AuthUserResponse;
import com.cupk.auth.repository.AuthUserRepository;
import com.cupk.payment.PaymentGateway;
import com.cupk.payment.PaymentGatewayResult;
import com.cupk.payment.QrCodeRenderer;
import com.cupk.rewards.VoucherCatalog;
import com.cupk.rewards.VoucherService;
import com.cupk.rewards.dto.VoucherItemResponse;
import java.math.BigDecimal;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * 学习服务，提供课程管理、教材商城、购物车和订单等学习相关业务逻辑。
 */
@Service
public class AcademyService {
    private static final long DEFAULT_USER_ID = 1L;

    private final AcademyRepository academyRepository;
    private final AuthUserRepository authUserRepository;
    private final VoucherService voucherService;
    private final List<PaymentGateway> paymentGateways;
    private final QrCodeRenderer qrCodeRenderer;

    /**
     * 构造函数，注入依赖组件。
     *
     * @param academyRepository 学习数据访问层
     * @param authUserRepository 用户认证数据访问层
     * @param voucherService 优惠券服务
     */
    public AcademyService(
            AcademyRepository academyRepository,
            AuthUserRepository authUserRepository,
            VoucherService voucherService,
            List<PaymentGateway> paymentGateways,
            QrCodeRenderer qrCodeRenderer
    ) {
        this.academyRepository = academyRepository;
        this.authUserRepository = authUserRepository;
        this.voucherService = voucherService;
        this.paymentGateways = paymentGateways;
        this.qrCodeRenderer = qrCodeRenderer;
    }

    /**
     * 获取在线开放课程列表。
     *
     * @return 在线开放课程列表
     */
    public List<AcademyCourseResponse> listOnlineOpenCourses() {
        return withCourseCovers(academyRepository.findOnlineOpenCourses());
    }

    /**
     * 获取学习中心首页数据。返回模拟数据，包含我的课程、课程作业和我的考试三个分区。
     *
     * @return 首页分区数据列表
     */
    public List<AcademyHomeSectionResponse> getAcademyHome() {
        return List.of(
                new AcademyHomeSectionResponse("my-courses", "我的课程", List.of(
                        new AcademyHomeItemResponse("人工智能导论", "在线开放课程", "32 学时 · 8 个章节"),
                        new AcademyHomeItemResponse("大学生创新实践", "通识课程", "24 学时 · 项目制学习"),
                        new AcademyHomeItemResponse("数据分析微专业", "微专业课程", "6 门课 · 能力认证")
                )),
                new AcademyHomeSectionResponse("course-assignments", "课程作业", List.of(
                        new AcademyHomeItemResponse("C语言程序设计（下）", "待提交", "第 3 章函数练习 · 截止本周五"),
                        new AcademyHomeItemResponse("劳动通论", "进行中", "专题讨论 1 篇 · 已完成 60%"),
                        new AcademyHomeItemResponse("数据分析微专业", "待批阅", "项目报告已提交 · 等待教师反馈")
                )),
                new AcademyHomeSectionResponse("my-exams", "我的考试", List.of(
                        new AcademyHomeItemResponse("高等数学阶段测验", "未开始", "7 月 12 日 09:00 · 60 分钟"),
                        new AcademyHomeItemResponse("程序设计单元测试", "可进入", "7 月 8 日前完成 · 3 次机会"),
                        new AcademyHomeItemResponse("通识课程结课考试", "已预约", "线上闭卷 · 系统自动判分")
                ))
        );
    }

    /**
     * 获取在线开放课程详情。
     *
     * @param id 课程ID
     * @return 课程详情
     */
    public AcademyCourseResponse getOnlineOpenCourse(String id) {
        return withCourseCover(academyRepository.findOnlineOpenCourseById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "课程不存在")));
    }

    /**
     * 获取教师已发布的在线开放课程列表。验证教师身份后查询。
     *
     * @param userId 用户ID
     * @return 教师已发布课程列表
     */
    public List<AcademyCourseResponse> listMyPublishedOnlineOpenCourses(Long userId) {
        long teacherUserId = normalizeUserId(userId);
        ensureTeacher(teacherUserId);
        return withCourseCovers(academyRepository.findPublishedOnlineOpenCourses(teacherUserId));
    }

    public TeacherWorkbenchResponse getTeacherWorkbench(Long userId) {
        long teacherUserId = normalizeUserId(userId);
        ensureTeacher(teacherUserId);
        int ungradedAssignments = academyRepository.countTeacherPendingAssignmentReviews(teacherUserId);
        int unreadComments = academyRepository.countTeacherUnreadCourseReviews(teacherUserId);
        int ungradedExams = academyRepository.countTeacherPendingExamReviews(teacherUserId);
        return new TeacherWorkbenchResponse(
                ungradedAssignments,
                unreadComments,
                ungradedExams,
                List.of(
                        new TeacherWorkbenchMetricResponse("未批改作业数", ungradedAssignments, "#5fbf9f"),
                        new TeacherWorkbenchMetricResponse("未读评论数", unreadComments, "#f2c04c"),
                        new TeacherWorkbenchMetricResponse("未批改考试数", ungradedExams, "#e87575")
                ),
                academyRepository.findTeacherMailboxMessages(teacherUserId)
        );
    }

    public TeacherWorkbenchResponse markTeacherMailboxRead(Long userId) {
        long teacherUserId = normalizeUserId(userId);
        ensureTeacher(teacherUserId);
        academyRepository.markTeacherMailboxRead(teacherUserId);
        return getTeacherWorkbench(teacherUserId);
    }

    /**
     * 删除教师已发布的在线开放课程。验证教师身份后删除。
     *
     * @param userId 用户ID
     * @param courseId 课程ID
     * @return 课程报名响应
     */
    public AcademyCourseEnrollmentResponse deletePublishedOnlineOpenCourse(Long userId, String courseId) {
        long teacherUserId = normalizeUserId(userId);
        ensureTeacher(teacherUserId);
        if (courseId == null || courseId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "课程编号不能为空");
        }
        int deleted = academyRepository.deletePublishedOnlineOpenCourse(teacherUserId, courseId.trim());
        if (deleted <= 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "课程不存在或不属于当前教师");
        }
        return new AcademyCourseEnrollmentResponse(false, "课程已删除");
    }

    /**
     * 获取通识课程列表。
     *
     * @return 通识课程列表
     */
    public List<AcademyCourseResponse> listGeneralCourses() {
        return withCourseCovers(academyRepository.findGeneralCourses());
    }

    /**
     * 获取通识课程详情。
     *
     * @param id 课程ID
     * @return 课程详情
     */
    public AcademyCourseResponse getGeneralCourse(String id) {
        return withCourseCover(academyRepository.findGeneralCourseById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "课程不存在")));
    }

    /**
     * 获取微专业课程列表。
     *
     * @return 微专业课程列表
     */
    public List<AcademyCourseResponse> listMicroMajorCourses() {
        return withCourseCovers(academyRepository.findMicroMajorCourses());
    }

    /**
     * 获取微专业课程详情。
     *
     * @param id 课程ID
     * @return 课程详情
     */
    public AcademyCourseResponse getMicroMajorCourse(String id) {
        return withCourseCover(academyRepository.findMicroMajorCourseById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "课程不存在")));
    }

    /**
     * 获取教材列表。转换封面路径为可访问URL。
     *
     * @return 教材列表
     */
    public List<AcademyTextbookResponse> listTextbooks() {
        return academyRepository.findTextbooks().stream()
                .map(textbook -> new AcademyTextbookResponse(
                        textbook.id(),
                        textbook.name(),
                        textbook.editor(),
                        textbook.category(),
                        textbook.publisher(),
                        textbook.publishDate(),
                        textbook.isbn(),
                        textbook.description(),
                        fileUrl(textbook.coverFilePath()),
                        textbook.coverUrl(),
                        textbook.coverFilePath(),
                        textbook.link()
                ))
                .toList();
    }

    /**
     * 获取教材详情。使用默认用户ID。
     *
     * @param id 教材ID
     * @return 教材详情
     */
    public AcademyTextbookDetailResponse getTextbook(String id) {
        return getTextbook(id, DEFAULT_USER_ID);
    }

    /**
     * 获取教材详情。包含评论列表和购买状态。
     *
     * @param id 教材ID
     * @param userId 用户ID
     * @return 教材详情
     */
    public AcademyTextbookDetailResponse getTextbook(String id, Long userId) {
        if (id == null || id.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "教材编号不能为空");
        }
        long normalizedUserId = normalizeUserId(userId);
        AcademyTextbookDetailResponse textbook = academyRepository.findTextbookById(id.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "教材不存在"));
        return withTextbookCover(textbook, normalizedUserId);
    }

    /**
     * 获取教材购物车列表。
     *
     * @param userId 用户ID
     * @return 购物车列表
     */
    public List<AcademyTextbookCartItemResponse> listTextbookCart(Long userId) {
        return academyRepository.findTextbookCartItems(normalizeUserId(userId));
    }

    /**
     * 添加教材到购物车。验证教材存在后添加。
     *
     * @param request 购物车请求
     * @return 更新后的购物车列表
     */
    public List<AcademyTextbookCartItemResponse> addTextbookCartItem(AcademyTextbookCartRequest request) {
        return addTextbookCartItem(request == null ? null : request.userId(), request);
    }

    public List<AcademyTextbookCartItemResponse> addTextbookCartItem(Long userId, AcademyTextbookCartRequest request) {
        if (request == null || request.textbookId() == null || request.textbookId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "教材编号不能为空");
        }
        getTextbook(request.textbookId());
        long normalizedUserId = normalizeUserId(userId);
        academyRepository.addTextbookCartItem(normalizedUserId, request.textbookId().trim(), normalizeQuantity(request.quantity()));
        return listTextbookCart(normalizedUserId);
    }

    /**
     * 删除购物车中的教材。
     *
     * @param userId 用户ID
     * @param itemId 购物车条目ID
     * @return 更新后的购物车列表
     */
    public List<AcademyTextbookCartItemResponse> deleteTextbookCartItem(Long userId, Long itemId) {
        long normalizedUserId = normalizeUserId(userId);
        if (itemId == null || itemId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "购物车条目不能为空");
        }
        academyRepository.deleteTextbookCartItem(normalizedUserId, itemId);
        return listTextbookCart(normalizedUserId);
    }

    /**
     * 更新购物车中教材的数量。
     *
     * @param userId 用户ID
     * @param itemId 购物车条目ID
     * @param quantity 数量
     * @return 更新后的购物车列表
     */
    public List<AcademyTextbookCartItemResponse> updateTextbookCartItem(Long userId, Long itemId, Integer quantity) {
        long normalizedUserId = normalizeUserId(userId);
        if (itemId == null || itemId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "购物车条目不能为空");
        }
        academyRepository.updateTextbookCartItem(normalizedUserId, itemId, normalizeQuantity(quantity));
        return listTextbookCart(normalizedUserId);
    }

    /**
     * 创建教材订单。支持从购物车创建或直接购买单个教材，可使用优惠券。
     *
     * @param request 订单请求
     * @return 订单响应
     */
    public AcademyTextbookOrderResponse createTextbookOrder(AcademyTextbookOrderRequest request) {
        return createTextbookOrder(request == null ? null : request.userId(), request);
    }

    public AcademyTextbookOrderResponse createTextbookOrder(Long userId, AcademyTextbookOrderRequest request) {
        long normalizedUserId = normalizeUserId(userId);
        List<Long> cartItemIds = request == null || request.cartItemIds() == null
                ? List.of()
                : request.cartItemIds().stream()
                        .filter(itemId -> itemId != null && itemId > 0)
                        .distinct()
                        .toList();
        if (!cartItemIds.isEmpty()) {
            BigDecimal originalAmount = academyRepository.sumTextbookCartItems(normalizedUserId, cartItemIds);
            TextbookVoucherChoice voucherChoice = resolveTextbookVoucherChoice(normalizedUserId, request, originalAmount);
            AcademyTextbookOrderResponseData order = academyRepository.createTextbookOrderFromCart(
                    normalizedUserId,
                    cartItemIds,
                    voucherChoice.voucherKey(),
                    voucherChoice.voucherName(),
                    voucherChoice.discountAmount()
            );
            if (order.orderNo() == null || order.orderNo().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择有效的购物车教材");
            }
            return toTextbookOrderResponse(order, "待支付", "订单已创建，请完成结算", false);
        }
        if (request == null || request.textbookId() == null || request.textbookId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "教材编号不能为空");
        }
        AcademyTextbookDetailResponse textbook = getTextbook(request.textbookId());
        BigDecimal originalAmount = (textbook.discountPrice() == null ? BigDecimal.ZERO : textbook.discountPrice())
                .multiply(BigDecimal.valueOf(normalizeQuantity(request.quantity())));
        TextbookVoucherChoice voucherChoice = resolveTextbookVoucherChoice(normalizedUserId, request, originalAmount);
        AcademyTextbookOrderResponseData order = academyRepository.createTextbookOrder(
                normalizedUserId,
                textbook,
                normalizeQuantity(request.quantity()),
                voucherChoice.voucherKey(),
                voucherChoice.voucherName(),
                voucherChoice.discountAmount()
        );
        return toTextbookOrderResponse(order, "待支付", "订单已创建", false);
    }

    /**
     * 支付教材订单。更新订单状态并消费优惠券。
     *
     * @param orderNo 订单编号
     * @param userId 用户ID
     * @return 订单响应
     */
    public AcademyTextbookOrderResponse payTextbookOrder(String orderNo, Long userId) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请使用微信或支付宝二维码完成付款后再确认订单");
    }

    public AcademyTextbookPaymentResponse createTextbookPayment(String orderNo, AcademyTextbookPaymentRequest request) {
        return createTextbookPayment(orderNo, request == null ? null : request.userId(), request);
    }

    public AcademyTextbookPaymentResponse createTextbookPayment(String orderNo, Long userId, AcademyTextbookPaymentRequest request) {
        if (orderNo == null || orderNo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "订单编号不能为空");
        }
        long normalizedUserId = normalizeUserId(userId);
        String normalizedOrderNo = orderNo.trim();
        String provider = normalizePaymentProvider(request == null ? null : request.provider());
        String paymentMode = normalizePaymentMode(request == null ? null : request.paymentMode());
        if ("PAGE".equals(paymentMode) && !"ALIPAY".equals(provider)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "网页收银台仅支持支付宝");
        }
        AcademyTextbookOrderResponseData order = academyRepository.findPendingTextbookOrder(normalizedUserId, normalizedOrderNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在或当前不可支付"));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);
        String gatewayOrderNo = createGatewayOrderNo(normalizedOrderNo);
        String sessionId = createPaymentSessionId(provider + ("PAGE".equals(paymentMode) ? "-PAGE" : ""), normalizedOrderNo);
        String subject = "StudyPlatform Textbook Order " + normalizedOrderNo;
        PaymentGatewayResult gatewayResult = "PAGE".equals(paymentMode)
                ? paymentGateway(provider).createPagePayment(gatewayOrderNo, subject, order.totalAmount(), createPaymentReturnUrl(sessionId))
                : paymentGateway(provider).createNativePayment(gatewayOrderNo, subject, order.totalAmount());
        TextbookPaymentSessionData session = academyRepository.createTextbookPaymentSession(
                normalizedUserId,
                normalizedOrderNo,
                gatewayOrderNo,
                sessionId,
                provider,
                order.totalAmount(),
                gatewayResult.qrPayload(),
                expiresAt
        );
        String paymentPayload = "PAGE".equals(paymentMode) ? createPaymentCashierUrl(session.sessionId()) : session.qrPayload();
        return new AcademyTextbookPaymentResponse(
                session.sessionId(),
                session.orderNo(),
                session.provider(),
                session.amount(),
                paymentPayload,
                session.status(),
                session.expiresAt(),
                gatewayResult.message().isBlank() ? "请使用" + paymentProviderLabel(provider) + "扫码支付" : gatewayResult.message()
        );
    }

    public AcademyTextbookPaymentStatusResponse getTextbookPaymentStatus(String sessionId) {
        TextbookPaymentSessionData session = findActiveTextbookPaymentSession(sessionId);
        if ("PENDING".equals(session.status()) && !session.expiresAt().isAfter(LocalDateTime.now())) {
            academyRepository.expireTextbookPaymentSession(session.sessionId());
            session = findActiveTextbookPaymentSession(session.sessionId());
        }
        if ("PENDING".equals(session.status())) {
            PaymentGatewayResult gatewayResult;
            try {
                gatewayResult = paymentGateway(session.provider()).queryPayment(session.gatewayOrderNo());
            } catch (ResponseStatusException ex) {
                return toTextbookPaymentStatusResponse(session, null, ex.getReason());
            }
            if ("PAID".equals(gatewayResult.status())) {
                academyRepository.markTextbookPaymentSessionPaid(session.sessionId());
                session = findActiveTextbookPaymentSession(session.sessionId());
            } else if ("EXPIRED".equals(gatewayResult.status())) {
                academyRepository.expireTextbookPaymentSession(session.sessionId());
                session = findActiveTextbookPaymentSession(session.sessionId());
            }
        }
        AcademyTextbookOrderResponse order = null;
        if ("PAID".equals(session.status())) {
            order = completePaidTextbookOrder(session);
        }
        return toTextbookPaymentStatusResponse(session, order);
    }

    public AcademyTextbookPaymentStatusResponse confirmLocalTextbookPayment(String sessionId) {
        return getTextbookPaymentStatus(sessionId);
    }

    public byte[] renderTextbookPaymentQr(String sessionId) {
        TextbookPaymentSessionData session = findActiveTextbookPaymentSession(sessionId);
        return qrCodeRenderer.renderPng(session.qrPayload());
    }

    public String renderTextbookPaymentCashier(String sessionId) {
        TextbookPaymentSessionData session = findActiveTextbookPaymentSession(sessionId);
        if (!"ALIPAY".equals(session.provider())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该支付会话不支持网页收银台");
        }
        return session.qrPayload();
    }

    private AcademyTextbookOrderResponse completePaidTextbookOrder(TextbookPaymentSessionData session) {
        AcademyTextbookOrderResponseData order = academyRepository.payTextbookOrder(session.userId(), session.orderNo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在或不可支付"));
        if (order.voucherKey() != null && !order.voucherKey().isBlank() && !order.voucherConsumed()) {
            voucherService.consumeDiscountVoucher(session.userId(), order.voucherKey());
            academyRepository.markTextbookOrderVoucherConsumed(session.userId(), order.orderNo());
        }
        academyRepository.deleteTextbookCartItemsForPaidOrder(session.userId(), order.orderNo());
        return toTextbookOrderResponse(order, "已支付", "支付成功，教材已购买", true);
    }

    /**
     * 解析教材订单的优惠券选择。验证优惠券有效性并计算折扣金额。
     *
     * @param userId 用户ID
     * @param request 订单请求
     * @param knownOriginalAmount 原始金额
     * @return 优惠券选择
     */
    private TextbookVoucherChoice resolveTextbookVoucherChoice(
            long userId,
            AcademyTextbookOrderRequest request,
            BigDecimal knownOriginalAmount
    ) {
        if (!Boolean.TRUE.equals(request == null ? null : request.useVoucher())) {
            return TextbookVoucherChoice.none();
        }
        String voucherKey = VoucherCatalog.normalize(request.voucherKey());
        if (voucherKey.isBlank()) {
            return TextbookVoucherChoice.none();
        }
        if (!voucherService.hasVoucher(userId, voucherKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "优惠券数量不足");
        }
        VoucherItemResponse item = voucherService.findDiscountVoucher(voucherKey);
        BigDecimal discountAmount = knownOriginalAmount == null
                ? BigDecimal.ZERO
                : voucherService.calculateDiscount(item, knownOriginalAmount);
        return new TextbookVoucherChoice(voucherKey, item.name(), discountAmount);
    }

    /**
     * 将订单数据转换为订单响应对象。
     *
     * @param order 订单数据
     * @param status 订单状态
     * @param message 提示消息
     * @param paid 是否已支付
     * @return 订单响应
     */
    private AcademyTextbookOrderResponse toTextbookOrderResponse(
            AcademyTextbookOrderResponseData order,
            String status,
            String message,
            boolean paid
    ) {
        return new AcademyTextbookOrderResponse(
                order.orderNo(),
                order.totalAmount(),
                order.originalAmount() == null ? order.totalAmount() : order.originalAmount(),
                order.discountAmount() == null ? BigDecimal.ZERO : order.discountAmount(),
                order.voucherKey(),
                order.voucherName(),
                status,
                message,
                paid
        );
    }

    private AcademyTextbookPaymentStatusResponse toTextbookPaymentStatusResponse(
            TextbookPaymentSessionData session,
            AcademyTextbookOrderResponse order
    ) {
        return toTextbookPaymentStatusResponse(session, order, null);
    }

    private AcademyTextbookPaymentStatusResponse toTextbookPaymentStatusResponse(
            TextbookPaymentSessionData session,
            AcademyTextbookOrderResponse order,
            String fallbackMessage
    ) {
        boolean paid = "PAID".equals(session.status()) && order != null && order.paid();
        String message = switch (session.status()) {
            case "PAID" -> paid ? "支付成功，订单已确认" : "支付已完成，正在确认订单";
            case "EXPIRED" -> "支付二维码已过期，请重新生成";
            default -> "等待扫码支付";
        };
        if (fallbackMessage != null && !fallbackMessage.isBlank()) {
            message = fallbackMessage;
        }
        return new AcademyTextbookPaymentStatusResponse(
                session.sessionId(),
                session.orderNo(),
                session.provider(),
                session.amount(),
                session.status(),
                paid,
                session.expiresAt(),
                order,
                message
        );
    }

    private TextbookPaymentSessionData findActiveTextbookPaymentSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "支付会话不能为空");
        }
        return academyRepository.findTextbookPaymentSession(sessionId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "支付会话不存在"));
    }

    private String normalizePaymentProvider(String provider) {
        String value = provider == null ? "" : provider.trim().toUpperCase(Locale.ROOT);
        if (value.equals("WECHAT") || value.equals("ALIPAY")) {
            return value;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择微信或支付宝支付");
    }

    private String normalizePaymentMode(String paymentMode) {
        String value = paymentMode == null ? "" : paymentMode.trim().toUpperCase(Locale.ROOT);
        if (value.isBlank() || "NATIVE".equals(value) || "QR".equals(value)) {
            return "NATIVE";
        }
        if ("PAGE".equals(value) || "CASHIER".equals(value)) {
            return "PAGE";
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择有效的支付方式");
    }

    private String paymentProviderLabel(String provider) {
        return "ALIPAY".equals(provider) ? "支付宝" : "微信";
    }

    private String createPaymentSessionId(String provider, String orderNo) {
        String safeOrderNo = orderNo == null ? "" : orderNo.replaceAll("[^A-Za-z0-9_-]", "");
        if (safeOrderNo.length() > 40) {
            safeOrderNo = safeOrderNo.substring(0, 40);
        }
        return provider + "-" + safeOrderNo + "-" + UUID.randomUUID().toString().replace("-", "");
    }

    private String createGatewayOrderNo(String orderNo) {
        String safeOrderNo = orderNo == null ? "" : orderNo.replaceAll("[^A-Za-z0-9_-]", "");
        if (safeOrderNo.length() > 42) {
            safeOrderNo = safeOrderNo.substring(0, 42);
        }
        return safeOrderNo + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String createPaymentCashierUrl(String sessionId) {
        return "/api/academy/textbook-payments/" + URLEncoder.encode(sessionId, StandardCharsets.UTF_8) + "/cashier";
    }

    private String createPaymentReturnUrl(String sessionId) {
        return "http://localhost:5173/academy/textbook-cart?paymentSession="
                + URLEncoder.encode(sessionId, StandardCharsets.UTF_8);
    }

    private PaymentGateway paymentGateway(String provider) {
        return paymentGateways.stream()
                .filter(gateway -> gateway.provider().equals(provider))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "支付渠道不可用：" + provider));
    }

    /**
     * 教材优惠券选择记录。
     *
     * @param voucherKey 优惠券编号
     * @param voucherName 优惠券名称
     * @param discountAmount 折扣金额
     */
    private record TextbookVoucherChoice(
            String voucherKey,
            String voucherName,
            BigDecimal discountAmount
    ) {
        /**
         * 创建空的优惠券选择。
         *
         * @return 空优惠券选择
         */
        static TextbookVoucherChoice none() {
            return new TextbookVoucherChoice(null, null, BigDecimal.ZERO);
        }
    }

    /**
     * 保存教材评论。验证用户已购买教材后保存评论。
     *
     * @param textbookId 教材ID
     * @param request 评论请求
     * @return 评论响应
     */
    public AcademyTextbookCommentResponse saveTextbookReview(String textbookId, AcademyTextbookReviewRequest request) {
        if (textbookId == null || textbookId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "教材编号不能为空");
        }
        getTextbook(textbookId);
        long userId = normalizeUserId(request == null ? null : request.userId());
        if (!academyRepository.hasPurchasedTextbook(userId, textbookId.trim())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "购买教材后才能评论");
        }
        String content = clean(request == null ? null : request.content(), 800);
        if (content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "评论内容不能为空");
        }
        String userName = clean(request == null ? null : request.userName(), 80);
        if (userName.isBlank()) {
            userName = "默认用户";
        }
        int rating = normalizeRating(request == null ? null : request.rating());
        return academyRepository.saveTextbookReview(userId, textbookId.trim(), userName, rating, content);
    }

    /**
     * 获取在线开放课程分类列表。
     *
     * @return 分类列表
     */
    public List<AcademyCategoryResponse> listOnlineOpenCourseCategories() {
        return academyRepository.findManagedCourseCategories("online-open-courses");
    }

    /**
     * 获取通识课程分类列表。
     *
     * @return 分类列表
     */
    public List<AcademyCategoryResponse> listGeneralCourseCategories() {
        return academyRepository.findManagedCourseCategories("general-courses");
    }

    /**
     * 获取微专业课程分类列表。
     *
     * @return 分类列表
     */
    public List<AcademyCategoryResponse> listMicroMajorCourseCategories() {
        return academyRepository.findManagedCourseCategories("micro-major-courses");
    }

    /**
     * 获取教材分类列表。
     *
     * @return 分类列表
     */
    public List<AcademyCategoryResponse> listTextbookCategories() {
        return academyRepository.findCategories("excellent_textbooks");
    }

    /**
     * 发布在线开放课程。验证教师身份，上传课程封面和视频，保存课程信息。
     *
     * @param userId 用户ID
     * @param courseName 课程名称
     * @param startTime 开课时间
     * @param category 课程分类
     * @param semesterPlan 学期计划
     * @param courseDetail 课程详情
     * @param courseOverview 课程概述
     * @param cover 课程封面
     * @param video 课程视频
     * @return 课程响应
     */
    public AcademyCourseResponse publishOnlineOpenCourse(
            Long userId,
            String courseName,
            String startTime,
            String category,
            String semesterPlan,
            String courseDetail,
            String courseOverview,
            MultipartFile cover,
            MultipartFile video
    ) {
        long publisherUserId = normalizeUserId(userId);
        AuthUserResponse user = ensureTeacher(publisherUserId);
        String normalizedName = clean(courseName, 120);
        String normalizedStartTime = clean(startTime, 64);
        String normalizedCategory = clean(category, 80);
        String normalizedSemesterPlan = clean(semesterPlan, 512);
        String normalizedDetail = clean(courseDetail, 4000);
        String normalizedOverview = clean(courseOverview, 1200);
        if (normalizedName.isBlank()
                || normalizedStartTime.isBlank()
                || normalizedCategory.isBlank()
                || normalizedSemesterPlan.isBlank()
                || normalizedDetail.isBlank()
                || normalizedOverview.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "课程信息不能为空");
        }
        if (!academyRepository.managedCourseCategoryExists("online-open-courses", normalizedCategory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "课程分类只能从管理员维护的分类中选择");
        }

        String coverPath = saveCourseFile(publisherUserId, cover, "cover", List.of("jpg", "jpeg", "png", "webp"), true);
        String videoPath = saveCourseFile(publisherUserId, video, "video", List.of("mp4", "webm", "ogg", "mov"), true);
        String courseId = academyRepository.publishOnlineOpenCourse(
                publisherUserId,
                normalizedName,
                clean(user.teacherName(), 80),
                clean(user.school(), 120),
                normalizedCategory,
                normalizedStartTime,
                normalizedSemesterPlan,
                normalizedDetail,
                normalizedOverview,
                coverPath,
                videoPath
        );
        return getOnlineOpenCourse(courseId);
    }

    /**
     * 报名参加课程。验证课程存在后添加报名记录。
     *
     * @param resourceType 课程类型
     * @param courseId 课程ID
     * @param userId 用户ID
     * @return 课程报名响应
     */
    public AcademyCourseEnrollmentResponse enrollCourse(String resourceType, String courseId, Long userId) {
        ensureCourseExists(resourceType, courseId);
        academyRepository.enrollCourse(resourceType, courseId, normalizeUserId(userId));
        return new AcademyCourseEnrollmentResponse(true, "已参加课程");
    }

    /**
     * 退出课程。验证课程存在后删除报名记录。
     *
     * @param resourceType 课程类型
     * @param courseId 课程ID
     * @param userId 用户ID
     * @return 课程报名响应
     */
    public AcademyCourseEnrollmentResponse unenrollCourse(String resourceType, String courseId, Long userId) {
        ensureCourseExists(resourceType, courseId);
        academyRepository.unenrollCourse(resourceType, courseId, normalizeUserId(userId));
        return new AcademyCourseEnrollmentResponse(false, "已退出课程");
    }

    /**
     * 获取用户已参加的课程列表。转换封面路径为可访问URL。
     *
     * @param userId 用户ID
     * @return 已参加课程列表
     */
    public List<AcademyEnrolledCourseResponse> listMyCourses(Long userId) {
        return academyRepository.findEnrolledCourses(normalizeUserId(userId)).stream()
                .map(this::withEnrolledCourseCover)
                .toList();
    }

    /**
     * 获取课程评论列表。验证课程存在后查询。
     *
     * @param resourceType 课程类型
     * @param courseId 课程ID
     * @return 评论列表
     */
    public List<AcademyCourseReviewResponse> listCourseReviews(String resourceType, String courseId) {
        ensureCourseExists(resourceType, courseId);
        return academyRepository.findCourseReviews(resourceType, courseId);
    }

    /**
     * 保存课程评论。验证课程存在和评论内容后保存。
     *
     * @param resourceType 课程类型
     * @param courseId 课程ID
     * @param request 评论请求
     * @return 评论响应
     */
    public AcademyCourseReviewResponse saveCourseReview(
            String resourceType,
            String courseId,
            Long userId,
            AcademyCourseReviewRequest request
    ) {
        ensureCourseExists(resourceType, courseId);
        long normalizedUserId = requireUserId(userId);
        AuthUserResponse user = authUserRepository.findResponseById(normalizedUserId);
        String userName = cleanUserName(user.username());
        String content = request.content().trim();
        if (content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "评价内容不能为空");
        }
        Long parentReviewId = request.parentReviewId();
        if (parentReviewId != null && !academyRepository.courseReviewExists(resourceType, courseId, parentReviewId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "回复的评论不存在");
        }
        return academyRepository.saveCourseReview(
                resourceType,
                courseId,
                normalizedUserId,
                userName,
                user.roleType(),
                request.rating(),
                content,
                parentReviewId
        );
    }

    public AcademyCourseReviewResponse replyCourseReview(Long userId, Long parentReviewId, String content) {
        long normalizedUserId = requireUserId(userId);
        AuthUserResponse user = authUserRepository.findResponseById(normalizedUserId);
        String safeContent = content == null ? "" : content.trim();
        if (safeContent.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "回复内容不能为空");
        }
        if (parentReviewId == null || parentReviewId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "回复的评论不存在");
        }
        try {
            return academyRepository.saveCourseReviewReply(
                    parentReviewId,
                    normalizedUserId,
                    cleanUserName(user.username()),
                    user.roleType(),
                    safeContent
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "回复的评论不存在");
        }
    }

    /**
     * 验证课程是否存在。根据课程类型调用对应的获取方法。
     *
     * @param resourceType 课程类型
     * @param courseId 课程ID
     */
    private void ensureCourseExists(String resourceType, String courseId) {
        switch (resourceType) {
            case "online-open-courses" -> getOnlineOpenCourse(courseId);
            case "general-courses" -> getGeneralCourse(courseId);
            case "micro-major-courses" -> getMicroMajorCourse(courseId);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "课程类型不支持");
        }
    }

    /**
     * 批量转换课程封面路径为可访问URL。
     *
     * @param courses 课程列表
     * @return 转换后的课程列表
     */
    private List<AcademyCourseResponse> withCourseCovers(List<AcademyCourseResponse> courses) {
        return courses.stream()
                .map(this::withCourseCover)
                .toList();
    }

    /**
     * 转换课程封面和视频路径为可访问URL。
     *
     * @param course 课程对象
     * @return 转换后的课程对象
     */
    private AcademyCourseResponse withCourseCover(AcademyCourseResponse course) {
        return new AcademyCourseResponse(
                course.id(),
                course.name(),
                course.teacher(),
                course.category(),
                course.school(),
                fileUrl(course.coverFilePath()),
                course.coverUrl(),
                course.coverFilePath(),
                course.startTime(),
                course.participants(),
                course.comment(),
                course.description(),
                course.semesterPlan(),
                course.overview(),
                fileUrl(course.videoFilePath()),
                course.videoFilePath(),
                course.link(),
                course.certified(),
                course.certificationLabel()
        );
    }

    /**
     * 转换教材封面路径为可访问URL，添加评论列表和购买状态。
     *
     * @param textbook 教材对象
     * @param userId 用户ID
     * @return 转换后的教材对象
     */
    private AcademyTextbookDetailResponse withTextbookCover(AcademyTextbookDetailResponse textbook, long userId) {
        List<AcademyTextbookCommentResponse> reviews = academyRepository.findTextbookReviews(textbook.id());
        List<AcademyTextbookCommentResponse> comments = reviews.isEmpty() ? textbook.comments() : reviews;
        boolean purchased = academyRepository.hasPurchasedTextbook(userId, textbook.id());
        return new AcademyTextbookDetailResponse(
                textbook.id(),
                textbook.name(),
                textbook.editor(),
                textbook.category(),
                textbook.publisher(),
                textbook.publishDate(),
                textbook.isbn(),
                textbook.description(),
                fileUrl(textbook.coverFilePath()),
                textbook.coverUrl(),
                textbook.coverFilePath(),
                textbook.link(),
                textbook.recommendation(),
                textbook.originalPrice(),
                textbook.discountPrice(),
                textbook.readerCount(),
                textbook.overview(),
                textbook.catalog(),
                comments,
                purchased
        );
    }

    /**
     * 验证用户是否为教师角色。
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    private AuthUserResponse ensureTeacher(long userId) {
        AuthUserResponse user = authUserRepository.findResponseById(userId);
        if (!"teacher".equals(user.roleType())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有教师可以使用该功能");
        }
        return user;
    }

    /**
     * 转换已参加课程的封面和视频路径为可访问URL。
     *
     * @param course 已参加课程对象
     * @return 转换后的课程对象
     */
    private AcademyEnrolledCourseResponse withEnrolledCourseCover(AcademyEnrolledCourseResponse course) {
        return new AcademyEnrolledCourseResponse(
                course.resourceType(),
                course.id(),
                course.name(),
                course.teacher(),
                course.category(),
                course.school(),
                fileUrl(course.coverFilePath()),
                course.coverUrl(),
                course.coverFilePath(),
                course.startTime(),
                course.participants(),
                course.comment(),
                course.description(),
                course.semesterPlan(),
                course.overview(),
                fileUrl(course.videoFilePath()),
                course.videoFilePath(),
                course.link(),
                course.enrolledAt()
        );
    }

    /**
     * 标准化用户ID。空值或无效值使用默认用户ID。
     *
     * @param userId 用户ID
     * @return 标准化后的用户ID
     */
    private Long normalizeUserId(Long userId) {
        return userId == null || userId <= 0 ? DEFAULT_USER_ID : userId;
    }

    private long requireUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录后再发表评论");
        }
        return userId;
    }

    private String cleanUserName(String userName) {
        if (userName == null || userName.isBlank()) {
            return "用户";
        }
        return userName.trim();
    }

    /**
     * 标准化数量。空值或无效值使用1，最大值限制为99。
     *
     * @param quantity 数量
     * @return 标准化后的数量
     */
    private int normalizeQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return 1;
        }
        return Math.min(quantity, 99);
    }

    /**
     * 标准化评分。空值使用5，范围限制在1到5之间。
     *
     * @param rating 评分
     * @return 标准化后的评分
     */
    private int normalizeRating(Integer rating) {
        if (rating == null) {
            return 5;
        }
        return Math.max(1, Math.min(rating, 5));
    }

    /**
     * 将文件路径转换为可访问的URL。
     *
     * @param coverFilePath 文件路径
     * @return 可访问的URL
     */
    private String fileUrl(String coverFilePath) {
        if (coverFilePath == null || coverFilePath.isBlank()) {
            return "";
        }
        String normalizedPath = coverFilePath.replace("\\", "/");
        if (normalizedPath.startsWith("storage/")) {
            normalizedPath = normalizedPath.substring("storage/".length());
        }
        String encodedPath = java.util.Arrays.stream(normalizedPath.split("/"))
                .map(part -> URLEncoder.encode(part, StandardCharsets.UTF_8).replace("+", "%20"))
                .reduce((left, right) -> left + "/" + right)
                .orElse("");
        return "/files/" + encodedPath;
    }

    /**
     * 保存课程文件（封面或视频）。验证文件格式后保存到存储目录。
     *
     * @param userId 用户ID
     * @param file 文件
     * @param type 文件类型（cover/video）
     * @param allowedExtensions 允许的文件扩展名
     * @param required 是否必填
     * @return 文件存储路径
     */
    private String saveCourseFile(
            long userId,
            MultipartFile file,
            String type,
            List<String> allowedExtensions,
            boolean required
    ) {
        if (file == null || file.isEmpty()) {
            if (required) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请上传课程" + ("cover".equals(type) ? "封面" : "视频"));
            }
            return null;
        }
        String extension = resolveExtension(file.getOriginalFilename());
        if (!allowedExtensions.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "课程" + ("cover".equals(type) ? "封面" : "视频") + "格式不支持");
        }
        Path storageDirectory = resolveStoragePath()
                .resolve("teacher_courses")
                .resolve(String.valueOf(userId))
                .normalize();
        String fileName = type + "-" + UUID.randomUUID() + "." + extension;
        Path targetPath = storageDirectory.resolve(fileName).normalize();
        if (!targetPath.startsWith(storageDirectory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件名不合法");
        }
        try {
            Files.createDirectories(storageDirectory);
            file.transferTo(targetPath);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "课程文件保存失败", ex);
        }
        return "teacher_courses/" + userId + "/" + fileName;
    }

    /**
     * 解析文件扩展名。
     *
     * @param originalFilename 原始文件名
     * @return 文件扩展名
     */
    private String resolveExtension(String originalFilename) {
        String fileName = originalFilename == null ? "" : originalFilename.toLowerCase(Locale.ROOT);
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1);
    }

    /**
     * 解析存储路径。优先使用当前目录下的storage，其次使用backend目录下的storage。
     *
     * @return 存储路径
     */
    private Path resolveStoragePath() {
        Path currentDirectory = Path.of("").toAbsolutePath();
        Path directStorage = currentDirectory.resolve("storage").normalize();
        if (Files.isDirectory(directStorage)) {
            return directStorage;
        }
        Path backendStorage = currentDirectory.resolve("StudyPlatform-back").resolve("storage").normalize();
        if (Files.isDirectory(backendStorage)) {
            return backendStorage;
        }
        return directStorage;
    }

    /**
     * 清理字符串，空值返回空字符串，超长截断。
     *
     * @param value 待清理的字符串
     * @param maxLength 最大长度
     * @return 清理后的字符串
     */
    private String clean(String value, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (maxLength > 0 && normalized.length() > maxLength) {
            return normalized.substring(0, maxLength);
        }
        return normalized;
    }
}
