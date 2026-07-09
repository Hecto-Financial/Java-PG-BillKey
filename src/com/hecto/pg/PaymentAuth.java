package com.hecto.pg;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 헥토파이낸셜 신용카드 구인증 결제 API 샘플 (빌키 발급 포함)
 *
 * 구인증 방식: 카드번호 + 유효기간 + 생년월일(식별번호) + 비밀번호 앞 2자리
 * 상점 아이디 설정에 따라 결제 완료 후 응답에 billKey가 함께 발급됩니다.
 * 빌키만 발급하려면 BillKeyIssue.java를, 비인증 결제는 PaymentNoAuth.java를 사용해주세요.
 *
 * API 경로  : /spay/APICardActionPay.do
 * 업무구분  : B0 (결제 고정값)
 * pktHash  : trdDt + trdTm + mchtId + mchtTrdNo + trdAmt(평문) + licenseKey
 *             ※ trdAmt는 암호화 전 평문값으로 해시를 생성해주세요.
 */
public class PaymentAuth {

    public static void main(String[] args) throws Exception {

        /* ================================================================= */
        /* STEP 01. 환경 설정                                                  */
        /* 구인증 MID(PG_MID_AUTH)를 사용합니다.                               */
        /* Config.java에서 상점아이디, 키, 서버 URL을 확인해주세요.               */
        /* ================================================================= */
        String mchtId     = Config.PG_MID_AUTH;   // 구인증 상점아이디
        String licenseKey = Config.LICENSE_KEY;
        String aesKey     = Config.AES256_KEY;
        String apiUrl     = Config.SERVER_URL + Config.PATH_PAYMENT;

        /* ================================================================= */
        /* STEP 02. params 파라미터 설정                                        */
        /* mchtTrdNo는 결제마다 유니크한 값을 사용해주세요.                        */
        /* ================================================================= */
        LocalDateTime now = LocalDateTime.now();
        String trdDt     = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String trdTm     = now.format(DateTimeFormatter.ofPattern("HHmmss"));
        String mchtTrdNo = "AUTH_PAY" + trdDt + trdTm;

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("mchtId",    mchtId);
        params.put("ver",       Config.VER);
        params.put("method",    Config.METHOD);
        params.put("bizType",   "B0");
        params.put("encCd",     Config.ENC_CD);
        params.put("mchtTrdNo", mchtTrdNo);
        params.put("trdDt",     trdDt);
        params.put("trdTm",     trdTm);
        params.put("mobileYn",  "N");             // 모바일 여부 (Y: 모바일 / N: PC)
        params.put("osType",    "W");             // OS 구분 (W: Windows / M: Mac / A: Android / I: iOS)

        /* ================================================================= */
        /* STEP 03. data 파라미터 설정                                          */
        /* 구인증은 idntNo(생년월일/사업자번호), cardPwd(비밀번호 앞 2자리)가 필수입니다. */
        /* trdAmt는 STEP 04 해시 생성에도 사용되므로 평문으로 먼저 세팅해주세요.      */
        /* ================================================================= */
        String trdAmt    = "1000";
        String cardNo    = "1111222233334444"; // 카드번호 (테스트용)
        String vldDtYear = "27";              // 유효기간 년 (YY)
        String vldDtMon  = "12";              // 유효기간 월 (MM)
        String idntNo    = "991231";           // 생년월일 6자리 또는 사업자번호 10자리
        String cardPwd   = "00";              // 카드 비밀번호 앞 2자리

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pktHash",    "");
        data.put("pmtprdNm",   "테스트상품");
        data.put("mchtCustNm", "홍길동");
        data.put("mchtCustId", "HongGilDong");
        data.put("email",      "HongGilDong@example.com");
        data.put("cardNo",     cardNo);        // → STEP 05에서 암호화
        data.put("vldDtYear",  vldDtYear);    // → STEP 05에서 암호화
        data.put("vldDtMon",   vldDtMon);     // → STEP 05에서 암호화
        data.put("idntNo",     idntNo);        // → STEP 05에서 암호화 (구인증 필수)
        data.put("cardPwd",    cardPwd);       // → STEP 05에서 암호화 (구인증 필수)
        data.put("instmtMon",  "00");          // 할부개월수 (00: 일시불 / 02~12: 할부)
        data.put("crcCd",      Config.CRC_CD);
        data.put("taxTypeCd",  "N");           // 과세구분 (N: 과세 / Y: 면세 / G: 복합과세)
        data.put("trdAmt",     trdAmt);        // → STEP 05에서 암호화
        data.put("taxAmt",     "");            // 과세금액 (복합과세 시 필수)
        data.put("vatAmt",     "");            // 부가세금액 (복합과세 시 필수)
        data.put("taxFreeAmt", "");            // 비과세금액 (복합과세 시 필수)
        data.put("svcAmt",     "");            // 봉사료 (선택)
        data.put("notiUrl",    "https://example.com/notiUrl"); // 결과통보 수신 URL로 변경해주세요.
        data.put("mchtParam",  "");

        /* ================================================================= */
        /* STEP 04. SHA-256 해시 생성 (pktHash)                                */
        /* 조합: trdDt + trdTm + mchtId + mchtTrdNo + trdAmt(평문) + licenseKey */
        /* ================================================================= */
        String hashPlain = trdDt + trdTm + mchtId + mchtTrdNo + trdAmt + licenseKey;
        String pktHash   = CryptoUtil.sha256(hashPlain);
        data.put("pktHash", pktHash);

        System.out.println("[ STEP 04 ] SHA-256 해시 생성");
        System.out.println("  평문 : " + hashPlain.replace(licenseKey, "***"));
        System.out.println("  해시 : " + pktHash);

        /* ================================================================= */
        /* STEP 05. AES-256-ECB 암호화                                         */
        /* 구인증 암호화 대상: cardNo, cardPwd, idntNo, vldDtMon, vldDtYear,    */
        /*                    trdAmt, taxAmt, vatAmt, taxFreeAmt, svcAmt       */
        /* ================================================================= */
        String[] encryptTargets = {
            "cardNo", "cardPwd", "idntNo", "vldDtMon", "vldDtYear",
            "trdAmt", "taxAmt", "vatAmt", "taxFreeAmt", "svcAmt"
        };

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
        /* ================================================================= */
        String respParams = JsonUtil.getSection(responseJson, "params");
        String respData   = JsonUtil.getSection(responseJson, "data");

        String outStatCd  = JsonUtil.getValue(respParams, "outStatCd");
        String outRsltCd  = JsonUtil.getValue(respParams, "outRsltCd");
        String outRsltMsg = JsonUtil.getValue(respParams, "outRsltMsg");
        String trdNo      = JsonUtil.getValue(respParams, "trdNo");
        String billKey    = JsonUtil.getValue(respData,   "billKey");
        String cardNm     = JsonUtil.getValue(respData,   "cardNm");
        String apprNo     = JsonUtil.getValue(respData,   "apprNo");
        String encTrdAmt  = JsonUtil.getValue(respData,   "trdAmt");

        /* ================================================================= */
        /* STEP 08. 응답 AES-256-ECB 복호화                                    */
        /* 복호화 대상: trdAmt                                                 */
        /* ================================================================= */
        String decTrdAmt = encTrdAmt.isEmpty() ? "" : CryptoUtil.aesDecrypt(aesKey, encTrdAmt);

        System.out.println("\n[ STEP 08 ] 응답 결과");
        System.out.println("  거래상태코드 (outStatCd)  : " + outStatCd  + "  ← 0021 이면 성공");
        System.out.println("  거절코드     (outRsltCd)  : " + outRsltCd  + "  ← 0000 이면 정상");
        System.out.println("  결과메세지   (outRsltMsg) : " + outRsltMsg);
        System.out.println("  헥토 거래번호 (trdNo)     : " + trdNo);
        System.out.println("  카드사명     (cardNm)     : " + cardNm);
        System.out.println("  승인번호     (apprNo)     : " + apprNo);
        System.out.println("  거래금액     (trdAmt)     : " + decTrdAmt);
        System.out.println("  ★ 빌키      (billKey)    : " + billKey);

        if ("0021".equals(outStatCd) && "0000".equals(outRsltCd)) {
            System.out.println("\n[성공] 결제가 완료되었습니다.");
            // TODO: 결제 결과(trdNo, decTrdAmt 등)를 DB에 저장해주세요.
            if (!billKey.isEmpty()) {
                // TODO: 빌키가 발급된 경우 billKey를 DB에 저장해주세요. (이후 BillKeyPayment.java에서 사용)
            }
        } else {
            System.out.println("\n[실패] 결제에 실패하였습니다.");
            System.out.println("       outRsltCd(거절코드)를 확인해주세요.");
        }
    }
}
