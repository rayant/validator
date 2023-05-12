package io.validator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.validator.dto.LoadRequest;
import io.validator.dto.LoadResponse;
import io.validator.repository.LoadRepository;
import io.validator.service.ValidationService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequiredArgsConstructor
@Slf4j
public class LoadController {

    private final ValidationService validationService;

    @PostMapping("/api/v1/validation")
    public LoadResponse validateLoad(@RequestBody LoadRequest loadRequest) {
        return validationService.validate(loadRequest);
    }

    @PostMapping("/api/v1/validation/process-file")
    @SneakyThrows
    public ResponseEntity<byte[]> processFile(@RequestParam("file") MultipartFile file) {
            String response = validationService.validateFile(file);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDispositionFormData("attachment", "output.txt");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(response.getBytes(StandardCharsets.UTF_8));
        }
}
