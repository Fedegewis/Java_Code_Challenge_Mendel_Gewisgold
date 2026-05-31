package com.challenge.mendel.dto;

public class SumTransactionResponse {

    private Double sum;

    public SumTransactionResponse() {
    }

    public SumTransactionResponse(Double sum) {
        this.sum = sum;
    }

    public Double getSum() {
        return sum;
    }

    public void setSum(Double sum) {
        this.sum = sum;
    }
}