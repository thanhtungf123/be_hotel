-- ========================================
-- Migration: Add Indexes for Room Search
-- Version: V2
-- Date: 2025-10-18
-- Purpose: Optimize room search queries
-- ========================================

-- Index cho column STATUS (available, occupied, maintenance)
-- Tăng tốc query: WHERE status = ?
CREATE NONCLUSTERED INDEX IX_rooms_status
ON rooms(status)
INCLUDE (room_id, room_name, price_per_night, capacity);

-- Index cho column PRICE_PER_NIGHT
-- Tăng tốc query: WHERE price_per_night BETWEEN ? AND ?
CREATE NONCLUSTERED INDEX IX_rooms_price
ON rooms(price_per_night ASC)
INCLUDE (room_id, room_name, status, capacity);

-- Composite index cho STATUS + PRICE (most common query)
-- Tăng tốc query: WHERE status IN (...) AND price_per_night <= ?
CREATE NONCLUSTERED INDEX IX_rooms_status_price
ON rooms(status, price_per_night ASC)
INCLUDE (room_id, room_name, capacity, bed_layout_id);

-- Index cho BED_LAYOUT_ID (FK lookup)
-- Tăng tốc JOIN với bed_layouts table
CREATE NONCLUSTERED INDEX IX_rooms_bed_layout
ON rooms(bed_layout_id)
INCLUDE (room_id, room_name, price_per_night, status);

-- Index cho CAPACITY (guest filter)
-- Tăng tốc query: WHERE capacity >= ?
CREATE NONCLUSTERED INDEX IX_rooms_capacity
ON rooms(capacity)
INCLUDE (room_id, room_name, price_per_night, status);

-- Full-text search index cho ROOM_NAME và ROOM_NUMBER
-- Tăng tốc query: WHERE room_name LIKE ? OR room_number LIKE ?
CREATE NONCLUSTERED INDEX IX_rooms_search
ON rooms(room_name, room_number)
INCLUDE (room_id, price_per_night, status, capacity);

-- ========================================
-- Statistics Update (SQL Server specific)
-- ========================================
-- Cập nhật thống kê để optimizer chọn index tốt hơn
UPDATE STATISTICS rooms;

-- ========================================
-- Performance Notes:
-- ========================================
-- 1. IX_rooms_status_price: Covering index cho query phổ biến nhất
-- 2. INCLUDE columns: Avoid key lookup, faster query
-- 3. Composite index order: Equality first (status), then range (price)
-- 4. Monitor index usage với: sys.dm_db_index_usage_stats
-- 5. Rebuild index định kỳ: ALTER INDEX ALL ON rooms REBUILD
-- ========================================


