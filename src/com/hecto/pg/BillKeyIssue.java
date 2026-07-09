package com.hecto.pg;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 헥토파이낸셜 신용카드 빌키 발급 API 샘플
 *
 * 결제 없이 빌키(자동결제키)만 발급받는 API입니다.
 * 발급된 빌키는 BillKeyPayment.java에서 정기/반복 결제에 사용해주세요.
 *
 * API 경로  : /spay/APICardAuth.do
 * 업무구분  : A4 (빌키 발급 고정값)
 * pktHash  : trdDt + trdTm + mchtId + mchtTrdNo + "0" + licenseKey
 *             ※ 빌키 발급은 금액이 없으므로 금액 자리에 반드시 "0"을 사용해주세요.
 */
public class BillKeyIssue {

    public static void main(String[] args) throws Exception {

        /* ================================================================= */
        /* STEP 01. 환경 설정                                                  */
        /* Config.java에서 상점아이디, 키, 서버 URL을 확인해주세요.               */
        /* ================================================================= */
        String mchtId     = Config.PG_MID_AUTH;   // 구인증 상점아이디 (빌키 발급은 구인증만 지원)
        String licenseKey = Config.LICENSE_KEY;   // SHA-256 해시 생성용 라이센스 키
        String aesKey     = Config.AES256_KEY;    // AES-256-ECB 암복호화 키
        String apiUrl     = Config.SERVER_URL + Config.PATH_BILLKEY_ISSUE;

        /* ================================================================= */
        /* STEP 02. params 파라미터 설정                                        */
        /* 요청일자/시간은 현재 시각으로 자동 세팅됩니다.                           */
        /* mchtTrdNo는 상점에서 생성하는 유니크한 주문번호를 사용해주세요.            */
        /* ================================================================= */
        LocalDateTime now = LocalDateTime.now();
        String trdDt     = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String trdTm     = now.format(DateTimeFormatter.ofPattern("HHmmss"));
        String mchtTrdNo = "AUTH_API" + trdDt + trdTm;  // 실 연동 시 유니크한 값으로 교체해주세요.

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("mchtId",    mchtId);          // 상점아이디
        params.put("ver",       Config.VER);      // 전문버전 (고정)
        params.put("method",    Config.METHOD);   // 결제수단: 신용카드 (고정)
        params.put("bizType",   "A4");            // 업무구분: 빌키 발급 (고정)
        params.put("encCd",     Config.ENC_CD);   // 암호화구분: AES-256-ECB (고정)
        params.put("mchtTrdNo", mchtTrdNo);       // 상점주문번호
        params.put("trdDt",     trdDt);           // 요청일자 (yyyyMMdd)
        params.put("trdTm",     trdTm);           // 요청시간 (HHmmss)
        params.put("mobileYn",  "N");             // 모바일 여부 (Y: 모바일 / N: PC)
        params.put("osType",    "W");             // OS 구분 (W: Windows / M: Mac / A: Android / I: iOS)

        /* ================================================================= */
        /* STEP 03. data 파라미터 설정                                          */
        /* 카드 정보는 STEP 05에서 AES-256-ECB 암호화 후 전송됩니다.              */
        /* 테스트 환경의 카드번호를 사용해주세요. 실 카드번호는 운영 환경에서만.       */
        /* ================================================================= */
        String cardNo    = "5221120000001621"; // 카드번호 (테스트용)
        String idntNo    = "620817";           // 생년월일 6자리 또는 사업자번호 10자리
        String vldDtMon  = "11";              // 유효기간 월 (MM)
        String vldDtYear = "25";              // 유효기간 년 (YY)
        String cardPwd   = "00";              // 카드 비밀번호 앞 2자리

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pktHash",    "");            // 해시값 (STEP 04에서 자동 세팅됩니다.)
        data.put("cardNo",     cardNo);        // 카드번호          → STEP 05에서 암호화
        data.put("idntNo",     idntNo);        // 식별번호          → STEP 05에서 암호화
        data.put("vldDtMon",   vldDtMon);     // 유효기간 월       → STEP 05에서 암호화
        data.put("vldDtYear",  vldDtYear);    // 유효기간 년       → STEP 05에서 암호화
        data.put("cardPwd",    cardPwd);       // 카드 비밀번호     → STEP 05에서 암호화
        data.put("mchtCustNm", "홍길동");      // 고객명 (선택)
        data.put("mchtCustId", "HongGilDong");// 고객 아이디 (선택)
        data.put("keyRegYn",   "Y");           // 빌키 발급 요청 여부 (Y: 발급 / N: 미발급)

        /* ================================================================= */
        /* STEP 04. SHA-256 해시 생성 (pktHash)                                */
        /* 조합: trdDt + trdTm + mchtId + mchtTrdNo + "0" + licenseKey        */
        /* ※ 빌키 발급은 금액이 없으므로 "0"(문자)을 고정으로 넣어주세요.           */
        /* ================================================================= */
        String hashPlain = trdDt + trdTm + mchtId + mchtTrdNo + "0" + licenseKey;
        String pktHash   = CryptoUtil.sha256(hashPlain);
        data.put("pktHash", pktHash);

        System.out.println("[ STEP 04 ] SHA-256 해시 생성");
        System.out.println("  평문 : " + hashPlain);
        System.out.println("  해시 : " + pktHash);

        /* ================================================================= */
        /* STEP 05. AES-256-ECB 암호화                                         */
        /* 암호화 대상: cardNo, idntNo, vldDtMon, vldDtYear, cardPwd           */
        /* 빈 값은 암호화 대상에서 제외됩니다.                                    */
        /* ================================================================= */
        String[] encryptTargets = {"cardNo", "idntNo", "vldDtMon", "vldDtYear", "cardPwd"};

        System.out.println("\n[ STEP 05 ] AES-256-ECB 암호화");
        for (String key : encryptTargets) {
            String plain = (String) data.get(key);
            if (plain != null && !plain.isEmpty()) {
                String cipher = CryptoUtil.aesEncrypt(aesKey, plain);
                data.put(key, cipher);
                System.out.printf("  %-12s : [%s] → [%s]%n", key, plain, cipher);
            }
        }

        /* ================================================================= */
        /* STEP 06. API 호출                                                   */
        /* 요청 구조: {"params": {...}, "data": {...}}                          */
        /* params, data 키 이름은 변경하지 말아주세요.                            */
        /* ================================================================= */
        Map<String, Object> reqMap = new LinkedHashMap<>();
        reqMap.put("params", params);
        reqMap.put("data",   data);
        String requestJson = JsonUtil.toJson(reqMap);

        System.out.println("\n[ STEP 06 ] API 호출");
        System.out.println("  URL     : " + apiUrl);
        System.out.println("  요청 JSON : " + requestJson);

        String responseJson = HttpUtil.post(apiUrl, requestJson, Config.CONN_TIMEOUT, Config.READ_TIMEOUT);
        System.out.println("  응답 JSON : " + responseJson);

        /* ================================================================= */
        /* STEP 07. 응답 파싱 및 결과 처리                                       */
        /* outStatCd = "0021" 이고 outRsltCd = "0000" 이면 성공입니다.           */
        /* 실패 시 outRsltCd(거절코드)를 확인해주세요.                             */
        /* ================================================================= */
        String respParams = JsonUtil.getSection(responseJson, "params");
        String respData   = JsonUtil.getSection(responseJson, "data");

        String outStatCd  = JsonUtil.getValue(respParams, "outStatCd");   // 거래상태코드
        String outRsltCd  = JsonUtil.getValue(respParams, "outRsltCd");   // 거절코드
        String outRsltMsg = JsonUtil.getValue(respParams, "outRsltMsg");  // 결과 메세지
        String trdNo      = JsonUtil.getValue(respParams, "trdNo");       // 헥토파이낸셜 거래번호
        String billKey    = JsonUtil.getValue(respData,   "billKey");     // ★ 발급된 빌키
        String cardNm     = JsonUtil.getValue(respData,   "cardNm");      // 카드사명
        String cardKind   = JsonUtil.getValue(respData,   "cardKind");    // 카드종류

        System.out.println("\n[ STEP 07 ] 응답 결과");
        System.out.println("  거래상태코드 (outStatCd)  : " + outStatCd  + "  ← 0021 이면 성공");
        System.out.println("  거절코드     (outRsltCd)  : " + outRsltCd  + "  ← 0000 이면 정상");
        System.out.println("  결과메세지   (outRsltMsg) : " + outRsltMsg);
        System.out.println("  헥토 거래번호 (trdNo)     : " + trdNo);
        System.out.println("  카드사명     (cardNm)     : " + cardNm);
        System.out.println("  카드종류     (cardKind)   : " + cardKind);
        System.out.println("  ★ 빌키      (billKey)    : " + billKey);

        if ("0021".equals(outStatCd) && "0000".equals(outRsltCd)) {
            System.out.println("\n[성공] 빌키 발급이 완료되었습니다.");
            // TODO: 발급된 billKey를 DB에 저장해주세요. (이후 BillKeyPayment.java에서 사용)
        } else {
            System.out.println("\n[실패] 빌키 발급에 실패하였습니다.");
            System.out.println("       outRsltCd(거절코드)를 확인해주세요.");
        }
    }
}
