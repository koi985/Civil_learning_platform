package com.civil.platform.module.controller;

import com.civil.platform.common.api.ApiResponse;
import com.civil.platform.common.annotation.OperationLog;
import com.civil.platform.module.entity.SysFileRecord;
import com.civil.platform.module.service.FileStorageService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.EOFException;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class UploadController {

    private final FileStorageService fileStorageService;

    public UploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "文件中心", name = "上传文件")
    public ApiResponse<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                                   @RequestParam(value = "bizType", required = false) String bizType) {
        return ApiResponse.success(fileStorageService.store(file, bizType));
    }

    @PostMapping("/register-certificate")
    @OperationLog(module = "文件中心", name = "注册上传教师资格证")
    public ApiResponse<Map<String, Object>> uploadRegisterCertificate(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(fileStorageService.store(file, "certificate"));
    }

    @GetMapping("/view/{fileId}")
    public ResponseEntity<?> view(@PathVariable Long fileId,
                                  @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) throws IOException {
        SysFileRecord fileRecord = fileStorageService.getById(fileId);
        File file = fileStorageService.resolveStoredFile(fileRecord);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = resolveMediaType(fileRecord, file);
        String encodedFileName = UriUtils.encode(fileRecord.getOriginalName(), StandardCharsets.UTF_8);
        FileSystemResource resource = new FileSystemResource(file);
        HttpHeaders headers = buildViewHeaders(fileRecord, file, encodedFileName);
        if (StringUtils.hasText(rangeHeader)) {
            List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
            if (!ranges.isEmpty()) {
                long fileLength = file.length();
                HttpRange range = ranges.get(0);
                long start = range.getRangeStart(fileLength);
                long end = range.getRangeEnd(fileLength);
                if (start >= fileLength || end < start) {
                    headers.set(HttpHeaders.CONTENT_RANGE, "bytes */" + fileLength);
                    return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                            .headers(headers)
                            .build();
                }
                long rangeLength = end - start + 1;
                InputStream inputStream = Files.newInputStream(file.toPath());
                skipFully(inputStream, start);
                headers.set(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileLength);
                headers.setContentLength(rangeLength);
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .headers(headers)
                        .contentType(mediaType)
                        .body(new InputStreamResource(new LimitedInputStream(inputStream, rangeLength)));
            }
        }
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(mediaType)
                .contentLength(file.length())
                .body(resource);
    }

    private HttpHeaders buildViewHeaders(SysFileRecord fileRecord, File file, String encodedFileName) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedFileName);
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        headers.setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic().getHeaderValue());
        headers.setETag("\"file-" + fileRecord.getId() + "-" + file.length() + "-" + file.lastModified() + "\"");
        headers.setLastModified(file.lastModified());
        return headers;
    }

    private MediaType resolveMediaType(SysFileRecord fileRecord, File file) {
        String recordedType = fileRecord.getFileType();
        if (StringUtils.hasText(recordedType) && !MediaType.APPLICATION_OCTET_STREAM_VALUE.equalsIgnoreCase(recordedType)) {
            try {
                return MediaType.parseMediaType(recordedType);
            } catch (Exception ignored) {
            }
        }
        try {
            String probedType = Files.probeContentType(file.toPath());
            if (StringUtils.hasText(probedType)) {
                return MediaType.parseMediaType(probedType);
            }
        } catch (IOException ignored) {
        }
        if (StringUtils.hasText(recordedType)) {
            try {
                return MediaType.parseMediaType(recordedType);
            } catch (Exception ignored) {
            }
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private void skipFully(InputStream inputStream, long bytesToSkip) throws IOException {
        long remaining = bytesToSkip;
        while (remaining > 0) {
            long skipped = inputStream.skip(remaining);
            if (skipped <= 0) {
                if (inputStream.read() == -1) {
                    throw new EOFException("Cannot skip to requested range");
                }
                skipped = 1;
            }
            remaining -= skipped;
        }
    }

    private static class LimitedInputStream extends FilterInputStream {
        private long remaining;

        LimitedInputStream(InputStream inputStream, long limit) {
            super(inputStream);
            this.remaining = limit;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int value = super.read();
            if (value != -1) {
                remaining--;
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int bytesRead = super.read(buffer, offset, (int) Math.min(length, remaining));
            if (bytesRead != -1) {
                remaining -= bytesRead;
            }
            return bytesRead;
        }
    }
}
