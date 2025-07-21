# **프로젝트: 매듭 (Maedeup / The Knot)**

**"사용자와 기회를 연결하는 실시간 이벤트 플랫폼"**

## **📖 프로젝트 소개**

**매듭**은 선착순 티켓팅이나 추첨 이벤트처럼 다수의 사용자가 동시에 참여하는 상황을 효과적으로 제어하기 위해 만들어진 웹 플랫폼입니다. 단순히 기능을 구현하는 것을 넘어, 대용량 트래픽 상황에서 발생하는 동시성 문제를 해결하고 모든 참가자에게 공정한 기회를 제공하는 것을 목표로 합니다.

## **💡 이름의 의미**

'매듭'은 사용자와 특별한 기회를 안정적으로 연결하고, 공정한 참여의 과정을 마무리 짓는다는 의미를 담고 있습니다.

## **⚙️ 환경 설정**

### **1. 환경변수 설정**

프로젝트에서 사용하는 민감한 정보들은 `.env` 파일로 관리됩니다.

```bash
# 프로젝트 루트에 .env 파일 생성
cp .env.example .env
```

**`.env` 파일 내용:**
```env
# Database Configuration
DB_URL=jdbc:mysql://localhost:3306/maedeup?serverTimezone=UTC&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true
DB_USERNAME=your-mysql-username
DB_PASSWORD=your-mysql-password

# Redis Configuration
REDIS_HOST=your-redis-host
REDIS_PORT=6379
REDIS_USERNAME=your-redis-username
REDIS_PASSWORD=your-redis-password

# Spring Security Configuration
SECURITY_USER_NAME=maedeup_test
SECURITY_USER_PASSWORD=1234
```

### **2. 데이터베이스 설정**

MySQL 데이터베이스를 생성하고 계정을 설정해주세요:

```sql
CREATE DATABASE maedeup CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'maedeup'@'localhost' IDENTIFIED BY 'your-mysql-password';
GRANT ALL PRIVILEGES ON maedeup.* TO 'maedeup'@'localhost';
FLUSH PRIVILEGES;
```

### **3. Redis 설정**

로컬 개발 환경에서는 Docker를 사용하여 Redis를 실행할 수 있습니다:

```bash
docker run -d --name redis-server -p 6379:6379 redis:latest
```

또는 Redis Cloud나 AWS ElastiCache 등의 클라우드 서비스를 사용하세요.

## **🚀 실행 방법**

### **1. 의존성 설치 및 빌드**

```bash
./gradlew build
```

### **2. 애플리케이션 실행**

```bash
./gradlew bootRun
```

또는 IDE에서 `MaedeupApplication.main()` 메서드를 실행하세요.

### **3. API 문서 확인**

애플리케이션 실행 후 Swagger UI에서 API 문서를 확인할 수 있습니다:

```
http://localhost:8080/swagger-ui/index.html
```
