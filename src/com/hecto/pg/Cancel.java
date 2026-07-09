package com.hecto.pg;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 헥토파이낸셜 신용카드 취소 API 샘플
 *
 * Payment.java 또는 BillKeyPayment.java로 결제된 건을 취소하는 API입니다.
 * 전체 취소와 부분 취소를 모두 지원하며, cnclOrd(취소회차)로 구분합니다.
 * orgTrdNo는 결제 응답의 trdNo 값을 사용해주세요.
 *
 * API 경로  : /spay/APICancel.do
 * 업무구분  : C0 (취소 고정값)
 * pktHash  : trdDt + trdTm + mchtId + mchtTrdNo + cnclAmt(평문) + licenseKey
 *             ※ cnclAmt는 암호화 전 평문값으로 해시를 생성해주세요.
 * 취소 MID  : 결제 시 사용한 MID와 동일한 MID를 사용해주세요.
 */
public class Cancel {

    public static void main(String[] args) throws Exception {

        /* ================================================================= */
        /* STEP 01. 환경 설정                                                  */
        /* Config.java에서 상점아이디, 키, 서버 URL을 확인해주세요.               */
        /* ================================================================= */
        String mchtId     = Config.PG_MID_AUTH;   // 결제(또는 빌키 발급) 시 사용한 MID와 동일하게 맞춰주세요.
        String licenseKey = Config.LICENSE_KEY;    // SHA-256 해시 생성용 라이센스 키
        String aesKey     = Config.AES256_KEY;     // AES-256-ECB 암복호화 키
        String apiUrl     = Config.SERVER_URL + Config.PATH_CANCEL;

        /* ================================================================= */
        /* STEP 02. params 파라미터 설정                                        */
        /* mchtTrdNo는 취소 요청 시 새로 생성하는 유니크한 주문번호입니다.           */
        /* 원거래의 주문번호가 아니므로 주의해주세요.                               */
        /* ================================================================= */
        LocalDateTime now = LocalDateTime.now();
        String trdDt     = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String trdTm     = now.format(DateTimeFormatter.ofPattern("HHmmss"));
        String mchtTrdNo = "CANCEL" + trdDt + trdTm; // 실 연동 시 유니크한 값으로 교체해주세요.

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("mchtId",    mchtId);          // 취소 전용 상점아이디
        params.put("ver",       Config.VER);      // 전문버전 (고정)
        params.put("method",    Config.METHOD);   // 결제수단: 신용카드 (고정)
        params.put("bizType",   "C0");            // 업무구분: 취소 (고정)
        params.put("encCd",     Config.ENC_CD);   // 암호화구분: AES-256-ECB (고정)
        params.put("mchtTrdNo", mchtTrdNo);       // 취소 요청용 신규 주문번호
        params.put("trdDt",     trdDt);           // 요청일자 (yyyyMMdd)
        params.put("trdTm",     trdTm);           // 요청시간 (HHmmss)
        params.put("mobileYn",  "N");             // 모바일 여부 (Y: 모바일 / N: PC)
        params.put("osType",    "W");             // OS 구분 (W: Windows / M: Mac / A: Android / I: iOS)

        /* ================================================================= */
        /* STEP 03. data 파라미터 설정                                          */
        /* orgTrdNo: 결제 응답에서 받은 trdNo(헥토파이낸셜 거래번호)를 입력해주세요.  */
        /* cnclAmt: STEP 04 해시 생성에도 사용되므로 평문 상태로 먼저 세팅해주세요.   */
        /* cnclOrd: 부분취소 시 이전 취소회차보다 큰 값을 입력해주세요. (001부터 시작) */
        /* ================================================================= */
        String cnclAmt = "1000";  // 취소금액 (평문 - 해시 생성 후 암호화됩니다.)
        // TODO: DB에서 취소할 거래의 trdNo를 조회해주세요.
        String orgTrdNo = "STFP_PGVAnx_mid_il0000000000000000000000000"; // 결제 응답의 trdNo로 교체해주세요.

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pktHash",    "");            // 해시값 (STEP 04에서 자동 세팅됩니다.)
        data.put("orgTrdNo",   orgTrdNo);      // 원거래번호 (결제 응답의 trdNo)
        data.put("cnclAmt",    cnclAmt);       // 취소금액          → STEP 05에서 암호화
        data.put("crcCd",      Config.CRC_CD); // 통화구분: KRW (고정)
        data.put("cnclOrd",    "001");         // 취소회차 (부분취소 시 순차 증가, 001→002→003...)
        data.put("cnclRsn",    "상품이 마음에 들지 않습니다."); // 취소사유 (선택)
        data.put("taxTypeCd",  "N");           // 과세구분 (N: 과세 / Y: 면세 / G: 복합과세)
        data.put("taxAmt",     "");            // 과세금액 (복합과세 시 필수)
        data.put("vatAmt",     "");            // 부가세금액 (복합과세 시 필수)
        data.put("taxFreeAmt", "");            // 비과세금액 (복합과세 시 필수)
        data.put("svcAmt",     "");            // 봉사료 (선택)

        /* ================================================================= */
        /* STEP 04. SHA-256 해시 생성 (pktHash)                                */
        /* 조합: trdDt + trdTm + mchtId + mchtTrdNo + cnclAmt(평문) + licenseKey */
        /* ※ cnclAmt는 암호화 전 평문값을 사용해주세요.                           */
        /* ================================================================= */
        String hashPlain = trdDt + trdTm + mchtId + mchtTrdNo + cnclAmt + licenseKey;
        String pktHash   = CryptoUtil.sha256(hashPlain);
        data.put("pktHash", pktHash);

        System.out.println("[ STEP 04 ] SHA-256 해시 생성");
        System.out.println("  평문 : " + (Config.DEBUG ? hashPlain : hashPlain.replace(licenseKey, "***")));
        System.out.println("  해시 : " + pktHash);

        /* ================================================================= */
        /* STEP 05. AES-256-ECB 암호화                                         */
        /* 암호화 대상: cnclAmt, taxAmt, vatAmt, taxFreeAmt, svcAmt            */
        /* 빈 값은 암호화 대상에서 자동 제외됩니다.                               */
        /* ================================================================= */
        String[] encryptTargets = {"cnclAmt", "taxAmt", "vatAmt", "taxFreeAmt", "svcAmt"};

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
        String encCnclAmt = JsonUtil.getValue(respData,   "cnclAmt");
        String encBlcAmt  = JsonUtil.getValue(respData,   "blcAmt");

        /* ================================================================= */
        /* STEP 08. 응답 AES-256-ECB 복호화                                    */
        /* 복호화 대상: cnclAmt(취소금액), blcAmt(취소가능잔액)                    */
        /* ================================================================= */
        String decCnclAmt = "";
        String decBlcAmt  = "";
        if (!encCnclAmt.isEmpty()) decCnclAmt = CryptoUtil.aesDecrypt(aesKey, encCnclAmt);
        if (!encBlcAmt.isEmpty())  decBlcAmt  = CryptoUtil.aesDecrypt(aesKey, encBlcAmt);

        System.out.println("\n[ STEP 08 ] 응답 결과");
        System.out.println("  거래상태코드  (outStatCd)  : " + outStatCd  + "  ← 0021 이면 성공");
        System.out.println("  거절코드      (outRsltCd)  : " + outRsltCd  + "  ← 0000 이면 정상");
        System.out.println("  결과메세지    (outRsltMsg) : " + outRsltMsg);
        System.out.println("  헥토 거래번호  (trdNo)     : " + trdNo);
        System.out.println("  취소금액      (cnclAmt)    : " + decCnclAmt);
        System.out.println("  취소가능잔액   (blcAmt)    : " + decBlcAmt);

        if ("0021".equals(outStatCd) && "0000".equals(outRsltCd)) {
            System.out.println("\n[성공] 취소가 완료되었습니다.");
            // TODO: 취소 결과를 DB에 업데이트해주세요.
        } else {
            System.out.println("\n[실패] 취소에 실패하였습니다.");
            System.out.println("       outRsltCd(거절코드)를 확인해주세요.");
        }
    }
}
