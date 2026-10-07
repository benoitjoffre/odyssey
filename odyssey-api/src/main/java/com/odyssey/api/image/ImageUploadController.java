package com.odyssey.api.image;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/images")
public class ImageUploadController {

    private final CloudinaryImageService cloudinaryImageService;

    public ImageUploadController(CloudinaryImageService cloudinaryImageService) {
        this.cloudinaryImageService = cloudinaryImageService;
    }

    @PostMapping(path = "/{folder}/upload", consumes = "multipart/form-data")
    public ImageUploadResponse upload(
        @PathVariable String folder,
        @RequestParam("file") MultipartFile file
    ) {
        return cloudinaryImageService.upload(folder, file);
    }
}
