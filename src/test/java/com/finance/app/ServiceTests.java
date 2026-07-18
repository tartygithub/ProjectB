package com.finance.app;

import com.finance.app.config.AppSecurityProperties;
import com.finance.app.dto.ProcessResult;
import com.finance.app.model.User;
import com.finance.app.repository.UserRepository;
import com.finance.app.service.FinancialMessageService;
import com.finance.app.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ServiceTests {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FinancialMessageService financialMessageService;

    @BeforeEach
    public void setup() {
        userRepository.deleteAll();
    }

    @Test
    public void testUserRegistrationSuccess() {
        User registered = userService.registerUser("john_doe", "SecurePass123");
        assertNotNull(registered.getId());
        assertEquals("john_doe", registered.getUsername());
        assertTrue(passwordEncoder.matches("SecurePass123", registered.getPassword()));
    }

    @Test
    public void testUserRegistrationDuplicateUsername() {
        userService.registerUser("john_doe", "SecurePass123");
        assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser("john_doe", "AnotherPass789");
        });
    }

    @Test
    public void testPasswordValidationInvalid() {
        // Too short
        assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser("user1", "123");
        });

        // No letters
        assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser("user2", "123456");
        });

        // No numbers
        assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser("user3", "abcdefg");
        });
    }

    @Test
    public void testProcessPain001Message() {
        String painXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pain.001.001.08\">\n" +
                "  <CstmrCdtTrfInitn>\n" +
                "    <GrpHdr>\n" +
                "      <MsgId>MSG-PAIN-001</MsgId>\n" +
                "    </GrpHdr>\n" +
                "  </CstmrCdtTrfInitn>\n" +
                "</Document>";

        ProcessResult result = financialMessageService.processMessage(painXml, "john_doe");
        assertEquals("pain.001", result.getMessageType());
        assertTrue(result.getResponsePayload().contains("pain.002.001.10"));
        assertTrue(result.getResponsePayload().contains("ACTC"));
        assertTrue(result.getResponsePayload().contains("MSG-PAIN-001"));
    }

    @Test
    public void testProcessPacs008Message() {
        String pacsXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08\">\n" +
                "  <FIToFICstmrCdtTrf>\n" +
                "    <GrpHdr>\n" +
                "      <MsgId>MSG-PACS-008</MsgId>\n" +
                "    </GrpHdr>\n" +
                "  </FIToFICstmrCdtTrf>\n" +
                "</Document>";

        ProcessResult result = financialMessageService.processMessage(pacsXml, "admin");
        assertEquals("pacs.008", result.getMessageType());
        assertTrue(result.getResponsePayload().contains("pacs.002.001.10"));
        assertTrue(result.getResponsePayload().contains("ACSP"));
        assertTrue(result.getResponsePayload().contains("MSG-PACS-008"));
    }

    @Test
    public void testProcessCamt053Message() {
        String camtXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:camt.053.001.08\">\n" +
                "  <BkToCstmrStmt>\n" +
                "    <GrpHdr>\n" +
                "      <MsgId>MSG-CAMT-053</MsgId>\n" +
                "    </GrpHdr>\n" +
                "  </BkToCstmrStmt>\n" +
                "</Document>";

        ProcessResult result = financialMessageService.processMessage(camtXml, "john_doe");
        assertEquals("camt.053", result.getMessageType());
        assertTrue(result.getResponsePayload().contains("camt.054.001.08"));
        assertTrue(result.getResponsePayload().contains("MSG-CAMT-053"));
    }

    @Test
    public void testProcessGenericXmlMessage() {
        String genericXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<MyCustomFinancialMessage>\n" +
                "  <MsgId>GEN-999</MsgId>\n" +
                "  <Payload>Data</Payload>\n" +
                "</MyCustomFinancialMessage>";

        ProcessResult result = financialMessageService.processMessage(genericXml, "john_doe");
        assertEquals("MyCustomFinancialMessage", result.getMessageType());
        assertTrue(result.getResponsePayload().contains("MyCustomFinancialMessageResponse"));
        assertTrue(result.getResponsePayload().contains("PROCESSED"));
        assertTrue(result.getResponsePayload().contains("GEN-999"));
    }

    @Test
    public void testInvalidXmlPayload() {
        assertThrows(IllegalArgumentException.class, () -> {
            financialMessageService.processMessage("Not XML Content", "user");
        });
    }
}
