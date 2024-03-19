package com.utipdam.mobility.controller;

import com.utipdam.mobility.FileUploadUtil;
import com.utipdam.mobility.business.DatasetDefinitionBusiness;
import com.utipdam.mobility.business.DatasetBusiness;
import com.utipdam.mobility.model.DatasetDTO;
import com.utipdam.mobility.model.DatasetDefinitionDTO;
import com.utipdam.mobility.model.FileUploadResponse;
import com.utipdam.mobility.model.entity.Dataset;
import com.utipdam.mobility.model.entity.DatasetDefinition;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;

@RestController
public class MobilityController {
    private static final Logger logger = LoggerFactory.getLogger(MobilityController.class);
    private final String START_TIME = "first_time_seen";
    @Autowired
    private DatasetDefinitionBusiness datasetDefinitionBusiness;

    @Autowired
    private DatasetBusiness datasetBusiness;

    @Value("${utipdam.app.internalApi}")
    private String uri;


    @PostMapping("/mobility/upload")
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        Map<String, Object> response = new HashMap<>();

        List<FileUploadResponse> listResponse = new ArrayList<>();
        DatasetDefinitionDTO dto = new DatasetDefinitionDTO();
        dto.setName("dash-upload-" + getRandomNumberString());

        DatasetDefinition ds = datasetDefinitionBusiness.save(dto);

        File fOrg;

        String path = "/data/mobility/" + ds.getId();
        fOrg = new File(path);
        fOrg.setReadable(true, false);
        fOrg.setWritable(true, false);
        fOrg.mkdirs();
        Files.setPosixFilePermissions(Path.of("/data/mobility/" + ds.getId()), PosixFilePermissions.fromString("rwxrwxrwx"));


        Path uploadPath = Paths.get(path);

        if (path != null) {
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


            Dataset dataset = new Dataset();
            dataset.setDatasetDefinition(ds);
            dataset.setStartDate(csvDate);
            dataset.setEndDate(csvDate);
            dataset.setResolution("daily");

            Dataset d = datasetBusiness.save(dataset);

            String fileName = "dataset-" + d.getId() + "-" + csvDate + ".csv";
            long size = file.getSize();

            FileUploadUtil.saveFile(fileName, file, uploadPath);

            FileUploadResponse fileUploadResponse = new FileUploadResponse();
            fileUploadResponse.setFileName(fileName);
            fileUploadResponse.setSize(size);
            listResponse.add(fileUploadResponse);

            File fi = new File(fileName);
            fi.setReadable(true, false);
            fi.setWritable(true, false);

        }

