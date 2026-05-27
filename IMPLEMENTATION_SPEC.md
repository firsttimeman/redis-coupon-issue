# Redis Coupon Issue - 앞으로 구현할 명세서

## 목표

Redis Streams와 Consumer Group을 사용해서 쿠폰 발급 요청을 비동기 작업으로 처리한다.

최종 흐름은 아래와 같다.

```text
POST /coupon-issues
-> MySQL에 PENDING 상태로 저장
-> Redis Stream에 쿠폰 발급 작업 메시지 등록
-> Worker가 메시지 소비
-> 상태 PROCESSING 변경
-> 쿠폰 코드 생성
-> 상태 SUCCESS 변경
-> Redis ACK
-> GET /coupon-issues/{id}로 결과 확인
```

## 현재 구현된 범위

- Spring Boot 기본 프로젝트
- MySQL / Redis Docker Compose 구성
- local / docker 프로필 분리
- CouponIssue 엔티티
- CouponIssueStatus enum
- CouponIssueRepository
- 쿠폰 발급 요청 생성 API
- 쿠폰 발급 요청 조회 API
- Request / Response DTO
- Validation 기본 설정
- Redis 연결 설정값

## 앞으로 구현할 범위

## 1단계: 예외 응답 정리

### 목적

API 요청이 잘못되었거나 조회 대상이 없을 때 응답 형식을 일정하게 만든다.

### 만들 파일

```text
src/main/java/com/example/coupon/api/GlobalExceptionHandler.java
src/main/java/com/example/coupon/api/ErrorResponse.java
src/main/java/com/example/coupon/exception/CouponIssueNotFoundException.java
```

### 처리할 예외

```text
MethodArgumentNotValidException -> 400 Bad Request
CouponIssueNotFoundException -> 404 Not Found
```

### 응답 형식

```json
{
  "message": "에러 메시지"
}
```

### 확인 방법

잘못된 요청:

```http
POST /coupon-issues
```

```json
{
  "userId": null,
  "couponName": "",
  "discountAmount": 0
}
```

없는 ID 조회:

```http
GET /coupon-issues/999
```

### 완료 기준

- Validation 실패 시 400 응답이 내려온다.
- 없는 쿠폰 발급 요청 조회 시 404 응답이 내려온다.
- 응답 body가 `{ "message": "..." }` 형태로 통일된다.

## 2단계: Redis Stream Producer 구현

### 목적

쿠폰 발급 요청이 DB에 저장된 뒤 Redis Stream에 작업 메시지를 등록한다.

### 만들 파일

```text
src/main/java/com/example/coupon/redis/CouponIssueStreamProperties.java
src/main/java/com/example/coupon/redis/RedisConfig.java
src/main/java/com/example/coupon/redis/CouponIssueStreamProducer.java
```

### 사용할 설정값

```yaml
coupon:
  issue:
    stream-key: coupon:issue:stream
    group: coupon-issue-group
    consumer: coupon-issue-worker-1
```

### Stream 이름

```text
coupon:issue:stream
```

### 메시지 필드

```text
couponIssueId
userId
couponName
createdAt
```

### 메시지 예시

```json
{
  "couponIssueId": "1",
  "userId": "1",
  "couponName": "WELCOME_3000",
  "createdAt": "2026-05-25T13:00:00"
}
```

### 완료 기준

- `CouponIssueStreamProducer`가 Redis Stream에 메시지를 추가할 수 있다.
- 메시지에는 DB 저장 이후 생성된 `couponIssueId`가 포함된다.
- Redis CLI에서 `XRANGE coupon:issue:stream - +`로 메시지를 확인할 수 있다.

## 3단계: Service에 Producer 연결

### 목적

쿠폰 발급 요청 생성 API가 DB 저장과 Redis Stream 발행을 함께 수행하게 만든다.

### 수정 파일

```text
src/main/java/com/example/coupon/application/CouponIssueService.java
```

### 현재 흐름

```text
CouponIssue 생성
-> DB 저장
-> 응답 반환
```

### 변경할 흐름

```text
CouponIssue 생성
-> DB 저장
-> Redis Stream 메시지 발행
-> 응답 반환
```

### 주의 사항

Redis 메시지는 반드시 `couponIssueRepository.save()` 이후의 `saved` 엔티티 기준으로 발행한다.

이유는 저장 전에는 `couponIssueId`가 없기 때문이다.

### 완료 기준

- `POST /coupon-issues` 호출 시 응답은 기존처럼 `PENDING`이다.
- 동시에 Redis Stream에 작업 메시지가 1개 추가된다.
- API 서버는 실제 쿠폰 코드를 아직 생성하지 않는다.

## 4단계: Redis 메시지 확인

### 목적

Worker를 만들기 전에 Producer가 정상적으로 메시지를 넣는지 확인한다.

### 실행 순서

Docker 인프라 실행:

```bash
docker compose up -d
```

애플리케이션 실행:

```bash
./gradlew bootRun --args='--spring.profiles.active=docker'
```

