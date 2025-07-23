package com.rpcframework.rpc.exception;

/**
 * 超时异常
 * 
 * <p>RPC调用、连接建立等操作超时时抛出的异常。
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class TimeoutException extends RpcException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 超时时间（毫秒）
     */
    private long timeoutMillis;
    
    public TimeoutException() {
        super();
    }
    
    public TimeoutException(String message) {
        super(message);
    }
    
    public TimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public TimeoutException(Throwable cause) {
        super(cause);
    }
    
    public TimeoutException(long timeoutMillis, String message) {
        super(message);
        this.timeoutMillis = timeoutMillis;
    }
    
    public TimeoutException(long timeoutMillis, String message, Throwable cause) {
        super(message, cause);
        this.timeoutMillis = timeoutMillis;
    }
    
    public long getTimeoutMillis() {
        return timeoutMillis;
    }
    
    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }
    
    @Override
    public String toString() {
        if (timeoutMillis > 0) {
            return "TimeoutException{" +
                    "timeoutMillis=" + timeoutMillis +
                    ", message='" + getMessage() + '\'' +
                    '}';
        }
        return super.toString();
    }
}