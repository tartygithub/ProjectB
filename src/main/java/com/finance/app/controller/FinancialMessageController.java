package com.finance.app.controller;

import com.finance.app.dto.ProcessResult;
import com.finance.app.service.FinancialMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Tag(name = "Financial Messages", description = "Ingest financial XML or ISO 20022 messages and reply using same standard")
public class FinancialMessageController {

    private final FinancialMessageService financialMessageService;

    @PostMapping(
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.ALL_VALUE},
            produces = MediaType.APPLICATION_XML_VALUE
    )
    @Operation(
            summary = "Ingest a financial message (XML or ISO20022)",
            description = "Accepts any valid XML or ISO 20022 message, parses it, determines type, saves it, and replies using the same standard.",
            responses = {
                @ApiResponse(
                    responseCode = "200",
                    description = "XML message processed successfully",
                    content = @Content(mediaType = "application/xml", schema = @Schema(type = "string"))
                ),
                @ApiResponse(responseCode = "400", description = "Invalid payload format"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    public ResponseEntity<String> ingestMessage(
            @RequestBody String xmlPayload,
            @Parameter(hidden = true) Principal principal) {

        String username = (principal != null) ? principal.getName() : "anonymous";
        try {
            ProcessResult result = financialMessageService.processMessage(xmlPayload, username);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_XML)
                    .body(result.getResponsePayload());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_XML)
                    .body("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Error><Message>" + e.getMessage() + "</Message></Error>");
        }
    }
}
