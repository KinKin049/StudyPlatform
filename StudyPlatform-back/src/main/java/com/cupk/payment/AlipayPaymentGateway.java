package com.cupk.payment;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * 支付宝支付网关实现。
 */
@Component
public class AlipayPaymentGateway implements PaymentGateway {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(8);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();
    private final String gatewayUrl;
    private final String appId;
    private final String privateKey;
    private final String privateKeyPath;
    private final String notifyUrl;

    /**
     * 构造支付宝支付网关。
     *
     * @param gatewayUrl 支付宝网关地址
     * @param appId 应用ID
     * @param privateKey 应用私钥
     * @param privateKeyPath 应用私钥文件路径
     * @param notifyUrl 异步通知地址
     */
    public AlipayPaymentGateway(
            @Value("${payment.alipay.gateway-url:https://openapi.alipay.com/gateway.do}") String gatewayUrl,
            @Value("${payment.alipay.app-id:}") String appId,
            @Value("${payment.alipay.private-key:}") String privateKey,
            @Value("${payment.alipay.private-key-path:}") String privateKeyPath,
            @Value("${payment.alipay.notify-url:}") String notifyUrl
    ) {
        this.gatewayUrl = gatewayUrl;
        this.appId = appId;
        this.privateKey = privateKey;
        this.privateKeyPath = privateKeyPath;
        this.notifyUrl = notifyUrl;
    }

    /**
     * 返回支付渠道标识。
     *
     * @return 支付渠道标识字符串
     */
    @Override
    public String provider() {
        return "ALIPAY";
    }

