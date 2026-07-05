package com.devcine.backend.dto.response;

import com.devcine.backend.entity.ApprovalRequest;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ApprovalResponse {

    private Integer id;
    private String type;
    private Integer refId;
    private String refCode;
    private String summary;
    private String reason;
    private String status;
    private String requestedByName;
    private String approvedByName;
    private String decisionNote;
    private LocalDateTime createdAt;
    private LocalDateTime decidedAt;

    public static ApprovalResponse from(ApprovalRequest r) {
        return ApprovalResponse.builder()
                .id(r.getId())
                .type(r.getType())
                .refId(r.getRefId())
                .refCode(r.getRefCode())
                .summary(r.getSummary())
                .reason(r.getReason())
                .status(r.getStatus())
                .requestedByName(r.getRequestedByName())
                .approvedByName(r.getApprovedByName())
                .decisionNote(r.getDecisionNote())
                .createdAt(r.getCreatedAt())
                .decidedAt(r.getDecidedAt())
                .build();
    }
}
