# 🏨 Room Search & Filter API Documentation

## Table of Contents

- [Overview](#overview)
- [Base URL](#base-url)
- [Endpoints](#endpoints)
- [Request Parameters](#request-parameters)
- [Response Format](#response-format)
- [Examples](#examples)
- [Error Handling](#error-handling)
- [Performance](#performance)

---

## Overview

API tìm kiếm và lọc phòng khách sạn với khả năng:

- ✅ Lọc theo giá (min/max)
- ✅ Lọc theo trạng thái (available, occupied, maintenance)
- ✅ Lọc theo loại giường (bed layout)
- ✅ Lọc theo tiện nghi (amenities)
- ✅ Lọc theo số lượng khách (capacity)
- ✅ Sắp xếp theo giá
- ✅ Phân trang (pagination)

---

## Base URL

```
http://localhost:8080/api/rooms
```

**Production**: `https://your-domain.com/api/rooms`

---

## Endpoints

### 1. List All Rooms

**GET** `/api/rooms`

Lấy danh sách tất cả phòng (chỉ hiển thị phòng `available`).

**Response:**

```json
[
  {
    "id": 1,
    "name": "Deluxe Ocean View",
    "type": "Deluxe",
    "capacity": 2,
    "sizeSqm": 35,
    "priceVnd": 2500000,
    "amenities": ["WiFi miễn phí", "Ban công"],
    "imageUrl": "https://example.com/room1.jpg",
    "popular": true
  }
]
```

---

### 2. Search & Filter Rooms

**GET** `/api/rooms/search`

Tìm kiếm phòng với nhiều bộ lọc.

#### Request Parameters

| Parameter   | Type    | Required | Default    | Description                                                                                  |
| ----------- | ------- | -------- | ---------- | -------------------------------------------------------------------------------------------- |
| `priceMin`  | Integer | No       | -          | Giá tối thiểu (VND)                                                                          |
| `priceMax`  | Integer | No       | -          | Giá tối đa (VND)                                                                             |
| `guests`    | Integer | No       | -          | Số lượng khách (filter theo capacity)                                                        |
| `types`     | String  | No       | -          | Loại giường, phân tách bằng dấu phẩy<br/>VD: `1 giường đôi,2 giường đơn`                     |
| `amenities` | String  | No       | -          | Tiện nghi, phân tách bằng dấu phẩy<br/>VD: `WiFi miễn phí,Ban công`                          |
| `status`    | String  | No       | -          | Trạng thái phòng, phân tách bằng dấu phẩy<br/>Values: `available`, `occupied`, `maintenance` |
| `sort`      | String  | No       | `priceAsc` | Sắp xếp<br/>Values: `priceAsc`, `priceDesc`, `ratingDesc`                                    |
| `page`      | Integer | No       | 0          | Số trang (bắt đầu từ 0)                                                                      |
| `size`      | Integer | No       | 10         | Số items mỗi trang (max: 50)                                                                 |

#### Query String Examples

```
# Basic search
/api/rooms/search

# Filter by price
/api/rooms/search?priceMax=3000000

# Filter by price range
/api/rooms/search?priceMin=1500000&priceMax=5000000

# Filter by status
/api/rooms/search?status=available

# Multiple statuses
/api/rooms/search?status=available,occupied

# Filter by guests
/api/rooms/search?guests=4

# Filter by bed type
/api/rooms/search?types=1%20giường%20đôi

# Multiple bed types
/api/rooms/search?types=1%20giường%20đôi,2%20giường%20đơn

# Filter by amenities
/api/rooms/search?amenities=WiFi%20miễn%20phí,Ban%20công

# Sort by price descending
/api/rooms/search?sort=priceDesc

# Pagination
/api/rooms/search?page=1&size=20

# Combined filters
/api/rooms/search?priceMax=3000000&guests=2&status=available&sort=priceAsc&page=0&size=10
```

#### Response Format

```json
{
  "items": [
    {
      "id": 1,
      "name": "Deluxe Ocean View",
      "type": "Deluxe",
      "capacity": 2,
      "sizeSqm": 35,
      "priceVnd": 2500000,
      "amenities": ["WiFi miễn phí", "Ban công", "Tầm nhìn biển"],
      "imageUrl": "https://example.com/room1.jpg",
      "popular": true,
      "rating": 4.7,
      "reviews": 120,
      "discount": null
    }
  ],
  "total": 15,
  "page": 0,
  "size": 10
}
```

**Response Fields:**

| Field   | Type    | Description                      |
| ------- | ------- | -------------------------------- |
| `items` | Array   | Danh sách phòng                  |
| `total` | Integer | Tổng số phòng (không phân trang) |
| `page`  | Integer | Trang hiện tại                   |
| `size`  | Integer | Số items mỗi trang               |

**Room Object Fields:**

| Field       | Type     | Description           |
| ----------- | -------- | --------------------- |
| `id`        | Long     | ID phòng              |
| `name`      | String   | Tên phòng             |
| `type`      | String   | Loại phòng            |
| `capacity`  | Integer  | Sức chứa (số khách)   |
| `sizeSqm`   | Integer  | Diện tích (m²)        |
| `priceVnd`  | Integer  | Giá/đêm (VND)         |
| `amenities` | String[] | Danh sách tiện nghi   |
| `imageUrl`  | String   | URL ảnh đại diện      |
| `popular`   | Boolean  | Phòng phổ biến        |
| `rating`    | Double   | Đánh giá (1-5)        |
| `reviews`   | Integer  | Số lượng đánh giá     |
| `discount`  | Integer  | % giảm giá (nullable) |

---

### 3. Get Room Detail

**GET** `/api/rooms/{id}`

Lấy thông tin chi tiết một phòng.

**Path Parameters:**

- `id` (Long): Room ID

**Response:**

```json
{
  "room": {
    "id": 1,
    "name": "Deluxe Ocean View",
    "capacity": 2,
    "priceVnd": 2500000,
    ...
  },
  "floorRange": "Tầng 2-5",
  "description": "Phòng Deluxe với view biển tuyệt đẹp...",
  "highlights": ["WiFi miễn phí", "Ban công"],
  "gallery": [
    "https://example.com/room1-1.jpg",
    "https://example.com/room1-2.jpg"
  ],
  "amenities": {
    "Tiện nghi cơ bản": ["WiFi miễn phí", "Điều hòa", "TV"],
    "Phòng tắm": ["Vòi sen", "Đồ dùng tắm"],
    "Dịch vụ": ["Giặt ủi", "Dọn phòng"]
  },
  "ratingHistogram": {
    "5": 78,
    "4": 32,
    "3": 10,
    "2": 3,
    "1": 1
  },
  "reviews": [
    {
      "author": "Khách Ẩn Danh",
      "avatar": "https://i.pravatar.cc/64",
      "rating": 5,
      "comment": "Phòng sạch đẹp, đúng mô tả.",
      "date": "2 tuần trước"
    }
  ]
}
```

---

## Examples

### Example 1: Tìm phòng giá ≤ 3 triệu

**Request:**

```bash
GET /api/rooms/search?priceMax=3000000
```

**Response:**

```json
{
  "items": [
    {
      "id": 2,
      "name": "Standard City View",
      "priceVnd": 1500000,
      ...
    },
    {
      "id": 1,
      "name": "Deluxe Ocean View",
      "priceVnd": 2500000,
      ...
    }
  ],
  "total": 2,
  "page": 0,
  "size": 10
}
```

---

### Example 2: Tìm phòng cho 4 người, còn trống

**Request:**

```bash
GET /api/rooms/search?guests=4&status=available
```

**Response:**

```json
{
  "items": [
    {
      "id": 3,
      "name": "Suite Presidential",
      "capacity": 4,
      "priceVnd": 8000000,
      ...
    }
  ],
  "total": 1,
  "page": 0,
  "size": 10
}
```

---

### Example 3: Tìm phòng có WiFi và Ban công, sắp xếp theo giá

**Request:**

```bash
GET /api/rooms/search?amenities=WiFi%20miễn%20phí,Ban%20công&sort=priceAsc
```

**Response:**

```json
{
  "items": [
    {
      "id": 1,
      "name": "Deluxe Ocean View",
      "priceVnd": 2500000,
      "amenities": ["WiFi miễn phí", "Ban công"],
      ...
    }
  ],
  "total": 1,
  "page": 0,
  "size": 10
}
```

---

### Example 4: Phân trang - Lấy 5 phòng mỗi trang

**Request (Page 0):**

```bash
GET /api/rooms/search?page=0&size=5
```

**Response:**

```json
{
  "items": [
    /* 5 rooms */
  ],
  "total": 15,
  "page": 0,
  "size": 5
}
```

**Request (Page 1):**

```bash
GET /api/rooms/search?page=1&size=5
```

**Response:**

```json
{
  "items": [
    /* next 5 rooms */
  ],
  "total": 15,
  "page": 1,
  "size": 5
}
```

---

## Error Handling

### 400 Bad Request

Invalid parameters.

```json
{
  "timestamp": "2025-10-18T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid parameter: priceMax must be positive"
}
```

### 404 Not Found

Room not found (for `/api/rooms/{id}`).

```json
{
  "timestamp": "2025-10-18T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Room not found"
}
```

### 500 Internal Server Error

Server error.

```json
{
  "timestamp": "2025-10-18T10:30:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred"
}
```

---

## Performance

### Response Time Benchmarks

| Scenario       | Expected Time | Notes                       |
| -------------- | ------------- | --------------------------- |
| No filters     | < 200ms       | Baseline                    |
| Price filter   | < 150ms       | Using index IX_rooms_price  |
| Status filter  | < 100ms       | Using index IX_rooms_status |
| Status + Price | < 100ms       | Using composite index       |
| Complex query  | < 300ms       | Multiple filters            |

### Optimization Tips

1. **Always use indexes**: Đã tạo 6 indexes (xem migration V2)
2. **Limit page size**: Max 50 items/page để tránh slow query
3. **Use status filter**: Giảm số lượng rows cần scan
4. **Avoid wildcard amenities**: Càng cụ thể càng tốt

### Monitoring

```sql
-- Check index usage
SELECT
    i.name AS IndexName,
    s.user_seeks,
    s.user_scans
FROM sys.dm_db_index_usage_stats s
JOIN sys.indexes i ON s.object_id = i.object_id
WHERE OBJECT_NAME(s.object_id) = 'rooms';
```

---

## Integration Guide

### Frontend (React/Axios)

```javascript
import axios from "axios";

const searchRooms = async (filters) => {
  const params = new URLSearchParams({
    priceMax: filters.priceMax || "",
    guests: filters.guests || "",
    status: (filters.status || []).join(","),
    sort: filters.sort || "priceAsc",
    page: filters.page || 0,
    size: filters.size || 10,
  });

  const { data } = await axios.get(`/api/rooms/search?${params.toString()}`);

  return data; // { items, total, page, size }
};

// Usage
const result = await searchRooms({
  priceMax: 3000000,
  guests: 2,
  status: ["available"],
  sort: "priceAsc",
  page: 0,
  size: 10,
});
```

### Java (Spring RestTemplate)

```java
String url = "http://localhost:8080/api/rooms/search";
UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
    .queryParam("priceMax", 3000000)
    .queryParam("guests", 2)
    .queryParam("status", "available")
    .queryParam("sort", "priceAsc")
    .queryParam("page", 0)
    .queryParam("size", 10);

PagedResponse<Room> response = restTemplate.getForObject(
    builder.toUriString(),
    PagedResponse.class
);
```

---

## CORS Configuration

API hỗ trợ CORS cho các origins:

- `http://localhost:5173` (Vite dev)
- `http://localhost:3000` (React dev)
- `http://localhost:4173` (Vite preview)

**Headers:**

- `Access-Control-Allow-Origin: *`
- `Access-Control-Allow-Methods: GET, POST, PUT, DELETE`

---

## Swagger Documentation

API documentation có sẵn tại:

```
http://localhost:8080/swagger-ui.html
```

Hoặc OpenAPI spec:

```
http://localhost:8080/v3/api-docs
```

---

## Version History

| Version | Date       | Changes                       |
| ------- | ---------- | ----------------------------- |
| v1.0    | 2025-10-18 | Initial release               |
| v1.1    | 2025-10-18 | Added status filter, priceMin |

---

## Support

Nếu gặp vấn đề, vui lòng:

1. Kiểm tra API tests: `be_hotel/src/test/resources/room-search-api-tests.md`
2. Xem logs: `application.log`
3. Check database: Verify indexes với migration README

**Contact**: support@luxestay.com





