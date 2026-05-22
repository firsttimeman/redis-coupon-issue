# Redis Coupon Issue

Redis Streams와 Consumer Group을 학습하기 위한 쿠폰 발급 비동기 처리 프로젝트입니다.

사용자가 쿠폰 발급을 요청하면 API 서버는 요청 정보를 MySQL에 저장하고 Redis Stream에 작업 메시지를 등록합니다. 이후 Worker가 메시지를 읽어 쿠폰 발급을 처리하고 상태를 갱신하는 구조를 직접 구현해보는 것이 목표입니다.

## Tech Stack

- Java 17
- Spring Boot
- Spring Data JPA
- Spring Data Redis
- MySQL
- Redis
- Gradle
