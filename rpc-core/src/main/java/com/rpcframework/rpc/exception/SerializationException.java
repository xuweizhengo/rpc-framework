package com.rpcframework.rpc.exception;

/**
 * 序列化异常
 * 
 * <p>序列化和反序列化过程中发生的异常。
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class SerializationException extends RpcException {
    
    private static final long serialVersionUID = 1L;
    
    public SerializationException() {
        super();
    }
    
    public SerializationException(String message) {
        super(message);
    }
    
    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public SerializationException(Throwable cause) {
        super(cause);
    }
}