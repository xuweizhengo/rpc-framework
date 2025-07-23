package com.rpcframework.rpc.serialize;

import com.rpcframework.rpc.exception.SerializationException;
import com.rpcframework.rpc.protocol.RpcProtocol;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 序列化管理器测试类
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class SerializerManagerTest {
    
    private JsonSerializer jsonSerializer;
    
    @Before
    public void setUp() {
        jsonSerializer = new JsonSerializer();
        // 清空管理器状态，重新初始化
        SerializerManager.clear();
        SerializerManager.registerSerializer(jsonSerializer);
        SerializerManager.setDefaultSerializer(jsonSerializer);
    }
    
    @After
    public void tearDown() {
        // 测试完成后恢复默认状态
        SerializerManager.clear();
        JsonSerializer defaultSerializer = new JsonSerializer();
        SerializerManager.registerSerializer(defaultSerializer);
        SerializerManager.setDefaultSerializer(defaultSerializer);
    }
    
    @Test
    public void testRegisterSerializer() {
        // 创建新的序列化器
        MockSerializer mockSerializer = new MockSerializer();
        
        // 注册序列化器
        SerializerManager.registerSerializer(mockSerializer);
        
        // 验证注册成功
        Serializer retrieved = SerializerManager.getSerializer(mockSerializer.getCodecType());
        assertNotNull("Retrieved serializer should not be null", retrieved);
        assertEquals("Retrieved serializer should be the same instance", mockSerializer, retrieved);
        assertTrue("Should have the mock serializer", 
            SerializerManager.hasSerializer(mockSerializer.getCodecType()));
    }
    
    @Test
    public void testGetSerializer() {
        // 获取已注册的序列化器
        Serializer retrieved = SerializerManager.getSerializer(RpcProtocol.CodecType.JSON);
        assertNotNull("Retrieved serializer should not be null", retrieved);
        assertEquals("Should be JSON serializer", RpcProtocol.CodecType.JSON, retrieved.getCodecType());
        
        // 获取未注册的序列化器
        Serializer notFound = SerializerManager.getSerializer((byte) 99);
        assertNull("Should return null for unregistered codec type", notFound);
    }
    
    @Test
    public void testGetRequiredSerializer() throws Exception {
        // 获取已注册的序列化器
        Serializer retrieved = SerializerManager.getRequiredSerializer(RpcProtocol.CodecType.JSON);
        assertNotNull("Retrieved serializer should not be null", retrieved);
        assertEquals("Should be JSON serializer", RpcProtocol.CodecType.JSON, retrieved.getCodecType());
        
        // 获取未注册的序列化器应该抛出异常
        try {
            SerializerManager.getRequiredSerializer((byte) 99);
            fail("Should throw SerializationException for unregistered codec type");
        } catch (SerializationException e) {
            assertTrue("Exception message should mention codec type", 
                e.getMessage().contains("codec type"));
        }
    }
    
    @Test
    public void testGetDefaultSerializer() {
        Serializer defaultSerializer = SerializerManager.getDefaultSerializer();
        assertNotNull("Default serializer should not be null", defaultSerializer);
        assertEquals("Default serializer should be JSON", 
            RpcProtocol.CodecType.JSON, defaultSerializer.getCodecType());
    }
    
    @Test
    public void testSetDefaultSerializer() {
        // 创建新的序列化器并注册
        MockSerializer mockSerializer = new MockSerializer();
        SerializerManager.registerSerializer(mockSerializer);
        
        // 设置为默认序列化器
        SerializerManager.setDefaultSerializer(mockSerializer);
        
        // 验证设置成功
        Serializer defaultSerializer = SerializerManager.getDefaultSerializer();
        assertEquals("Default serializer should be the mock serializer", mockSerializer, defaultSerializer);
    }
    
    @Test
    public void testSetDefaultSerializerWithNull() {
        try {
            SerializerManager.setDefaultSerializer(null);
            fail("Should throw IllegalArgumentException for null serializer");
        } catch (IllegalArgumentException e) {
            assertTrue("Exception message should mention null", 
                e.getMessage().toLowerCase().contains("null"));
        }
    }
    
    @Test
    public void testSetDefaultSerializerNotRegistered() {
        try {
            MockSerializer unregistered = new MockSerializer();
            SerializerManager.setDefaultSerializer(unregistered);
            fail("Should throw IllegalArgumentException for unregistered serializer");
        } catch (IllegalArgumentException e) {
            assertTrue("Exception message should mention registration", 
                e.getMessage().toLowerCase().contains("register"));
        }
    }
    
    @Test
    public void testRemoveSerializer() {
        // 注册一个序列化器
        MockSerializer mockSerializer = new MockSerializer();
        SerializerManager.registerSerializer(mockSerializer);
        
        // 验证已注册
        assertTrue("Should have mock serializer", 
            SerializerManager.hasSerializer(mockSerializer.getCodecType()));
        
        // 移除序列化器
        Serializer removed = SerializerManager.removeSerializer(mockSerializer.getCodecType());
        assertEquals("Removed serializer should be the mock serializer", mockSerializer, removed);
        
        // 验证已移除
        assertFalse("Should not have mock serializer after removal", 
            SerializerManager.hasSerializer(mockSerializer.getCodecType()));
        
        // 移除不存在的序列化器
        Serializer notRemoved = SerializerManager.removeSerializer((byte) 99);
        assertNull("Should return null when removing non-existent serializer", notRemoved);
    }
    
    @Test
    public void testRemoveDefaultSerializer() {
        // 注册两个序列化器
        MockSerializer mockSerializer = new MockSerializer();
        SerializerManager.registerSerializer(mockSerializer);
        
        // 设置为默认
        SerializerManager.setDefaultSerializer(mockSerializer);
        
        // 移除默认序列化器
        SerializerManager.removeSerializer(mockSerializer.getCodecType());
        
        // 验证默认序列化器已更改（应该是JSON序列化器）
        Serializer newDefault = SerializerManager.getDefaultSerializer();
        assertNotNull("Should have a new default serializer", newDefault);
        assertNotEquals("New default should not be the removed serializer", mockSerializer, newDefault);
    }
    
    @Test
    public void testHasSerializer() {
        // 测试已注册的序列化器
        assertTrue("Should have JSON serializer", 
            SerializerManager.hasSerializer(RpcProtocol.CodecType.JSON));
        
        // 测试未注册的序列化器
        assertFalse("Should not have unregistered serializer", 
            SerializerManager.hasSerializer((byte) 99));
    }
    
    @Test
    public void testGetSerializerCount() {
        int initialCount = SerializerManager.getSerializerCount();
        assertEquals("Initial count should be 1 (JSON)", 1, initialCount);
        
        // 注册新的序列化器
        MockSerializer mockSerializer = new MockSerializer();
        SerializerManager.registerSerializer(mockSerializer);
        
        int newCount = SerializerManager.getSerializerCount();
        assertEquals("Count should increase by 1", initialCount + 1, newCount);
        
        // 移除序列化器
        SerializerManager.removeSerializer(mockSerializer.getCodecType());
        
        int finalCount = SerializerManager.getSerializerCount();
        assertEquals("Count should return to initial", initialCount, finalCount);
    }
    
    @Test
    public void testClear() {
        // 注册序列化器
        MockSerializer mockSerializer = new MockSerializer();
        SerializerManager.registerSerializer(mockSerializer);
        
        assertTrue("Should have serializers before clear", SerializerManager.getSerializerCount() > 0);
        
        // 清空
        SerializerManager.clear();
        
        assertEquals("Should have no serializers after clear", 0, SerializerManager.getSerializerCount());
        assertNull("Default serializer should be null", SerializerManager.getDefaultSerializer());
    }
    
    @Test
    public void testRegisterNullSerializer() {
        try {
            SerializerManager.registerSerializer(null);
            fail("Should throw IllegalArgumentException for null serializer");
        } catch (IllegalArgumentException e) {
            assertTrue("Exception message should mention null", 
                e.getMessage().toLowerCase().contains("null"));
        }
    }
    
    @Test
    public void testReplaceSerializer() {
        // 注册第一个序列化器
        MockSerializer first = new MockSerializer();
        SerializerManager.registerSerializer(first);
        
        // 注册相同编码类型的第二个序列化器
        MockSerializer second = new MockSerializer();
        SerializerManager.registerSerializer(second);
        
        // 验证第二个序列化器替换了第一个
        Serializer retrieved = SerializerManager.getSerializer(first.getCodecType());
        assertEquals("Should get the second serializer", second, retrieved);
        assertNotEquals("Should not get the first serializer", first, retrieved);
    }
    
    /**
     * 模拟序列化器，用于测试
     */
    private static class MockSerializer implements Serializer {
        
        @Override
        public <T> byte[] serialize(T obj) throws SerializationException {
            return obj.toString().getBytes();
        }
        
        @Override
        public <T> T deserialize(byte[] bytes, Class<T> clazz) throws SerializationException {
            return null; // 简单实现，仅用于测试
        }
        
        @Override
        public byte getCodecType() {
            return (byte) 100; // 使用一个不冲突的编码类型
        }
        
        @Override
        public String getName() {
            return "mock";
        }
    }
}