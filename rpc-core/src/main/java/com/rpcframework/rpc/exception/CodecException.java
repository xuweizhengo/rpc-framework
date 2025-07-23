package com.rpcframework.rpc.exception;

/**
 * 编解码异常
 * 
 * <p>协议编码和解码过程中发生的异常。
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class CodecException extends RpcException {
    
    private static final long serialVersionUID = 1L;
    
    public CodecException() {
        super();
    }
    
    public CodecException(String message) {
        super(message);
    }
    
    public CodecException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public CodecException(Throwable cause) {
        super(cause);
    }
    
    public CodecException(int errorCode, String message) {
        super(errorCode, message);
    }
    
    public CodecException(int errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}