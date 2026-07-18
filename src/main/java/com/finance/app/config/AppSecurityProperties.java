package com.finance.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.security")
@Data
public class AppSecurityProperties {
    private boolean ldapEnabled = false;
    private String basicValidationRegex = "^(?=.*[a-zA-Z])(?=.*\\d).{6,}$";
    private String basicValidationMessage = "Password must be at least 6 characters long and contain at least one letter and one number.";
}
