package com.codeWithJeff.SampleSpringBootApplication;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeWithJeff.SampleSpringBootApplication.dto.StudentRequestDto;
import com.codeWithJeff.SampleSpringBootApplication.dto.StudentResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Value;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2")
class StudentControllerIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${local.server.port}")
    private int port;

    @Test
    void createAndListStudent() throws Exception {
        StudentRequestDto requestDto = StudentRequestDto.builder()
                .firstName("Andrew")
                .lastName("Candare")
                .email("andrew@example.com")
                .age(22)
                .course("Computer Science")
                .build();

        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest createRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/students"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestDto)))
                    .build();

            HttpResponse<String> createResponse = httpClient.send(createRequest, HttpResponse.BodyHandlers.ofString());

            StudentResponseDto createdStudent = objectMapper.readValue(createResponse.body(), StudentResponseDto.class);

            assertThat(createResponse.statusCode()).isEqualTo(201);
            assertThat(createdStudent).isNotNull();
            assertThat(createdStudent.getId()).isNotNull();
            assertThat(createdStudent.getEmail()).isEqualTo("andrew@example.com");

            HttpRequest listRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/students"))
                    .GET()
                    .build();

            HttpResponse<String> listResponse = httpClient.send(listRequest, HttpResponse.BodyHandlers.ofString());

            StudentResponseDto[] students = objectMapper.readValue(listResponse.body(), StudentResponseDto[].class);

            assertThat(listResponse.statusCode()).isEqualTo(200);
            assertThat(students).isNotNull();
            assertThat(students).hasSize(1);
            assertThat(students[0].getEmail()).isEqualTo("andrew@example.com");
        }
    }
}
