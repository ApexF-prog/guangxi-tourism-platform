package com.gxtourism.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class Md5Util {
    public static String md5(String text) {
        try {
            byte[] bytes = MessageDigest.getInstance("MD5")
                    .digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("MD5 not available", e);
        }
    }
}
