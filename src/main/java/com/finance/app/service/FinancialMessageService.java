package com.finance.app.service;

import com.finance.app.dto.ProcessResult;
import com.finance.app.model.FinancialMessage;
import com.finance.app.repository.FinancialMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialMessageService {

    private final FinancialMessageRepository financialMessageRepository;

    // A fast pattern to extract tag values or namespace names without requiring full Jaxb schema generation
    private static final Pattern DOCUMENT_PATTERN = Pattern.compile("<(?:\\w+:)?Document[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAIN001_PATTERN = Pattern.compile("pain\\.001", Pattern.CASE_INSENSITIVE);
    private static final Pattern PACS008_PATTERN = Pattern.compile("pacs\\.008", Pattern.CASE_INSENSITIVE);
    private static final Pattern CAMT053_PATTERN = Pattern.compile("camt\\.053", Pattern.CASE_INSENSITIVE);

    public ProcessResult processMessage(String requestXml, String username) {
        if (requestXml == null || requestXml.trim().isEmpty()) {
            throw new IllegalArgumentException("Payload cannot be empty");
        }

        String msgType = detectMessageType(requestXml);
        String responseXml = generateReply(msgType, requestXml);

        // Save to Database
        FinancialMessage msg = FinancialMessage.builder()
                .messageType(msgType)
                .payloadRequest(requestXml)
                .payloadResponse(responseXml)
                .createdAt(LocalDateTime.now())
                .processedByUser(username)
                .build();

        financialMessageRepository.save(msg);

        return ProcessResult.builder()
                .messageType(msgType)
                .requestPayload(requestXml)
                .responsePayload(responseXml)
                .build();
    }

    private String detectMessageType(String xml) {
        // Simple heuristic validation & detection
        if (!xml.trim().startsWith("<")) {
            throw new IllegalArgumentException("Invalid XML format: Must start with '<'");
        }

        Matcher docMatcher = DOCUMENT_PATTERN.matcher(xml);
        if (docMatcher.find()) {
            String docTag = docMatcher.group();
            if (PAIN001_PATTERN.matcher(docTag).find() || PAIN001_PATTERN.matcher(xml).find()) {
                return "pain.001";
            } else if (PACS008_PATTERN.matcher(docTag).find() || PACS008_PATTERN.matcher(xml).find()) {
                return "pacs.008";
            } else if (CAMT053_PATTERN.matcher(docTag).find() || CAMT053_PATTERN.matcher(xml).find()) {
                return "camt.053";
            }
            return "ISO20022-generic";
        }

        // Generic XML with custom root tag
        Matcher rootMatcher = Pattern.compile("<([a-zA-Z0-9_:-]+)").matcher(xml);
        if (rootMatcher.find()) {
            return rootMatcher.group(1);
        }

        return "generic-xml";
    }

    private String generateReply(String msgType, String requestXml) {
        String msgId = extractValue(requestXml, "MsgId", "UNKN-ID-001");
        String creDtTm = LocalDateTime.now().toString();

        switch (msgType) {
            case "pain.001":
                // Reply with pain.002.001.10 (CustomerPaymentStatusReport)
                return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pain.002.001.10\">\n" +
                        "  <CstmrPmtStsRpt>\n" +
                        "    <GrpHdr>\n" +
                        "      <MsgId>RPT-" + System.currentTimeMillis() + "</MsgId>\n" +
                        "      <CreDtTm>" + creDtTm + "</CreDtTm>\n" +
                        "    </GrpHdr>\n" +
                        "    <OrgnlGrpInfAndSts>\n" +
                        "      <OrgnlMsgId>" + msgId + "</OrgnlMsgId>\n" +
                        "      <OrgnlMsgNmId>pain.001.001.08</OrgnlMsgNmId>\n" +
                        "      <GrpSts>ACTC</GrpSts>\n" +
                        "    </OrgnlGrpInfAndSts>\n" +
                        "  </CstmrPmtStsRpt>\n" +
                        "</Document>";

            case "pacs.008":
                // Reply with pacs.002.001.10 (FinancialInstantPaymentStatusReport)
                return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.002.001.10\">\n" +
                        "  <FIToFIPmtStsRpt>\n" +
                        "    <GrpHdr>\n" +
                        "      <MsgId>FIToFI-RPT-" + System.currentTimeMillis() + "</MsgId>\n" +
                        "      <CreDtTm>" + creDtTm + "</CreDtTm>\n" +
                        "    </GrpHdr>\n" +
                        "    <OrgnlGrpInfAndSts>\n" +
                        "      <OrgnlMsgId>" + msgId + "</OrgnlMsgId>\n" +
                        "      <OrgnlMsgNmId>pacs.008.001.08</OrgnlMsgNmId>\n" +
                        "      <GrpSts>ACSP</GrpSts>\n" +
                        "    </OrgnlGrpInfAndSts>\n" +
                        "  </FIToFIPmtStsRpt>\n" +
                        "</Document>";

            case "camt.053":
                // Reply with camt.054.001.08 (BankToCustomerDebitCreditNotification)
                return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:camt.054.001.08\">\n" +
                        "  <BkToCstmrDbtCdtNtfctn>\n" +
                        "    <GrpHdr>\n" +
                        "      <MsgId>NTF-" + System.currentTimeMillis() + "</MsgId>\n" +
                        "      <CreDtTm>" + creDtTm + "</CreDtTm>\n" +
                        "    </GrpHdr>\n" +
                        "    <Ntfctn>\n" +
                        "      <Id>NTF-ID-" + System.currentTimeMillis() + "</Id>\n" +
                        "      <CreDtTm>" + creDtTm + "</CreDtTm>\n" +
                        "      <OrgnlGrpInf>\n" +
                        "        <OrgnlMsgId>" + msgId + "</OrgnlMsgId>\n" +
                        "        <OrgnlMsgNmId>camt.053.001.08</OrgnlMsgNmId>\n" +
                        "      </OrgnlGrpInf>\n" +
                        "    </Ntfctn>\n" +
                        "  </BkToCstmrDbtCdtNtfctn>\n" +
                        "</Document>";

            default:
                // Generic XML payload format fallback reply
                return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<" + msgType + "Response>\n" +
                        "  <Status>PROCESSED</Status>\n" +
                        "  <OriginalMsgId>" + msgId + "</OriginalMsgId>\n" +
                        "  <Timestamp>" + creDtTm + "</Timestamp>\n" +
                        "</" + msgType + "Response>";
        }
    }

    private String extractValue(String xml, String tagName, String defaultValue) {
        Pattern pattern = Pattern.compile("<(?:\\w+:)?" + tagName + ">([^<]+)</(?:\\w+:)?" + tagName + ">");
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return defaultValue;
    }
}
