package com.luxestay.hotel.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {
    private final Cloudinary cloudinary;

    @PostMapping(path="/id-card", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String,Object> uploadIdCard(@RequestPart("file") MultipartFile file) throws Exception {
        var res = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", "luxestay/id-cards",
                "resource_type", "image"
        ));
        return Map.of("url", res.get("secure_url"));
    }
}
