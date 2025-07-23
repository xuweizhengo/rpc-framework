package com.rpcframework.rpc.model;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * RPC响应模型边缘用例测试类
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class RpcResponseEdgeCaseTest {
    
    @Test
    public void testSuccessWithNullResult() {
        String requestId = "test-123";
        RpcResponse response = RpcResponse.success(requestId, null);
        
        assertEquals("Request ID should match", requestId, response.getRequestId());
        assertTrue("Should be success", response.isSuccess());
        assertNull("Result should be null", response.getResult());
    }
    
    @Test
    public void testFailureWithCustomStatusCodes() {
        // Test various custom status codes
        RpcResponse response1 = RpcResponse.failure("test-1", 422, "Unprocessable Entity");
        assertEquals("Status should be 422", 422, response1.getStatus());
        assertFalse("Should not be success", response1.isSuccess());
        
        RpcResponse response2 = RpcResponse.failure("test-2", 429, "Too Many Requests");
        assertEquals("Status should be 429", 429, response2.getStatus());
        assertFalse("Should not be success", response2.isSuccess());
    }
    
    @Test
    public void testExceptionWithNullMessage() {
        RuntimeException exception = new RuntimeException(); // No message
        RpcResponse response = RpcResponse.exception("test-123", exception);
        
        assertEquals("Status should be 500", 500, response.getStatus());
        assertNotNull("Message should have fallback for exception without message", response.getMessage());
        assertTrue("Message should contain exception class name", 
            response.getMessage().contains("RuntimeException"));
        assertTrue("Should have exception", response.hasException());
    }
    
    @Test
    public void testExceptionWithNestedCause() {
        Exception cause = new IllegalArgumentException("Root cause");
        RuntimeException exception = new RuntimeException("Wrapper exception", cause);
        
        RpcResponse response = RpcResponse.exception("test-123", exception);
        
        assertEquals("Exception should be set", exception, response.getException());
        assertEquals("Should have root cause", cause, response.getException().getCause());
    }
    
    @Test
    public void testProcessingTimeBoundaryValues() {
        RpcResponse response = new RpcResponse();
        
        // Test with 0 processing time
        response.setProcessingTime(0);
        assertEquals("Processing time should be 0", 0, response.getProcessingTime());
        
        // Test with maximum long value
        response.setProcessingTime(Long.MAX_VALUE);
        assertEquals("Processing time should be max long", Long.MAX_VALUE, response.getProcessingTime());
        
        // Test with negative value (might represent error state)
        response.setProcessingTime(-1);
        assertEquals("Processing time should be -1", -1, response.getProcessingTime());
    }
    
    @Test
    public void testTimestampModification() {
        RpcResponse response = new RpcResponse();
        long originalTimestamp = response.getTimestamp();
        
        // Modify timestamp
        long newTimestamp = System.currentTimeMillis() + 1000;
        response.setTimestamp(newTimestamp);
        
        assertEquals("Timestamp should be updated", newTimestamp, response.getTimestamp());
        assertNotEquals("Timestamp should be different from original", originalTimestamp, response.getTimestamp());
    }
    
    @Test
    public void testResultWithComplexObject() {
        // Test with complex result object
        Object complexResult = new Object() {
            @Override
            public String toString() {
                return "ComplexResult{data=sensitive}";
            }
        };
        
        RpcResponse response = RpcResponse.success("test-123", complexResult);
        assertEquals("Complex result should be preserved", complexResult, response.getResult());
    }
    
    @Test
    public void testStaticMethodsChaining() {
        // Test that static methods return properly configured objects
        RpcResponse successResponse = RpcResponse.success("test-1", "result");
        successResponse.setProcessingTime(100);
                
        assertEquals("Chaining should work", 100, successResponse.getProcessingTime());
        assertTrue("Should still be success", successResponse.isSuccess());
    }
    
    @Test
    public void testIsSuccessWithBoundaryStatusCodes() {
        RpcResponse response = new RpcResponse();
        
        // Test status codes around 200
        response.setStatus(199);
        assertFalse("Status 199 should not be success", response.isSuccess());
        
        response.setStatus(200);
        assertTrue("Status 200 should be success", response.isSuccess());
        
        response.setStatus(201);
        assertFalse("Status 201 should not be success", response.isSuccess());
    }
    
    @Test
    public void testToStringWithNullFields() {
        RpcResponse response = new RpcResponse();
        // All fields are null/default except timestamp
        
        String toString = response.toString();
        assertTrue("toString should contain class name", toString.contains("RpcResponse"));
        assertTrue("toString should handle null fields gracefully", toString.contains("null"));
    }
    
    @Test
    public void testEqualsWithComplexScenarios() {
        // Test with different timestamp but same requestId
        RpcResponse response1 = new RpcResponse("test-123");
        try {
            Thread.sleep(1); // Ensure different timestamp
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        RpcResponse response2 = new RpcResponse("test-123");
        
        assertEquals("Responses with same requestId should be equal regardless of timestamp", 
                    response1, response2);
    }
}