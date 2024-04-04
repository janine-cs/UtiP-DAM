package com.utipdam.mobility.controller;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.RFC4180Parser;
import com.opencsv.RFC4180ParserBuilder;
import com.opencsv.exceptions.CsvValidationException;
import com.utipdam.mobility.FileUploadUtil;
import com.utipdam.mobility.business.DatasetDefinitionBusiness;
import com.utipdam.mobility.business.DatasetBusiness;
import com.utipdam.mobility.config.AuthTokenFilter;
import com.utipdam.mobility.config.RestTemplateClient;
import com.utipdam.mobility.model.*;
import com.utipdam.mobility.model.entity.*;
import com.utipdam.mobility.model.repository.RoleRepository;
import com.utipdam.mobility.model.repository.UserRepository;
import org.apache.commons.validator.GenericValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public class MobilityController {
    private static final Logger logger = LoggerFactory.getLogger(MobilityController.class);
    private final String START_TIME = "first_time_seen";
    private final String RESOLUTION = "daily";
    private final String DATE_FORMAT = "yyyy-MM-dd";
    private final Integer HIGH_RISK = 10;
    private final Integer LOW_RISK = 50;

    @Value("${utipdam.app.maxFileSize}")
    private long MAX_FILE_SIZE;

    @Autowired
    private DatasetDefinitionBusiness datasetDefinitionBusiness;

    @Autowired
    private DatasetBusiness datasetBusiness;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    AuthenticationManager authenticationManager;


    @PostMapping("/mobility/upload")
    public ResponseEntity<Resource> anonymizeOnly(@RequestPart MultipartFile file,
                                                  @RequestPart String k) {

        String errorMessage;

        if (!isNumeric(k) || (Integer.parseInt(k) < 0 || Integer.parseInt(k) > 100)) {
            errorMessage = "k must be a number between 0 - 100. You provided " + k;
            logger.error(errorMessage);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, errorMessage);
        }

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
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
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
                if (dateIndex < 0) {
                    dateIndex = Arrays.asList(nextRecord).indexOf("start_time");
                }

                if (dateIndex > 0 && (line = br.readLine()) != null) {
                    csvDate = line.split(",")[dateIndex];
                    csvDate = csvDate.split(" ")[0];
                    if (!GenericValidator.isDate(csvDate, DATE_FORMAT, true)) {
                        errorMessage = "first_time_seen must be yyyy-MM-dd HH:mm:ss format";
                        logger.error(errorMessage);
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, errorMessage);
                    }
                }
            }

        } catch (IOException e) {
            errorMessage = e.getMessage();
            logger.error(errorMessage);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
        }

        if (csvDate != null) {
            UUID uuid = UUID.randomUUID();
            fileName = "dataset-" + uuid + "-" + csvDate + ".csv";

            //process anonymization


            long dataPoints;
            try {
                FileUploadUtil.saveFile(fileName, file, uploadPath);
            } catch (IOException e) {
                errorMessage = e.getMessage();
                logger.error(errorMessage);
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
            }

            File fi = new File(path + "/" + fileName);
            fi.setReadable(true, false);
            fi.setWritable(true, false);

            Path filePath = Paths.get(uploadPath + "/" + fileName);
            try (Stream<String> stream = Files.lines(filePath)) {
                dataPoints = stream.count() - 1;
                Dataset d = new Dataset();
                d.setId(uuid);
                d.setDatasetDefinition(ds);
                d.setStartDate(Date.valueOf(csvDate));
                d.setEndDate(Date.valueOf(csvDate));
                d.setResolution(RESOLUTION);
                d.setK(Integer.parseInt(k));
                d.setDataPoints(dataPoints);

                datasetBusiness.save(d);

                br = new BufferedReader(
                        new InputStreamReader(new FileSystemResource("/data/mobility/" + ds.getId() + "/" + fileName).getInputStream()));

                String lineDownload;
                StringBuffer inputBuffer = new StringBuffer();
                HttpHeaders responseHeaders = new HttpHeaders();

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
                InputStream is = new ByteArrayInputStream(inputStr.getBytes(StandardCharsets.UTF_8));
                InputStreamResource resource = new InputStreamResource(is);
                return ResponseEntity.ok()
                        .headers(responseHeaders)
                        .contentLength(is.available())
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(resource);

            } catch (IOException e) {
                errorMessage = e.getMessage();
                logger.error(errorMessage);
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
            }
        }

        errorMessage = "An error occurred while processing your request";
        logger.error(errorMessage);
        throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);

    }

    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @PostMapping("/mobility/anonymizationJob")
    public ResponseEntity<Resource> anonymizeAndSave(@RequestPart MultipartFile file,
                                                     @RequestPart String dataset) {
        ObjectMapper mapper = new ObjectMapper();
        String errorMessage;
        DatasetDefinitionDTO dto;
        try {
            JsonNode rootNode = mapper.readTree(dataset);
            dto = mapper.readValue(rootNode.toString(), DatasetDefinitionDTO.class);
        }catch(JsonProcessingException e){
            errorMessage = e.getMessage();
            logger.error(errorMessage);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, errorMessage);
        }

        if ((dto.getK() < 0 || dto.getK() > 100)) {
            errorMessage = "k must be a number between 0 - 100. You provided " + dto.getK();
            logger.error(errorMessage);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, errorMessage);
        }

        if (!Objects.requireNonNull(file.getOriginalFilename()).endsWith(".csv")) {
            errorMessage = "Please upload a csv file. You provided " + file.getOriginalFilename();
            logger.error(errorMessage);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, errorMessage);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            errorMessage = "Exceeded max file size " + MAX_FILE_SIZE;
            logger.error(errorMessage);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, errorMessage);
        }

        if (dto.getName() == null) {
            errorMessage = "Name is required";
            logger.error(errorMessage);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, errorMessage);
        }

        if (dto.getCountryCode() == null) {
            errorMessage = "Country is required";
            logger.error(errorMessage);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, errorMessage);
        }

        if (dto.getOrganization() == null) {
            errorMessage = "Dataset owner organization details are required";
            logger.error(errorMessage);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, errorMessage);
        }

        if (AuthTokenFilter.usernameLoggedIn == null) {
            errorMessage = "Login is required";
            logger.error(errorMessage);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, errorMessage);
        }else{
            Optional<User> user = userRepository.findByUsername(AuthTokenFilter.usernameLoggedIn);
            user.ifPresent(value -> dto.setUserId(value.getId()));
        }

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
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
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
                if (dateIndex < 0) {
                    dateIndex = Arrays.asList(nextRecord).indexOf("start_time");
                }

                if (dateIndex > 0 && (line = br.readLine()) != null) {
                    csvDate = line.split(",")[dateIndex];
                    csvDate = csvDate.split(" ")[0];
                    if (!GenericValidator.isDate(csvDate, DATE_FORMAT, true)) {
                        errorMessage = "first_time_seen must be yyyy-MM-dd HH:mm:ss format";
                        logger.error(errorMessage);
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, errorMessage);
                    }
                }
            }

        } catch (IOException e) {
            errorMessage = e.getMessage();
            logger.error(errorMessage);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
        }

        if (csvDate != null) {
            UUID uuid = UUID.randomUUID();
            fileName = "dataset-" + uuid + "-" + csvDate + ".csv";

            //process anonymization


            long dataPoints;
            try {
                FileUploadUtil.saveFile(fileName, file, uploadPath);
            } catch (IOException e) {
                errorMessage = e.getMessage();
                logger.error(errorMessage);
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
            }

            File fi = new File(path + "/" + fileName);
            fi.setReadable(true, false);
            fi.setWritable(true, false);

            Path filePath = Paths.get(uploadPath + "/" + fileName);
            try (Stream<String> stream = Files.lines(filePath)) {
                dataPoints = stream.count() - 1;
                Dataset d = new Dataset();
                d.setId(uuid);
                d.setDatasetDefinition(ds);
                d.setStartDate(Date.valueOf(csvDate));
                d.setEndDate(Date.valueOf(csvDate));
                d.setResolution(dto.getResolution());
                d.setK(dto.getK());
                d.setDataPoints(dataPoints);

                datasetBusiness.save(d);

                br = new BufferedReader(
                        new InputStreamReader(new FileSystemResource("/data/mobility/" + ds.getId() + "/" + fileName).getInputStream()));

                String lineDownload;
                StringBuffer inputBuffer = new StringBuffer();
                HttpHeaders responseHeaders = new HttpHeaders();

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
                InputStream is = new ByteArrayInputStream(inputStr.getBytes(StandardCharsets.UTF_8));
                InputStreamResource resource = new InputStreamResource(is);
                return ResponseEntity.ok()
                        .headers(responseHeaders)
                        .contentLength(is.available())
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(resource);

            } catch (IOException e) {
                errorMessage = e.getMessage();
                logger.error(errorMessage);
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
            }
        }

        errorMessage = "An error occurred while processing your request";
        logger.error(errorMessage);
        throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);

    }

    @GetMapping("/mobility/visitorDetection")
    public ResponseEntity<Map<String, Object>> findMeHere(@RequestParam Integer[] locationIds,
                                                          @RequestParam UUID datasetId) {
        Map<String, Object> response = new HashMap<>();
        String errorMessage;
//        if (locationIds.length < 2) {
//            errorMessage = "Please select 2 or more locations";
//            logger.error(errorMessage);
//            response.put("error", errorMessage);
//            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
//        }

        if (Arrays.stream(locationIds).anyMatch(item -> item == 0)) {
            errorMessage = "location id 0 is invalid";
            logger.error(errorMessage);
            response.put("error", errorMessage);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        List<Integer> locIdList = Arrays.stream(locationIds).toList();
        Optional<Dataset> datasetIdCheck = datasetBusiness.getById(datasetId);
        if (datasetIdCheck.isPresent()) {
            Dataset dataset = datasetIdCheck.get();
            Optional<DatasetDefinition> dd = datasetDefinitionBusiness.getById(dataset.getDatasetDefinition().getId());
            if (dd.isPresent()) {
                DatasetDefinition datasetDef = dd.get();
                List<VisitorTracks> visitorTracks = new ArrayList<>();
                RFC4180Parser rfc4180Parser = new RFC4180ParserBuilder().build();

                if (datasetDef.getInternal() == null || !datasetDef.getInternal()) {
                    File file = new File("/data/mobility/" + datasetDef.getId());
                    File[] files = file.listFiles((d, name) -> name.contains(dataset.getId().toString()));
                    if (files != null && files.length > 0) {
                        if (files[0].getName().contains(dataset.getId().toString())) {
                            try (CSVReader csvReader = new CSVReaderBuilder(new FileReader(files[0])).withSkipLines(1).withCSVParser(rfc4180Parser).build()) {
                                String[] nextRecord;

                                while ((nextRecord = csvReader.readNext()) != null) {
                                    VisitorTracks visitorTrack = createVisitorTrack(nextRecord);
                                    if (visitorTrack != null) {
                                        visitorTracks.add(visitorTrack);
                                    }
                                }
                            } catch (IOException | CsvValidationException e) {
                                errorMessage = e.getMessage();
                                logger.error(errorMessage);
                                response.put("error", errorMessage);
                                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
                            }
                            visitorTracks.sort(Comparator.comparing(VisitorTracks::getVisitorId)
                                    .thenComparing(VisitorTracks::getFirstTimeSeen));
                            Map<Long, List<Integer>> vIdMap = visitorTracks.stream().filter(p -> p.getRegionId() > 0)
                                    .collect(Collectors.groupingBy(
                                            VisitorTracks::getVisitorId,
                                            Collectors.mapping(VisitorTracks::getRegionId, Collectors.toList())));

                            int i = 0;
                            for (Map.Entry<Long, List<Integer>> entry : vIdMap.entrySet()) {
                                if (Collections.indexOfSubList(entry.getValue(), locIdList) > -1) {
                                    i++;
                                }
                            }


                            response.put("count", i);
                            response.put("riskLevel", getRiskLevel(i));
                            return new ResponseEntity<>(response, HttpStatus.OK);
                        }

                    }


                } else {
                    logger.info("retrieving result from internal server");
                    //get response from internal archive server
                    RestTemplateClient restTemplate = new RestTemplateClient();
                    String domain = dd.get().getServer().getDomain();

                    if (domain != null) {
                        String uri = domain + "/internal/mobility/visitorDetection";

                        String url = UriComponentsBuilder
                                .fromUriString(uri)
                                .queryParam("datasetDefinitionId", datasetDef.getId())
                                .queryParam("datasetId", datasetId)
                                .queryParam("locationIds", Arrays.toString(locationIds))
                                .build().toUriString();
                        logger.info(url);
                        try {
                            JsonNode node = restTemplate.restTemplate().exchange(url,
                                    HttpMethod.GET, null, JsonNode.class).getBody();
                            if (node != null) {
                                JsonFactory jsonFactory = new JsonFactory();
                                ObjectMapper objectMapper = new ObjectMapper(jsonFactory);
                                JsonNode jsonNode = objectMapper.readValue(node.toString(), JsonNode.class);
                                if (jsonNode == null) {
                                    errorMessage = "An error occurred while processing your request";
                                    logger.error(errorMessage);
                                    response.put("error", errorMessage);
                                    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                                } else {
                                    response.put("count", jsonNode.asInt());
                                    response.put("riskLevel", getRiskLevel(jsonNode.asInt()));
                                    return new ResponseEntity<>(response, HttpStatus.OK);
                                }
                            }
                        } catch (Exception ex) {
                            errorMessage = ex.getMessage();
                            logger.error(errorMessage);
                            response.put("error", errorMessage);
                            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                    }
                }
            }
        }
        errorMessage = "An error occurred while processing your request";
        logger.error(errorMessage);
        response.put("error", errorMessage);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private RiskLevelDTO getRiskLevel(int count) {
        if (count < 1) {
            return new RiskLevelDTO(RiskLevel.no_match);
        }

        if (count < HIGH_RISK) {
            return new RiskLevelDTO(RiskLevel.high);
        } else if (count < LOW_RISK) {
            return new RiskLevelDTO(RiskLevel.low);
        } else {
            return new RiskLevelDTO(RiskLevel.no_risk);
        }

    }

    private static VisitorTracks createVisitorTrack(String[] metadata) {
        int siteId, regionId, populationType, globalId;
        long visitorId;
        String beginTime, endTime;
        //site_id,region_id,visitor_id,device_id,device_type,population_type,global_id,first_time_seen,last_time_seen
        try {

            siteId = Integer.parseInt(metadata[0]);
            regionId = Integer.parseInt(metadata[1]);
            visitorId = Long.parseLong(metadata[2]);
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

    //TODO for client use
    @PostMapping("/mobility/external/anonymizationJob")
    public ResponseEntity<Resource> anonymizeExternalAPI(@RequestPart DatasetDefinitionDTO dataset,
                                                         @RequestPart MultipartFile file) {
        String errorMessage;

        if (dataset.getName() == null) {
            errorMessage = "Name is required";
            logger.error(errorMessage);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, errorMessage);
        }

        if (dataset.getK() == null || (dataset.getK() < 0 || dataset.getK() > 100)) {
            errorMessage = "k must be a number between 0 - 100. You provided " + dataset.getK();
            logger.error(errorMessage);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, errorMessage);
        }

        if (!Objects.requireNonNull(file.getOriginalFilename()).endsWith(".csv")) {
            errorMessage = "Please upload a csv file. You provided " + file.getOriginalFilename();
            logger.error(errorMessage);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, errorMessage);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            errorMessage = "Exceeded max file size " + MAX_FILE_SIZE;
            logger.error(errorMessage);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, errorMessage);
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
            errorMessage = e.getMessage();
            logger.error(errorMessage);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
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
                if (dateIndex < 0) {
                    dateIndex = Arrays.asList(nextRecord).indexOf("start_time");
                }
                if (dateIndex > 0 && (line = br.readLine()) != null) {
                    csvDate = line.split(",")[dateIndex];
                    csvDate = csvDate.split(" ")[0];
                    if (!GenericValidator.isDate(csvDate, DATE_FORMAT, true)) {
                        errorMessage = "first_time_seen must be yyyy-MM-dd HH:mm:ss format";
                        logger.error(errorMessage);
                        throw new ResponseStatusException(
                                HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
                    }
                }
            }
            br.close();
        } catch (IOException e) {
            errorMessage = e.getMessage();
            logger.error(errorMessage);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
        }


        if (csvDate != null) {
            UUID uuid = UUID.randomUUID();
            fileName = "dataset-" + uuid + "-" + csvDate + ".csv";

            //process anonymization


            long dataPoints;
            try {
                FileUploadUtil.saveFile(fileName, file, uploadPath);
            } catch (IOException e) {
                errorMessage = e.getMessage();
                logger.error(errorMessage);
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
            }

            File fi = new File(path + "/" + fileName);
            fi.setReadable(true, false);
            fi.setWritable(true, false);

            Path filePath = Paths.get(uploadPath + "/" + fileName);
            try (Stream<String> stream = Files.lines(filePath)) {
                dataPoints = stream.count() - 1;
                Dataset d = new Dataset();
                d.setId(uuid);
                d.setDatasetDefinition(ds);
                d.setStartDate(Date.valueOf(csvDate));
                d.setEndDate(Date.valueOf(csvDate));
                d.setResolution(RESOLUTION);
                d.setK(dataset.getK());
                d.setDataPoints(dataPoints);

                datasetBusiness.save(d);

                br = new BufferedReader(
                        new InputStreamReader(new FileSystemResource("/data/mobility/" + ds.getId() + "/" + fileName).getInputStream()));

                String lineDownload;
                StringBuffer inputBuffer = new StringBuffer();
                HttpHeaders responseHeaders = new HttpHeaders();

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
                InputStream is = new ByteArrayInputStream(inputStr.getBytes(StandardCharsets.UTF_8));
                InputStreamResource resource = new InputStreamResource(is);
                return ResponseEntity.ok()
                        .headers(responseHeaders)
                        .contentLength(is.available())
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(resource);

            } catch (IOException e) {
                errorMessage = e.getMessage();
                logger.error(errorMessage);
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
            }
        }

        errorMessage = "An error occurred while processing your request";
        logger.error(errorMessage);
        throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
    }

    public static String getRandomNumberString() {
        Random rnd = new Random();
        int number = rnd.nextInt(999999);
        return String.format("%06d", number);
    }
}