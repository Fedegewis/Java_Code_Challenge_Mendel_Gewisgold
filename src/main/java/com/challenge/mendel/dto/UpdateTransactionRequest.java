package com.challenge.mendel.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateTransactionRequest {

    private Double amount;
    private String type;

    @JsonProperty("parent_id")
    private Long parentId;

    public UpdateTransactionRequest() {
    }

    public UpdateTransactionRequest(Double amount, String type, Long parentId) {
        this.amount = amount;
        this.type = type;
        this.parentId = parentId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
}