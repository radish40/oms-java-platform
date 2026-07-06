package com.example.oms.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

@Schema(description = "处置详情响应")
public record DisposalDetailResponse(
        @Schema(description = "工作流ID") @JsonProperty("workflow_id") String workflowId,
        @Schema(description = "工作流基本信息") DisposalWorkflowResponse.DisposalWorkflowItem workflow,
        @Schema(description = "处置记录列表") List<DisposalRecordItem> records,
        @Schema(description = "处置草稿列表") List<ActionDraftItem> action_drafts,
        @Schema(description = "回滚计划列表") List<RollbackPlanItem> rollback_plans) {

    @Schema(description = "处置记录条目")
    public record DisposalRecordItem(
            @Schema(description = "记录ID") long id,
            @Schema(description = "步骤类型") @JsonProperty("step_type") String stepType,
            @Schema(description = "操作人") String actor,
            @Schema(description = "决策内容") String decision,
            @Schema(description = "备注") String note,
            @Schema(description = "关联草稿") @JsonProperty("draft_action") Map<String, Object> draftAction,
            @Schema(description = "创建时间") @JsonProperty("created_at") String createdAt) {
    }

    @Schema(description = "处置草稿条目")
    public record ActionDraftItem(
            @Schema(description = "草稿ID") long id,
            @Schema(description = "行动类型") @JsonProperty("action_type") String actionType,
            @Schema(description = "描述") String description,
            @Schema(description = "风险等级") @JsonProperty("risk_level") String riskLevel,
            @Schema(description = "是否需要审批") @JsonProperty("requires_approval") boolean requiresApproval,
            @Schema(description = "排序序号") @JsonProperty("sort_order") int sortOrder,
            @Schema(description = "创建时间") @JsonProperty("created_at") String createdAt) {
    }

    @Schema(description = "回滚计划条目")
    public record RollbackPlanItem(
            @Schema(description = "计划ID") long id,
            @Schema(description = "计划名称") @JsonProperty("plan_name") String planName,
            @Schema(description = "描述") String description,
            @Schema(description = "回滚步骤") List<Map<String, Object>> steps,
            @Schema(description = "触发条件") List<Map<String, Object>> triggers,
            @Schema(description = "状态") String status,
            @Schema(description = "创建时间") @JsonProperty("created_at") String createdAt,
            @Schema(description = "更新时间") @JsonProperty("updated_at") String updatedAt) {
    }
}
