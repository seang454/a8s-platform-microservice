package co.istad.iam_service.feature.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    @PreAuthorize("hasAnyAuthority('user:read:own')")
    @GetMapping("/message")
    public ResponseEntity<?> getMessages () {
        return new ResponseEntity<>(Map.of(
                "message", "B sl o"
        ), HttpStatus.OK);
    }
}
