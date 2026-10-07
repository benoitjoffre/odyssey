package com.odyssey.api.image;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CloudinaryImageService {

    private static final long MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024;
    private static final Map<String, String> ALLOWED_FOLDERS = Map.of(
        "experiences", "odyssey/experiences",
        "travel-events", "odyssey/travel-events"
    );
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/jpeg",
        "image/png",
        "image/webp",
        "image/gif"
    );

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;

    public CloudinaryImageService(
        @Value("${cloudinary.cloud-name:}") String cloudName,
        @Value("${cloudinary.api-key:}") String apiKey,
        @Value("${cloudinary.api-secret:}") String apiSecret
    ) {
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    public ImageUploadResponse upload(String folder, MultipartFile file) {
        String cloudinaryFolder = ALLOWED_FOLDERS.get(folder);
        if (cloudinaryFolder == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported image folder");
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An image file is required");
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Image must not exceed 10 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only JPEG, PNG, WebP, and GIF images are supported");
        }
        if (!StringUtils.hasText(cloudName) || !StringUtils.hasText(apiKey) || !StringUtils.hasText(apiSecret)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Cloudinary is not configured");
        }

        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
            "cloud_name", cloudName,
            "api_key", apiKey,
            "api_secret", apiSecret,
            "secure", true
        ));
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                    "folder", cloudinaryFolder,
                    "resource_type", "image"
                )
            );
            Object secureUrl = result.get("secure_url");
            if (secureUrl instanceof String imageUrl && StringUtils.hasText(imageUrl)) {
                return new ImageUploadResponse(imageUrl);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Cloudinary did not return a secure image URL");
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Image upload to Cloudinary failed", exception);
        }
    }
}
