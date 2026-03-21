package ru.aigul.mts_service.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.aigul.mts_service.dto.UserCreateRequest;
import ru.aigul.mts_service.dto.UserResponse;
import ru.aigul.mts_service.mapper.UserMapper;
import ru.aigul.mts_service.model.User;
import ru.aigul.mts_service.service.UserService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest req) {
        User created = userService.createUser(req);
        UserResponse resp = userMapper.toDto(created);
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }
}
