// src/main/java/com/luxestay/hotel/controller/EmployeeAdminController.java
package com.luxestay.hotel.controller;

import com.luxestay.hotel.dto.employee.EmployeeRequest;
import com.luxestay.hotel.dto.employee.EmployeeResponse;
import com.luxestay.hotel.model.*;
import com.luxestay.hotel.model.entity.BookingEntity;
import com.luxestay.hotel.repository.AccountRepository;
import com.luxestay.hotel.repository.EmployeeRepository;
import com.luxestay.hotel.repository.RoleRepository;
import com.luxestay.hotel.repository.WorkShiftRepository;
import com.luxestay.hotel.service.AccountService;
import com.luxestay.hotel.service.AuthService;

import com.luxestay.hotel.service.EmployeeService;
import com.luxestay.hotel.service.ServicesService;
import com.luxestay.hotel.util.AuthorizationHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://localhost:4173",
        "http://localhost:3000"
})
@RequiredArgsConstructor
public class AdminController {

    @Autowired
    private final AccountService accountService;
    @Autowired
    private final EmployeeService employeeService;
    @Autowired
    private final ServicesService servicesService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    private WorkShiftRepository workShiftRepository;
    
    private final AuthorizationHelper authHelper;

    /* ---------- CRUD EMPLOYEE ---------- */
    /*
    Chỉ lấy 1 thông tin cho employee
    Phục vụ trang edit nhân viên
     */
    @GetMapping("/employees/{id}")
    public Employee get(@PathVariable("id") Integer id, HttpServletRequest request) {
        authHelper.requireAdmin(request);
        return employeeService.get(id);
    }
    /*
    Tạo/Thêm 1 nhân viên mới
    Các thành phần tạo: mã nhân viên, tài khoản email, lịch làm việc(Sẽ tạo bên workshift/schedule
     */
    @PostMapping("/employees")
    @ResponseStatus(HttpStatus.CREATED)
    public Employee create(@Valid @RequestBody Employee body,
                           @RequestParam(required = false) Integer accountId,
                           HttpServletRequest request) {
        authHelper.requireAdmin(request);
        return employeeService.create(body, accountId);
    }
    /*
    Chỉnh sử thông tin nhân viên
     */
    @PutMapping("/employees/{id}")
    public Employee update(@PathVariable("id") Integer id, 
                          @RequestBody Employee patch,
                          HttpServletRequest request) {
        authHelper.requireAdmin(request);
        return employeeService.update(id, patch);
    }
    /*
    Hàm tên xoá nhưng phục vụ mục đích ẩn nhân viên đi và hoạt động của cái mã đó sẽ bị tắt đi
     */
    @DeleteMapping("/employees/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Integer id, HttpServletRequest request) {
        authHelper.requireAdmin(request);
        employeeService.delete(id);
    }

    //Tạo danh sách chứa thông tin nhân viên
    @GetMapping("/employees")
    public List<Employee> getEmployees(HttpServletRequest request) {
        authHelper.requireAdmin(request);
        return employeeService.getAll();
    }

    //Chỉ liệt kê các danh sách khách hàng và hiện thỉ cho nhân viên
    @GetMapping("/employees/accounts")
    public List<Account> getAccountsRoleCustomer() {
        return accountService.getAllbyRoleId(1);
    }
    /* ---------- CRUD ACCOUNT ---------- */


    /*
    Liệt kê danh sách các tài khoản cho Admin
    khác mỗi chữ "s" ở tên hàm
     */
    @GetMapping("/accounts")
    public List<Account> getAccounts(HttpServletRequest request) {
//        requireAdmin(request);
        return accountService.findAll();
    }

    /*
    Lấy 1 thông tin dựa trên id của tài khoản
    khác mỗi chữ "s" ở tên hàm
     */
    @GetMapping("/accounts/{id}")
    public Account getAccount(@PathVariable("id") Integer id, HttpServletRequest request) {
        return accountService.findById(id);
    }

