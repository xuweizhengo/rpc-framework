package com.rpcframework.rpc.serialize;

import com.rpcframework.rpc.exception.SerializationException;
import com.rpcframework.rpc.model.RpcRequest;
import com.rpcframework.rpc.model.RpcResponse;
import com.rpcframework.rpc.protocol.RpcProtocol;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JSON序列化器测试类
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class JsonSerializerTest {
    
    private final JsonSerializer serializer = new JsonSerializer();
    
    @Test
    public void testGetCodecType() {
        assertEquals("Codec type should be JSON", 
            RpcProtocol.CodecType.JSON, serializer.getCodecType());
    }
    
    @Test
    public void testGetName() {
        assertEquals("Serializer name should be json", "json", serializer.getName());
    }
    
    @Test
    public void testSerializeRpcRequest() throws Exception {
        // 创建测试请求
        RpcRequest request = new RpcRequest();
        request.setRequestId("test-123");
        request.setInterfaceName("com.example.UserService");
        request.setMethodName("getUserById");
        request.setParameterTypes(new Class[]{Long.class, String.class});
        request.setParameters(new Object[]{123L, "test"});
        request.setVersion("1.0.0");
        request.setGroup("default");
        request.setTimeout(5000L);
        
        // 序列化
        byte[] bytes = serializer.serialize(request);
        assertNotNull("Serialized bytes should not be null", bytes);
        assertTrue("Serialized bytes should not be empty", bytes.length > 0);
        
        // 反序列化
        RpcRequest deserialized = serializer.deserialize(bytes, RpcRequest.class);
        assertNotNull("Deserialized object should not be null", deserialized);
        
        // 验证字段
        assertEquals("Request ID should match", request.getRequestId(), deserialized.getRequestId());
        assertEquals("Interface name should match", request.getInterfaceName(), deserialized.getInterfaceName());
        assertEquals("Method name should match", request.getMethodName(), deserialized.getMethodName());
        assertArrayEquals("Parameter types should match", request.getParameterTypes(), deserialized.getParameterTypes());
        assertEquals("Version should match", request.getVersion(), deserialized.getVersion());
        assertEquals("Group should match", request.getGroup(), deserialized.getGroup());
        assertEquals("Timeout should match", request.getTimeout(), deserialized.getTimeout());
        
        // 验证参数（JSON反序列化后数字类型可能会变化）
        assertNotNull("Parameters should not be null", deserialized.getParameters());
        assertEquals("Parameter count should match", request.getParameters().length, deserialized.getParameters().length);
    }
    
    @Test
    public void testSerializeRpcResponse() throws Exception {
        // 创建成功响应
        RpcResponse response = RpcResponse.success("test-123", "Hello World");
        response.setProcessingTime(100L);
        
        // 序列化
        byte[] bytes = serializer.serialize(response);
        assertNotNull("Serialized bytes should not be null", bytes);
        assertTrue("Serialized bytes should not be empty", bytes.length > 0);
        
        // 反序列化
        RpcResponse deserialized = serializer.deserialize(bytes, RpcResponse.class);
        assertNotNull("Deserialized object should not be null", deserialized);
        
        // 验证字段
        assertEquals("Request ID should match", response.getRequestId(), deserialized.getRequestId());
        assertEquals("Status should match", response.getStatus(), deserialized.getStatus());
        assertEquals("Message should match", response.getMessage(), deserialized.getMessage());
        assertEquals("Result should match", response.getResult(), deserialized.getResult());
        assertEquals("Processing time should match", response.getProcessingTime(), deserialized.getProcessingTime());
        assertTrue("Should be success", deserialized.isSuccess());
        assertFalse("Should not have exception", deserialized.hasException());
    }
    
    @Test
    public void testSerializeExceptionResponse() throws Exception {
        // 创建异常响应
        RuntimeException ex = new RuntimeException("Test exception");
        RpcResponse response = RpcResponse.exception("test-456", ex);
        
        // 序列化
        byte[] bytes = serializer.serialize(response);
        assertNotNull("Serialized bytes should not be null", bytes);
        
        // 反序列化
        RpcResponse deserialized = serializer.deserialize(bytes, RpcResponse.class);
        assertNotNull("Deserialized object should not be null", deserialized);
        
        // 验证字段
        assertEquals("Request ID should match", response.getRequestId(), deserialized.getRequestId());
        assertEquals("Status should match", response.getStatus(), deserialized.getStatus());
        assertEquals("Message should match", response.getMessage(), deserialized.getMessage());
        assertFalse("Should not be success", deserialized.isSuccess());
        // 注意：异常对象的序列化可能会丢失一些信息
    }
    
    @Test
    public void testSerializeComplexObjects() throws Exception {
        // 测试复杂对象序列化
        Map<String, Object> complexData = new HashMap<>();
        complexData.put("string", "test");
        complexData.put("number", 42);
        complexData.put("boolean", true);
        complexData.put("list", Arrays.asList("a", "b", "c"));
        
        Map<String, String> nested = new HashMap<>();
        nested.put("key1", "value1");
        nested.put("key2", "value2");
        complexData.put("nested", nested);
        
        // 序列化
        byte[] bytes = serializer.serialize(complexData);
        assertNotNull("Serialized bytes should not be null", bytes);
        
        // 反序列化（使用Map类型）
        @SuppressWarnings("unchecked")
        Map<String, Object> deserialized = serializer.deserialize(bytes, Map.class);
        assertNotNull("Deserialized object should not be null", deserialized);
        
        // 验证基本字段
        assertEquals("String field should match", "test", deserialized.get("string"));
        assertEquals("Boolean field should match", true, deserialized.get("boolean"));
        assertNotNull("List field should not be null", deserialized.get("list"));
        assertNotNull("Nested field should not be null", deserialized.get("nested"));
    }
    
    @Test
    public void testSerializeNullObject() {
        try {
            serializer.serialize(null);
            fail("Should throw SerializationException for null object");
        } catch (SerializationException e) {
            assertTrue("Exception message should mention null", 
                e.getMessage().toLowerCase().contains("null"));
        }
    }
    
    @Test
    public void testDeserializeNullBytes() {
        try {
            serializer.deserialize(null, String.class);
            fail("Should throw SerializationException for null bytes");
        } catch (SerializationException e) {
            assertTrue("Exception message should mention null", 
                e.getMessage().toLowerCase().contains("null"));
        }
    }
    
    @Test
    public void testDeserializeEmptyBytes() {
        try {
            serializer.deserialize(new byte[0], String.class);
            fail("Should throw SerializationException for empty bytes");
        } catch (SerializationException e) {
            assertTrue("Exception message should mention empty", 
                e.getMessage().toLowerCase().contains("empty"));
        }
    }
    
    @Test
    public void testDeserializeNullClass() {
        try {
            serializer.deserialize("test".getBytes(), null);
            fail("Should throw SerializationException for null class");
        } catch (SerializationException e) {
            assertTrue("Exception message should mention null", 
                e.getMessage().toLowerCase().contains("null"));
        }
    }
    
    @Test
    public void testDeserializeInvalidData() {
        try {
            // 尝试将无效JSON数据反序列化
            byte[] invalidJson = "invalid json data".getBytes();
            serializer.deserialize(invalidJson, RpcRequest.class);
            fail("Should throw SerializationException for invalid JSON");
        } catch (SerializationException e) {
            assertNotNull("Exception should have a message", e.getMessage());
            assertNotNull("Exception should have a cause", e.getCause());
        }
    }
    
    @Test
    public void testSupports() {
        // 测试支持的类型
        assertTrue("Should support String", serializer.supports(String.class));
        assertTrue("Should support Integer", serializer.supports(Integer.class));
        assertTrue("Should support RpcRequest", serializer.supports(RpcRequest.class));
        assertTrue("Should support RpcResponse", serializer.supports(RpcResponse.class));
        assertTrue("Should support Map", serializer.supports(Map.class));
        assertTrue("Should support List", serializer.supports(List.class));
        assertTrue("Should support primitive arrays", serializer.supports(int[].class));
        
        // 测试不支持的类型
        assertFalse("Should not support interface", serializer.supports(Runnable.class));
        assertFalse("Should not support null", serializer.supports(null));
    }
    
    @Test
    public void testSerializeDeserializeConsistency() throws Exception {
        // 测试多次序列化和反序列化的一致性
        RpcRequest original = new RpcRequest();
        original.setRequestId("consistency-test");
        original.setInterfaceName("com.test.Service");
        original.setMethodName("testMethod");
        
        for (int i = 0; i < 10; i++) {
            byte[] bytes = serializer.serialize(original);
            RpcRequest deserialized = serializer.deserialize(bytes, RpcRequest.class);
            
            assertEquals("Request ID should be consistent", 
                original.getRequestId(), deserialized.getRequestId());
            assertEquals("Interface name should be consistent", 
                original.getInterfaceName(), deserialized.getInterfaceName());
            assertEquals("Method name should be consistent", 
                original.getMethodName(), deserialized.getMethodName());
        }
    }
    
    @Test
    public void testGetObjectMapper() {
        // 测试获取ObjectMapper实例
        assertNotNull("ObjectMapper should not be null", JsonSerializer.getObjectMapper());
        
        // 验证是单例
        assertSame("Should return same ObjectMapper instance", 
            JsonSerializer.getObjectMapper(), JsonSerializer.getObjectMapper());
    }
}