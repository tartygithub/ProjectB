package com.finance.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.app.dto.LoginRequest;
import com.finance.app.dto.RegisterRequest;
import com.finance.app.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        // Any setup needed
    }

    @Test
    public void testRegisterUserEndpointSuccess() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("test_user_api")
                .password("API_Password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully: test_user_api"));
    }

    @Test
    public void testRegisterUserEndpointWeakPassword() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("weak_user")
                .password("123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testLoginEndpointSuccess() throws Exception {
        // Register user first
        userService.registerUser("auth_user", "ValidPassword55");

        LoginRequest loginRequest = LoginRequest.builder()
                .username("auth_user")
                .password("ValidPassword55")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("Login successful for user: auth_user"));
    }

    @Test
    public void testLoginEndpointUnauthorized() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("unknown_user")
                .password("random_password")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testIngestMessageWithoutAuth() throws Exception {
        mockMvc.perform(post("/api/messages")
                        .contentType(MediaType.APPLICATION_XML)
                        .content("<any>XML</any>"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "authenticated_user", roles = {"USER"})
    public void testIngestMessageWithAuthSuccess() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pain.001.001.08\">\n" +
                "  <CstmrCdtTrfInitn>\n" +
                "    <GrpHdr>\n" +
                "      <MsgId>XML-AUTHENTICATED-001</MsgId>\n" +
                "    </GrpHdr>\n" +
                "  </CstmrCdtTrfInitn>\n" +
                "</Document>";

        mockMvc.perform(post("/api/messages")
                        .contentType(MediaType.APPLICATION_XML)
                        .content(xml))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_XML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("pain.002.001.10")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("XML-AUTHENTICATED-001")));
    }
}
