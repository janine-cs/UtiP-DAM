package com.utipdam.internal.controller;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;

@RestController
public class DownloadController {
    @GetMapping("/analytics")
    public ResponseEntity<?> download(@RequestParam String siteId, @RequestParam String type) throws IOException {
        HttpHeaders responseHeaders = new HttpHeaders();

        String path = "/data/sites/site"+siteId+"/analytics/";
        File dir = new File(path);
        FileFilter fileFilter = new WildcardFileFilter("*_"+type+"_daily_*");
        File[] files = dir.listFiles(fileFilter);
        if (files == null) {
            String str = "File not found";
            ByteArrayResource resource = new ByteArrayResource(str.getBytes(StandardCharsets.UTF_8));
            ContentDisposition contentDisposition = ContentDisposition.builder("inline")
                    .filename("error.txt")
                    .build();

            responseHeaders.setContentDisposition(contentDisposition);

            return ResponseEntity.internalServerError()
                    .headers(responseHeaders)
                    .contentLength(str.length())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        }else{
            Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
            File fi = files[0];
            BufferedReader file = new BufferedReader(
                    new InputStreamReader(new FileSystemResource(fi).getInputStream()));
            StringBuffer inputBuffer = new StringBuffer();
            String line;

            while ((line = file.readLine()) != null) {
                inputBuffer.append(line);
                inputBuffer.append('\n');
            }
            file.close();
            String inputStr = inputBuffer.toString();

            ContentDisposition contentDisposition = ContentDisposition.builder("inline")
                    .filename(fi.getName())
                    .build();
            responseHeaders.setContentDisposition(contentDisposition);
            InputStream stream = new ByteArrayInputStream(inputStr.getBytes(StandardCharsets.UTF_8));
            InputStreamResource resource = new InputStreamResource(stream);
            return ResponseEntity.ok()
                    .headers(responseHeaders)
                    .contentLength(stream.available())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        }
    }


    @GetMapping("/mobility")
    public ResponseEntity<?> downloadMobility(@RequestParam String datasetDefinition) throws IOException {
        HttpHeaders responseHeaders = new HttpHeaders();

        String path = "/data/mobility/" + datasetDefinition + "/";
        File dir = new File(path);
        File[] files = dir.listFiles();
        if (files == null) {
            String str = "File not found";
            ByteArrayResource resource = new ByteArrayResource(str.getBytes(StandardCharsets.UTF_8));
            ContentDisposition contentDisposition = ContentDisposition.builder("inline")
                    .filename("error.txt")
                    .build();

            responseHeaders.setContentDisposition(contentDisposition);

            return ResponseEntity.internalServerError()
                    .headers(responseHeaders)
                    .contentLength(str.length())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } else {
            Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
            File fi = files[0];
            BufferedReader file = new BufferedReader(
                    new InputStreamReader(new FileSystemResource(fi).getInputStream()));
            StringBuffer inputBuffer = new StringBuffer();
            String line;

            while ((line = file.readLine()) != null) {
                inputBuffer.append(line);
                inputBuffer.append('\n');
            }
            file.close();
            String inputStr = inputBuffer.toString();

            ContentDisposition contentDisposition = ContentDisposition.builder("inline")
                    .filename(fi.getName())
                    .build();
            responseHeaders.setContentDisposition(contentDisposition);
            InputStream stream = new ByteArrayInputStream(inputStr.getBytes(StandardCharsets.UTF_8));
            InputStreamResource resource = new InputStreamResource(stream);
            return ResponseEntity.ok()
                    .headers(responseHeaders)
                    .contentLength(stream.available())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        }
    }
}
