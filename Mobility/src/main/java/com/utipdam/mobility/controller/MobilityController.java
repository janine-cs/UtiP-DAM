package com.utipdam.mobility.controller;

import com.utipdam.mobility.FileUploadUtil;
import com.utipdam.mobility.model.FileUploadResponse;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;

@RestController
public class MobilityController {
    private static final Logger logger = LoggerFactory.getLogger(MobilityController.class);

    @PostMapping("/mobility/upload/{datasetId}")
    public ResponseEntity<Map<String, Object>> upload(@PathVariable Integer datasetId,
                                                      @RequestParam("file") MultipartFile file) throws IOException {
        Map<String, Object> response = new HashMap<>();

        List<FileUploadResponse> listResponse = new ArrayList<>();

        File fOrg;

        String path = "/data/mobility/";
        fOrg = new File(path);
        fOrg.setReadable(true, false);
        fOrg.setWritable(true, false);
        fOrg.mkdirs();
        Files.setPosixFilePermissions(Path.of("/data/mobility/"), PosixFilePermissions.fromString("rwxrwxrwx"));

        path = "/data/mobility/" + datasetId;
        fOrg = new File(path);
        fOrg.setReadable(true, false);
        fOrg.setWritable(true, false);
        fOrg.mkdirs();
        Files.setPosixFilePermissions(Path.of("/data/mobility/"+datasetId), PosixFilePermissions.fromString("rwxrwxrwx"));


        Path uploadPath = Paths.get(path);

        if (path != null) {
            int idx = file.getOriginalFilename().indexOf("_");
            int idxEnd = file.getOriginalFilename().indexOf(".");
            String date = file.getOriginalFilename().substring(idx + 1, idxEnd);
            File dir = new File(path);
            FileFilter fileFilter = new WildcardFileFilter("*_" + date + "*");
            File[] files = dir.listFiles(fileFilter);
            if (files != null) {
                for (File fi : files) {
                    if (fi.delete()) {
                        logger.info("Deleted " + fi.getAbsolutePath());
                    }
                }
            }

            String fileName = StringUtils.cleanPath(file.getOriginalFilename());
            long size = file.getSize();

            FileUploadUtil.saveFile(fileName, file, uploadPath);

            FileUploadResponse fileUploadResponse = new FileUploadResponse();
            fileUploadResponse.setFileName(fileName);
            fileUploadResponse.setSize(size);
            listResponse.add(fileUploadResponse);

            files = dir.listFiles(fileFilter);
            if (files != null) {
                for (File fi : files) {
                    fi.setReadable(true, false);
                    fi.setWritable(true, false);
                }
            }

        }

        response.put("data", listResponse);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


}