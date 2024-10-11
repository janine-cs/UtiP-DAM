package com.utipdam.internal.controller;


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.RFC4180Parser;
import com.opencsv.RFC4180ParserBuilder;
import com.opencsv.exceptions.CsvValidationException;
import com.utipdam.internal.model.FileUploadResponse;
import com.utipdam.internal.model.Dataset;
import com.utipdam.internal.model.VisitorTracks;
import com.utipdam.internal.model.VisitorTracksNew;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.validator.GenericValidator;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
public class MobilityController {
    private static final Logger logger = LoggerFactory.getLogger(MobilityController.class);

    @Value("${utipdam.app.internalApi}")
    private String uri;

    @Value("${utipdam.app.anonymization}")
    private String ANONYMIZATION_VERSION;

    private final String START_TIME = "start_time";
    private final String RESOLUTION = "daily";
    private final String DATE_FORMAT = "yyyy-MM-dd";

    private final String[] NEW_CSV_FORMAT = {"dataset_id", "location_id", "anonymized_unique_id", "start_time", "end_time", "distance"};

    //internal server use. upload & anonymize
    //existing dataset
    @PostMapping("/mobility/upload")
    public ResponseEntity<Map<String, Object>> uploadInternal(@RequestPart String datasetDefinition,
                                                              @RequestPart String k,
                                                              @RequestPart String file) {

        Map<String, Object> response = new HashMap<>();
        String errorMessage;
        if (!isNumeric(k)) {
            errorMessage = "k must be a number between 0 - 100. You provided " + k;
            logger.error(errorMessage);
            response.put("error", errorMessage);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (!Objects.requireNonNull(file.endsWith(".csv"))) {
            errorMessage = "Please upload a csv file. You provided " + file;
            logger.error(errorMessage);
            response.put("error", errorMessage);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        File fOrg;
        String path = "/data/mobility/" + datasetDefinition;
        try {
            fOrg = new File(path);
            fOrg.setReadable(true, false);
            fOrg.setWritable(true, false);
            fOrg.mkdirs();
            Files.setPosixFilePermissions(Path.of("/data/mobility/" + datasetDefinition), PosixFilePermissions.fromString("rwxrwxrwx"));
        } catch (IOException e) {
            errorMessage = e.getMessage();
            logger.error(errorMessage);
            response.put("error", errorMessage);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        //anonymization process
        String fileName;
        String csvDate = null;

        UUID uuid = UUID.randomUUID();
        fileName = "dataset-" + uuid + "-.csv";

        String strPath = path + "/" + fileName;
        File fi = new File(strPath);
        fi.setReadable(true, false);
        fi.setWritable(true, false);

        try {
            String pyPath = "/opt/utils/anonymization-v"+ANONYMIZATION_VERSION+".py";
            logger.info("version " + ANONYMIZATION_VERSION);
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c","/opt/python_projects/envs/lbacore/bin/python3 " + pyPath + " --input " + file + " --k " + k + " | tail -n +2");
            processBuilder.redirectErrorStream(true);
            processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(fi));
            Process process = processBuilder.start();

            int exitVal = process.waitFor();
            logger.info("exitVal " + exitVal);
            if (exitVal == 0) {
                String line;
                long i = 0;
                int dateIndex = -1;
                BufferedReader br = new BufferedReader(new FileReader(strPath));
                StringBuffer inputBuffer = new StringBuffer();

                while ((line = br.readLine()) != null) {
                    if (i == 0 || dateIndex < 0) {
                        if (!line.contains("_id")){
                            continue;
                        }
                        String[] nextRecord = line.split(",");
                        dateIndex = Arrays.asList(nextRecord).indexOf(START_TIME);
                    }

                    inputBuffer.append(line);
                    inputBuffer.append('\n');

                    if (dateIndex > 0 && i == 1) {
                        csvDate = line.split(",")[dateIndex];
                        csvDate = csvDate.split(" ")[0];
                        if (!GenericValidator.isDate(csvDate, DATE_FORMAT, true)) {
                            errorMessage = "first_time_seen must be yyyy-MM-dd HH:mm:ss format";
                            logger.error(errorMessage);
                            throw new ResponseStatusException(
                                    HttpStatus.BAD_REQUEST, errorMessage);
                        }
                    }
                    i++;
                }
                br.close();
                fileName = "dataset-" + uuid + "-" + csvDate + ".csv";
                String strPathNew = path + "/" + fileName;
                Path oldPath = Paths.get(strPath);
                Path newPath = Paths.get(strPathNew);
                Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);

                if (csvDate != null) {
                    long dataPoints = i- 1;
                    logger.info("dataPoints:" + dataPoints);
                    JSONObject request = new JSONObject();
                    request.put("id", uuid);
                    request.put("datasetDefinitionId", datasetDefinition);
                    request.put("startDate", csvDate);
                    request.put("endDate", csvDate);
                    request.put("resolution", RESOLUTION);
                    request.put("k", k);
                    request.put("dataPoints", dataPoints);
                    HttpEntity<String> entity = new HttpEntity<>(request.toString(), headers);
                    try {
                        long size = Files.size(newPath);

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


                    } catch (HttpClientErrorException | IOException e) {
                        File f = new File(strPathNew);
                        if (f.delete()) {
                            logger.info(f + " file deleted");
                        }
                        errorMessage = e.getMessage();
                        logger.error(errorMessage);
                        response.put("error", errorMessage);
                        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
                    }

                }

            } else {
                errorMessage = "Error in anonymization.py command";
                logger.error(errorMessage);
                response.put("error", errorMessage);
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }

        } catch (IOException | InterruptedException | JSONException e) {
            errorMessage = e.getMessage();
            logger.error(errorMessage);
            response.put("error", errorMessage);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);

    }

    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @GetMapping("/mobility/download")
    public ResponseEntity<StreamingResponseBody> downloadInternal(@RequestParam String datasetDefinitionId,
                                                                  @RequestParam String datasetIds) {

        String[] datasetArr = datasetIds.split(",");
        if (datasetArr.length < 1) {
            logger.error("Please select at least one dataset - datasetIds");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        String path = "/data/mobility/" + datasetDefinitionId;
        File dir = new File(path);
        if (dir.exists()) {
            StreamingResponseBody streamResponse = clientOut -> {
                try (ZipOutputStream zos = new ZipOutputStream(clientOut)) {
                    for (String datasetId : datasetArr) {
                        FileFilter fileFilter = new WildcardFileFilter("*dataset-" + datasetId + "-*");
                        File[] files = dir.listFiles(fileFilter);
                        if (files != null && files.length > 0) {
                            File fi = files[0];
                            addToZipFile(zos, new FileSystemResource(fi).getInputStream(), fi.getName());
                        }
                    }
                } finally {
                    clientOut.close();
                }

            };

            return ResponseEntity.ok()
                    .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=datasets.zip")
                    .contentType(MediaType.parseMediaType("application/zip")).body(streamResponse);
        } else {
            logger.error("dataset definition path not found");
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

    }

    private void addToZipFile(ZipOutputStream zos, InputStream fis, String filename) throws IOException {
        ZipEntry zipEntry = new ZipEntry(filename);
        zos.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zos.write(bytes, 0, length);
        }
        zos.closeEntry();
        fis.close();
    }

    @GetMapping("/mobility/visitorDetection")
    public Integer findMeHere(@RequestParam String datasetDefinitionId,
                              @RequestParam String datasetId,
                              @RequestParam String locationIds) {
        String errorMessage;
        RFC4180Parser rfc4180Parser = new RFC4180ParserBuilder().build();
        int[] arr = Arrays.stream(locationIds.substring(1, locationIds.length() - 1).split(","))
                .map(String::trim).mapToInt(Integer::parseInt).toArray();
        List<Integer> locIdList = Arrays.stream(arr).boxed().collect(Collectors.toList());
        File file = new File("/data/mobility/" + datasetDefinitionId);
        logger.info(datasetDefinitionId);
        logger.info(datasetId);
        File[] files = file.listFiles((d, name) -> name.contains(datasetId));
        if (files != null && files.length > 0) {
            List<VisitorTracks> visitorTracks = new ArrayList<>();
            List<VisitorTracksNew> visitorTracksNew = new ArrayList<>();
            boolean newFormat = false;
            try (CSVReader csvReader = new CSVReaderBuilder(new FileReader(files[0])).withSkipLines(1).withCSVParser(rfc4180Parser).build()) {
                String[] nextRecord;
                int i =0;
                while ((nextRecord = csvReader.readNext()) != null) {
                    if (i == 0){
                        newFormat = Arrays.stream(nextRecord).count() == Arrays.stream(NEW_CSV_FORMAT).count();
                        logger.info("new format " + newFormat);
                    }
                    if (newFormat) {
                        VisitorTracksNew visitorTrackNew = createVisitorTrackNew(nextRecord);
                        if (visitorTrackNew != null) {
                            visitorTracksNew.add(visitorTrackNew);
                        }
                    } else {
                        VisitorTracks visitorTrack = createVisitorTrack(nextRecord);
                        if (visitorTrack != null) {
                            visitorTracks.add(visitorTrack);
                        }
                    }
                    i++;
                }
            } catch (IOException | CsvValidationException e) {
                errorMessage = e.getMessage();
                logger.error(errorMessage);
                return null;
            }
            Map<String, List<Integer>> vIdMap;
            if (newFormat) {
                visitorTracksNew.sort(Comparator.comparing(VisitorTracksNew::getAnonymizedUniqueId)
                        .thenComparing(VisitorTracksNew::getStartTime));
                vIdMap = visitorTracksNew.stream().collect(Collectors.groupingBy(
                        VisitorTracksNew::getAnonymizedUniqueId,
                        Collectors.mapping(VisitorTracksNew::getLocationId, Collectors.toList())));
            }else{
                visitorTracks.sort(Comparator.comparing(VisitorTracks::getVisitorId)
                        .thenComparing(VisitorTracks::getFirstTimeSeen));
                vIdMap = visitorTracks.stream().filter(p -> p.getRegionId() > 0)
                        .collect(Collectors.groupingBy(
                                VisitorTracks::getVisitorId,
                                Collectors.mapping(VisitorTracks::getRegionId, Collectors.toList())));
            }
            Map<String, List<Integer>> vIdMapFiltered = new HashMap<>();

            Iterator<Map.Entry<String, List<Integer>>> iterator = vIdMap.entrySet().iterator();
            Map.Entry<String, List<Integer>> prev = null;

            while (iterator.hasNext()) {
                Map.Entry<String, List<Integer>> next = iterator.next();

                if (prev != null) {
                    if (prev.getValue().size() > 1) {
                        List<Integer> newList = new ArrayList<>();
                        for (int i = 0; i < prev.getValue().size(); i++) {
                            if ((i + 1) < prev.getValue().size()) {
                                if (!Objects.equals(prev.getValue().get(i), prev.getValue().get(i + 1))) {
                                    newList.add(prev.getValue().get(i));
                                }
                            } else {
                                newList.add(prev.getValue().get(i));
                            }

                        }

                        vIdMapFiltered.put(prev.getKey(), newList);
                    } else {
                        vIdMapFiltered.put(prev.getKey(), prev.getValue());
                    }
                }

                prev = next;

            }

            //last entry
            if (prev != null) {
                if (prev.getValue().size() > 1) {
                    List<Integer> newList = new ArrayList<>();
                    for (int i = 0; i < prev.getValue().size(); i++) {
                        if ((i + 1) < prev.getValue().size()) {
                            if (!Objects.equals(prev.getValue().get(i), prev.getValue().get(i + 1))) {
                                newList.add(prev.getValue().get(i));
                            }
                        } else {
                            newList.add(prev.getValue().get(i));
                        }

                    }

                    vIdMapFiltered.put(prev.getKey(), newList);
                } else {
                    vIdMapFiltered.put(prev.getKey(), prev.getValue());
                }
            }

            int i = 0;
            for (Map.Entry<String, List<Integer>> entry : vIdMap.entrySet()) {
                if (Collections.indexOfSubList(entry.getValue(), locIdList) > -1) {
                    i++;
                }
            }

            return i;
        } else {
            logger.error("No matching dataset definition");
            return null;
        }
    }

    private static VisitorTracksNew createVisitorTrackNew(String[] metadata) {
        int datasetId, locationId;
        String startTime, endTime, distance, anonymizedUniqueId;
        //dataset_id,location_id,anonymized_unique_id,start_time,end_time,distance
        try {

            datasetId = Integer.parseInt(metadata[0]);
            locationId = Integer.parseInt(metadata[1]);
            anonymizedUniqueId = metadata[2];
            startTime = metadata[3];
            endTime = metadata[4];
            distance = metadata[5];

            return new VisitorTracksNew(datasetId, locationId, anonymizedUniqueId, startTime, endTime, distance);

        } catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    private static VisitorTracks createVisitorTrack(String[] metadata) {
        int siteId, regionId, populationType, globalId;
        String beginTime, endTime, visitorId;
        //site_id,region_id,visitor_id,device_id,device_type,population_type,global_id,first_time_seen,last_time_seen
        try {

            siteId = Integer.parseInt(metadata[0]);
            regionId = Integer.parseInt(metadata[1]);
            visitorId = metadata[2];
            populationType = Integer.parseInt(metadata[5]);
            globalId = Integer.parseInt(metadata[6]);

            beginTime = metadata[7];
            endTime = metadata[8];

            return new VisitorTracks(siteId, regionId, visitorId, populationType, globalId, beginTime, endTime);

        } catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }
    }

}