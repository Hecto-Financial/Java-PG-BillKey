package com.hecto.pg;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 헥토파이낸셜 신용카드 빌키 삭제 API 샘플
 *
 * 발급받은 빌키를 삭제하는 API입니다.
 * 삭제된 빌키는 복구할 수 없으며, 이후 빌키 결제에 사용할 수 없습니다.
 * 삭제 전 해당 빌키로 진행 중인 결제가 없는지 반드시 확인해주세요.
 *
 * API 경로  : /spay/APICardActionDelkey.do
 * 업무구분  : A1 (빌키 삭제 고정값)
 * pktHash  : trdDt + trdTm + mchtId + mchtTrdNo + "0" + licenseKey
 */
public class BillKeyDelete {

    public static void main(String[] args) throws Exception {

        /* ================================================================= */
        /* STEP 01. 환경 설정                                                  */
        /* 빌키 발급 시 사용한 MID와 동일한 MID를 사용해주세요.                    */
        /* Config.java에서 상점아이디, 키, 서버 URL을 확인해주세요.               */
        /* ================================================================= */
        String mchtId     = Config.PG_MID_AUTH;   // 빌키 발급 시 사용한 MID와 동일하게 맞춰주세요.
        String licenseKey = Config.LICENSE_KEY;
        String aesKey     = Config.AES256_KEY;
        String apiUrl     = Config.SERVER_URL + Config.PATH_BILLKEY_DELETE;

        /* ================================================================= */
        /* STEP 02. params 파라미터 설정                                        */
        /* mchtTrdNo는 삭제 요청 시 새로 생성하는 유니크한 주문번호입니다.           */
        /* ================================================================= */
        LocalDateTime now = LocalDateTime.now();
        String trdDt     = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String trdTm     = now.format(DateTimeFormatter.ofPattern("HHmmss"));
        String mchtTrdNo = "BILLKEY_DEL" + trdDt + trdTm; // 실 연동 시 유니크한 값으로 교체해주세요.

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("mchtId",    mchtId);          // 상점아이디
        params.put("ver",       Config.VER);      // 전문버전 (고정)
        params.put("method",    Config.METHOD);   // 결제수단: 신용카드 (고정)
        params.put("bizType",   "A1");            // 업무구분: 빌키 삭제 (고정)
        params.put("encCd",     Config.ENC_CD);   // 암호화구분: AES-256-ECB (고정)
        params.put("mchtTrdNo", mchtTrdNo);       // 삭제 요청용 신규 주문번호
        params.put("trdDt",     trdDt);           // 요청일자 (yyyyMMdd)
        params.put("trdTm",     trdTm);           // 요청시간 (HHmmss)
        params.put("mobileYn",  "N");             // 모바일 여부 (Y: 모바일 / N: PC)
        params.put("osType",    "W");             // OS 구분 (W: Windows / M: Mac / A: Android / I: iOS)

        /* ================================================================= */
        /* STEP 03. data 파라미터 설정                                          */
        /* billKey: 삭제할 빌키를 입력해주세요.                                  */
        /* ================================================================= */
        // TODO: DB에서 삭제할 billKey를 조회해주세요.
        String billKey = "SBILL_0123456789"; // ★ DB에서 조회한 빌키를 입력해주세요.

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pktHash", "");        // 해시값 (STEP 04에서 자동 세팅됩니다.)
        data.put("billKey", billKey);   // 삭제할 빌키
        data.put("etcInfo", "");        // 해지사유코드 (선택)

        /* ================================================================= */
        /* STEP 04. SHA-256 해시 생성 (pktHash)                                */
        /* 조합: trdDt + trdTm + mchtId + mchtTrdNo + "0" + licenseKey        */
        /* ================================================================= */
        String hashPlain = trdDt + trdTm + mchtId + mchtTrdNo + "0" + licenseKey;
        String pktHash   = CryptoUtil.sha256(hashPlain);
        data.put("pktHash", pktHash);

        System.out.println("[ STEP 04 ] SHA-256 해시 생성");
        System.out.println("  평문 : " + hashPlain);
        System.out.println("  해시 : " + pktHash);

        /* ================================================================= */
        /* STEP 05. API 호출                                                   */
        /* 요청 구조: {"params": {...}, "data": {...}}                          */
        /* ================================================================= */
        Map<String, Object> reqMap = new LinkedHashMap<>();
        reqMap.put("params", params);
        reqMap.put("data",   data);
        String requestJson = JsonUtil.toJson(reqMap);

        System.out.println("\n[ STEP 05 ] API 호출");
        System.out.println("  URL     : " + apiUrl);
        System.out.println("  요청 JSON : " + requestJson);

        String responseJson = HttpUtil.post(apiUrl, requestJson, Config.CONN_TIMEOUT, Config.READ_TIMEOUT);
        System.out.println("  응답 JSON : " + responseJson);

        /* ================================================================= */
        /* STEP 06. 응답 파싱 및 결과 처리                                       */
        /* outStatCd = "0021" 이고 outRsltCd = "0000" 이면 성공입니다.           */
        /* ================================================================= */
        String respParams = JsonUtil.getSection(responseJson, "params");
        String respData   = JsonUtil.getSection(responseJson, "data");

        String outStatCd    = JsonUtil.getValue(respParams, "outStatCd");
        String outRsltCd    = JsonUtil.getValue(respParams, "outRsltCd");
        String outRsltMsg   = JsonUtil.getValue(respParams, "outRsltMsg");
        String trdNo        = JsonUtil.getValue(respParams, "trdNo");
        String respBillKey  = JsonUtil.getValue(respData,   "billKey");

        System.out.println("\n[ STEP 06 ] 응답 결과");
        System.out.println("  거래상태코드 (outStatCd)  : " + outStatCd  + "  ← 0021 이면 성공");
        System.out.println("  거절코드     (outRsltCd)  : " + outRsltCd  + "  ← 0000 이면 정상");
        System.out.println("  결과메세지   (outRsltMsg) : " + outRsltMsg);
        System.out.println("  헥토 거래번호 (trdNo)     : " + trdNo);
        System.out.println("  삭제된 빌키   (billKey)   : " + respBillKey);

        if ("0021".equals(outStatCd) && "0000".equals(outRsltCd)) {
            System.out.println("\n[성공] 빌키 삭제가 완료되었습니다.");
            // TODO: DB에서 해당 billKey를 삭제 또는 비활성화 처리해주세요.
        } else {
            System.out.println("\n[실패] 빌키 삭제에 실패하였습니다.");
            System.out.println("       outRsltCd(거절코드)를 확인해주세요.");
        }
    }
}