    /**
     * 创建支付宝扫码支付（预下单）。
     *
     * @param orderNo 商户订单号
     * @param subject 订单标题
     * @param amount 订单金额
     * @return 支付网关结果，包含二维码链接
     */
    @Override
    public PaymentGatewayResult createNativePayment(String orderNo, String subject, BigDecimal amount) {
        requireConfigured();
        Map<String, String> bizContent = new LinkedHashMap<>();
        bizContent.put("out_trade_no", orderNo);
        bizContent.put("total_amount", amount.setScale(2, RoundingMode.HALF_UP).toPlainString());
        bizContent.put("subject", subject);
        String response = post("alipay.trade.precreate", PaymentJson.object(bizContent));
        String qrCode = PaymentJson.stringValue(response, "qr_code");
        String code = PaymentJson.stringValue(response, "code");
        if (qrCode.isBlank() || !"10000".equals(code)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "支付宝预下单失败：" + resolveAlipayMessage(response));
        }
        return new PaymentGatewayResult(provider() + "-" + orderNo, provider(), qrCode, "PENDING", "", "请使用支付宝扫码支付");
    }

    /**
     * 创建支付宝网页支付（电脑网站支付）。
     *
     * @param orderNo 商户订单号
     * @param subject 订单标题
     * @param amount 订单金额
     * @param returnUrl 同步返回地址
     * @return 支付网关结果，包含跳转表单HTML
     */
    @Override
    public PaymentGatewayResult createPagePayment(String orderNo, String subject, BigDecimal amount, String returnUrl) {
        requireConfigured();
        Map<String, String> bizContent = new LinkedHashMap<>();
        bizContent.put("out_trade_no", orderNo);
        bizContent.put("total_amount", amount.setScale(2, RoundingMode.HALF_UP).toPlainString());
        bizContent.put("subject", subject);
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        String formHtml = pagePayForm("alipay.trade.page.pay", PaymentJson.object(bizContent), returnUrl);
        return new PaymentGatewayResult(provider() + "-" + orderNo, provider(), formHtml, "PENDING", "", "请在支付宝沙箱网页收银台完成付款");
    }

    /**
     * 查询支付宝订单支付状态。
     *
     * @param orderNo 商户订单号
     * @return 支付网关结果，包含支付状态
     */
    @Override
    public PaymentGatewayResult queryPayment(String orderNo) {
        requireConfigured();
        Map<String, String> bizContent = Map.of("out_trade_no", orderNo);
        String response = post("alipay.trade.query", PaymentJson.object(bizContent));
        String tradeStatus = PaymentJson.stringValue(response, "trade_status");
        String tradeNo = PaymentJson.stringValue(response, "trade_no");
        String status = switch (tradeStatus) {
            case "TRADE_SUCCESS", "TRADE_FINISHED" -> "PAID";
            case "WAIT_BUYER_PAY" -> "PENDING";
            case "TRADE_CLOSED" -> "EXPIRED";
            default -> "PENDING";
        };
        return new PaymentGatewayResult(provider() + "-" + orderNo, provider(), "", status, tradeNo, resolveAlipayMessage(response));
    }

    /**
     * 向支付宝网关发送POST请求。
     *
     * @param method 接口方法名
     * @param bizContent 业务参数JSON
     * @return 支付宝响应内容
     */
    private String post(String method, String bizContent) {
        try {
            Map<String, String> params = commonParams(method, bizContent);
            params.put("sign", sign(params));
            HttpRequest request = HttpRequest.newBuilder(URI.create(gatewayUrl))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(formEncode(params)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "支付宝接口请求失败：" + response.statusCode());
            }
            return response.body();
        } catch (java.net.http.HttpTimeoutException e) {
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "支付宝接口请求超时，请检查沙箱网关网络或稍后重试", e);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "支付宝接口请求失败：" + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "支付宝接口请求被中断", e);
        }
    }

    /**
     * 生成支付宝网页支付跳转表单HTML。
     *
     * @param method 接口方法名
     * @param bizContent 业务参数JSON
     * @param returnUrl 同步返回地址
     * @return 表单HTML字符串
     */
    private String pagePayForm(String method, String bizContent, String returnUrl) {
        Map<String, String> params = commonParams(method, bizContent);
        if (returnUrl != null && !returnUrl.isBlank()) {
            params.put("return_url", returnUrl.trim());
        }
        params.put("sign", sign(params));
        StringBuilder builder = new StringBuilder();
        builder.append("<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\">");
        builder.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
        builder.append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">");
        builder.append("<title>支付宝沙箱收银台</title>");
        builder.append("<style>body{margin:0;min-height:100vh;display:grid;place-items:center;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI','Microsoft YaHei',sans-serif;background:#f5f7fb;color:#1f2937}.box{padding:28px 32px;border-radius:16px;background:#fff;box-shadow:0 20px 60px rgba(15,23,42,.12);text-align:center}.box h1{margin:0 0 10px;font-size:22px}.box p{margin:0 0 18px;color:#64748b}.box button{border:0;border-radius:999px;padding:10px 18px;background:#1677ff;color:#fff;font-weight:700;cursor:pointer}</style>");
        builder.append("</head>");
        builder.append("<body><div class=\"box\"><h1>正在跳转到支付宝沙箱收银台</h1><p>如果页面没有自动跳转，请点击下面的按钮继续。</p>");
        builder.append("<form id=\"alipayPagePayForm\" method=\"post\" accept-charset=\"UTF-8\" action=\"")
                .append(escapeHtml(gatewayUrl))
                .append("\">");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            builder.append("<input type=\"hidden\" name=\"")
                    .append(escapeHtml(entry.getKey()))
                    .append("\" value=\"")
                    .append(escapeHtml(entry.getValue()))
                    .append("\">");
        }
        builder.append("<button type=\"submit\">前往支付宝收银台</button>");
        builder.append("</form></div><script>document.getElementById('alipayPagePayForm').submit();</script>");
        builder.append("</body></html>");
        return builder.toString();
    }

    /**
     * 构造支付宝请求公共参数。
     *
     * @param method 接口方法名
     * @param bizContent 业务参数JSON
     * @return 公共参数Map
     */
    private Map<String, String> commonParams(String method, String bizContent) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("app_id", appId.trim());
        params.put("method", method);
        params.put("format", "JSON");
        params.put("charset", "utf-8");
        params.put("sign_type", "RSA2");
        params.put("timestamp", LocalDateTime.now().format(TIME_FORMATTER));
        params.put("version", "1.0");
        params.put("biz_content", bizContent);
        if (!notifyUrl.isBlank()) {
            params.put("notify_url", notifyUrl.trim());
        }
        return params;
    }

    /**
     * 对请求参数进行RSA2签名。
     *
     * @param params 待签名参数
     * @return Base64编码的签名值
     */
    private String sign(Map<String, String> params) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(readPrivateKey());
            signature.update(canonical(params).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "支付宝签名失败：" + e.getMessage(), e);
        }
    }

    /**
     * 读取支付宝应用私钥。
     *
     * @return 应用私钥对象
     * @throws Exception 读取或解析私钥失败时抛出
     */
    private PrivateKey readPrivateKey() throws Exception {
        String key = privateKey == null || privateKey.isBlank()
                ? Files.readString(Path.of(privateKeyPath.trim()), StandardCharsets.UTF_8)
                : privateKey;
        String normalized = key
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(normalized);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    /**
     * 构造待签名字符串（按字典序拼接参数）。
     *
     * @param params 请求参数
     * @return 待签名字符串
     */
    private String canonical(Map<String, String> params) {
        List<Map.Entry<String, String>> entries = new ArrayList<>(params.entrySet());
        entries.removeIf(entry -> "sign".equals(entry.getKey()) || entry.getValue() == null || entry.getValue().isBlank());
        entries.sort(Comparator.comparing(Map.Entry::getKey));
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, String> entry : entries) {
            parts.add(entry.getKey() + "=" + entry.getValue());
        }
        return String.join("&", parts);
    }

    /**
     * 将参数进行表单URL编码。
     *
     * @param params 请求参数
     * @return URL编码后的表单字符串
     */
    private String formEncode(Map<String, String> params) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            parts.add(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                    + "="
                    + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return String.join("&", parts);
    }

    /**
     * 对字符串进行HTML转义。
     *
     * @param value 原始字符串
     * @return 转义后的字符串
     */
    private String escapeHtml(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /**
     * 解析支付宝响应消息。
     *
     * @param response 支付宝响应内容
     * @return 错误消息
     */
    private String resolveAlipayMessage(String response) {
        String subMessage = PaymentJson.stringValue(response, "sub_msg");
        if (!subMessage.isBlank()) {
            return subMessage;
        }
        String message = PaymentJson.stringValue(response, "msg");
        return message.isBlank() ? "未知错误" : message;
    }

    /**
     * 校验支付宝支付配置是否完整。
     */
    private void requireConfigured() {
        boolean hasPrivateKey = (privateKey != null && !privateKey.isBlank())
                || (privateKeyPath != null && !privateKeyPath.isBlank());
        if (appId == null || appId.isBlank() || !hasPrivateKey) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "支付宝支付未配置：请设置 payment.alipay.app-id 和应用私钥");
        }
    }
}
