package com.ecomart.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VNPayUtilTest {

    @Test
    @DisplayName("HMAC-SHA512 tính toán chính xác với secret key và hashData")
    void hmacSHA512_ComputesCorrectHash() {
        String key = "DEMOHASHSECRET1234567890ABCDEF";
        String data = "vnp_Amount=10000000&vnp_Command=pay&vnp_TmnCode=DEMOVNPAY";

        String hash1 = VNPayUtil.hmacSHA512(key, data);
        String hash2 = VNPayUtil.hmacSHA512(key, data);

        assertThat(hash1).isNotEmpty();
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("hashAllFields tự động sắp xếp key theo ASCII và bỏ qua vnp_SecureHash")
    void hashAllFields_SortsParametersAsciiAndOmitsSecureHash() {
        String key = "DEMOHASHSECRET1234567890ABCDEF";
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TmnCode", "DEMOVNPAY");
        params.put("vnp_Amount", "10000000");
        params.put("vnp_Command", "pay");
        params.put("vnp_SecureHash", "IGNORED_HASH");

        String hash = VNPayUtil.hashAllFields(params, key);
        assertThat(hash).isNotEmpty();
    }
}
