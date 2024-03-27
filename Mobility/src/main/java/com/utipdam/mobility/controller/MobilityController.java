package com.utipdam.mobility.controller;

import com.utipdam.mobility.FileUploadUtil;
import com.utipdam.mobility.business.DatasetDefinitionBusiness;
import com.utipdam.mobility.business.DatasetBusiness;
import com.utipdam.mobility.config.RestTemplateClient;
import com.utipdam.mobility.model.DatasetDefinitionDTO;
import com.utipdam.mobility.model.FileUploadResponse;
import com.utipdam.mobility.model.entity.Dataset;
import com.utipdam.mobility.model.entity.DatasetDefinition;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.validator.GenericValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.Date;
import java.util.*;
import java.util.stream.Stream;

@RestController
public class MobilityController {
    private static final Logger logger = LoggerFactory.getLogger(MobilityController.class);
    private final String START_TIME = "start_time";
    private final String RESOLUTION = "daily";

    @Autowired
    private DatasetDefinitionBusiness datasetDefinitionBusiness;

    @Autowired
    private DatasetBusiness datasetBusiness;

    private final String DATE_FORMAT = "yyyy-MM-dd";

    @Value("${utipdam.app.maxFileSize}")
    private long MAX_FILE_SIZE;

    @PostMapping("/mobility/upload-test")
    public ResponseEntity<Map<String, Object>> uploadTest(@RequestPart MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/mobility/upload-k")
    public ResponseEntity<Map<String, Object>> uploadTest(@RequestPart String k) {
        Map<String, Object> response = new HashMap<>();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @PostMapping("/mobility/upload")
    public ResponseEntity<Map<String, Object>> upload(@RequestPart String k,
                                                      @RequestPart MultipartFile file) {

        Map<String, Object> response = new HashMap<>();
        String errorMessage;
        if (!isNumeric(k)){
            errorMessage = "k must be a number between 0 - 100. You provided " + k;
            logger.error(errorMessage);
            response.put("error", errorMessage);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (!Objects.requireNonNull(file.getOriginalFilename()).endsWith(".csv")){
            errorMessage = "Please upload a csv file. You provided " + file.getOriginalFilename();
            logger.error(errorMessage);
            response.put("error", errorMessage);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            errorMessage = "Exceeded max file size " + MAX_FILE_SIZE;
            logger.error(errorMessage);
            response.put("error", errorMessage);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        List<FileUploadResponse> listResponse = new ArrayList<>();
        DatasetDefinitionDTO dto = new DatasetDefinitionDTO();
        dto.setName("dash-upload-" + getRandomNumberString());
        dto.setDescription("Dataset uploaded from the web channel");
        dto.setCountryCode("KR");
        dto.setFee(0.0);

        DatasetDefinition ds = datasetDefinitionBusiness.save(dto);

        File fOrg;
        String path = "/data/mobility/" + ds.getId();
        try {
            fOrg = new File(path);
            fOrg.setReadable(true, false);
            fOrg.setWritable(true, false);
            fOrg.mkdirs();
            Files.setPosixFilePermissions(Path.of("/data/mobility/" + ds.getId()), PosixFilePermissions.fromString("rwxrwxrwx"));
        } catch (IOException e) {
            errorMessage = e.getMessage();
            logger.error(errorMessage);
            response.put("error", errorMessage);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }


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
                    if (!GenericValidator.isDate(csvDate, DATE_FORMAT, true)){
                        errorMessage = "start_time must be yyyy-MM-dd HH:mm:ss format";
                        logger.error(errorMessage);
                        response.put("error", errorMessage);
                        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                    }
                }
            }

        } catch (IOException e) {
            errorMessage = e.getMessage();
            logger.error(errorMessage);
            response.put("error", errorMessage);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (csvDate != null){
            UUID uuid = UUID.randomUUID();
            fileName = "dataset-" + uuid + "-" + csvDate + ".csv";

            //process anonymization


            long dataPoints;
            try {
                FileUploadUtil.saveFile(fileName, file, uploadPath);
            } catch (IOException e) {
                errorMessage = e.getMessage();
                logger.error(errorMessage);
                response.put("error", errorMessage);
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            File fi = new File(fileName);
            fi.setReadable(true, false);
            fi.setWritable(true, false);

            Path filePath = Paths.get(uploadPath + "/" + fileName);
            try (Stream<String> stream = Files.lines(filePath)) {
                dataPoints = stream.count();
                Dataset dataset = new Dataset();
                dataset.setId(uuid);
                dataset.setDatasetDefinition(ds);
                if (csvDate != null) {
                    dataset.setStartDate(Date.valueOf(csvDate));
                    dataset.setEndDate(Date.valueOf(csvDate));
                }
                dataset.setResolution(RESOLUTION);
                dataset.setK(Integer.parseInt(k));
                dataset.setDataPoints(dataPoints);

                Dataset d = datasetBusiness.save(dataset);

                long size = Files.size(filePath) - 1;

                FileUploadResponse fileUploadResponse = new FileUploadResponse();
                fileUploadResponse.setDatasetDefinitionId(ds.getId());
                fileUploadResponse.setDatasetId(d.getId());
                fileUploadResponse.setFileName(fileName);
                fileUploadResponse.setSize(size);
                listResponse.add(fileUploadResponse);
                response.put("data", listResponse);
                return new ResponseEntity<>(response, HttpStatus.OK);

            } catch (IOException e) {
                errorMessage = e.getMessage();
                logger.error(errorMessage);
                response.put("error", errorMessage);
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);

    }
    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }

    @GetMapping("/mobility/download")
    public ResponseEntity<Resource> download(@RequestParam UUID datasetId) {
        HttpHeaders responseHeaders = new HttpHeaders();
        Optional<Dataset> dataset = datasetBusiness.getById(datasetId);
        if (dataset.isPresent()) {
            Dataset datasetObj = dataset.get();
            Optional<DatasetDefinition> df = datasetDefinitionBusiness.getById(datasetObj.getDatasetDefinition().getId());

            if (df.isPresent()) {
                DatasetDefinition definitionObj = df.get();
                if (definitionObj.getInternal() == null || !definitionObj.getInternal()) {
                    logger.info(definitionObj.getInternal().toString());
                    String path = "/data/mobility/" + datasetObj.getDatasetDefinition().getId();
                    File dir = new File(path);
                    FileFilter fileFilter = new WildcardFileFilter("*dataset-" + datasetId + "-*");
                    File[] files = dir.listFiles(fileFilter);
                    if (files != null) {
                        for (File fi : files) {

                            try {
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
                            } catch (IOException e) {
                                logger.error(e.getMessage());
                            }
                        }
                    }
                } else {
                    logger.info("downloading from internal server");
                    //download from internal archive server
                    RestTemplateClient restTemplate = new RestTemplateClient();
                    String domain = df.get().getServer().getDomain();

                    if (domain != null){
                        String uri = domain + "/internal/mobility/download";
                        logger.info(uri);
                        String url = UriComponentsBuilder
                                .fromUriString(uri)
                                .queryParam("datasetDefinitionId", definitionObj.getId())
                                .queryParam("datasetId", datasetObj.getId())
                                .build().toUriString();
                        try {
                            return restTemplate.restTemplate().exchange(url,
                                    HttpMethod.GET, null, new ParameterizedTypeReference<>() {
                                    });

                        } catch (Exception ex) {
                            logger.error(ex.getMessage());
                        }
                    }

                }
            }
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }


    //TODO for client use
    //creates new dataset and mobility record
    @PostMapping("/mobility/anonymizationJob")
    public ResponseEntity<Resource> anonymizeExternalAPI(@RequestPart DatasetDefinitionDTO dataset,
                                                         @RequestPart("file") MultipartFile file) {

        if (dataset.getName() == null) {
            logger.error("Name is required");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        DatasetDefinition ds = datasetDefinitionBusiness.save(dataset);

        File fOrg;
        String path = "/data/mobility/" + ds.getId();
        try {
            fOrg = new File(path);
            fOrg.setReadable(true, false);
            fOrg.setWritable(true, false);
            fOrg.mkdirs();
            Files.setPosixFilePermissions(Path.of("/data/mobility/" + ds.getId()), PosixFilePermissions.fromString("rwxrwxrwx"));
        } catch (IOException e) {
            logger.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Path uploadPath = Paths.get(path);
        String fileName;

        BufferedReader br;
        String csvDate = null;
        String line;
        try {
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
            br.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (csvDate != null) {
            Dataset dt = new Dataset();
            dt.setResolution(dataset.getResolution());
            dt.setDatasetDefinition(ds);
            dt.setStartDate(Date.valueOf(csvDate));
            dt.setEndDate(Date.valueOf(csvDate));
            dt.setK(dataset.getK());

            Dataset d = datasetBusiness.save(dt);

            fileName = "dataset-" + d.getId() + "-" + csvDate + ".csv";
            StringBuffer inputBuffer = new StringBuffer();
            HttpHeaders responseHeaders = new HttpHeaders();

            try {
                FileUploadUtil.saveFile(fileName, file, uploadPath);

                File fi = new File(fileName);
                fi.setReadable(true, false);
                fi.setWritable(true, false);

                br = new BufferedReader(
                        new InputStreamReader(new FileSystemResource("/data/mobility/" + ds.getId() + "/" + fileName).getInputStream()));

                String lineDownload;

                while ((lineDownload = br.readLine()) != null) {
                    inputBuffer.append(lineDownload);
                    inputBuffer.append('\n');
                }
                br.close();

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

            } catch (IOException e) {
                logger.error(e.getMessage());
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }

        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    public static String getRandomNumberString() {
        Random rnd = new Random();
        int number = rnd.nextInt(999999);
        return String.format("%06d", number);
    }
}