package com.rpcframework.rpc.exception;

/**
 * RPC框架基础异常类
 * 
 * <p>所有RPC相关异常的基类，提供统一的异常处理机制。
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class RpcException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 错误码
     */
    private int errorCode;
    
    public RpcException() {
        super();
    }
    
    public RpcException(String message) {
        super(message);
    }
    
    public RpcException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public RpcException(Throwable cause) {
        super(cause);
    }
    
    public RpcException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public RpcException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public int getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }
    
    @Override
    public String toString() {
        if (errorCode != 0) {
            return "RpcException{" +
                    "errorCode=" + errorCode +
                    ", message='" + getMessage() + '\'' +
                    '}';
        }
        return super.toString();
    }
}