        response.put("data", listResponse);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }





    //TODO
    @RequestMapping(path = "/mobility/anonymize", method = RequestMethod.GET)
    public ResponseEntity<Resource> anonymize(@RequestParam UUID datasetId,
                                              @RequestParam String resolution, @RequestParam Integer k) {
        return null;
    }


    @RequestMapping(path = "/mobility/download", method = RequestMethod.GET)
    public ResponseEntity<Resource> download(@RequestParam UUID datasetId) throws IOException {
        HttpHeaders responseHeaders = new HttpHeaders();
        Optional<Dataset> dataset = datasetBusiness.getById(datasetId);
        if (dataset.isPresent()) {
            Dataset datasetObj = dataset.get();
            Optional<DatasetDefinition> df = datasetDefinitionBusiness.getById(datasetObj.getDatasetDefinition().getId());

            if (df.isPresent()) {
                DatasetDefinition definitionObj = df.get();

                if ((definitionObj.getInternal() != null && !definitionObj.getInternal()) || definitionObj.getInternal() == null) {
                    logger.info(definitionObj.getInternal().toString());
                    String path = "/data/mobility/" + datasetObj.getDatasetDefinition().getId();
                    File dir = new File(path);
                    FileFilter fileFilter = new WildcardFileFilter("*dataset-" + datasetId + "-*");
                    File[] files = dir.listFiles(fileFilter);
                    logger.info("*dataset-" + datasetId + "-*");
                    logger.info("files - " + files.length);
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
                } else {
                    logger.info("downloading from internal server");
                    //download from internal archive server
                    RestTemplate restTemplate = new RestTemplate();

                    String url = UriComponentsBuilder
                            .fromUriString(uri)
                            .queryParam("datasetDefinitionId", definitionObj.getId())
                            .queryParam("datasetId", datasetObj.getId())
                            .build().toUriString();

                    return restTemplate.exchange(url,
                            HttpMethod.GET, null, new ParameterizedTypeReference<ResponseEntity<Resource>>() {
                            }).getBody();

                }
            }


        }

        return null;
    }


    //TODO for client use
    //creates new dataset and mobility record
    @PostMapping(path = "/anonymize/anonymizationJob")
    public ResponseEntity<Resource> anonymizeExternalAPI(@RequestPart DatasetDefinitionDTO dataset,
                                                         @RequestPart("file") MultipartFile file) throws IOException {

        if (dataset.getName() == null) {
            dataset.setName("dash-upload-" + getRandomNumberString());
        }
        DatasetDefinition ds = datasetDefinitionBusiness.save(dataset);

        File fOrg;

        String path = "/data/mobility/" + ds.getId();
        fOrg = new File(path);
        fOrg.setReadable(true, false);
        fOrg.setWritable(true, false);
        fOrg.mkdirs();
        Files.setPosixFilePermissions(Path.of("/data/mobility/" + ds.getId()), PosixFilePermissions.fromString("rwxrwxrwx"));


        Path uploadPath = Paths.get(path);
        String fileName = null;
        if (path != null) {
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


            Dataset dt = new Dataset();
            dt.setResolution(dataset.getResolution());
            dt.setDatasetDefinition(ds);
            dt.setStartDate(csvDate);
            dt.setEndDate(csvDate);
            dt.setK(dataset.getK());

            Dataset d = datasetBusiness.save(dt);

            fileName = "dataset-" + d.getId() + "-" + csvDate + ".csv";

            FileUploadUtil.saveFile(fileName, file, uploadPath);

            File fi = new File(fileName);
            fi.setReadable(true, false);
            fi.setWritable(true, false);

        }


        HttpHeaders responseHeaders = new HttpHeaders();

        BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileSystemResource("/data/mobility/" + ds.getId() +"/" +fileName).getInputStream()));
        StringBuffer inputBuffer = new StringBuffer();
        String line;

        while ((line = br.readLine()) != null) {
            inputBuffer.append(line);
            inputBuffer.append('\n');
        }
        br.close();
        String inputStr = inputBuffer.toString();

        ContentDisposition contentDisposition = ContentDisposition.builder("inline")
                .filename("/data/mobility/" + ds.getId() +"/" + fileName)
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


    //internal server use. upload & anonymize
    //existing dataset
    @PostMapping("/anonymize/anonymizationJob/item")
    public ResponseEntity<Map<String, Object>> uploadInternal(@RequestPart String dataset,
                                                              @RequestPart("file") MultipartFile file) throws IOException {

        List<FileUploadResponse> listResponse = new ArrayList<>();
        Map<String, Object> response = new HashMap<>();

        File fOrg;

        String path = "/data/mobility/" + dataset;
        fOrg = new File(path);
        fOrg.setReadable(true, false);
        fOrg.setWritable(true, false);
        fOrg.mkdirs();
        Files.setPosixFilePermissions(Path.of("/data/mobility/" + dataset), PosixFilePermissions.fromString("rwxrwxrwx"));


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

    @RequestMapping(path = "/mobility/download/internal", method = RequestMethod.GET)
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

    public static String getRandomNumberString() {
        Random rnd = new Random();
        int number = rnd.nextInt(999999);
        return String.format("%06d", number);
    }
}