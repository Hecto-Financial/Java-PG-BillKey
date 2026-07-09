package com.hecto.pg;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 헥토파이낸셜 암복호화 유틸리티
 *
 * 지원 알고리즘
 *   - AES-256-ECB  : 요청/응답 파라미터 암복호화 (encCd = "23")
 *   - SHA-256      : pktHash 해시 생성
 *
 * JDK 표준 라이브러리(javax.crypto, java.security)만 사용하므로 별도 의존성이 없습니다.
 */
public class CryptoUtil {

    /**
     * AES-256-ECB 암호화
     *
     * 암호화 대상 파라미터: cardNo, cardPwd, idntNo, vldDtMon, vldDtYear,
     *                       trdAmt, taxAmt, vatAmt, taxFreeAmt, svcAmt,
     *                       cnclAmt (취소 시)
     *
     * @param key       32바이트 암호화 키 (Config.AES256_KEY)
     * @param plainText 암호화할 평문
     * @return Base64 인코딩된 암호문
     */
    public static String aesEncrypt(String key, String plainText) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * AES-256-ECB 복호화
     *
     * 복호화 대상 파라미터: 응답의 trdAmt, cnclAmt, blcAmt 등
     *
     * @param key        32바이트 암호화 키 (Config.AES256_KEY)
     * @param cipherText Base64 인코딩된 암호문
     * @return 복호화된 평문
     */
    public static String aesDecrypt(String key, String cipherText) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText.trim()));
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    /**
     * SHA-256 해시 생성 (pktHash 계산)
     *
     * 각 API별 해시 조합
     *   - 빌키 발급  : trdDt + trdTm + mchtId + mchtTrdNo + "0"     + licenseKey
     *   - 결제       : trdDt + trdTm + mchtId + mchtTrdNo + trdAmt  + licenseKey
     *   - 빌키 결제  : trdDt + trdTm + mchtId + mchtTrdNo + trdAmt  + licenseKey
     *   - 취소       : trdDt + trdTm + mchtId + mchtTrdNo + cnclAmt + licenseKey
     *
     * @param input 해시 입력 평문 (조합된 문자열)
     * @return 16진수 소문자 SHA-256 해시값 (64자)
     */
    public static String sha256(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
