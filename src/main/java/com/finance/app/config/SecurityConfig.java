package com.finance.app.config;

import com.finance.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final AppSecurityProperties appSecurityProperties;

    @Value("${spring.ldap.urls:ldap://localhost:8389}")
    private String ldapUrl;

    @Value("${spring.ldap.base:dc=finance,dc=com}")
    private String ldapBase;

    @Value("${spring.ldap.username:cn=admin,dc=finance,dc=com}")
    private String ldapUsername;

    @Value("${spring.ldap.password:admin}")
    private String ldapPassword;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DefaultSpringSecurityContextSource contextSource() {
        DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(
                List.of(ldapUrl), ldapBase);
        contextSource.setUserDn(ldapUsername);
        contextSource.setPassword(ldapPassword);
        return contextSource;
    }

    @Bean
    public LdapUserSearch userSearch(DefaultSpringSecurityContextSource contextSource) {
        return new FilterBasedLdapUserSearch("", "(uid={0})", contextSource);
    }

    @Bean
    public LdapAuthenticator ldapAuthenticator(DefaultSpringSecurityContextSource contextSource, LdapUserSearch userSearch) {
        BindAuthenticator authenticator = new BindAuthenticator(contextSource);
        authenticator.setUserSearch(userSearch);
        return authenticator;
    }

    @Bean
    public LdapAuthoritiesPopulator ldapAuthoritiesPopulator(DefaultSpringSecurityContextSource contextSource) {
        DefaultLdapAuthoritiesPopulator populator = new DefaultLdapAuthoritiesPopulator(contextSource, "ou=groups");
        populator.setGroupRoleAttribute("cn");
        populator.setConvertToUpperCase(true);
        populator.setRolePrefix("ROLE_");
        return populator;
    }

    @Bean
    public LdapAuthenticationProvider ldapAuthenticationProvider(LdapAuthenticator ldapAuthenticator, LdapAuthoritiesPopulator ldapAuthoritiesPopulator) {
        return new LdapAuthenticationProvider(ldapAuthenticator, ldapAuthoritiesPopulator);
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            LdapAuthenticationProvider ldapAuthenticationProvider,
            DaoAuthenticationProvider daoAuthenticationProvider) {
        if (appSecurityProperties.isLdapEnabled()) {
            return new ProviderManager(List.of(ldapAuthenticationProvider));
        } else {
            return new ProviderManager(List.of(daoAuthenticationProvider));
        }
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/register",
                    "/api/auth/login",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/api-docs/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .authenticationManager(authenticationManager)
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
