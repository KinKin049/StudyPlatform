package com.cupk.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * 文件签名验证工具类，通过读取文件头部的魔数（Magic Number）判断文件真实类型。
 * 防止用户通过修改扩展名上传恶意文件。
 */
public final class FileSignatureValidator {
    private FileSignatureValidator() {
    }

    /**
     * 验证上传的图片文件，通过文件头魔数判断真实格式。
     *
     * @param file 上传的图片文件
     * @param allowedExtensions 允许的扩展名列表
     * @return 检测到的图片格式扩展名
     */
    public static String requireImage(MultipartFile file, List<String> allowedExtensions) {
        byte[] header = readHeader(file, 16);
        String detected = detectImage(header);
        if (detected == null || !allowedExtensions.contains(detected)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片内容与格式不匹配");
        }
        return detected;
    }

    /**
     * 验证上传的视频文件，通过文件头魔数判断真实格式。
     *
     * @param file 上传的视频文件
     * @param allowedExtensions 允许的扩展名列表
     * @return 检测到的视频格式扩展名
     */
    public static String requireVideo(MultipartFile file, List<String> allowedExtensions) {
        byte[] header = readHeader(file, 16);
        String detected = detectVideo(header);
        if (detected == null || !allowedExtensions.contains(detected)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "视频内容与格式不匹配");
        }
        return detected;
    }

    /**
     * 通过文件头魔数检测图片格式，支持jpg、png、webp。
     *
     * @param header 文件头字节数组
     * @return 图片格式扩展名，无法识别返回null
     */
    private static String detectImage(byte[] header) {
        if (startsWith(header, 0xFF, 0xD8, 0xFF)) {
            return "jpg";
        }
        if (startsWith(header, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return "png";
        }
        if (header.length >= 12
                && ascii(header, 0, 4).equals("RIFF")
                && ascii(header, 8, 4).equals("WEBP")) {
            return "webp";
        }
        return null;
    }

    /**
     * 通过文件头魔数检测视频格式，支持mp4、webm、ogg。
     *
     * @param header 文件头字节数组
     * @return 视频格式扩展名，无法识别返回null
     */
    private static String detectVideo(byte[] header) {
        if (header.length >= 12 && ascii(header, 4, 4).equals("ftyp")) {
            return "mp4";
        }
        if (startsWith(header, 0x1A, 0x45, 0xDF, 0xA3)) {
            return "webm";
        }
        if (ascii(header, 0, 4).equals("OggS")) {
            return "ogg";
        }
        return null;
    }

    /**
     * 读取文件头部指定长度的字节。
     *
     * @param file 上传的文件
     * @param length 需要读取的字节长度
     * @return 文件头字节数组
     */
    private static byte[] readHeader(MultipartFile file, int length) {
        try (InputStream input = file.getInputStream()) {
            return input.readNBytes(length);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件内容读取失败", ex);
        }
    }

    /**
     * 判断字节数组是否以指定的魔数开头。
     *
     * @param source 源字节数组
     * @param values 期望的魔数序列
     * @return 匹配返回true，否则返回false
     */
    private static boolean startsWith(byte[] source, int... values) {
        if (source.length < values.length) {
            return false;
        }
        for (int i = 0; i < values.length; i += 1) {
            if ((source[i] & 0xFF) != values[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 将字节数组中指定范围的字节转换为ASCII字符串。
     *
     * @param source 源字节数组
     * @param offset 起始偏移量
     * @param length 读取长度
     * @return 转换后的ASCII字符串
     */
    private static String ascii(byte[] source, int offset, int length) {
        if (source.length < offset + length) {
            return "";
        }
        StringBuilder builder = new StringBuilder(length);
        for (int i = offset; i < offset + length; i += 1) {
            builder.append((char) source[i]);
        }
        return builder.toString();
    }
}
