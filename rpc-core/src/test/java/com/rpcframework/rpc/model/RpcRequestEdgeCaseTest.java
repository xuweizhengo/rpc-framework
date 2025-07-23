package com.rpcframework.rpc.model;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * RPC请求模型边缘用例测试类
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class RpcRequestEdgeCaseTest {
    
    @Test
    public void testServiceKeyWithSpecialCharacters() {
        RpcRequest request = new RpcRequest();
        request.setInterfaceName("com.example.Service$Inner");
        request.setVersion("1.0.0-SNAPSHOT");
        request.setGroup("test@group");
        
        String serviceKey = request.getServiceKey();
        assertEquals("Service key should handle special characters", 
                    "com.example.Service$Inner:1.0.0-SNAPSHOT:test@group", serviceKey);
    }
    
    @Test
    public void testServiceKeyWithEmptyStrings() {
        RpcRequest request = new RpcRequest();
        request.setInterfaceName("com.example.Service");
        request.setVersion("");
        request.setGroup("");
        
        String serviceKey = request.getServiceKey();
        assertEquals("Service key should handle empty strings", 
                    "com.example.Service", serviceKey);
    }
    
    @Test
    public void testMethodKeyWithPrimitiveTypes() {
        RpcRequest request = new RpcRequest();
        request.setMethodName("calculate");
        request.setParameterTypes(new Class[]{int.class, long.class, boolean.class});
        
        String methodKey = request.getMethodKey();
        assertEquals("Method key should handle primitive types", 
                    "calculate(int,long,boolean)", methodKey);
    }
    
    @Test
    public void testMethodKeyWithArrayTypes() {
        RpcRequest request = new RpcRequest();
        request.setMethodName("processArray");
        request.setParameterTypes(new Class[]{String[].class, int[][].class});
        
        String methodKey = request.getMethodKey();
        assertEquals("Method key should handle array types", 
                    "processArray([Ljava.lang.String;,[[I)", methodKey);
    }
    
    @Test
    public void testParametersWithNullValues() {
        RpcRequest request = new RpcRequest();
        request.setParameters(new Object[]{null, "test", null});
        request.setParameterTypes(new Class[]{String.class, String.class, Object.class});
        
        Object[] parameters = request.getParameters();
        assertNull("First parameter should be null", parameters[0]);
        assertEquals("Second parameter should be 'test'", "test", parameters[1]);
        assertNull("Third parameter should be null", parameters[2]);
    }
    
    @Test
    public void testTimeoutBoundaryValues() {
        RpcRequest request = new RpcRequest();
        
        // Test with 0 timeout
        request.setTimeout(0);
        assertEquals("Timeout should be 0", 0, request.getTimeout());
        
        // Test with maximum long value
        request.setTimeout(Long.MAX_VALUE);
        assertEquals("Timeout should be max long", Long.MAX_VALUE, request.getTimeout());
        
        // Test with negative value
        request.setTimeout(-1);
        assertEquals("Timeout should be -1", -1, request.getTimeout());
    }
    
    @Test
    public void testVeryLongServiceName() {
        RpcRequest request = new RpcRequest();
        StringBuilder longName = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longName.append("com.example.VeryLongServiceName");
        }
        
        request.setInterfaceName(longName.toString());
        String serviceKey = request.getServiceKey();
        assertEquals("Should handle very long service names", longName.toString(), serviceKey);
    }
    
    @Test
    public void testEqualsWithSameObject() {
        RpcRequest request = new RpcRequest();
        request.setRequestId("test-123");
        
        assertTrue("Object should equal itself", request.equals(request));
    }
    
    @Test
    public void testEqualsWithNull() {
        RpcRequest request = new RpcRequest();
        assertFalse("Object should not equal null", request.equals(null));
    }
    
    @Test
    public void testEqualsWithDifferentClass() {
        RpcRequest request = new RpcRequest();
        assertFalse("Object should not equal different class", request.equals("string"));
    }
    
    @Test
    public void testHashCodeConsistency() {
        RpcRequest request = new RpcRequest();
        request.setRequestId("test-123");
        
        int hashCode1 = request.hashCode();
        int hashCode2 = request.hashCode();
        
        assertEquals("Hash code should be consistent", hashCode1, hashCode2);
    }
}