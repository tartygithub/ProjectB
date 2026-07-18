package com.finance.app.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "financial_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_type", nullable = false)
    private String messageType; // e.g. "pain.001", "pacs.008", "camt.053", "generic-xml"

    @Lob
    @Column(name = "payload_request", nullable = false, length = 100000)
    private String payloadRequest; // The full received XML string

    @Lob
    @Column(name = "payload_response", nullable = false, length = 100000)
    private String payloadResponse; // The generated reply XML string

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_by_user")
    private String processedByUser;
}