쿠폰 발급 요청:

```http
POST /coupon-issues
```

```json
{
  "userId": 1,
  "couponName": "WELCOME_3000",
  "discountAmount": 3000
}
```

Redis CLI 접속:

```bash
docker compose exec redis redis-cli
```

Stream 조회:

```redis
XRANGE coupon:issue:stream - +
```

### 완료 기준

- Stream에 `couponIssueId`, `userId`, `couponName`, `createdAt` 필드가 보인다.
- 이 시점의 DB 상태는 아직 `PENDING`이어도 정상이다.

## 5단계: Consumer Group 초기화

### 목적

Worker가 Redis Stream을 Consumer Group 방식으로 읽을 수 있도록 그룹을 생성한다.

### 만들 파일

```text
src/main/java/com/example/coupon/redis/CouponIssueStreamGroupInitializer.java
```

### 사용할 값

```text
streamKey: coupon:issue:stream
group: coupon-issue-group
```

### 처리 조건

- 애플리케이션 시작 시 Consumer Group을 생성한다.
- 이미 그룹이 있으면 에러로 종료하지 않고 무시한다.
- Stream이 없을 수 있으므로 필요한 경우 Stream 생성까지 고려한다.

### 완료 기준

- 애플리케이션 시작 시 Consumer Group이 준비된다.
- 이미 생성된 그룹이 있어도 재실행이 가능하다.

## 6단계: Worker 구현

### 목적

Redis Stream 메시지를 읽어서 실제 쿠폰 발급 처리를 수행한다.

### 만들 파일

```text
src/main/java/com/example/coupon/redis/CouponIssueStreamWorker.java
```

### Worker 처리 흐름

```text
Redis Stream 메시지 읽기
-> couponIssueId 추출
-> DB에서 CouponIssue 조회
-> 상태 PROCESSING 변경
-> 쿠폰 코드 생성
-> 상태 SUCCESS 변경
-> Redis ACK
```

### 쿠폰 코드 형식

1차 구현에서는 단순한 문자열이면 충분하다.

```text
{couponName}-{userId}-{랜덤값}
```

예시:

```text
WELCOME_3000-1-A8F3
```

### 상태 변경

처리 시작:

```text
PENDING -> PROCESSING
```

처리 완료:

```text
PROCESSING -> SUCCESS
```

### 완료 기준

- Worker가 Redis Stream 메시지를 읽는다.
- 메시지의 `couponIssueId`로 DB 데이터를 조회한다.
- 쿠폰 코드가 생성된다.
- DB 상태가 `SUCCESS`로 변경된다.
- `processedAt`이 기록된다.
- Redis ACK가 호출된다.

## 7단계: 전체 MVP 확인

### 실행 흐름

```text
docker compose up -d
./gradlew bootRun --args='--spring.profiles.active=docker'
POST /coupon-issues
GET /coupon-issues/{couponIssueId}
```

### 기대 결과

POST 응답:

```json
{
  "couponIssueId": 1,
  "status": "PENDING"
}
```

처리 후 GET 응답:

```json
{
  "couponIssueId": 1,
  "userId": 1,
  "couponName": "WELCOME_3000",
  "discountAmount": 3000,
  "issuedCouponCode": "WELCOME_3000-1-A8F3",
  "status": "SUCCESS",
  "retryCount": 0,
  "errorMessage": null,
  "createdAt": "...",
  "updatedAt": "...",
  "processedAt": "..."
}
```

### 서버 로그 예시

```text
[COUPON ISSUED] userId=1 couponName=WELCOME_3000 code=WELCOME_3000-1-A8F3
Processed coupon issue message. messageId=..., couponIssueId=1
```

### MVP 완료 기준

- API는 요청을 빠르게 `PENDING`으로 저장한다.
- Redis Stream에 작업 메시지가 쌓인다.
- Worker가 메시지를 읽고 쿠폰 발급을 완료한다.
- DB 상태가 `SUCCESS`로 바뀐다.
- Redis ACK까지 수행된다.

## 8단계: MVP 이후 확장 후보

1차 MVP가 끝난 뒤 아래 기능을 하나씩 추가한다.

- 실패 시 `retryCount` 증가
- 실패 사유 `errorMessage` 저장
- 최대 재시도 횟수 초과 시 `DEAD_LETTER` 상태 처리
- Pending 메시지 복구
- 쿠폰 재고 차감
- 사용자별 중복 발급 방지
- 사용자별 1회 발급 제한
- 여러 Worker 실행 테스트
- 테스트 코드 추가
- Redis CLI 기반 Stream 상태 확인 절차 README 정리

## 추천 구현 순서 요약

```text
1. GlobalExceptionHandler
2. ErrorResponse
3. CouponIssueStreamProperties
4. RedisConfig
5. CouponIssueStreamProducer
6. CouponIssueService에 Producer 연결
7. Redis Stream 메시지 확인
8. CouponIssueStreamGroupInitializer
9. CouponIssueStreamWorker
10. 전체 MVP 확인
```
