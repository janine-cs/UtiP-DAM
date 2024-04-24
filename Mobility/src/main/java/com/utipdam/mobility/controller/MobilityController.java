package com.utipdam.mobility.controller;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.RFC4180Parser;
import com.opencsv.RFC4180ParserBuilder;
import com.opencsv.exceptions.CsvValidationException;
import com.utipdam.mobility.business.DatasetDefinitionBusiness;
import com.utipdam.mobility.business.DatasetBusiness;
import com.utipdam.mobility.business.OrderBusiness;
import com.utipdam.mobility.config.AuthTokenFilter;
import com.utipdam.mobility.config.RestTemplateClient;
import com.utipdam.mobility.model.*;
import com.utipdam.mobility.model.entity.*;
import com.utipdam.mobility.model.repository.RoleRepository;
import com.utipdam.mobility.model.repository.UserRepository;
import org.antlr.v4.runtime.misc.IntegerList;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
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
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.util.UriComponentsBuilder;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
public class MobilityController {
    private static final Logger logger = LoggerFactory.getLogger(MobilityController.class);
    private final String START_TIME = "first_time_seen";
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
    private OrderBusiness orderBusiness;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    AuthenticationManager authenticationManager;


    @PostMapping(value = {"/mobility/upload", "/mobility/anonymize"})
    public ResponseEntity<Resource> anonymizeOnly(@RequestPart MultipartFile file,
                                                  @RequestPart String k) {

        String errorMessage;

        if (file.isEmpty()) {
            errorMessage = "File is required";
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
        if (!isNumeric(k) || (Integer.parseInt(k) < 0 || Integer.parseInt(k) > 100)) {
            errorMessage = "k must be a number between 0 - 100. You provided " + k;
            logger.error(errorMessage);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, errorMessage);
        }

        String csvDate = null;
        StringBuffer inputBuffer = new StringBuffer();

        //anonymization process
        try {
            byte[] encoded = file.getBytes();
            String content = new String(encoded, StandardCharsets.UTF_8);
            ProcessBuilder processBuilder = new ProcessBuilder("python3","/opt/utils/anonymization.py","--input",content,"--k",k);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            int exitVal = process.waitFor();
            String line;
            long i = 0;
            logger.info("exitVal " + exitVal);
            if (exitVal == 0) {
                int dateIndex = -1;
                while ((line = br.readLine()) != null) {
                    if (i == 0 || dateIndex < 0) {
                        if (!line.contains("site")){
                            continue;
                        }
                        String[] nextRecord = line.split(",");
                        dateIndex = Arrays.asList(nextRecord).indexOf(START_TIME);
                        if (dateIndex < 0) {
                            dateIndex = Arrays.asList(nextRecord).indexOf("start_time");
                        }

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
                logger.info("dataPoints:" + (i - 1));
                if (csvDate != null) {
                    HttpHeaders responseHeaders = new HttpHeaders();

                    String inputStr = inputBuffer.toString();

                    ContentDisposition contentDisposition = ContentDisposition.builder("inline")
                            .filename("dataset-" + csvDate + ".csv")
                            .build();
                    responseHeaders.setContentDisposition(contentDisposition);
                    InputStream inputStream = new ByteArrayInputStream(inputStr.getBytes(StandardCharsets.UTF_8));
                    InputStreamResource resource = new InputStreamResource(inputStream);
                    br.close();

                    return ResponseEntity.ok()
                            .headers(responseHeaders)
                            .contentLength(inputStream.available())
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .body(resource);


                } else {
                    errorMessage = "An error occurred while processing your request";
                    logger.error(errorMessage);
                    throw new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
                }

            } else {
                errorMessage = "Error in anonymization.py command";
                logger.error(errorMessage);
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
            }

        } catch (IOException | InterruptedException e) {
            errorMessage = e.getMessage();
            logger.error(errorMessage);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
        }

    }

    @GetMapping("/mobility/download")
    public ResponseEntity<byte[]> download(@RequestParam UUID[] datasetIds) {
        String errorMessage;

        if (datasetIds.length < 1) {
            errorMessage = "Please select at least one dataset - datasetIds";
            logger.error(errorMessage);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Optional<Dataset> dataset = datasetBusiness.getById(datasetIds[0]);
        if (dataset.isPresent()) {
            Dataset datasetObj = dataset.get();

            Optional<DatasetDefinition> df = datasetDefinitionBusiness.getById(datasetObj.getDatasetDefinition().getId());

            if (df.isPresent()) {
                DatasetDefinition definitionObj = df.get();
                if (definitionObj.getInternal() == null || !definitionObj.getInternal()) {
                    logger.info(definitionObj.getInternal().toString());
                    String path = "/data/mobility/" + datasetObj.getDatasetDefinition().getId();
                    File dir = new File(path);

                    StreamingResponseBody streamResponse = clientOut -> {
                        try (ZipOutputStream zos = new ZipOutputStream(clientOut)) {
                            for (UUID datasetId : datasetIds) {
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
                    ByteArrayOutputStream os =  new ByteArrayOutputStream(1024);
                    incrementDownload(definitionObj.getId());
                    try {
                        streamResponse.writeTo(os);
                        return ResponseEntity.ok()
                                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=datasets.zip")
                                .contentType(MediaType.parseMediaType("application/zip")).body(os.toByteArray());

                    } catch (IOException ex) {
                        logger.error(ex.getMessage());
                        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                } else {
                    logger.info("downloading from internal server");
                    //download from internal archive server
                    RestTemplateClient restTemplate = new RestTemplateClient();
                    if (df.get().getServer() == null){
                        logger.error("No internal server specified");
                        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                    String domain = df.get().getServer().getDomain();

                    if (domain != null) {
                        String uri = domain + "/internal/mobility/download";
                        logger.info(uri);
                        String strList = Arrays.toString(datasetIds);
                        String url = UriComponentsBuilder
                                .fromUriString(uri)
                                .queryParam("datasetDefinitionId", definitionObj.getId().toString())
                                .queryParam("datasetIds", strList.substring(1, strList.length() - 1).replaceAll(" ", "").trim())
                                .build().toUriString();
                        try {
                            ResponseEntity<Resource> exchange = restTemplate.restTemplate()
                                    .exchange(url, HttpMethod.GET, null, Resource.class);
                            byte[] inputStream = Objects.requireNonNull(exchange.getBody()).getContentAsByteArray();

                            incrementDownload(definitionObj.getId());

                            return ResponseEntity.ok()
                                    .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=datasets.zip")
                                    .contentType(MediaType.parseMediaType("application/zip")).body(inputStream);
                        } catch (KeyStoreException | NoSuchAlgorithmException | KeyManagementException | IOException ex) {
                            logger.error(ex.getMessage());
                            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                    }

                }
            }
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private void incrementDownload(UUID datasetDefinitionId){
        DownloadsByDay downloadsByDay = orderBusiness.getByDatasetDefinitionIdAndDate(datasetDefinitionId, Date.valueOf(LocalDate.now()));
        if (downloadsByDay == null){
            downloadsByDay = new DownloadsByDay(datasetDefinitionId, Date.valueOf(LocalDate.now()), 1);
            orderBusiness.saveDownloads(downloadsByDay);
        }else{
            orderBusiness.incrementCount(downloadsByDay.getId());
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
        } catch (JsonProcessingException e) {
            errorMessage = e.getMessage();
            logger.error(errorMessage);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, errorMessage);
        }

        if (file.isEmpty()) {
            errorMessage = "File is required";
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

        if ((dto.getK() < 0 || dto.getK() > 100)) {
            errorMessage = "k must be a number between 0 - 100. You provided " + dto.getK();
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
        } else {
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

        String fileName;
        String csvDate = null;
        StringBuffer inputBuffer = new StringBuffer();

        //anonymization process
        try {
            byte[] encoded = file.getBytes();
            String content = new String(encoded, StandardCharsets.UTF_8);
            ProcessBuilder processBuilder = new ProcessBuilder("python3", "/opt/utils/anonymization.py",
                    "--input", content , "--k", String.valueOf(dto.getK()));
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            int exitVal = process.waitFor();
            String line;
            long i = 0;
            if (exitVal == 0) {

                int dateIndex = -1;
                while ((line = br.readLine()) != null) {
                    if (i == 0 || dateIndex < 0) {
                        if (!line.contains("site")){
                            continue;
                        }
                        String[] nextRecord = line.split(",");
                        dateIndex = Arrays.asList(nextRecord).indexOf(START_TIME);
                        if (dateIndex < 0) {
                            dateIndex = Arrays.asList(nextRecord).indexOf("start_time");
                        }

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

                if (csvDate != null) {
                    String inputStr = inputBuffer.toString();

                    UUID uuid = UUID.randomUUID();
                    fileName = "dataset-" + uuid + "-" + csvDate + ".csv";
                    String strPath = path + "/" + fileName;
                    File fi = new File(strPath);
                    fi.setReadable(true, false);
                    fi.setWritable(true, false);

                    long dataPoints = i- 1;
                    logger.info("dataPoints:" + dataPoints);
                    Dataset d = new Dataset();

                    d.setId(uuid);
                    d.setDatasetDefinition(ds);
                    d.setStartDate(Date.valueOf(csvDate));
                    d.setEndDate(Date.valueOf(csvDate));
                    d.setResolution(dto.getResolution());
                    d.setK(dto.getK());
                    d.setDataPoints(dataPoints);

                    datasetBusiness.save(d);

                    try {
                        FileUtils.writeByteArrayToFile(fi, inputStr.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        errorMessage = e.getMessage();
                        logger.error(errorMessage);
                        throw new ResponseStatusException(
                                HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
                    }

                    HttpHeaders responseHeaders = new HttpHeaders();

                    ContentDisposition contentDisposition = ContentDisposition.builder("inline")
                            .filename(fileName)
                            .build();
                    responseHeaders.setContentDisposition(contentDisposition);
                    InputStream inputStream = new ByteArrayInputStream(inputStr.getBytes(StandardCharsets.UTF_8));
                    InputStreamResource resource = new InputStreamResource(inputStream);
                    br.close();

                    return ResponseEntity.ok()
                            .headers(responseHeaders)
                            .contentLength(inputStream.available())
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .body(resource);
                }

            } else {
                errorMessage = "Error in anonymization.py command";
                logger.error(errorMessage);
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
            }

        } catch (IOException | InterruptedException e) {
            errorMessage = e.getMessage();
            logger.error(errorMessage);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
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
        if (locationIds.length < 1) {
            errorMessage = "Please provide at least one location point - locationIds";
            logger.error(errorMessage);
            response.put("error", errorMessage);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

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
                            Map<Long, List<Integer>> vIdMapFiltered = new HashMap<>();

                            Iterator<Map.Entry<Long, List<Integer>>> iterator = vIdMap.entrySet().iterator();
                            Map.Entry<Long, List<Integer>> prev = null;

                            while (iterator.hasNext()) {
                                Map.Entry<Long, List<Integer>> next = iterator.next();

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

                            for (Map.Entry<Long, List<Integer>> entry : vIdMapFiltered.entrySet()) {
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

    @GetMapping("/mobility/audit")
    public ResponseEntity<Map<String, Object>> audit(@RequestPart MultipartFile file,
                                                     @RequestPart String k) {

        String errorMessage;
        Map<String, Object> response = new HashMap<>();
        if (file.isEmpty()) {
            errorMessage = "File is required";
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

//        if (file.getSize() > MAX_FILE_SIZE) {
//            errorMessage = "Exceeded max file size " + MAX_FILE_SIZE;
//            logger.error(errorMessage);
//            throw new ResponseStatusException(
//                    HttpStatus.BAD_REQUEST, errorMessage);
//        }

        if (!isNumeric(k) || Integer.parseInt(k) < 2) {
            errorMessage = "k must be a number between 2 - dataset size. You provided " + k;
            logger.error(errorMessage);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, errorMessage);
        }
//
//        try {
//            byte[] encoded = file.getBytes();
//            String content = new String(encoded, StandardCharsets.UTF_8);
//            ProcessBuilder processBuilder = new ProcessBuilder("python3", "/opt/utils/audit.py",
//                    "--input", content , "--k", k);
//            processBuilder.redirectErrorStream(true);
//
//            Process process = processBuilder.start();
//            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            int exitVal = process.waitFor();
//            StringBuilder out = new StringBuilder();
//            String line;
//            if (exitVal == 0) {
//                while ((line = br.readLine()) != null) {
//                    out.append(line);
//                }
//
//                ObjectMapper mapper = new ObjectMapper();
//                JsonNode node = mapper.readValue(mapper.writeValueAsString(out).replaceAll("\"", "").replaceAll("'", "\""), JsonNode.class);
//
//                response.put("data", node.get("data") );
//                response.put("minK", node.get("minK") );
//            return new ResponseEntity<>(response, HttpStatus.OK);
//            } else {
//                errorMessage = "Error in audit.py command";
//                logger.error(errorMessage);
//                throw new ResponseStatusException(
//                        HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
//            }
//
//        } catch (IOException | InterruptedException e) {
//            errorMessage = e.getMessage();
//            logger.error(errorMessage);
//            throw new ResponseStatusException(
//                    HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
//        }

        try {
            Reader reader = new InputStreamReader(file.getInputStream());
            List<VisitorTracks> visitorTracks = new ArrayList<>();
            RFC4180Parser rfc4180Parser = new RFC4180ParserBuilder().build();
            try (CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).withCSVParser(rfc4180Parser).build()) {
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
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            visitorTracks.sort(Comparator.comparing(VisitorTracks::getVisitorId)
                    .thenComparing(VisitorTracks::getFirstTimeSeen));
            Map<Long, List<Integer>> vIdMap = visitorTracks.stream().filter(p -> p.getRegionId() > 0)
                    .collect(Collectors.groupingBy(
                            VisitorTracks::getVisitorId,
                            Collectors.mapping(VisitorTracks::getRegionId, Collectors.toList())));
            Map<Long, List<Integer>> vIdMapFiltered = new HashMap<>();

            Iterator<Map.Entry<Long, List<Integer>>> iterator = vIdMap.entrySet().iterator();
            Map.Entry<Long, List<Integer>> prev = null;

            while (iterator.hasNext()) {
                Map.Entry<Long, List<Integer>> next = iterator.next();

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

            Map<IntegerList, Long> vMapResult = vIdMapFiltered.values().stream().filter(v -> !v.isEmpty()).collect(Collectors.groupingBy(IntegerList::new, Collectors.counting()));

            if (vMapResult.entrySet().isEmpty()){
                response.put("data", null);
                response.put("minK", null);
            }else{
                Long min = Collections.min(vMapResult.values());
                vMapResult = vMapResult.entrySet().stream().filter(v -> v.getValue() < Integer.parseInt(k)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                response.put("data", vMapResult);
                response.put("minK", min);
            }

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IOException e) {
            errorMessage = e.getMessage();
            logger.error(errorMessage);
            response.put("error", errorMessage);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/deviceToVisitorId")
    public String deviceToVisitorId(@RequestParam Integer sensorId, @RequestParam String mac) {
        return Hashing.sha256().hashString(sensorId + "_" + formatToValidMac(mac), StandardCharsets.UTF_8).toString();
    }

    private static String formatToValidMac(String mac) {
        return mac.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
    }
}