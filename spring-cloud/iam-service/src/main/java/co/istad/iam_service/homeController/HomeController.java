package co.istad.iam_service.homeController;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping({"/", "/home"})
    public String home(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "IAM service home";
        }

        return "IAM service home. Logged in as: " + authentication.getName();
    }
}
