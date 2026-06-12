package ru.aigul.mts_service.api;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/ui/forms")
@RequiredArgsConstructor
public class CamundaFormController {

    private final ResourceLoader resourceLoader;

    @GetMapping(value = "/{formId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getForm(@PathVariable String formId) throws IOException {
        Resource resource = resourceLoader.getResource("classpath:forms/" + formId + ".form");
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }
}
