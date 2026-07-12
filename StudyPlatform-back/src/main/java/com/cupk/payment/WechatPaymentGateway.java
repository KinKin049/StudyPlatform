package com.cupk.payment;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
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
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * 微信支付网关实现。
 */
@Component
public class WechatPaymentGateway implements PaymentGateway {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(8);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();
    private final String baseUrl;
    private final String appId;
    private final String mchId;
    private final String serialNo;
    private final String privateKey;
    private final String privateKeyPath;
    private final String notifyUrl;

    /**
     * 构造微信支付网关。
     *
     * @param baseUrl 微信支付网关地址
     * @param appId 应用ID
     * @param mchId 商户号
     * @param serialNo 商户证书序列号
     * @param privateKey 商户私钥
     * @param privateKeyPath 商户私钥文件路径
     * @param notifyUrl 异步通知地址
     */
    public WechatPaymentGateway(
            @Value("${payment.wechat.base-url:https://api.mch.weixin.qq.com}") String baseUrl,
            @Value("${payment.wechat.app-id:}") String appId,
            @Value("${payment.wechat.mch-id:}") String mchId,
            @Value("${payment.wechat.serial-no:}") String serialNo,
            @Value("${payment.wechat.private-key:}") String privateKey,
            @Value("${payment.wechat.private-key-path:}") String privateKeyPath,
            @Value("${payment.wechat.notify-url:}") String notifyUrl
    ) {
        this.baseUrl = baseUrl;
        this.appId = appId;
        this.mchId = mchId;
        this.serialNo = serialNo;
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
        return "WECHAT";
    }

    /**
     * 创建微信扫码支付（Native下单）。
     *
     * @param orderNo 商户订单号
     * @param subject 订单标题
     * @param amount 订单金额
     * @return 支付网关结果，包含二维码链接
     */
    @Override
    public PaymentGatewayResult createNativePayment(String orderNo, String subject, BigDecimal amount) {
        requireConfigured();
        Map<String, Object> amountJson = Map.of("total", amountToCents(amount));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("appid", appId.trim());
        body.put("mchid", mchId.trim());
        body.put("description", subject);
        body.put("out_trade_no", orderNo);
        body.put("notify_url", notifyUrl == null || notifyUrl.isBlank() ? "https://localhost/wechat-pay/notify" : notifyUrl.trim());
        body.put("amount", amountJson);
        String response = request("POST", "/v3/pay/transactions/native", PaymentJson.object(body));
        String codeUrl = PaymentJson.stringValue(response, "code_url");
        if (codeUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "微信 Native 下单失败：" + response);
        }
        return new PaymentGatewayResult(provider() + "-" + orderNo, provider(), codeUrl, "PENDING", "", "请使用微信扫码支付");
    }

    /**
     * 查询微信订单支付状态。
     *
     * @param orderNo 商户订单号
     * @return 支付网关结果，包含支付状态
     */
    @Override
    public PaymentGatewayResult queryPayment(String orderNo) {
        requireConfigured();
        String response = request("GET", "/v3/pay/transactions/out-trade-no/" + orderNo + "?mchid=" + mchId.trim(), "");
        String tradeState = PaymentJson.stringValue(response, "trade_state");
        String transactionId = PaymentJson.stringValue(response, "transaction_id");
        String status = switch (tradeState) {
            case "SUCCESS" -> "PAID";
            case "CLOSED", "REVOKED", "PAYERROR" -> "EXPIRED";
            default -> "PENDING";
        };
        String message = PaymentJson.stringValue(response, "trade_state_desc");
        return new PaymentGatewayResult(provider() + "-" + orderNo, provider(), "", status, transactionId, message.isBlank() ? tradeState : message);
    }

    /**
     * 向微信支付网关发送HTTP请求。
     *
     * @param method HTTP方法（GET或POST）
     * @param pathAndQuery 请求路径及查询参数
     * @param body 请求体内容
     * @return 微信支付响应内容
     */
    private String request(String method, String pathAndQuery, String body) {
        try {
            URI uri = URI.create(baseUrl + pathAndQuery);
            String token = authorization(method, pathAndQuery, body);
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "application/json")
                    .header("Authorization", token);
            if ("POST".equals(method)) {
                builder.header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            } else {
                builder.GET();
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "微信支付接口请求失败：" + response.statusCode() + " " + response.body());
            }
            return response.body();
        } catch (java.net.http.HttpTimeoutException e) {
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "微信支付接口请求超时，请检查网络或稍后重试", e);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "微信支付接口请求失败：" + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "微信支付接口请求被中断", e);
        }
    }

    /**
     * 生成微信支付Authorization请求头。
     *
     * @param method HTTP方法
     * @param pathAndQuery 请求路径及查询参数
     * @param body 请求体内容
     * @return Authorization头字符串
     */
    private String authorization(String method, String pathAndQuery, String body) {
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String message = method + "\n" + pathAndQuery + "\n" + timestamp + "\n" + nonce + "\n" + body + "\n";
        String signature = sign(message);
        return "WECHATPAY2-SHA256-RSA2048 "
                + "mchid=\"" + mchId.trim() + "\","
                + "nonce_str=\"" + nonce + "\","
                + "timestamp=\"" + timestamp + "\","
                + "serial_no=\"" + serialNo.trim() + "\","
                + "signature=\"" + signature + "\"";
    }

    /**
     * 对消息进行RSA-SHA256签名。
     *
     * @param message 待签名消息
     * @return Base64编码的签名值
     */
    private String sign(String message) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(readPrivateKey());
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "微信支付签名失败：" + e.getMessage(), e);
        }
    }

    /**
     * 读取微信支付商户私钥。
     *
     * @return 商户私钥对象
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
     * 将金额转换为分（整数）。
     *
     * @param amount 订单金额
     * @return 金额对应的分值
     */
    private int amountToCents(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    /**
     * 校验微信支付配置是否完整。
     */
    private void requireConfigured() {
        boolean hasPrivateKey = (privateKey != null && !privateKey.isBlank())
                || (privateKeyPath != null && !privateKeyPath.isBlank());
        if (appId == null || appId.isBlank()
                || mchId == null || mchId.isBlank()
                || serialNo == null || serialNo.isBlank()
                || !hasPrivateKey) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "微信支付未配置：请设置 payment.wechat.app-id、mch-id、serial-no 和商户私钥");
        }
    }
}
