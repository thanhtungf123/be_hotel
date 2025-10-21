package com.luxestay.hotel.controller;

import com.luxestay.hotel.dto.PagedResponse;
import com.luxestay.hotel.model.Room;
import com.luxestay.hotel.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@WebMvcTest(RoomController.class)
@DisplayName("Room Search & Filter API Tests")
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoomService roomService;

    private List<Room> mockRooms;

    @BeforeEach
    void setUp() {
        // Mock data
        Room room1 = new Room(1L, "Deluxe Ocean View", "Deluxe", 2, 35, 2500000,
                new String[] { "WiFi", "Ban công" }, "https://picsum.photos/800", true);
        Room room2 = new Room(2L, "Standard City View", "Standard", 2, 25, 1500000,
                new String[] { "WiFi" }, "https://picsum.photos/801", false);
        Room room3 = new Room(3L, "Suite Presidential", "Suite", 4, 80, 8000000,
                new String[] { "WiFi", "Jacuzzi", "Minibar" }, "https://picsum.photos/802", true);

        mockRooms = Arrays.asList(room1, room2, room3);
    }

    @Test
    @DisplayName("Test 1: GET /api/rooms - List all rooms")
    void testListAllRooms() throws Exception {
        when(roomService.listRooms()).thenReturn(mockRooms);

        mockMvc.perform(get("/api/rooms")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].name").value("Deluxe Ocean View"))
                .andExpect(jsonPath("$[1].priceVnd").value(1500000));
    }

    @Test
    @DisplayName("Test 2: GET /api/rooms/search - No filters (default)")
    void testSearchRoomsNoFilters() throws Exception {
        PagedResponse<Room> mockResponse = new PagedResponse<>(mockRooms, 3, 0, 10);
        when(roomService.search(any())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/rooms/search")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    @DisplayName("Test 3: Filter by PRICE (priceMax)")
    void testFilterByPrice() throws Exception {
        List<Room> filtered = mockRooms.stream()
                .filter(r -> r.getPriceVnd() <= 2000000)
                .toList();
        PagedResponse<Room> mockResponse = new PagedResponse<>(filtered, 2, 0, 10);
        when(roomService.search(any())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/rooms/search")
                .param("priceMax", "2000000")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[*].priceVnd", everyItem(lessThanOrEqualTo(2000000))));
    }

    @Test
    @DisplayName("Test 4: Filter by STATUS (available)")
    void testFilterByStatus() throws Exception {
        PagedResponse<Room> mockResponse = new PagedResponse<>(mockRooms, 3, 0, 10);
        when(roomService.search(any())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/rooms/search")
                .param("status", "available")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    @DisplayName("Test 5: Filter by MULTIPLE STATUS (available,occupied)")
    void testFilterByMultipleStatus() throws Exception {
        PagedResponse<Room> mockResponse = new PagedResponse<>(mockRooms, 3, 0, 10);
        when(roomService.search(any())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/rooms/search")
                .param("status", "available,occupied")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    @DisplayName("Test 6: Filter by GUESTS (capacity)")
    void testFilterByGuests() throws Exception {
        List<Room> filtered = mockRooms.stream()
                .filter(r -> r.getCapacity() >= 4)
                .toList();
        PagedResponse<Room> mockResponse = new PagedResponse<>(filtered, 1, 0, 10);
        when(roomService.search(any())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/rooms/search")
                .param("guests", "4")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].capacity", greaterThanOrEqualTo(4)));
    }

    @Test
    @DisplayName("Test 7: Filter by TYPES (bed layout)")
    void testFilterByTypes() throws Exception {
        PagedResponse<Room> mockResponse = new PagedResponse<>(mockRooms, 3, 0, 10);
        when(roomService.search(any())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/rooms/search")
                .param("types", "1 giường đôi,2 giường đơn")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    @DisplayName("Test 8: Filter by AMENITIES")
    void testFilterByAmenities() throws Exception {
        List<Room> filtered = mockRooms.stream()
                .filter(r -> Arrays.asList(r.getAmenities()).contains("WiFi"))
                .toList();
        PagedResponse<Room> mockResponse = new PagedResponse<>(filtered, 3, 0, 10);
        when(roomService.search(any())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/rooms/search")
                .param("amenities", "WiFi")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)));
    }

    @Test
    @DisplayName("Test 9: SORT by price ASC")
    void testSortByPriceAsc() throws Exception {
        List<Room> sorted = mockRooms.stream()
                .sorted((a, b) -> Integer.compare(a.getPriceVnd(), b.getPriceVnd()))
                .toList();
        PagedResponse<Room> mockResponse = new PagedResponse<>(sorted, 3, 0, 10);
        when(roomService.search(any())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/rooms/search")
                .param("sort", "priceAsc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].priceVnd").value(1500000))
                .andExpect(jsonPath("$.items[2].priceVnd").value(8000000));
    }

    @Test
    @DisplayName("Test 10: SORT by price DESC")
    void testSortByPriceDesc() throws Exception {
        List<Room> sorted = mockRooms.stream()
                .sorted((a, b) -> Integer.compare(b.getPriceVnd(), a.getPriceVnd()))
                .toList();
        PagedResponse<Room> mockResponse = new PagedResponse<>(sorted, 3, 0, 10);
        when(roomService.search(any())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/rooms/search")
                .param("sort", "priceDesc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].priceVnd").value(8000000));
    }

    @Test
    @DisplayName("Test 11: PAGINATION - Page 0, Size 2")
    void testPaginationPage0() throws Exception {
        List<Room> page0 = mockRooms.subList(0, 2);
        PagedResponse<Room> mockResponse = new PagedResponse<>(page0, 3, 0, 2);
        when(roomService.search(any())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/rooms/search")
                .param("page", "0")
                .param("size", "2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.total").value(3));
    }

    @Test
    @DisplayName("Test 12: PAGINATION - Page 1, Size 2")
    void testPaginationPage1() throws Exception {
        List<Room> page1 = mockRooms.subList(2, 3);
        PagedResponse<Room> mockResponse = new PagedResponse<>(page1, 3, 1, 2);
        when(roomService.search(any())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/rooms/search")
                .param("page", "1")
                .param("size", "2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    @DisplayName("Test 13: COMBINED FILTERS (price + status + guests)")
    void testCombinedFilters() throws Exception {
        List<Room> filtered = mockRooms.stream()
                .filter(r -> r.getPriceVnd() <= 3000000 && r.getCapacity() >= 2)
                .toList();
        PagedResponse<Room> mockResponse = new PagedResponse<>(filtered, 2, 0, 10);
        when(roomService.search(any())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/rooms/search")
                .param("priceMax", "3000000")
                .param("guests", "2")
                .param("status", "available")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    @DisplayName("Test 14: EMPTY RESULT - No rooms match filter")
    void testEmptyResult() throws Exception {
        PagedResponse<Room> mockResponse = new PagedResponse<>(List.of(), 0, 0, 10);
        when(roomService.search(any())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/rooms/search")
                .param("priceMax", "100")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    @DisplayName("Test 15: PRICE RANGE (priceMin + priceMax)")
    void testPriceRange() throws Exception {
        List<Room> filtered = mockRooms.stream()
                .filter(r -> r.getPriceVnd() >= 1500000 && r.getPriceVnd() <= 3000000)
                .toList();
        PagedResponse<Room> mockResponse = new PagedResponse<>(filtered, 2, 0, 10);
        when(roomService.search(any())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/rooms/search")
                .param("priceMin", "1500000")
                .param("priceMax", "3000000")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)));
    }
}





