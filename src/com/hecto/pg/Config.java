package com.hecto.pg;

/**
 * 헥토파이낸셜 PG 연동 설정
 *
 * ※ 보안 주의사항
 *    - LICENSE_KEY, AES256_KEY 는 외부에 절대 노출되어서는 안 됩니다.
 *    - 운영 환경에서는 환경변수 또는 KMS를 통해 주입하십시오.
 *    - 상용 MID 및 키는 헥토파이낸셜 영업 담당자에게 발급 요청하십시오.
 */
public class Config {

    /* ===== 상점 아이디 (MID) ===== */
    /** 구인증 MID - 카드번호 + 유효기간 + 생년월일 + 비밀번호 */
    public static final String PG_MID_AUTH   = "nxca_ks_gu";

    /** 비인증 MID - 카드번호 + 유효기간만 */
    public static final String PG_MID_NOAUTH = "nxca_jt_bi";

    /* ===== 키 정보 (외부 노출 금지) ===== */
    /** SHA-256 해시 생성용 라이센스 키 */
    public static final String LICENSE_KEY = "ST1009281328226982205";

    /** AES-256-ECB 파라미터 암복호화 키 */
    public static final String AES256_KEY = "pgSettle30y739r82jtd709yOfZ2yK5K";

    /* ===== 디버그 모드 ===== */
    /** true: 해시 평문 전체 출력 / false: licenseKey 마스킹 (운영 배포 시 false로 변경) */
    public static final boolean DEBUG = true;

    /* ===== 서버 URL ===== */
    public static final String SERVER_URL = "https://tbgw.settlebank.co.kr";   // 테스트 서버
 // public static final String SERVER_URL = "https://gw.settlebank.co.kr";     // 운영 서버

    /* ===== 통신 타임아웃 (ms) ===== */
    public static final int CONN_TIMEOUT = 5_000;
    public static final int READ_TIMEOUT = 25_000;

    /* ===== API 경로 ===== */
    /** 빌키 발급 API */
    public static final String PATH_BILLKEY_ISSUE = "/spay/APICardAuth.do";

    /** 결제 API (빌키 발급 포함) / 빌키 결제 API - 같은 엔드포인트 사용 */
    public static final String PATH_PAYMENT = "/spay/APICardActionPay.do";

    /** 취소 API */
    public static final String PATH_CANCEL = "/spay/APICancel.do";

    /** 빌키 삭제 API */
    public static final String PATH_BILLKEY_DELETE = "/spay/APICardActionDelkey.do";

    /* ===== 고정 파라미터 ===== */
    public static final String VER    = "0A19";  // 전문버전
    public static final String METHOD = "CA";    // 결제수단: 신용카드
    public static final String ENC_CD = "23";    // 암호화구분: AES-256-ECB
    public static final String CRC_CD = "KRW";   // 통화구분
}
