/*
 * Developed by Kechagias Konstantinos.
 * Copyright (c) 2019. MIT License
 */

package eu.hbp.mip.model.galaxy;

import com.google.gson.annotations.SerializedName;

public class ErrorResponse {

    @SerializedName("err_msg")
    String errMsg;

    @SerializedName("err_code")
    String errCode;

    public ErrorResponse() {
    }

    public ErrorResponse(String errMsg, String errCode) {
        this.errMsg = errMsg;
        this.errCode = errCode;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    public String getErrCode() {
        return errCode;
    }

    public void setErrCode(String errCode) {
        this.errCode = errCode;
    }
}
