package com.rpcframework.rpc.model;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * RPC响应模型测试类
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class RpcResponseTest {
    
    @Test
    public void testDefaultConstructor() {
        RpcResponse response = new RpcResponse();
        
        // 验证时间戳被设置
        assertTrue("Timestamp should be set on creation", response.getTimestamp() > 0);
        
        // 验证时间戳接近当前时间
        long now = System.currentTimeMillis();
        assertTrue("Timestamp should be close to current time", 
                  Math.abs(now - response.getTimestamp()) < 1000);
    }
    
    @Test
    public void testConstructorWithRequestId() {
        String requestId = "test-request-123";
        RpcResponse response = new RpcResponse(requestId);
        
        // 验证requestId被设置
        assertEquals("Request ID should be set", requestId, response.getRequestId());
        
        // 验证时间戳被设置
        assertTrue("Timestamp should be set on creation", response.getTimestamp() > 0);
    }
    
    @Test
    public void testSuccessStaticMethod() {
        String requestId = "test-request-123";
        String result = "test result";
        
        RpcResponse response = RpcResponse.success(requestId, result);
        
        // 验证成功响应的属性
        assertEquals("Request ID should match", requestId, response.getRequestId());
        assertEquals("Status should be 200", 200, response.getStatus());
        assertEquals("Message should be Success", "Success", response.getMessage());
        assertEquals("Result should match", result, response.getResult());
        assertTrue("Should be success", response.isSuccess());
        assertFalse("Should not have exception", response.hasException());
    }
    
    @Test
    public void testFailureStaticMethod() {
        String requestId = "test-request-123";
        int status = 400;
        String message = "Bad Request";
        
        RpcResponse response = RpcResponse.failure(requestId, status, message);
        
        // 验证失败响应的属性
        assertEquals("Request ID should match", requestId, response.getRequestId());
        assertEquals("Status should match", status, response.getStatus());
        assertEquals("Message should match", message, response.getMessage());
        assertFalse("Should not be success", response.isSuccess());
        assertFalse("Should not have exception", response.hasException());
    }
    
    @Test
    public void testExceptionStaticMethod() {
        String requestId = "test-request-123";
        RuntimeException exception = new RuntimeException("Test exception");
        
        RpcResponse response = RpcResponse.exception(requestId, exception);
        
        // 验证异常响应的属性
        assertEquals("Request ID should match", requestId, response.getRequestId());
        assertEquals("Status should be 500", 500, response.getStatus());
        assertEquals("Message should be exception message", "Test exception", response.getMessage());
        assertEquals("Exception should match", exception, response.getException());
        assertFalse("Should not be success", response.isSuccess());
        assertTrue("Should have exception", response.hasException());
    }
    
    @Test
    public void testIsSuccess() {
        RpcResponse response = new RpcResponse();
        
        // 测试成功状态
        response.setStatus(200);
        assertTrue("Status 200 should be success", response.isSuccess());
        
        // 测试失败状态
        response.setStatus(400);
        assertFalse("Status 400 should not be success", response.isSuccess());
        
        response.setStatus(500);
        assertFalse("Status 500 should not be success", response.isSuccess());
    }
    
    @Test
    public void testHasException() {
        RpcResponse response = new RpcResponse();
        
        // 测试无异常情况
        assertFalse("Should not have exception initially", response.hasException());
        
        // 测试有异常情况
        response.setException(new RuntimeException("Test"));
        assertTrue("Should have exception after setting", response.hasException());
        
        // 测试设置为null的情况
        response.setException(null);
        assertFalse("Should not have exception after setting to null", response.hasException());
    }
    
    @Test
    public void testSettersAndGetters() {
        RpcResponse response = new RpcResponse();
        
        // 设置属性
        response.setRequestId("test-123");
        response.setStatus(200);
        response.setMessage("Success");
        response.setResult("test result");
        response.setProcessingTime(100L);
        
        RuntimeException exception = new RuntimeException("Test exception");
        response.setException(exception);
        
        long timestamp = System.currentTimeMillis();
        response.setTimestamp(timestamp);
        
        // 验证属性
        assertEquals("Request ID should match", "test-123", response.getRequestId());
        assertEquals("Status should match", 200, response.getStatus());
        assertEquals("Message should match", "Success", response.getMessage());
        assertEquals("Result should match", "test result", response.getResult());
        assertEquals("Exception should match", exception, response.getException());
        assertEquals("Timestamp should match", timestamp, response.getTimestamp());
        assertEquals("Processing time should match", 100L, response.getProcessingTime());
    }
    
    @Test
    public void testEquals() {
        RpcResponse response1 = new RpcResponse();
        RpcResponse response2 = new RpcResponse();
        
        // 测试空requestId的情况
        assertEquals("Responses with null requestId should be equal", response1, response2);
        
        // 测试相同requestId的情况
        response1.setRequestId("test-123");
        response2.setRequestId("test-123");
        assertEquals("Responses with same requestId should be equal", response1, response2);
        
        // 测试不同requestId的情况
        response2.setRequestId("test-456");
        assertNotEquals("Responses with different requestId should not be equal", response1, response2);
    }
    
    @Test
    public void testHashCode() {
        RpcResponse response1 = new RpcResponse();
        RpcResponse response2 = new RpcResponse();
        
        // 测试空requestId的情况
        assertEquals("Hash codes for null requestId should be equal", 
                    response1.hashCode(), response2.hashCode());
        
        // 测试相同requestId的情况
        response1.setRequestId("test-123");
        response2.setRequestId("test-123");
        assertEquals("Hash codes for same requestId should be equal", 
                    response1.hashCode(), response2.hashCode());
    }
    
    @Test
    public void testToString() {
        RpcResponse response = new RpcResponse();
        response.setRequestId("test-123");
        response.setStatus(200);
        response.setMessage("Success");
        response.setResult("test result");
        
        String toString = response.toString();
        
        // 验证toString包含关键信息
        assertTrue("toString should contain requestId", toString.contains("test-123"));
        assertTrue("toString should contain status", toString.contains("200"));
        assertTrue("toString should contain message", toString.contains("Success"));
        assertTrue("toString should contain result", toString.contains("test result"));
        assertTrue("toString should contain class name", toString.contains("RpcResponse"));
    }
    
    @Test
    public void testStaticMethodsWithNullRequestId() {
        // 测试静态方法处理null requestId的情况
        RpcResponse successResponse = RpcResponse.success(null, "result");
        assertNull("Success response should handle null requestId", successResponse.getRequestId());
        
        RpcResponse failureResponse = RpcResponse.failure(null, 400, "error");
        assertNull("Failure response should handle null requestId", failureResponse.getRequestId());
        
        RpcResponse exceptionResponse = RpcResponse.exception(null, new RuntimeException());
        assertNull("Exception response should handle null requestId", exceptionResponse.getRequestId());
    }
}