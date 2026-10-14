package com.civil.platform.module.service;

import com.civil.platform.common.exception.BizException;
import com.civil.platform.config.FileStorageProperties;
import com.civil.platform.module.entity.SysFileRecord;
import com.civil.platform.module.mapper.SysFileRecordMapper;
import com.civil.platform.common.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class FileStorageService {

    private final FileStorageProperties fileStorageProperties;
    private final SysFileRecordMapper sysFileRecordMapper;
    private final Path projectRoot;
    private final Path basePath;

    public FileStorageService(FileStorageProperties fileStorageProperties, SysFileRecordMapper sysFileRecordMapper) {
        this.fileStorageProperties = fileStorageProperties;
        this.sysFileRecordMapper = sysFileRecordMapper;
        this.projectRoot = resolveProjectRoot();
        this.basePath = resolveInsideProject(fileStorageProperties.getBasePath());
    }

    public Map<String, Object> store(MultipartFile file, String bizType) {
        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件不能为空");
        }
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = StringUtils.getFilenameExtension(originalFilename);
            String storedName = UUID.randomUUID().toString().replace("-", "") + (extension == null ? "" : "." + extension);
            Path folderPath = basePath.resolve(resolveFolderName(bizType)).normalize();
            ensureInsideProject(folderPath);
            File folder = folderPath.toFile();
            if (!folder.exists() && !folder.mkdirs()) {
                throw new BizException("创建上传目录失败");
            }
            File target = new File(folder, storedName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            SysFileRecord fileRecord = new SysFileRecord();
            fileRecord.setOriginalName(originalFilename);
            fileRecord.setStoredName(storedName);
            fileRecord.setFilePath(target.getAbsolutePath());
            fileRecord.setFileUrl(fileStorageProperties.getPublicPrefix() + storedName);
            fileRecord.setFileType(file.getContentType());
            fileRecord.setFileSize(file.getSize());
            fileRecord.setBizType(bizType);
            try {
                fileRecord.setUploaderId(SecurityUtils.getUserId());
            } catch (Exception ignored) {
            }
            sysFileRecordMapper.insert(fileRecord);
            fileRecord.setFileUrl(fileStorageProperties.getPublicPrefix() + fileRecord.getId());
            sysFileRecordMapper.updateById(fileRecord);

            Map<String, Object> result = new HashMap<>();
            result.put("id", fileRecord.getId());
            result.put("name", fileRecord.getOriginalName());
            result.put("url", fileRecord.getFileUrl());
            result.put("fileType", fileRecord.getFileType());
            result.put("fileSize", fileRecord.getFileSize());
            return result;
        } catch (IOException e) {
            throw new BizException("文件保存失败");
        }
    }

    public SysFileRecord getById(Long fileId) {
        SysFileRecord fileRecord = sysFileRecordMapper.selectById(fileId);
        if (fileRecord == null) {
            throw new BizException(404, "文件不存在");
        }
        return fileRecord;
    }

    public File resolveStoredFile(SysFileRecord fileRecord) {
        Path recordedPath = Paths.get(fileRecord.getFilePath());
        if (!recordedPath.isAbsolute()) {
            recordedPath = projectRoot.resolve(recordedPath);
        }
        recordedPath = recordedPath.toAbsolutePath().normalize();
        ensureInsideProject(recordedPath);
        return recordedPath.toFile();
    }

    private Path resolveProjectRoot() {
        Path workingDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if ("backend".equalsIgnoreCase(workingDir.getFileName().toString()) && workingDir.getParent() != null) {
            return workingDir.getParent();
        }
        return workingDir;
    }

    private Path resolveInsideProject(String configuredPath) {
        Path resolved = Paths.get(configuredPath);
        if (!resolved.isAbsolute()) {
            resolved = projectRoot.resolve(resolved);
        }
        resolved = resolved.toAbsolutePath().normalize();
        ensureInsideProject(resolved);
        return resolved;
    }

    private void ensureInsideProject(Path path) {
        if (!path.toAbsolutePath().normalize().startsWith(projectRoot)) {
            throw new BizException("上传目录必须位于项目目录内");
        }
    }

    private String resolveFolderName(String bizType) {
        String folderName = StringUtils.hasText(bizType) ? bizType.toLowerCase() : "common";
        return folderName.replaceAll("[^a-z0-9_-]", "_");
    }
}
