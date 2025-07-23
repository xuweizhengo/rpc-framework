package com.rpcframework.rpc.model;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * RPC请求模型测试类
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class RpcRequestTest {
    
    @Test
    public void testDefaultConstructor() {
        RpcRequest request = new RpcRequest();
        
        // 验证时间戳被设置
        assertTrue("Timestamp should be set on creation", request.getTimestamp() > 0);
        
        // 验证时间戳接近当前时间
        long now = System.currentTimeMillis();
        assertTrue("Timestamp should be close to current time", 
                  Math.abs(now - request.getTimestamp()) < 1000);
    }
    
    @Test
    public void testSettersAndGetters() {
        RpcRequest request = new RpcRequest();
        
        // 设置属性
        request.setRequestId("test-request-123");
        request.setInterfaceName("com.example.UserService");
        request.setMethodName("getUserById");
        request.setParameterTypes(new Class[]{Long.class});
        request.setParameters(new Object[]{123L});
        request.setVersion("1.0.0");
        request.setGroup("default");
        request.setTimeout(5000L);
        
        // 验证属性
        assertEquals("Request ID should match", "test-request-123", request.getRequestId());
        assertEquals("Interface name should match", "com.example.UserService", request.getInterfaceName());
        assertEquals("Method name should match", "getUserById", request.getMethodName());
        assertArrayEquals("Parameter types should match", new Class[]{Long.class}, request.getParameterTypes());
        assertArrayEquals("Parameters should match", new Object[]{123L}, request.getParameters());
        assertEquals("Version should match", "1.0.0", request.getVersion());
        assertEquals("Group should match", "default", request.getGroup());
        assertEquals("Timeout should match", 5000L, request.getTimeout());
    }
    
    @Test
    public void testGetServiceKey() {
        RpcRequest request = new RpcRequest();
        request.setInterfaceName("com.example.UserService");
        
        // 测试只有接口名的情况
        assertEquals("Service key should be interface name only", 
                    "com.example.UserService", request.getServiceKey());
        
        // 测试有版本号的情况
        request.setVersion("1.0.0");
        assertEquals("Service key should include version", 
                    "com.example.UserService:1.0.0", request.getServiceKey());
        
        // 测试有版本号和分组的情况
        request.setGroup("default");
        assertEquals("Service key should include version and group", 
                    "com.example.UserService:1.0.0:default", request.getServiceKey());
        
        // 测试空版本号但有分组的情况
        request.setVersion("");
        assertEquals("Service key should handle empty version", 
                    "com.example.UserService:default", request.getServiceKey());
    }
    
    @Test
    public void testGetMethodKey() {
        RpcRequest request = new RpcRequest();
        request.setMethodName("getUserById");
        
        // 测试无参数方法
        assertEquals("Method key for no params should be method()", 
                    "getUserById()", request.getMethodKey());
        
        // 测试单参数方法
        request.setParameterTypes(new Class[]{Long.class});
        assertEquals("Method key for single param should include param type", 
                    "getUserById(java.lang.Long)", request.getMethodKey());
        
        // 测试多参数方法
        request.setParameterTypes(new Class[]{Long.class, String.class, Integer.class});
        assertEquals("Method key for multiple params should include all param types", 
                    "getUserById(java.lang.Long,java.lang.String,java.lang.Integer)", request.getMethodKey());
    }
    
    @Test
    public void testEquals() {
        RpcRequest request1 = new RpcRequest();
        RpcRequest request2 = new RpcRequest();
        
        // 测试空requestId的情况
        assertEquals("Requests with null requestId should be equal", request1, request2);
        
        // 测试相同requestId的情况
        request1.setRequestId("test-123");
        request2.setRequestId("test-123");
        assertEquals("Requests with same requestId should be equal", request1, request2);
        
        // 测试不同requestId的情况
        request2.setRequestId("test-456");
        assertNotEquals("Requests with different requestId should not be equal", request1, request2);
    }
    
    @Test
    public void testHashCode() {
        RpcRequest request1 = new RpcRequest();
        RpcRequest request2 = new RpcRequest();
        
        // 测试空requestId的情况
        assertEquals("Hash codes for null requestId should be equal", 
                    request1.hashCode(), request2.hashCode());
        
        // 测试相同requestId的情况
        request1.setRequestId("test-123");
        request2.setRequestId("test-123");
        assertEquals("Hash codes for same requestId should be equal", 
                    request1.hashCode(), request2.hashCode());
    }
    
    @Test
    public void testToString() {
        RpcRequest request = new RpcRequest();
        request.setRequestId("test-123");
        request.setInterfaceName("com.example.UserService");
        request.setMethodName("getUserById");
        
        String toString = request.toString();
        
        // 验证toString包含关键信息
        assertTrue("toString should contain requestId", toString.contains("test-123"));
        assertTrue("toString should contain interfaceName", toString.contains("com.example.UserService"));
        assertTrue("toString should contain methodName", toString.contains("getUserById"));
        assertTrue("toString should contain class name", toString.contains("RpcRequest"));
    }
    
    @Test
    public void testNullSafeServiceKey() {
        RpcRequest request = new RpcRequest();
        request.setInterfaceName("com.example.UserService");
        request.setVersion(null);
        request.setGroup(null);
        
        // 验证null值不会导致异常
        String serviceKey = request.getServiceKey();
        assertEquals("Service key should handle null version and group", 
                    "com.example.UserService", serviceKey);
    }
    
    @Test
    public void testNullSafeMethodKey() {
        RpcRequest request = new RpcRequest();
        request.setMethodName("testMethod");
        request.setParameterTypes(null);
        
        // 验证null参数类型不会导致异常
        String methodKey = request.getMethodKey();
        assertEquals("Method key should handle null parameter types", 
                    "testMethod()", methodKey);
    }
}