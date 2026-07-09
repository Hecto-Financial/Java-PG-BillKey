package com.hecto.pg;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 헥토파이낸셜 API 통신 유틸리티
 *
 * HTTP POST 방식으로 JSON 요청을 전송하고 JSON 응답을 반환합니다.
 * JDK 표준 라이브러리(java.net)만 사용하므로 별도 의존성이 없습니다.
 */
public class HttpUtil {

    /**
     * HTTP POST 요청 전송
     *
     * @param urlStr      요청 URL (Config.SERVER_URL + Config.PATH_*)
     * @param jsonBody    전송할 JSON 문자열 {"params":{...}, "data":{...}}
     * @param connTimeout 연결 타임아웃 (ms, Config.CONN_TIMEOUT)
     * @param readTimeout 수신 타임아웃 (ms, Config.READ_TIMEOUT)
     * @return 헥토파이낸셜 서버 응답 JSON 문자열
     */
    public static String post(String urlStr, String jsonBody, int connTimeout, int readTimeout) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = URI.create(urlStr).toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(connTimeout);
            conn.setReadTimeout(readTimeout);
            conn.setDoOutput(true);

            // 요청 전송
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            // 응답 수신 (4xx/5xx는 ErrorStream에서 수신, null 반환 시 빈 문자열 처리)
            int statusCode = conn.getResponseCode();
            InputStream is = statusCode < 400 ? conn.getInputStream() : conn.getErrorStream();
            if (is == null) return "";
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
