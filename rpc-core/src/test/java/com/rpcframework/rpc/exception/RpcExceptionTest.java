package com.rpcframework.rpc.exception;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * RPC异常类测试
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class RpcExceptionTest {
    
    @Test
    public void testRpcExceptionWithMessage() {
        String message = "RPC operation failed";
        RpcException exception = new RpcException(message);
        
        assertEquals("Message should match", message, exception.getMessage());
        assertNull("Cause should be null", exception.getCause());
    }
    
    @Test
    public void testRpcExceptionWithMessageAndCause() {
        String message = "RPC operation failed";
        Throwable cause = new RuntimeException("Root cause");
        
        RpcException exception = new RpcException(message, cause);
        
        assertEquals("Message should match", message, exception.getMessage());
        assertEquals("Cause should match", cause, exception.getCause());
    }
    
    @Test
    public void testRpcExceptionWithCause() {
        Throwable cause = new IllegalArgumentException("Invalid argument");
        RpcException exception = new RpcException(cause);
        
        assertEquals("Cause should match", cause, exception.getCause());
        assertNotNull("Message should not be null", exception.getMessage());
    }
    
    @Test
    public void testSerializationExceptionWithMessage() {
        String message = "Serialization failed";
        SerializationException exception = new SerializationException(message);
        
        assertEquals("Message should match", message, exception.getMessage());
        assertNull("Cause should be null", exception.getCause());
    }
    
    @Test
    public void testSerializationExceptionWithMessageAndCause() {
        String message = "Serialization failed";
        Throwable cause = new RuntimeException("JSON parse error");
        
        SerializationException exception = new SerializationException(message, cause);
        
        assertEquals("Message should match", message, exception.getMessage());
        assertEquals("Cause should match", cause, exception.getCause());
    }
    
    @Test
    public void testCodecExceptionWithMessage() {
        String message = "Codec operation failed";
        CodecException exception = new CodecException(message);
        
        assertEquals("Message should match", message, exception.getMessage());
        assertNull("Cause should be null", exception.getCause());
    }
    
    @Test
    public void testCodecExceptionWithMessageAndCause() {
        String message = "Codec operation failed";
        Throwable cause = new RuntimeException("Encoding error");
        
        CodecException exception = new CodecException(message, cause);
        
        assertEquals("Message should match", message, exception.getMessage());
        assertEquals("Cause should match", cause, exception.getCause());
    }
    
    @Test
    public void testNetworkExceptionWithMessage() {
        String message = "Network connection failed";
        NetworkException exception = new NetworkException(message);
        
        assertEquals("Message should match", message, exception.getMessage());
        assertNull("Cause should be null", exception.getCause());
    }
    
    @Test
    public void testNetworkExceptionWithMessageAndCause() {
        String message = "Network connection failed";
        Throwable cause = new RuntimeException("Connection timeout");
        
        NetworkException exception = new NetworkException(message, cause);
        
        assertEquals("Message should match", message, exception.getMessage());
        assertEquals("Cause should match", cause, exception.getCause());
    }
    
    @Test
    public void testTimeoutExceptionWithMessage() {
        String message = "Request timeout";
        TimeoutException exception = new TimeoutException(message);
        
        assertEquals("Message should match", message, exception.getMessage());
        assertNull("Cause should be null", exception.getCause());
    }
    
    @Test
    public void testTimeoutExceptionWithMessageAndCause() {
        String message = "Request timeout";
        Throwable cause = new RuntimeException("Socket timeout");
        
        TimeoutException exception = new TimeoutException(message, cause);
        
        assertEquals("Message should match", message, exception.getMessage());
        assertEquals("Cause should match", cause, exception.getCause());
    }
    
    @Test
    public void testExceptionInheritance() {
        // 测试异常继承关系
        SerializationException serializationEx = new SerializationException("test");
        assertTrue("SerializationException should be RpcException", 
            serializationEx instanceof RpcException);
        
        CodecException codecEx = new CodecException("test");
        assertTrue("CodecException should be RpcException", 
            codecEx instanceof RpcException);
        
        NetworkException networkEx = new NetworkException("test");
        assertTrue("NetworkException should be RpcException", 
            networkEx instanceof RpcException);
        
        TimeoutException timeoutEx = new TimeoutException("test");
        assertTrue("TimeoutException should be RpcException", 
            timeoutEx instanceof RpcException);
    }
    
    @Test
    public void testExceptionStackTrace() {
        Throwable rootCause = new IllegalStateException("Root cause");
        RpcException exception = new RpcException("Wrapper exception", rootCause);
        
        StackTraceElement[] stackTrace = exception.getStackTrace();
        assertNotNull("Stack trace should not be null", stackTrace);
        assertTrue("Stack trace should not be empty", stackTrace.length > 0);
    }
    
    @Test
    public void testExceptionToString() {
        String message = "Test exception message";
        RpcException exception = new RpcException(message);
        
        String toString = exception.toString();
        assertNotNull("toString should not be null", toString);
        assertTrue("toString should contain class name", 
            toString.contains("RpcException"));
        assertTrue("toString should contain message", 
            toString.contains(message));
    }
}