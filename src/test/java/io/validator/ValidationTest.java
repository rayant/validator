package io.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.validator.dto.LoadRequest;
import io.validator.dto.LoadResponse;
import io.validator.repository.LoadRepository;
import io.validator.service.ValidationService;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static net.bytebuddy.matcher.ElementMatchers.is;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = ValidatorApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(
        locations = "classpath:application.test.properties")
public class ValidationTest {

    @Autowired
    private LoadRepository loadRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ValidationService validationService;

    @Autowired
    private MockMvc mvc;

    @BeforeAll
    public void cleanDb() {
        loadRepository.deleteAll();
    }

    @Test
    @SneakyThrows
    public void testFileValidation() {
        Map<String, Map<String, LoadResponse>> expected = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(this.getClass().getClassLoader().getResource("expected.txt").getFile()));) {
            String line;
            while ((line = br.readLine()) != null) {
                LoadResponse response = objectMapper.readValue(line, LoadResponse.class);
                expected.putIfAbsent(response.getCustomerId(), new HashMap<>());
                expected.get(response.getCustomerId()).put(response.getId(), response);
            }
        }


        try (BufferedReader br = new BufferedReader(new FileReader(this.getClass().getClassLoader().getResource("input.txt").getFile()))) {
            String line;

            while ((line = br.readLine()) != null) {
                LoadRequest loadRequest = objectMapper.readValue(line, LoadRequest.class);
                LoadResponse response = validationService.validate(loadRequest);
                LoadResponse expectedResponse = expected.get(response.getCustomerId()).get(response.getId());
                Assert.assertEquals(String.format("accepted does not match for loadResponse %s", objectMapper.writeValueAsString(response)), expectedResponse.isAccepted(), response.isAccepted());
            }
        }
    }

    @Test
    @SneakyThrows
    public void bruteforceTest() {
        LocalDateTime time = LocalDateTime.now();
        Queue<Future<LoadResponse>> resultsFuture = new LinkedList<>();
        List<LoadResponse> results = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (int i = 0; i < 1000; i++) {
            if (i % 10 == 0) {
                time = time.plusDays(1);
            }
            LoadRequest loadRequest = LoadRequest.builder()
                    .id(String.valueOf(i))
                    .customerId("1")
                    .loadAmount("$10")
                    .time(time)
                    .build();
            Future<LoadResponse> future = executor.submit(() -> objectMapper.readValue(mvc.perform(post("/api/v1/validation")
                            .content(objectMapper.writeValueAsString(loadRequest))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString(),LoadResponse.class));

            resultsFuture.add(future);
        }

        while (!resultsFuture.isEmpty()) {
            Future<LoadResponse> futureResponse = resultsFuture.peek();
            if (futureResponse.isDone()) {
                results.add(futureResponse.get());
                resultsFuture.remove(futureResponse);
            }
        }

        Assert.assertEquals(1000, results.size());
        long accepted = results.stream().filter(LoadResponse::isAccepted).count();
        Assert.assertEquals(300, accepted);
    }
}
