/*
 * Developed by Kechagias Konstantinos.
 * Copyright (c) 2019. MIT License
 */

package eu.hbp.mip.dto;

public class StringDtoResponse {

    private String response;

    public StringDtoResponse() {
    }

    public StringDtoResponse(String response) {
        this.response = response;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}
