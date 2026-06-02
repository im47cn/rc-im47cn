package com.rc.notification.domain.translation;

/**
 * JSONata 转换引擎异常
 * <p>
 * 封装 jsonata-java 原生异常，保留错误偏移量（Character Offset），
 * 供前端仿真面板精准定位错误位置。
 */
public class TranslationEngineException extends RuntimeException {

    /** 表达式中的错误字符偏移量，-1 表示未知位置 */
    private final int errorOffset;

    public TranslationEngineException(String message, int errorOffset) {
        super(message);
        this.errorOffset = errorOffset;
    }

    public TranslationEngineException(String message, int errorOffset, Throwable cause) {
        super(message, cause);
        this.errorOffset = errorOffset;
    }

    public int getErrorOffset() {
        return errorOffset;
    }
}