    /*
    Tạo tài khoản cho Khách hàng hoặc Tài Khoản riêng dành riêng cho nhân sự
     */
    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public Account createAccount(@RequestBody Account account,
                                 @RequestParam(name = "password",required = false) String password,
                                 HttpServletRequest request) {
        if (password != null && !password.isBlank()) {
            // Ưu tiên password (plain) từ query param -> hash
            account.setPasswordHash(passwordEncoder.encode(password));
        } else {
            // Nếu không có password param, mà passwordHash đang là plain → encode (tránh lưu plain text)
            if (needsEncoding(account.getPasswordHash())) {
                account.setPasswordHash(passwordEncoder.encode(account.getPasswordHash()));
            }
        }

        accountService.saveCreate(account);
        return account;
    }

    // Chỉnh sửa thông tin tài khoản của bên Admin
    @PutMapping("/accounts/{id}")
    public void updateAccount(@PathVariable("id") Integer id,
                              @RequestBody Account updatedAccount,
                              @RequestParam(name = "password",required = false) String password,
                              @RequestParam(name = "active", required = false) Boolean active) {
        Account existing = accountService.findById(id);

        existing.setFullName(updatedAccount.getFullName());
        existing.setPasswordHash(updatedAccount.getPasswordHash());
        existing.setRole(updatedAccount.getRole());

        // Mã hoá password
            if (password != null && !password.isBlank()) {
            existing.setPasswordHash(passwordEncoder.encode(password));
        } else if (updatedAccount.getPasswordHash() != null) {
            existing.setPasswordHash(
                    needsEncoding(updatedAccount.getPasswordHash())
                            ? passwordEncoder.encode(updatedAccount.getPasswordHash())
                            : updatedAccount.getPasswordHash()
            );
        }

        if (updatedAccount.getIsActive() != null) {
            existing.setIsActive(updatedAccount.getIsActive());
        } else if (active != null) {
            existing.setIsActive(active);
        }


        accountService.save(existing);
    }

    /*
     Đổi trạng thái account kích hoạt --> không kích hoạt
     Được dùng để khoá tài khoản và ngăn người dùng tạo lại tài khoản mới dựa trên thông tin cũ
     */
    @DeleteMapping("/accounts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@PathVariable("id") Integer id, HttpServletRequest request) {
        if (accountService.findById(id) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found");
        }
        accountService.delete(id);
    }
    //==========================================================================================
    private boolean needsEncoding(String rawOrHash) {
        if (rawOrHash == null || rawOrHash.isBlank()) return false; // không encode chuỗi rỗng
        String s = rawOrHash.trim();
        // Bcrypt thường bắt đầu bằng $2a$ / $2b$ / $2y$ (Spring Security BCryptPasswordEncoder)
        return !(s.startsWith("$2a$") || s.startsWith("$2b$") || s.startsWith("$2y$"));
    }
    //==========================================================================================
    @GetMapping("/accountHistory/{id}")
    public List<BookingEntity> getAccountHistory(@PathVariable("id") Integer id) {
        return employeeService.getHistory(id);
    }

    //========================================CRUD_SERVICE======================================

    @GetMapping("/services")
    public List<Services> getServices() {
        return servicesService.getAll();
    }

    @GetMapping("/services/{id}")
    public Services getService(@PathVariable("id") Integer id) {
        return servicesService.findById(id);
    }

    @PostMapping("/service/create")
//    @ResponseStatus(HttpStatus.CREATED)
    public void create(@RequestBody Services body) {
        servicesService.addService(body);
    }

    @PutMapping("services/{id}")
    public void updateService(@PathVariable("id") Integer id, @RequestBody Services patch) {
        servicesService.editService(id, patch);
    }

    @DeleteMapping("services/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteService(@PathVariable("id") Integer id) {
        servicesService.deleteService(id);
    }

    //==========================================Workshift========================================

