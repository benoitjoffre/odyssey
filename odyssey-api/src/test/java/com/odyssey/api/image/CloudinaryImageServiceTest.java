package com.odyssey.api.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

class CloudinaryImageServiceTest {

    private final CloudinaryImageService service = new CloudinaryImageService("", "", "");

    @Test
    void uploadRejectsUnsupportedFolders() {
        MockMultipartFile file = image("image/png");

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.upload("other", file)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void uploadRejectsNonImageContentTypes() {
        MockMultipartFile file = new MockMultipartFile("file", "document.txt", "text/plain", new byte[] { 1 });

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.upload("experiences", file)
        );

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, exception.getStatusCode());
    }

    @Test
    void uploadReportsMissingCloudinaryConfiguration() {
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.upload("experiences", image("image/png"))
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
    }

    private MockMultipartFile image(String contentType) {
        return new MockMultipartFile("file", "image", contentType, new byte[] { 1 });
    }
}
