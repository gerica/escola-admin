package com.escola.admin.exception;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BaseException extends Exception {
    private static final long serialVersionUID = 1L;

    public BaseException() {
    }

    public BaseException(String msg) {
        super(msg);
    }

    public BaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public BaseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public BaseException(Throwable cause) {
        super(cause);
    }

    /**
     * Encapsula exceções genéricas e inesperadas em uma BaseException.
     */
    public static BaseException handleGenericException(Throwable e) {
        log.error("Ocorreu um erro inesperado.", e);
        return new BaseException("Ocorreu um erro inesperado ao salvar o usuário.", e);
    }
}
