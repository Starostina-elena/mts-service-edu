package ru.aigul.mts_service.jca;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class TaigaConnectionImpl implements TaigaConnection {

    private static final String TAIGA_BASE_URL = "http://taiga-back:8000/api/v1";
    private static final RestTemplate restTemplate = new RestTemplate();

    public TaigaConnectionImpl() {
    }

    @Override
    public void createIssue(String subject, String description) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("subject", subject);
            payload.put("description", description);
            payload.put("type", 1);
            payload.put("priority", 3);
            payload.put("severity", 3);
            payload.put("status", 1);
            payload.put("project", 1);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-TAIGA-TOKEN", getTaigaToken());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            restTemplate.postForObject(TAIGA_BASE_URL + "/issues", request, String.class);
            log.info("Created Taiga issue: {}", subject);
        } catch (Exception e) {
            log.error("Failed to create Taiga issue", e);
        }
    }

    @Override
    public void updateIssueStatus(Long issueId, String status) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("status", getStatusId(status));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-TAIGA-TOKEN", getTaigaToken());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            restTemplate.patchForObject(TAIGA_BASE_URL + "/issues/" + issueId, request, String.class);
            log.info("Updated Taiga issue {} status to {}", issueId, status);
        } catch (Exception e) {
            log.error("Failed to update Taiga issue", e);
        }
    }

    private String getTaigaToken() {
        try {
            Map<String, String> credentials = new HashMap<>();
            credentials.put("username", "manager");
            credentials.put("password", "managerpass");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(credentials, headers);
            String response = restTemplate.postForObject(TAIGA_BASE_URL + "/auth", request, String.class);

            if (response != null && response.contains("\"auth_token\"")) {
                return response.substring(response.indexOf("\"auth_token\":\"") + 14, response.indexOf("\"", response.indexOf("\"auth_token\":\"") + 14));
            }
        } catch (Exception e) {
            log.error("Failed to get Taiga token", e);
        }
        return "";
    }

    private int getStatusId(String status) {
        return switch (status) {
            case "APPROVED" -> 2;
            case "REJECTED" -> 3;
            default -> 1;
        };
    }

    @Override
    public void close() {
    }
}
