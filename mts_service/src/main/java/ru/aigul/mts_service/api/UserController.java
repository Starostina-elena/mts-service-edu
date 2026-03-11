package ru.aigul.mts_service.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.aigul.mts_service.api.dto.UserCreateRequest;
import ru.aigul.mts_service.api.dto.UserResponse;
import ru.aigul.mts_service.model.User;
import ru.aigul.mts_service.service.UserService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest req) {
        User created = userService.createUser(req.getEmail(), req.getName(), req.getPassword());
        UserResponse resp = new UserResponse(
                created.getId(),
                created.getEmail(),
                created.getName(),
                created.getRole().name(),
                created.getCreatedAt()
        );
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }
}
