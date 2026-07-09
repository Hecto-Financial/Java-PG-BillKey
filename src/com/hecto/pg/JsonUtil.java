package com.hecto.pg;

import java.util.Map;

/**
 * 헥토파이낸셜 JSON 유틸리티
 *
 * 외부 라이브러리 없이 요청 JSON 빌드 및 응답 JSON 파싱을 처리합니다.
 * 헥토파이낸셜 API의 {"params":{...}, "data":{...}} 구조에 최적화되어 있습니다.
 */
public class JsonUtil {

    /**
     * Map을 JSON 문자열로 변환
     *
     * 중첩 Map(params/data 구조)을 지원합니다.
     *
     * @param map 변환할 Map (값이 Map인 경우 중첩 JSON 객체로 처리)
     * @return JSON 문자열
     */
    @SuppressWarnings("unchecked")
    public static String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(escape(entry.getKey())).append("\":");
            Object val = entry.getValue();
            if (val instanceof Map) {
                sb.append(toJson((Map<String, Object>) val));
            } else {
                sb.append("\"").append(escape(String.valueOf(val == null ? "" : val))).append("\"");
            }
            first = false;
        }
        return sb.append("}").toString();
    }

    /**
     * 응답 JSON에서 특정 섹션(params 또는 data) 추출
     *
     * @param json    응답 JSON 전문
     * @param section 추출할 섹션 이름 ("params" 또는 "data")
     * @return 해당 섹션의 JSON 문자열, 없으면 "{}"
     */
    public static String getSection(String json, String section) {
        int idx = json.indexOf("\"" + section + "\"");
        if (idx < 0) return "{}";
        idx = json.indexOf("{", idx);
        if (idx < 0) return "{}";
        int depth = 0, start = idx;
        for (int i = idx; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return json.substring(start, i + 1);
        }
        return "{}";
    }

    /**
     * JSON 문자열에서 특정 키의 값 추출 (단순 평면 파싱)
     *
     * getSection()으로 섹션을 먼저 분리한 후 사용하십시오.
     *
     * @param json JSON 문자열
     * @param key  찾을 키
     * @return 키에 해당하는 값, 없으면 ""
     */
    public static String getValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int idx = json.indexOf(searchKey);
        if (idx < 0) return "";
        idx = json.indexOf(":", idx + searchKey.length());
        if (idx < 0) return "";
        idx++;
        while (idx < json.length() && json.charAt(idx) == ' ') idx++;
        if (idx >= json.length()) return "";

        if (json.charAt(idx) == '"') {
            // 문자열 값
            int start = idx + 1;
            int end = start;
            while (end < json.length()) {
                if (json.charAt(end) == '"' && json.charAt(end - 1) != '\\') break;
                end++;
            }
            return json.substring(start, end);
        } else {
            // 숫자/null 값
            int end = idx;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
            String result = json.substring(idx, end).trim();
            return "null".equals(result) ? "" : result;
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
