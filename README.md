# Java-PG-BillKey

![Java](https://img.shields.io/badge/Java-8%2B-orange?style=flat-square)
![No Dependencies](https://img.shields.io/badge/dependencies-none-brightgreen?style=flat-square)
![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)

헥토파이낸셜 PG 신용카드 수기결제 및 빌키 API Java 연동 샘플입니다.  
수기결제(구인증/비인증)로 카드 정보를 직접 입력해 결제하거나, 빌키를 발급받아 정기/반복 결제에 활용할 수 있습니다.  
외부 라이브러리 없이 JDK 표준 라이브러리만 사용합니다.

---

## 시작하기

**1. `Config.java` 설정값 교체**

```java
PG_MID_AUTH   = "발급받은 구인증 MID";
PG_MID_NOAUTH = "발급받은 비인증 MID";
LICENSE_KEY   = "발급받은 라이센스 키";
AES256_KEY    = "발급받은 AES-256 키";
SERVER_URL    = "https://gw.settlebank.co.kr";  // 운영 서버
```

> 현재 `Config.java`에는 **테스트 전용** 값이 설정되어 있습니다.  
> 실 서비스 연동 전 반드시 헥토파이낸셜에서 발급받은 값으로 교체해주세요.

**2. 원하는 클래스의 `main()` 실행**

IDE(IntelliJ IDEA, Eclipse 등)에서 각 클래스를 열고 `main()` 메서드를 직접 실행해주세요.

---

## 클래스 구성

| 클래스 | 역할 |
|--------|------|
| `Config` | 상점아이디, 키, 서버 URL 등 환경 설정 |
| `CryptoUtil` | AES-256-ECB 암복호화 · SHA-256 해시 |
| `HttpUtil` | HTTP POST 요청 전송 |
| `JsonUtil` | JSON 빌드 및 파싱 |
| `BillKeyIssue` | **빌키 발급** — 0원 인증으로 빌키만 발급 |
| `PaymentAuth` | **구인증 결제** — 카드번호 + 유효기간 + 생년월일 + 비밀번호 |
| `PaymentNoAuth` | **비인증 결제** — 카드번호 + 유효기간만 (빌키 동시 발급 가능) |
| `BillKeyPayment` | **빌키 결제** — 발급받은 빌키로 재결제 |
| `BillKeyDelete` | **빌키 삭제** — 발급된 빌키 삭제 (복구 불가) |
| `Cancel` | **취소** — 전체 취소 및 부분 취소 |

---

## 빌키 결제 플로우

### 플로우 1 — 빌키 발급 후 결제

0원 인증으로 빌키를 먼저 발급하고, 이후 결제는 빌키만으로 처리합니다.  
정기 결제, 구독 결제에 적합합니다.

```
BillKeyIssue  →  (billKey 저장)  →  BillKeyPayment  →  Cancel
```

### 플로우 2 — 1회차 결제 + 빌키 동시 발급

첫 결제 시 카드 정보를 입력받아 결제와 빌키를 동시에 발급합니다.  
발급된 빌키는 결제 응답의 `billKey` 필드로 전달됩니다.

```
PaymentAuth / PaymentNoAuth  →  (billKey + trdNo 저장)  →  BillKeyPayment  →  Cancel
```

---

## 상점아이디(MID) 구분

| 구분 | 설정값 | 사용 클래스 |
|------|--------|-------------|
| 구인증 | `Config.PG_MID_AUTH` | BillKeyIssue, PaymentAuth |
| 비인증 | `Config.PG_MID_NOAUTH` | PaymentNoAuth |
| 공통 | 발급 시 사용한 MID | BillKeyPayment, Cancel |

> 구인증/비인증 MID 모두 빌키 발급을 지원합니다.  
> `BillKeyPayment`와 `Cancel`은 빌키 발급 또는 결제에 사용한 MID와 동일한 MID를 사용해주세요.

---

## 암호화 규격

| 항목 | 규격 |
|------|------|
| 암호화 | AES-256-ECB / PKCS5Padding / Base64 |
| 무결성 검증 | SHA-256 (pktHash) |

**pktHash 조합 규칙**

| API | 조합 |
|-----|------|
| 빌키 발급 | `trdDt + trdTm + mchtId + mchtTrdNo + "0" + licenseKey` |
| 빌키 삭제 | `trdDt + trdTm + mchtId + mchtTrdNo + "0" + licenseKey` |
| 결제 (구인증 / 비인증 / 빌키) | `trdDt + trdTm + mchtId + mchtTrdNo + trdAmt(평문) + licenseKey` |
| 취소 | `trdDt + trdTm + mchtId + mchtTrdNo + cnclAmt(평문) + licenseKey` |

> 금액 필드(trdAmt, cnclAmt)는 암호화 전 **평문값**으로 해시를 생성해주세요.

