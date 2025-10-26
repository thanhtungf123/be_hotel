package com.luxestay.hotel.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImageStorageService {
    private final Cloudinary cloudinary;

    public String upload(MultipartFile file, String folder) {
        try {
            Map upload = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder != null ? folder : "luxestay/ids",
                            "resource_type", "image",
                            "use_filename", true,
                            "unique_filename", true,
                            "overwrite", false
                    ));
            Object url = upload.get("secure_url");
            if (url == null) url = upload.get("url");
            return url != null ? url.toString() : null;
        } catch (Exception e) {
            throw new RuntimeException("Upload thất bại", e);
        }
    }
}
