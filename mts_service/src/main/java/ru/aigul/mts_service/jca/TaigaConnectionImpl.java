package ru.aigul.mts_service.jca;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class TaigaConnectionImpl implements TaigaConnection {

    private static final String TAIGA_BASE_URL = "http://taiga-back:8000/api/v1";
    private static final RestTemplate restTemplate = new RestTemplate();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // If Taiga returns authentication cookies (JWT + refresh), we store them here and send as Cookie header
    private static volatile String lastAuthCookie = null;

    public TaigaConnectionImpl() {
    }

    @Override
    public void createIssue(String subject, String description) {
        try {
            if (subject == null || subject.isBlank()) {
                log.warn("Refusing to create Taiga issue with empty subject");
                return;
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("subject", subject);
            payload.put("description", description == null ? "" : description);
            payload.put("type", 1);
            payload.put("priority", 3);
            payload.put("severity", 3);
            payload.put("status", 1);

            // project can be configured via env TAIGA_PROJECT (id or slug/name)
            String projectEnv = System.getenv("TAIGA_PROJECT");
            Integer projectId = null;
            if (projectEnv != null && !projectEnv.isBlank()) {
                try {
                    projectId = Integer.parseInt(projectEnv);
                } catch (NumberFormatException nfe) {
                    // not an integer, try to resolve by slug/name
                    projectId = findProjectId(projectEnv);
                }
            }
            if (projectId == null) {
                // fallback to default project id 1
                projectId = 1;
            }
            payload.put("project", projectId);

            HttpHeaders headers = prepareAuthHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            String jsonPayload;
            try {
                jsonPayload = objectMapper.writeValueAsString(payload);
            } catch (JsonProcessingException jpe) {
                log.error("Failed to serialize Taiga payload to JSON", jpe);
                return;
            }

            log.info("Taiga request JSON: {}", jsonPayload);
            log.debug("Taiga request headers: {}", headers);

            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);
            try {
                ResponseEntity<String> resp = restTemplate.postForEntity(TAIGA_BASE_URL + "/issues", request, String.class);
                log.info("Created Taiga issue: {} (status={})", subject, resp.getStatusCode().value());
            } catch (HttpClientErrorException he) {
                String body = he.getResponseBodyAsString();
                log.error("Taiga API returned error {} : {}", he.getStatusCode(), body);
                throw he;
            }
        } catch (Exception e) {
            log.error("Failed to create Taiga issue", e);
        }
    }

    @Override
    public void updateIssueStatus(Long issueId, String status) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("status", getStatusId(status));

            HttpHeaders headers = prepareAuthHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            restTemplate.exchange(TAIGA_BASE_URL + "/issues/" + issueId, HttpMethod.PATCH, request, String.class);
            log.info("Updated Taiga issue {} status to {}", issueId, status);
        } catch (Exception e) {
            log.error("Failed to update Taiga issue", e);
        }
    }

    private String getTaigaToken() {
        try {
            // If TAIGA_TOKEN is provided (recommended in environments), use it directly
            String envToken = System.getenv("TAIGA_TOKEN");
            if (envToken != null && !envToken.isBlank()) {
                log.info("Using TAIGA_TOKEN from environment (redacted)");
                return envToken;
            }
            String user = System.getenv().getOrDefault("TAIGA_USER", "manager");
            String pass = System.getenv().getOrDefault("TAIGA_PASSWORD", "managerpass");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Try multiple auth payload shapes to be compatible with different Taiga versions
            Map<String, Object> payload1 = new HashMap<>();
            payload1.put("username", user);
            payload1.put("password", pass);

            Map<String, Object> payload2 = new HashMap<>(payload1);
            payload2.put("type", "normal");

            Map<String, Object> payload3 = new HashMap<>(payload1);
            payload3.put("type", "plain");

            Map<String, Object> payload4 = new HashMap<>(payload1);
            payload4.put("type", "credentials");

            List<Map<String, Object>> candidates = List.of(payload1, payload2, payload3, payload4);

            for (Map<String, Object> candidate : candidates) {
                try {
                    String json = objectMapper.writeValueAsString(candidate);
                    log.debug("Trying Taiga auth payload (JSON): {}", json);
                    HttpEntity<String> request = new HttpEntity<>(json, headers);
                    ResponseEntity<Map> response = restTemplate.postForEntity(TAIGA_BASE_URL + "/auth", request, Map.class);
                    Map body = response.getBody();
                    log.debug("Auth response (JSON attempt): {} headers={}", body, response.getHeaders());
                    // If server returns body token
                    if (body != null && (body.containsKey("auth_token") || body.containsKey("token"))) {
                        Object t = body.containsKey("auth_token") ? body.get("auth_token") : body.get("token");
                        return String.valueOf(t);
                    }
                    // If cookies set with token/refresh, capture them
                    List<String> setCookies = response.getHeaders().get("Set-Cookie");
                    if (setCookies != null && !setCookies.isEmpty()) {
                        // combine cookies into single cookie header value
                        String cookieHeader = setCookies.stream()
                                .map(s -> s.split(";", 2)[0])
                                .collect(Collectors.joining("; "));
                        lastAuthCookie = cookieHeader;
                        log.info("Captured Taiga auth cookie: {}", cookieHeader);
                        // try to extract a token-like cookie value to return as token too
                        for (String ck : cookieHeader.split("; ")) {
                            if (ck.contains("token=") || ck.contains("access=")) {
                                String val = ck.substring(ck.indexOf('=') + 1);
                                return val;
                            }
                        }
                    }
                    if (body != null && body.containsKey("_error_message")) {
                        log.warn("Taiga auth attempt returned error: {}", body.get("_error_message"));
                    }
                } catch (HttpClientErrorException he) {
                    String respBody = he.getResponseBodyAsString();
                    log.warn("Taiga auth candidate (JSON) failed: status={} body={}", he.getStatusCode(), respBody);
                }

                // Try form-encoded variant
                try {
                    HttpHeaders formHeaders = new HttpHeaders();
                    formHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                    String typeVal = candidate.containsKey("type") ? String.valueOf(candidate.get("type")) : "";
                    String form;
                    if (typeVal.isBlank()) {
                        form = String.format("username=%s&password=%s", user, pass);
                    } else {
                        form = String.format("type=%s&username=%s&password=%s", typeVal, user, pass);
                    }
                    log.debug("Trying Taiga auth payload (form): {}", form);
                    HttpEntity<String> formReq = new HttpEntity<>(form, formHeaders);
                    ResponseEntity<Map> respForm = restTemplate.postForEntity(TAIGA_BASE_URL + "/auth", formReq, Map.class);
                    Map bodyForm = respForm.getBody();
                    log.debug("Auth response (form attempt): {} headers={}", bodyForm, respForm.getHeaders());
                    if (bodyForm != null && (bodyForm.containsKey("auth_token") || bodyForm.containsKey("token"))) {
                        Object t = bodyForm.containsKey("auth_token") ? bodyForm.get("auth_token") : bodyForm.get("token");
                        return String.valueOf(t);
                    }
                    List<String> setCookiesF = respForm.getHeaders().get("Set-Cookie");
                    if (setCookiesF != null && !setCookiesF.isEmpty()) {
                        String cookieHeader = setCookiesF.stream()
                                .map(s -> s.split(";", 2)[0])
                                .collect(Collectors.joining("; "));
                        lastAuthCookie = cookieHeader;
                        log.info("Captured Taiga auth cookie (form): {}", cookieHeader);
                        for (String ck : cookieHeader.split("; ")) {
                            if (ck.contains("token=") || ck.contains("access=")) {
                                String val = ck.substring(ck.indexOf('=') + 1);
                                return val;
                            }
                        }
                    }
                } catch (HttpClientErrorException he) {
                    String respBody = he.getResponseBodyAsString();
                    log.warn("Taiga auth candidate (form) failed: status={} body={}", he.getStatusCode(), respBody);
                }
            }

            log.error("Failed to obtain Taiga auth token after trying payload variants");
        } catch (JsonProcessingException jpe) {
            log.error("Failed to serialize Taiga auth payload", jpe);
        } catch (Exception e) {
            log.error("Failed to get Taiga token", e);
        }
        return "";
    }

    /**
     * Prepare headers for authenticated Taiga requests.
     * Priority: TAIGA_TOKEN env -> Authorization: Bearer <token> (if JWT-looking) -> Cookie header (if we captured it) -> X-TAIGA-TOKEN
     */
    private HttpHeaders prepareAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String envToken = System.getenv("TAIGA_TOKEN");
        if (envToken != null && !envToken.isBlank()) {
            // If token looks like JWT (has dots) use Bearer, else use X-TAIGA-TOKEN
            if (envToken.contains(".")) {
                headers.set("Authorization", "Bearer " + envToken);
            } else {
                headers.set("X-TAIGA-TOKEN", envToken);
            }
            return headers;
        }

        // If we captured a cookie from auth, use it
        if (lastAuthCookie != null && !lastAuthCookie.isBlank()) {
            headers.set("Cookie", lastAuthCookie);
            return headers;
        }

        // fallback: try to obtain a token dynamically
        String token = getTaigaToken();
        if (token != null && !token.isBlank()) {
            if (token.contains(".")) {
                headers.set("Authorization", "Bearer " + token);
            } else {
                headers.set("X-TAIGA-TOKEN", token);
            }
        }
        return headers;
    }

    @SuppressWarnings("unchecked")
    private Integer findProjectId(String projectIdentifier) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String token = getTaigaToken();
            if (token != null && !token.isBlank()) {
                headers.set("X-TAIGA-TOKEN", token);
            }
            HttpEntity<Void> request = new HttpEntity<>(headers);
            // Try to fetch projects list and match by slug or name
            ResponseEntity<List> resp = restTemplate.exchange(TAIGA_BASE_URL + "/projects", HttpMethod.GET, request, List.class);
            List projects = resp.getBody();
            if (projects != null) {
                for (Object o : projects) {
                    if (o instanceof Map) {
                        Map p = (Map) o;
                        Object id = p.get("id");
                        Object slug = p.get("slug");
                        Object name = p.get("name");
                        if (projectIdentifier.equals(String.valueOf(slug)) || projectIdentifier.equals(String.valueOf(name))) {
                            return Integer.parseInt(String.valueOf(id));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to find project id for '{}'", projectIdentifier, e);
        }
        return null;
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
