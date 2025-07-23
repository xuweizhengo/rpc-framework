package com.rpcframework.rpc.exception;

/**
 * 网络通信异常
 * 
 * <p>网络连接、传输等过程中发生的异常。
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class NetworkException extends RpcException {
    
    private static final long serialVersionUID = 1L;
    
    public NetworkException() {
        super();
    }
    
    public NetworkException(String message) {
        super(message);
    }
    
    public NetworkException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public NetworkException(Throwable cause) {
        super(cause);
    }
    
    public NetworkException(int errorCode, String message) {
        super(errorCode, message);
    }
    
    public NetworkException(int errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}