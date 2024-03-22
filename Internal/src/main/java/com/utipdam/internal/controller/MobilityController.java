package com.utipdam.internal.controller;


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utipdam.internal.FileUploadUtil;
import com.utipdam.internal.model.FileUploadResponse;
import com.utipdam.internal.model.Dataset;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import java.util.stream.Stream;

import static com.utipdam.internal.InternalApplication.token;

@RestController
public class MobilityController {
    private static final Logger logger = LoggerFactory.getLogger(MobilityController.class);

    @Value("${utipdam.app.internalApi}")
    private String uri;

    private final String START_TIME = "first_time_seen"; //TODO
    private final String RESOLUTION = "daily";
    private final int K = 20;

    //internal server use. upload & anonymize
    //existing dataset
    @PostMapping("/mobility/anonymization")
    public ResponseEntity<Map<String, Object>> uploadInternal(@RequestPart String datasetDefinition,
                                                              @RequestPart("file") MultipartFile file) throws IOException {

        Map<String, Object> response = new HashMap<>();

        File fOrg;

        String path = "/data/mobility/" + datasetDefinition;
        fOrg = new File(path);
        fOrg.setReadable(true, false);
        fOrg.setWritable(true, false);
        fOrg.mkdirs();
        Files.setPosixFilePermissions(Path.of("/data/mobility/" + datasetDefinition), PosixFilePermissions.fromString("rwxrwxrwx"));


        Path uploadPath = Paths.get(path);
        String fileName;
        BufferedReader br;
        String csvDate = null;

        try {
            String line;
            InputStream is = file.getInputStream();
            br = new BufferedReader(new InputStreamReader(is));
            line = br.readLine();
            String[] nextRecord;
            if (line != null) {
                nextRecord = line.split(",");
                int dateIndex = Arrays.asList(nextRecord).indexOf(START_TIME);
                if (dateIndex > 0 && (line = br.readLine()) != null) {
                    csvDate = line.split(",")[dateIndex];
                    csvDate = csvDate.split(" ")[0];
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        //headers.set("Authorization", "Bearer "+ token);

        //logger.info(token);

        //logger.info(requestJson);
        headers.setContentType(MediaType.APPLICATION_JSON);


        UUID uuid = UUID.randomUUID();
        fileName = "dataset-" + uuid + "-" + csvDate + ".csv";

        //process anonymization
        long dataPoints;
        HttpEntity<String> entity = null;
        FileUploadUtil.saveFile(fileName, file, uploadPath);

        File fi = new File(fileName);
        fi.setReadable(true, false);
        fi.setWritable(true, false);

        Path filePath = Paths.get(uploadPath + "/" + fileName);
        try (Stream<String> stream = Files.lines(filePath)) {
            dataPoints = stream.count();
            JSONObject request = new JSONObject();
            request.put("id", uuid);
            request.put("datasetDefinitionId", datasetDefinition);
            request.put("startDate", csvDate);
            request.put("endDate", csvDate);
            request.put("resolution", RESOLUTION);
            request.put("k", K);
            request.put("dataPoints", dataPoints);
            entity = new HttpEntity<>(request.toString(), headers);
        } catch (JSONException e) {
            logger.error(e.getMessage());
        }
        long size = Files.size(filePath);

        try {
            JsonNode node = restTemplate.exchange(uri + "/dataset",
                    HttpMethod.POST, entity, JsonNode.class).getBody();

            if (node != null) {
                JsonFactory jsonFactory = new JsonFactory();
                ObjectMapper objectMapper = new ObjectMapper(jsonFactory);
                JsonNode nodeResp = objectMapper.readValue(node.get("data").toString(), JsonNode.class);
                Dataset dataset = new ObjectMapper().readValue(nodeResp.toString(), new TypeReference<>() {
                });
                if (dataset != null) {
                    FileUploadResponse fileUploadResponse = new FileUploadResponse();
                    fileUploadResponse.setDatasetDefinitionId(UUID.fromString(datasetDefinition));
                    fileUploadResponse.setDatasetId(dataset.getId());
                    fileUploadResponse.setFileName(fileName);
                    fileUploadResponse.setSize(size);
                    response.put("data", fileUploadResponse);
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }
            }

        } catch (HttpClientErrorException e) {
            File f = new File(uploadPath + "/" + fileName);
            if (f.delete()) {
                logger.info(f + " file deleted");
            }
            logger.error(e.getMessage());
        }

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);

    }


    @GetMapping("/mobility/download")
    public ResponseEntity<Resource> downloadInternal(@RequestParam UUID datasetDefinitionId,
                                                     @RequestParam UUID datasetId) throws IOException {
        HttpHeaders responseHeaders = new HttpHeaders();

        String path = "/data/mobility/" + datasetDefinitionId;
        File dir = new File(path);
        FileFilter fileFilter = new WildcardFileFilter("*dataset-" + datasetId + "-*");
        File[] files = dir.listFiles(fileFilter);
        if (files != null) {
            for (File fi : files) {

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


        return null;
    }

}