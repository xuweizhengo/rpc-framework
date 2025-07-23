package com.rpcframework.rpc.serialize;

import com.rpcframework.rpc.exception.SerializationException;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Serializer接口测试类
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class SerializerInterfaceTest {
    
    @Test
    public void testSerializerInterface() {
        // 创建一个简单的实现来测试接口
        TestSerializer serializer = new TestSerializer();
        
        // 测试接口方法
        assertEquals("Codec type should be 99", (byte) 99, serializer.getCodecType());
        assertEquals("Name should be test", "test", serializer.getName());
        assertTrue("Should support String", serializer.supports(String.class));
        assertFalse("Should not support null", serializer.supports(null));
    }
    
    @Test
    public void testSerializerDefaultMethods() {
        TestSerializer serializer = new TestSerializer();
        
        // 测试默认的supports方法实现
        assertTrue("Should support String by default", serializer.supports(String.class));
        assertTrue("Should support Integer by default", serializer.supports(Integer.class));
        assertFalse("Should not support null", serializer.supports(null));
    }
    
    /**
     * 测试用的Serializer实现
     */
    private static class TestSerializer implements Serializer {
        
        @Override
        public <T> byte[] serialize(T obj) throws SerializationException {
            if (obj == null) {
                throw new SerializationException("Cannot serialize null");
            }
            return obj.toString().getBytes();
        }
        
        @Override
        public <T> T deserialize(byte[] bytes, Class<T> clazz) throws SerializationException {
            if (bytes == null || clazz == null) {
                throw new SerializationException("Bytes and class cannot be null");
            }
            
            String str = new String(bytes);
            if (clazz == String.class) {
                @SuppressWarnings("unchecked")
                T result = (T) str;
                return result;
            }
            
            throw new SerializationException("Unsupported type: " + clazz.getName());
        }
        
        @Override
        public byte getCodecType() {
            return (byte) 99;
        }
        
        @Override
        public String getName() {
            return "test";
        }
        
        @Override
        public boolean supports(Class<?> clazz) {
            if (clazz == null) {
                return false;
            }
            return String.class.equals(clazz) || Integer.class.equals(clazz);
        }
    }
}