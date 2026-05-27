# Redis Coupon Issue - 다음 작업 정리

## 현재 상태

현재 프로젝트는 아래까지 구현되어 있다.

- Spring Boot 프로젝트 기본 구성
- MySQL / Redis Docker Compose 구성
- local / docker 프로필 분리
- CouponIssue 도메인 엔티티
- CouponIssueStatus enum
- CouponIssueRepository
- 쿠폰 발급 요청 생성 API
- 쿠폰 발급 요청 조회 API
- Request / Response DTO
- Lombok 설정

## 실행 프로필

### 로컬 MySQL로 실행

로컬에 설치된 MySQL을 사용할 때 사용한다.

```text
Active profiles: local
```

터미널 실행:

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### Docker MySQL로 실행

Docker Compose로 띄운 MySQL을 사용할 때 사용한다.

```text
Active profiles: docker
```

터미널 실행:

```bash
docker compose up -d
./gradlew bootRun --args='--spring.profiles.active=docker'
```

## 1단계: API 동작 테스트

### 1. 쿠폰 발급 요청 생성

```http
POST /coupon-issues
```

Request:

```json
{
  "userId": 1,
  "couponName": "WELCOME_3000",
  "discountAmount": 3000
}
```

기대 응답:

```json
{
  "couponIssueId": 1,
  "status": "PENDING"
}
```

### 2. 쿠폰 발급 요청 조회

```http
GET /coupon-issues/1
```

기대 응답:

```json
{
  "couponIssueId": 1,
  "userId": 1,
  "couponName": "WELCOME_3000",
  "discountAmount": 3000,
  "issuedCouponCode": null,
  "status": "PENDING",
  "retryCount": 0,
  "errorMessage": null,
  "createdAt": "...",
  "updatedAt": "...",
  "processedAt": null
}
```

## 2단계: 예외 응답 정리

만들 파일:

```text
src/main/java/com/example/coupon/api/GlobalExceptionHandler.java
```

처리할 예외:

```text
MethodArgumentNotValidException -> 400 Bad Request
CouponIssueNotFoundException -> 404 Not Found
```

응답 형태:

```json
{
  "message": "에러 메시지"
}
```

테스트할 요청:

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

## 3단계: Redis Stream Producer 구현

목표:

```text
POST /coupon-issues
-> MySQL에 PENDING 저장
-> Redis Stream에 쿠폰 발급 작업 메시지 등록
```

만들 파일:

```text
src/main/java/com/example/coupon/redis/CouponIssueStreamProperties.java
src/main/java/com/example/coupon/redis/RedisConfig.java
src/main/java/com/example/coupon/redis/CouponIssueStreamProducer.java
```

Stream 이름:

```text
coupon:issue:stream
```

메시지 필드:

```text
couponIssueId
userId
couponName
createdAt
```

메시지 예시:

```json
{
  "couponIssueId": "1",
  "userId": "1",
  "couponName": "WELCOME_3000",
  "createdAt": "2026-05-25T13:00:00"
}
```

## 4단계: Service에 Redis Producer 연결

수정 파일:

```text
src/main/java/com/example/coupon/application/CouponIssueService.java
```

현재 흐름:

```text
CouponIssue 생성
-> DB 저장
-> 응답 반환
```

변경할 흐름:

```text
CouponIssue 생성
-> DB 저장
-> Redis Stream 메시지 발행
-> 응답 반환
```

주의:

```text
Redis 메시지는 반드시 DB 저장 이후 saved 엔티티로 발행한다.
그래야 couponIssueId가 존재한다.
```

## 5단계: Redis 메시지 확인

Docker Redis에 접속:

```bash
docker compose exec redis redis-cli
```

Stream 조회:

```redis
XRANGE coupon:issue:stream - +
```

기대 결과:

```text
couponIssueId
1
userId
1
couponName
WELCOME_3000
createdAt
...
```

## 오늘 목표

오늘은 여기까지 하면 충분하다.

```text
1. local 또는 docker 프로필로 서버 실행
2. POST /coupon-issues 성공 확인
3. GET /coupon-issues/{id} 성공 확인
4. validation 실패 400 처리
5. 없는 ID 404 처리
6. Redis Stream Producer 구현
7. POST 요청 시 Redis Stream 메시지 쌓이는지 확인
```

## 다음 단계

Redis Producer까지 끝나면 다음은 Worker 구현이다.

```text
Consumer Group 생성
Worker가 Redis Stream 메시지 읽기
couponIssueId로 DB 조회
PROCESSING 변경
쿠폰 코드 생성
SUCCESS 변경
Redis ACK
```
