package com.example.oms.platform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "评测候选列表响应")
public record EvaluationCandidateListResponse(
        @Schema(description = "评测候选列表") List<EvaluationCandidateResponse> candidates) {
}
