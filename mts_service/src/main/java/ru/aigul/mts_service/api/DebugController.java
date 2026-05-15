package ru.aigul.mts_service.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.aigul.mts_service.model.User;
import ru.aigul.mts_service.service.UserService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/internal/debug", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class DebugController {

    private final UserService userService;

    @GetMapping("/auth")
    public Map<String, Object> authInfo(Authentication auth) {
        Map<String, Object> out = new HashMap<>();
        if (auth == null) {
            out.put("authenticated", false);
            return out;
        }
        out.put("authenticated", auth.isAuthenticated());
        out.put("principal", auth.getPrincipal());
        out.put("name", auth.getName());
        List<String> auths = auth.getAuthorities() == null ? List.of() : auth.getAuthorities().stream().map(a -> a.getAuthority()).collect(Collectors.toList());
        out.put("authorities", auths);

        try {
            User u = userService.findOrCreateFromAuthentication(auth);
            Map<String, Object> uMap = new HashMap<>();
            uMap.put("id", u.getId());
            uMap.put("email", u.getEmail());
            uMap.put("name", u.getName());
            uMap.put("role", u.getRole());
            out.put("user", uMap);
        } catch (Exception e) {
            out.put("userError", e.getMessage());
        }

        return out;
    }
}

