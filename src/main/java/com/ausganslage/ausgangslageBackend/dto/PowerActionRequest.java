package com.ausganslage.ausgangslageBackend.dto;

import com.ausganslage.ausgangslageBackend.enums.ActionType;

public class PowerActionRequest {
    private ActionType actionType;
    private Long targetPlayerId;

    public PowerActionRequest() {
    }

    public PowerActionRequest(ActionType actionType, Long targetPlayerId) {
        this.actionType = actionType;
        this.targetPlayerId = targetPlayerId;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public Long getTargetPlayerId() {
        return targetPlayerId;
    }

    public void setTargetPlayerId(Long targetPlayerId) {
        this.targetPlayerId = targetPlayerId;
    }
}

