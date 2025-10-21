# 📚 LuXeStay Hotel - API Documentation

## Overview

Hệ thống quản lý khách sạn với các module:

- 🏨 **Room Management** - Quản lý phòng, tìm kiếm, lọc
- 📅 **Booking Management** - Đặt phòng, hủy phòng
- 👤 **Account Management** - Quản lý tài khoản, phân quyền
- 👔 **Employee Management** - Quản lý nhân viên
- 💳 **Payment Integration** - Thanh toán trực tuyến

---

## Documentation Index

### Core APIs

1. **[Room Search & Filter API](./API_ROOM_SEARCH.md)** ⭐

   - Tìm kiếm phòng với nhiều bộ lọc
   - Phân trang, sắp xếp
   - **Status**: ✅ Complete

2. **Booking API** (Coming soon)

   - Tạo booking
   - Hủy booking (workflow 2-step)
   - Lịch sử đặt phòng

3. **Authentication API** (Coming soon)

   - Login/Logout
   - Register
   - Session management

4. **Admin API** (Coming soon)
   - Account CRUD
   - Employee CRUD
   - Reports

---

## Quick Start

### 1. Prerequisites

```bash
- Java 17+
- Maven 3.8+
- SQL Server 2019+
- Node.js 18+ (for frontend)
```

### 2. Backend Setup

```bash
cd be_hotel
mvn clean install
mvn spring-boot:run
```

**Server**: `http://localhost:8080`

### 3. Frontend Setup

```bash
cd fe_hotel
npm install
npm run dev
```

**App**: `http://localhost:5173`

---

## API Base URLs

| Environment | Base URL                    |
| ----------- | --------------------------- |
| Development | `http://localhost:8080/api` |
| Production  | `https://api.luxestay.com`  |

---

## Authentication

Hiện tại sử dụng **token-based** authentication:

```http
GET /api/bookings
X-Auth-Token: <your-token-here>
```

**Lấy token:**

```bash
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password"
}
```

**Response:**

```json
{
  "token": "a1b2c3d4-...",
  "accountId": 1,
  "fullName": "Nguyễn Văn A",
  "role": "customer"
}
```

---

## Common Response Formats

### Success Response (List)

```json
{
  "items": [
    /* array of objects */
  ],
  "total": 100,
  "page": 0,
  "size": 10
}
```

### Success Response (Single Object)

```json
{
  "id": 1,
  "name": "...",
  ...
}
```

### Error Response

```json
{
  "timestamp": "2025-10-18T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed"
}
```

---

## HTTP Status Codes

| Code | Meaning      | Usage                  |
| ---- | ------------ | ---------------------- |
| 200  | OK           | Request thành công     |
| 201  | Created      | Tạo mới thành công     |
| 204  | No Content   | Xóa thành công         |
| 400  | Bad Request  | Validation lỗi         |
| 401  | Unauthorized | Chưa đăng nhập         |
| 403  | Forbidden    | Không có quyền         |
| 404  | Not Found    | Resource không tồn tại |
| 500  | Server Error | Lỗi server             |

---

## Database Schema

### Core Tables

```
accounts          - Tài khoản người dùng
roles             - Phân quyền (customer, staff, admin)
employees         - Thông tin nhân viên
rooms             - Phòng khách sạn
bed_layouts       - Loại giường
room_images       - Ảnh phòng
bookings          - Đặt phòng
services          - Dịch vụ khách sạn
booking_services  - Dịch vụ kèm booking
payments          - Thanh toán
reviews           - Đánh giá
```

**ERD Diagram**: Coming soon

---

## Testing

### Unit Tests

```bash
cd be_hotel
mvn test
```

### API Tests (Manual)

```bash
# Xem: be_hotel/src/test/resources/room-search-api-tests.md
curl -X GET "http://localhost:8080/api/rooms/search"
```

### Postman Collection

Import file: `be_hotel/docs/postman/LuXeStay.postman_collection.json`

---

## Performance

### Response Time Targets

| Endpoint              | Target  | Notes              |
| --------------------- | ------- | ------------------ |
| GET /api/rooms        | < 200ms | With indexes       |
| GET /api/rooms/search | < 300ms | Complex query      |
| POST /api/bookings    | < 500ms | Include validation |
| GET /api/bookings/my  | < 200ms | Paginated          |

### Optimization

1. **Database Indexes** - 6 indexes cho rooms table
2. **Pagination** - Default size = 10, max = 50
3. **Caching** - Coming soon (Redis)
4. **Query Optimization** - JPQL với JOIN FETCH

---

## Security

### Current Implementation

- ✅ BCrypt password hashing
- ✅ Token-based authentication
- ✅ CORS configuration
- ⚠️ In-memory session (not production-ready)

### Recommendations

- 🔴 Migrate to JWT tokens
- 🔴 Add refresh token mechanism
- 🔴 Implement rate limiting
- 🔴 Add input sanitization

---

## CORS Configuration

Allowed origins:

```
http://localhost:5173
http://localhost:3000
http://localhost:4173
http://127.0.0.1:5173
```

**Production**: Update `CorsConfig.java`

---

## Environment Variables

### Backend (application.properties)

```properties
# Database
spring.datasource.url=jdbc:sqlserver://HEH;databaseName=hotel_booking_system
spring.datasource.username=sa
spring.datasource.password=123

# Server
server.port=8080

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

### Frontend (.env)

```env
VITE_API_BASE=http://localhost:8080/api
```

---

## Deployment

### Docker (Coming soon)

```bash
docker-compose up
```

### Manual Deploy

**Backend:**

```bash
mvn clean package
java -jar target/hotel-0.0.1-SNAPSHOT.jar
```

**Frontend:**

```bash
npm run build
# Serve dist/ folder with nginx
```

---

## Monitoring & Logging

### Application Logs

```bash
tail -f logs/application.log
```

### SQL Query Logs

```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

### Health Check

```bash
GET /actuator/health
```

---

## Troubleshooting

### Issue: Cannot connect to database

**Solution:**

1. Check SQL Server is running
2. Verify connection string in `application.properties`
3. Ensure database `hotel_booking_system` exists

### Issue: CORS errors

**Solution:**

1. Check frontend URL in `CorsConfig.java`
2. Clear browser cache
3. Verify `Access-Control-Allow-Origin` header

### Issue: Slow query performance

**Solution:**

1. Run database indexes migration (V2)
2. Execute `UPDATE STATISTICS rooms;`
3. Check query plan with SQL Profiler

---

## Contributing

### Code Style

- Java: Follow Google Java Style Guide
- JavaScript: Use ESLint + Prettier
- Git commits: Conventional Commits format

### Pull Request Process

1. Create feature branch: `feature/your-feature`
2. Write tests
3. Update documentation
4. Submit PR with description

---

## Changelog

### v1.0.0 (2025-10-18)

- ✅ Room search & filter API
- ✅ Database indexes optimization
- ✅ API documentation
- ✅ Test cases

### v0.9.0 (2025-10-14)

- Initial project setup
- Basic CRUD operations
- Authentication system

---

## License

Copyright © 2025 LuXeStay Hotel. All rights reserved.

---

## Support & Contact

- **Email**: support@luxestay.com
- **Documentation**: [GitHub Wiki](https://github.com/yourorg/luxestay)
- **Issues**: [GitHub Issues](https://github.com/yourorg/luxestay/issues)

---

## Related Documentation

- [Room Search API](./API_ROOM_SEARCH.md)
- [Database Migrations](../src/main/resources/db/migration/README.md)
- [Test Cases](../src/test/resources/room-search-api-tests.md)





