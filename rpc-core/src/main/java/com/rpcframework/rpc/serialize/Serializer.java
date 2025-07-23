package com.rpcframework.rpc.serialize;

import com.rpcframework.rpc.exception.SerializationException;

/**
 * 序列化器接口
 * 
 * <p>定义序列化和反序列化的标准接口，支持多种序列化协议的扩展。
 * 采用SPI机制，支持运行时动态加载序列化器实现。
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public interface Serializer {
    
    /**
     * 序列化对象
     * 
     * @param obj 要序列化的对象
     * @param <T> 对象类型
     * @return 序列化后的字节数组
     * @throws SerializationException 序列化失败时抛出
     */
    <T> byte[] serialize(T obj) throws SerializationException;
    
    /**
     * 反序列化对象
     * 
     * @param bytes 序列化的字节数组
     * @param clazz 目标类型
     * @param <T> 目标类型
     * @return 反序列化后的对象
     * @throws SerializationException 反序列化失败时抛出
     */
    <T> T deserialize(byte[] bytes, Class<T> clazz) throws SerializationException;
    
    /**
     * 获取序列化器的编码类型
     * 
     * @return 编码类型，对应RpcProtocol.CodecType中的常量
     * @see com.rpcframework.rpc.protocol.RpcProtocol.CodecType
     */
    byte getCodecType();
    
    /**
     * 获取序列化器名称
     * 
     * @return 序列化器名称，用于日志和调试
     */
    String getName();
    
    /**
     * 检查是否支持指定类型的序列化
     * 
     * @param clazz 要检查的类型
     * @return true-支持，false-不支持
     */
    default boolean supports(Class<?> clazz) {
        return true; // 默认支持所有类型
    }
}