package com.luxestay.hotel.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.Map;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
@CrossOrigin(
  origins = {"http://localhost:5173","http://127.0.0.1:5173","http://localhost:3000"},
  allowedHeaders = {"X-Auth-Token","Authorization","Content-Type"},
  exposedHeaders = {"Location"}
)
public class UploadController {
  private final Cloudinary cloudinary;

  @PostMapping(path="/id-card", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> uploadIdCard(@RequestPart("file") MultipartFile file) {
    try {
      if (file == null || file.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("message","File rỗng"));
      }
      String ct = file.getContentType() != null ? file.getContentType() : "";
      if (!ct.startsWith("image/")) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
            .body(Map.of("message","Chỉ chấp nhận file ảnh"));
      }

      var res = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
          "folder", "aurora-palace/id-cards",
          "resource_type", "image",
          "secure", true,
          "use_filename", true,
          "unique_filename", true
      ));
      return ResponseEntity.ok(Map.of("url", res.get("secure_url")));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("message","Upload thất bại", "error", e.getMessage()));
    }
  }

  @PostMapping(path="/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> uploadAvatar(@RequestPart("file") MultipartFile file) {
    try {
      if (file == null || file.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("message","File rỗng"));
      }
      String ct = file.getContentType() != null ? file.getContentType() : "";
      if (!ct.startsWith("image/")) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
            .body(Map.of("message","Chỉ chấp nhận file ảnh"));
      }

      var res = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
          "folder", "aurora-palace/avatars",
          "resource_type", "image",
          "secure", true,
          "use_filename", true,
          "unique_filename", true
      ));
      return ResponseEntity.ok(Map.of("url", res.get("secure_url")));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("message","Upload thất bại", "error", e.getMessage()));
    }
  }

  @PostMapping(path="/room-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> uploadRoomImage(@RequestPart("file") MultipartFile file) {
    try {
      if (file == null || file.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("message","File rỗng"));
      }
      String ct = file.getContentType() != null ? file.getContentType() : "";
      if (!ct.startsWith("image/")) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
            .body(Map.of("message","Chỉ chấp nhận file ảnh"));
      }

      var res = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
          "folder", "aurora-palace/rooms",
          "resource_type", "image",
          "secure", true,
          "use_filename", true,
          "unique_filename", true
      ));
      return ResponseEntity.ok(Map.of("url", res.get("secure_url")));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("message","Upload thất bại", "error", e.getMessage()));
    }
  }
}

