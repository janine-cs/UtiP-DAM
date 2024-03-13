package com.utipdam.mobility.controller;

import com.utipdam.mobility.FileUploadUtil;
import com.utipdam.mobility.business.DatasetBusiness;
import com.utipdam.mobility.business.MobilityBusiness;
import com.utipdam.mobility.model.DatasetDTO;
import com.utipdam.mobility.model.FileUploadResponse;
import com.utipdam.mobility.model.entity.Dataset;
import com.utipdam.mobility.model.entity.Mobility;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    private final String  START_TIME = "first_time_seen";
    @Autowired
    private DatasetBusiness datasetBusiness;

    @Autowired
    private MobilityBusiness mobilityBusiness;


    @PostMapping("/mobility/upload")
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        Map<String, Object> response = new HashMap<>();

        List<FileUploadResponse> listResponse = new ArrayList<>();
        DatasetDTO dataset = new DatasetDTO();
        dataset.setName("dash-upload-"+getRandomNumberString());

        Dataset ds = datasetBusiness.save(dataset);

        File fOrg;

        String path = "/data/mobility/uploads/"+ds.getId();
        fOrg = new File(path);
        fOrg.setReadable(true, false);
        fOrg.setWritable(true, false);
        fOrg.mkdirs();
        Files.setPosixFilePermissions(Path.of("/data/mobility/uploads/"+ds.getId()), PosixFilePermissions.fromString("rwxrwxrwx"));


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
                if (line != null){
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


            Mobility mobility = new Mobility();
            mobility.setResolution("daily");
            mobility.setDatasetId(ds.getId());
            mobility.setStartDate(csvDate);
            mobility.setEndDate(csvDate);

            Mobility mb = mobilityBusiness.save(mobility);

            String fileName = "mobility" + mb.getId() + "-" + csvDate + ".csv";
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
    @RequestMapping(path = "/mobility/anonymization", method = RequestMethod.GET)
    public ResponseEntity<Resource> anonymize(@RequestParam Integer mobilityId, @RequestParam Integer kValue) {
        return null;
    }

    @RequestMapping(path = "/mobility/download", method = RequestMethod.GET)
    public ResponseEntity<Resource> download(@RequestParam Integer mobilityId) throws IOException {
        HttpHeaders responseHeaders = new HttpHeaders();

        Optional<Mobility> mobility = mobilityBusiness.getById(mobilityId);
        if (mobility.isPresent()){
            String path = "/data/mobility/uploads/" + mobility.get().getDatasetId();
            File dir = new File(path);
            FileFilter fileFilter = new WildcardFileFilter("*mobility" + mobilityId + "-*");
            File[]  files = dir.listFiles(fileFilter);
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
        }

        return null;
    }


    //not used
    @PostMapping("/mobility/upload/internal/{datasetId}")
    public ResponseEntity<Map<String, Object>> uploadInternal(@PathVariable Integer datasetId,
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

    public static String getRandomNumberString() {
        Random rnd = new Random();
        int number = rnd.nextInt(999999);
        return String.format("%06d", number);
    }
}