/*
 * Developed by Kechagias Konstantinos.
 * Copyright (c) 2019. MIT License
 */

package eu.hbp.mip.model.galaxy;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class ErrorResponse {

    @SerializedName("result")
    List<ErrorMessage> result;

    public ErrorResponse() {
    }

    public ErrorResponse(String errMsg) {
        this.result = new ArrayList<>();
        this.result.add(new ErrorMessage(errMsg));
    }

    public static class ErrorMessage {

        @SerializedName("data")
        String errMsg;

        @SerializedName("type")
        String errType;

        public ErrorMessage() {
        }

        public ErrorMessage(String errMsg) {
            this.errMsg = errMsg;
            this.errType = "text/plain+error";
        }

        public String getErrMsg() {
            return errMsg;
        }

        public void setErrMsg(String errMsg) {
            this.errMsg = errMsg;
        }
    }
}
