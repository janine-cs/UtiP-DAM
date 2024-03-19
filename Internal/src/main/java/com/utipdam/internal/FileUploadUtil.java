package com.utipdam.internal;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FileUploadUtil {
    public static void saveFile(String fileName, MultipartFile multipartFile, Path uploadPath)
            throws IOException {

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        //String fileCode = RandomStringUtils.randomAlphanumeric(8);

        try (InputStream inputStream = multipartFile.getInputStream()) {
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ioe) {
            throw new IOException("Could not save file: " + fileName, ioe);
        }

        //return fileCode;
    }
}