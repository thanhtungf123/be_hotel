# Room Search API - Manual Test Cases

## Base URL

```
http://localhost:8080/api/rooms
```

## Test Cases

### ✅ Test 1: List All Rooms

```bash
curl -X GET "http://localhost:8080/api/rooms" \
  -H "Accept: application/json"
```

**Expected**: List of all available rooms

---

### ✅ Test 2: Search - No Filters

```bash
curl -X GET "http://localhost:8080/api/rooms/search" \
  -H "Accept: application/json"
```

**Expected**: Paginated result with default values (page=0, size=10)

---

### ✅ Test 3: Filter by Price (Max)

```bash
curl -X GET "http://localhost:8080/api/rooms/search?priceMax=2000000" \
  -H "Accept: application/json"
```

**Expected**: Only rooms ≤ 2,000,000 VND

---

### ✅ Test 4: Filter by Price Range (Min + Max)

```bash
curl -X GET "http://localhost:8080/api/rooms/search?priceMin=1500000&priceMax=3000000" \
  -H "Accept: application/json"
```

**Expected**: Rooms between 1.5M - 3M VND

---

### ✅ Test 5: Filter by Status (Single)

```bash
curl -X GET "http://localhost:8080/api/rooms/search?status=available" \
  -H "Accept: application/json"
```

**Expected**: Only available rooms

---

### ✅ Test 6: Filter by Status (Multiple)

```bash
curl -X GET "http://localhost:8080/api/rooms/search?status=available,occupied" \
  -H "Accept: application/json"
```

**Expected**: Rooms with status "available" OR "occupied"

---

### ✅ Test 7: Filter by Guests (Capacity)

```bash
curl -X GET "http://localhost:8080/api/rooms/search?guests=4" \
  -H "Accept: application/json"
```

**Expected**: Rooms with capacity ≥ 4 guests

---

### ✅ Test 8: Filter by Room Types (Bed Layout)

```bash
curl -X GET "http://localhost:8080/api/rooms/search?types=1%20gi%C6%B0%E1%BB%9Dng%20%C4%91%C3%B4i" \
  -H "Accept: application/json"
```

**Expected**: Rooms with "1 giường đôi" bed layout

---

### ✅ Test 9: Filter by Multiple Types

```bash
curl -X GET "http://localhost:8080/api/rooms/search?types=1%20gi%C6%B0%E1%BB%9Dng%20%C4%91%C3%B4i,2%20gi%C6%B0%E1%BB%9Dng%20%C4%91%C6%A1n" \
  -H "Accept: application/json"
```

**Expected**: Rooms matching any of the specified bed layouts

---

### ✅ Test 10: Filter by Amenities

```bash
curl -X GET "http://localhost:8080/api/rooms/search?amenities=WiFi%20mi%E1%BB%85n%20ph%C3%AD" \
  -H "Accept: application/json"
```

**Expected**: Rooms with "WiFi miễn phí" amenity

---

### ✅ Test 11: Filter by Multiple Amenities

```bash
curl -X GET "http://localhost:8080/api/rooms/search?amenities=WiFi%20mi%E1%BB%85n%20ph%C3%AD,Ban%20c%C3%B4ng" \
  -H "Accept: application/json"
```

**Expected**: Rooms with at least one of the specified amenities

---

### ✅ Test 12: Sort by Price ASC

```bash
curl -X GET "http://localhost:8080/api/rooms/search?sort=priceAsc" \
  -H "Accept: application/json"
```

**Expected**: Rooms sorted from cheapest to most expensive

---

### ✅ Test 13: Sort by Price DESC

```bash
curl -X GET "http://localhost:8080/api/rooms/search?sort=priceDesc" \
  -H "Accept: application/json"
```

**Expected**: Rooms sorted from most expensive to cheapest

---

### ✅ Test 14: Pagination - Page 0, Size 5

```bash
curl -X GET "http://localhost:8080/api/rooms/search?page=0&size=5" \
  -H "Accept: application/json"
```

**Expected**: First 5 rooms (items[0-4])

---

### ✅ Test 15: Pagination - Page 1, Size 5

```bash
curl -X GET "http://localhost:8080/api/rooms/search?page=1&size=5" \
  -H "Accept: application/json"
```

**Expected**: Next 5 rooms (items[5-9])

---

### ✅ Test 16: Combined Filters

```bash
curl -X GET "http://localhost:8080/api/rooms/search?priceMax=3000000&guests=2&status=available&sort=priceAsc&page=0&size=10" \
  -H "Accept: application/json"
```

**Expected**: Available rooms ≤ 3M VND, capacity ≥ 2, sorted by price

---

### ✅ Test 17: Complex Query (All Parameters)

```bash
curl -X GET "http://localhost:8080/api/rooms/search?priceMin=1000000&priceMax=5000000&guests=2&types=1%20gi%C6%B0%E1%BB%9Dng%20%C4%91%C3%B4i&amenities=WiFi%20mi%E1%BB%85n%20ph%C3%AD&status=available&sort=priceAsc&page=0&size=10" \
  -H "Accept: application/json"
```

**Expected**: Filtered + sorted + paginated result

---

### ✅ Test 18: Empty Result

```bash
curl -X GET "http://localhost:8080/api/rooms/search?priceMax=1" \
  -H "Accept: application/json"
```

**Expected**:

```json
{
  "items": [],
  "total": 0,
  "page": 0,
  "size": 10
}
```

---

## Response Format

### Success Response

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
      "amenities": ["WiFi miễn phí", "Ban công"],
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

## Performance Benchmarks

| Test Case                   | Expected Response Time | Notes                       |
| --------------------------- | ---------------------- | --------------------------- |
| No filters                  | < 200ms                | Baseline query              |
| Price filter                | < 150ms                | Using IX_rooms_price        |
| Status filter               | < 100ms                | Using IX_rooms_status       |
| Status + Price              | < 100ms                | Using IX_rooms_status_price |
| With pagination             | < 150ms                | Efficient with indexes      |
| Complex query (all filters) | < 300ms                | Multiple index usage        |

## SQL Query Examples

### Query Generated (No Filters)

```sql
SELECT r FROM RoomEntity r
LEFT JOIN FETCH r.bedLayout bl
WHERE r.status IN ('available')
ORDER BY r.pricePerNight ASC
OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY;
```

### Query Generated (With Filters)

```sql
SELECT r FROM RoomEntity r
LEFT JOIN FETCH r.bedLayout bl
WHERE r.status IN ('available', 'occupied')
  AND r.pricePerNight >= 1000000
  AND r.pricePerNight <= 5000000
  AND bl.layoutName IN ('1 giường đôi')
ORDER BY r.pricePerNight ASC
OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY;
```

## Tips for Testing

1. **Use Postman Collection**: Import as collection for easier testing
2. **Check Response Headers**: Verify `Content-Type: application/json`
3. **Validate Pagination**: total / size = expected pages
4. **Test Edge Cases**: Empty results, invalid params, large page numbers
5. **Performance**: Monitor response times with `time curl ...`
6. **Database State**: Ensure test data exists in `rooms` table

## Troubleshooting

### Issue: Empty Results

- Check if rooms exist in database
- Verify filters are not too restrictive
- Check room status (default only shows 'available')

### Issue: Slow Response

- Run `UPDATE STATISTICS rooms;`
- Check if indexes are created (see migration README)
- Monitor with SQL Profiler

### Issue: 500 Error

- Check application logs
- Verify database connection
- Check for SQL syntax errors in repository query