    @GetMapping("/schedules")
    public List<WorkShift> getShiftsInRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            HttpServletRequest request) {
        // Allow both admin and staff to view schedules
        authHelper.requireAdminOrStaff(request);
        return workShiftRepository.findByStartTimeBetween(start, end);
    }

    @GetMapping("/schedules/{shiftId}")
    public WorkShift getShift(@PathVariable("shiftId") Integer id, HttpServletRequest request) {
        // Allow both admin and staff to view schedule details
        authHelper.requireAdminOrStaff(request);
        return workShiftRepository.findById(id).get();
    }

    @GetMapping("/workshifts")
    public List<WorkShift> getWorkShifts(HttpServletRequest request){
        // Allow both admin and staff to view all work shifts
        authHelper.requireAdminOrStaff(request);
        return workShiftRepository.findAll();
    }

    // Tạo một ca làm việc mới
    // THAY ĐỔI 2: Trả về WorkShift trực tiếp và dùng @ResponseStatus.
    @PostMapping("/schedules/create")
    @ResponseStatus(HttpStatus.CREATED) // Sẽ trả về HTTP 201 (Created) khi thành công
    public WorkShift createShift(@RequestBody WorkShift newShift, HttpServletRequest request) {
        authHelper.requireAdmin(request);
        if (newShift.getEmployee() == null || newShift.getEmployee().getId() == null) {
            // LƯU Ý: Không có ResponseEntity, cách duy nhất để báo lỗi
            // "Bad Request" (HTTP 400) là ném một Exception.
            throw new IllegalArgumentException("Employee ID không được rỗng khi tạo ca làm việc.");
        }

        // Tìm nhân viên đầy đủ từ DB
        int employeeId = newShift.getEmployee().getId();
        Employee employee = employeeService.get(employeeId);

        newShift.setEmployee(employee);
        newShift.setId(0);

        if (newShift.getStatus() == null || newShift.getStatus().isEmpty()) {
            newShift.setStatus("Scheduled");
        }

        return workShiftRepository.save(newShift);
    }

    // Cập nhật một ca làm việc
    // THAY ĐỔI 3: Trả về WorkShift trực tiếp.
    // Spring sẽ tự động trả về HTTP 200 (OK) khi thành công.
    @PutMapping("/schedules/{shiftId}")
    public WorkShift updateShift(
            @PathVariable int shiftId,
            @RequestBody WorkShift updateData,
            HttpServletRequest request) {
        authHelper.requireAdmin(request);
        WorkShift existingShift = workShiftRepository.findById(shiftId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ca làm việc với ID: " + shiftId));

        // Cập nhật nhân viên nếu ID thay đổi
        if (updateData.getEmployee() != null && updateData.getEmployee().getId() != null &&
                !updateData.getEmployee().getId().equals(existingShift.getEmployee().getId())) {

            int newEmployeeId = updateData.getEmployee().getId();
            Employee newEmployee = employeeService.get(newEmployeeId);
            existingShift.setEmployee(newEmployee);
        }

        // Cập nhật các thông tin khác
        existingShift.setStartTime(updateData.getStartTime());
        existingShift.setEndTime(updateData.getEndTime());
        existingShift.setShiftDetails(updateData.getShiftDetails());
        existingShift.setStatus(updateData.getStatus());

        return workShiftRepository.save(existingShift);
    }

    // Xóa một ca làm việc
    // THAY ĐỔI 4: Trả về void và dùng @ResponseStatus.
    @DeleteMapping("/schedules/{shiftId}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // Sẽ trả về HTTP 204 (No Content) khi thành công
    public void deleteShift(@PathVariable int shiftId, HttpServletRequest request) {
        authHelper.requireAdmin(request);
        // LƯU Ý: Phải kiểm tra trước khi xóa
        if (!workShiftRepository.existsById(shiftId)) {
            // Ném exception để báo lỗi "Not Found"
            // Nếu không có @ControllerAdvice, lỗi này sẽ thành HTTP 500
            throw new RuntimeException("Không tìm thấy ca làm việc với ID: " + shiftId);
        }

        workShiftRepository.deleteById(shiftId);
        // Không trả về gì cả (void)
    }



}
