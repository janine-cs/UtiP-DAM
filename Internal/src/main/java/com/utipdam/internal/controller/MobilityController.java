package com.utipdam.internal.controller;


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utipdam.internal.FileUploadUtil;
import com.utipdam.internal.model.FileUploadResponse;
import com.utipdam.internal.model.Dataset;
import org.apache.commons.io.filefilter.WildcardFileFilter;
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

import static com.utipdam.internal.InternalApplication.token;

@RestController
public class MobilityController {
    private static final Logger logger = LoggerFactory.getLogger(MobilityController.class);

    @Value("${utipdam.app.internalApi}")
    private String uri;

    private final String START_TIME = "first_time_seen";

    //internal server use. upload & anonymize
    //existing dataset
    @PostMapping("/mobility/anonymization")
    public ResponseEntity<Map<String, Object>> uploadInternal(@RequestPart String datasetDefinition,
                                                              @RequestPart("file") MultipartFile file) throws IOException {


        List<FileUploadResponse> listResponse = new ArrayList<>();
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
        String requestJson = "{\"datasetDefinitionId\": \""+datasetDefinition+"\", \"startDate\": \""+csvDate+"\" ," +
                "\"endDate\": \""+csvDate+"\", \"resolution\":\"daily\"," +
                "\"k\":20}";
        //logger.info(requestJson);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(requestJson ,headers);
        try {
            JsonNode node = restTemplate.exchange(uri+ "/dataset",
                    HttpMethod.POST, entity, JsonNode.class).getBody();

            if (node != null){
                JsonFactory jsonFactory = new JsonFactory();
                ObjectMapper objectMapper = new ObjectMapper(jsonFactory);
                JsonNode nodeResp = objectMapper.readValue(node.get("data").toString(), JsonNode.class);
                Dataset dataset = new ObjectMapper().readValue(nodeResp.toString(), new TypeReference<>() {
                });
                if (dataset != null){
                    fileName = "dataset-" + dataset.getId() + "-" + dataset.getStartDate() + ".csv";

                    FileUploadUtil.saveFile(fileName, file, uploadPath);

                    File fi = new File(fileName);
                    fi.setReadable(true, false);
                    fi.setWritable(true, false);
                }
            }

        }catch (HttpClientErrorException e){
            e.printStackTrace();
//                try {
//                    Dataset datasetObj = restTemplate.exchange(url,
//                            HttpMethod.GET, null, new ParameterizedTypeReference<Dataset>() {
//                            }).getBody();
//                }catch (HttpClientErrorException exception){
//                    exception.printStackTrace();
//
//                }
        }

        response.put("data", listResponse);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(path = "/mobility/download", method = RequestMethod.GET)
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