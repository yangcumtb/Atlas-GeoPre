package com.example.atlasgeopre.models;

/**
 * 请求成功的返回
 *
 * @author spot
 * @Date 2018/1/4 22:38
 */
public class SuccessResponseData extends ResponseData {

    public SuccessResponseData() {
        super(true, DEFAULT_SUCCESS_CODE, DEFAULT_SUCCESS_MESSAGE, null);
    }

    public SuccessResponseData(Object object) {
        super(true, DEFAULT_SUCCESS_CODE, DEFAULT_SUCCESS_MESSAGE, object);
    }

    public SuccessResponseData(String code, String message, Object object) {
        super(true, code, message, object);
    }
}
