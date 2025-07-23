package com.rpcframework.rpc.serialize;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.rpcframework.rpc.exception.SerializationException;
import com.rpcframework.rpc.protocol.RpcProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON序列化器实现
 * 
 * <p>基于Jackson库实现的JSON序列化器，具备以下特性：
 * <ul>
 * <li>高性能：复用ObjectMapper实例，避免重复创建</li>
 * <li>容错性：忽略未知属性，兼容协议演进</li>
 * <li>可读性：JSON格式便于调试和日志记录</li>
 * <li>跨语言：JSON格式支持多语言互操作</li>
 * </ul>
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class JsonSerializer implements Serializer {
    
    private static final Logger logger = LoggerFactory.getLogger(JsonSerializer.class);
    
    /**
     * Jackson ObjectMapper实例，线程安全
     */
    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();
    
    /**
     * 创建并配置ObjectMapper
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // 序列化配置
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        
        // 反序列化配置
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        
        return mapper;
    }
    
    @Override
    public <T> byte[] serialize(T obj) throws SerializationException {
        if (obj == null) {
            throw new SerializationException("Cannot serialize null object");
        }
        
        try {
            byte[] bytes = OBJECT_MAPPER.writeValueAsBytes(obj);
            if (logger.isDebugEnabled()) {
                logger.debug("Serialized object of type {} to {} bytes", 
                    obj.getClass().getSimpleName(), bytes.length);
            }
            return bytes;
        } catch (Exception e) {
            String message = String.format("Failed to serialize object of type %s: %s", 
                obj.getClass().getName(), e.getMessage());
            logger.error(message, e);
            throw new SerializationException(message, e);
        }
    }
    
    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            throw new SerializationException("Cannot deserialize null or empty bytes");
        }
        
        if (clazz == null) {
            throw new SerializationException("Target class cannot be null");
        }
        
        try {
            T result = OBJECT_MAPPER.readValue(bytes, clazz);
            if (logger.isDebugEnabled()) {
                logger.debug("Deserialized {} bytes to object of type {}", 
                    bytes.length, clazz.getSimpleName());
            }
            return result;
        } catch (Exception e) {
            String message = String.format("Failed to deserialize %d bytes to type %s: %s", 
                bytes.length, clazz.getName(), e.getMessage());
            logger.error(message, e);
            throw new SerializationException(message, e);
        }
    }
    
    @Override
    public byte getCodecType() {
        return RpcProtocol.CodecType.JSON;
    }
    
    @Override
    public String getName() {
        return "json";
    }
    
    @Override
    public boolean supports(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        
        // JSON序列化支持大部分Java类型，但有一些限制
        // 支持基本类型数组
        if (clazz.isArray() && clazz.getComponentType().isPrimitive()) {
            return true;
        }
        
        // 支持常用的接口类型如Map、List、Collection等
        if (clazz.isInterface() && !clazz.isAnnotation()) {
            return java.util.Map.class.isAssignableFrom(clazz) || 
                   java.util.List.class.isAssignableFrom(clazz) ||
                   java.util.Collection.class.isAssignableFrom(clazz) ||
                   java.io.Serializable.class.isAssignableFrom(clazz);
        }
        
        // 支持大部分标准Java类型
        return true;
    }
    
    /**
     * 获取ObjectMapper实例，供测试和高级用法使用
     * 
     * @return ObjectMapper实例
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }
}