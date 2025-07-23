package com.rpcframework.rpc.serialize;

import com.rpcframework.rpc.exception.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 序列化器管理器
 * 
 * <p>负责管理所有序列化器实例，提供序列化器的注册、获取和管理功能。
 * 采用单例模式和线程安全设计，支持运行时动态添加序列化器。
 * 
 * <p>主要功能：
 * <ul>
 * <li>序列化器注册和管理</li>
 * <li>默认序列化器配置</li>
 * <li>序列化器查找和获取</li>
 * <li>序列化器生命周期管理</li>
 * </ul>
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class SerializerManager {
    
    private static final Logger logger = LoggerFactory.getLogger(SerializerManager.class);
    
    /**
     * 序列化器存储映射：编码类型 -> 序列化器实例
     */
    private static final ConcurrentMap<Byte, Serializer> SERIALIZERS = new ConcurrentHashMap<>();
    
    /**
     * 默认序列化器，用于未指定序列化类型的场景
     */
    private static volatile Serializer defaultSerializer;
    
    // 静态初始化，注册默认序列化器
    static {
        try {
            // 注册JSON序列化器作为默认序列化器
            JsonSerializer jsonSerializer = new JsonSerializer();
            registerSerializer(jsonSerializer);
            setDefaultSerializer(jsonSerializer);
            
            logger.info("SerializerManager initialized with default JSON serializer");
        } catch (Exception e) {
            logger.error("Failed to initialize SerializerManager", e);
            throw new RuntimeException("Failed to initialize SerializerManager", e);
        }
    }
    
    /**
     * 注册序列化器
     * 
     * @param serializer 序列化器实例
     * @throws IllegalArgumentException 如果序列化器为null或编码类型冲突
     */
    public static void registerSerializer(Serializer serializer) {
        if (serializer == null) {
            throw new IllegalArgumentException("Serializer cannot be null");
        }
        
        byte codecType = serializer.getCodecType();
        Serializer existing = SERIALIZERS.put(codecType, serializer);
        
        if (existing != null && existing != serializer) {
            logger.warn("Replaced existing serializer {} with {} for codec type {}", 
                existing.getName(), serializer.getName(), codecType);
        } else {
            logger.info("Registered serializer {} for codec type {}", 
                serializer.getName(), codecType);
        }
    }
    
    /**
     * 获取指定编码类型的序列化器
     * 
     * @param codecType 编码类型
     * @return 序列化器实例，如果未找到返回null
     */
    public static Serializer getSerializer(byte codecType) {
        return SERIALIZERS.get(codecType);
    }
    
    /**
     * 获取指定编码类型的序列化器，如果未找到则抛出异常
     * 
     * @param codecType 编码类型
     * @return 序列化器实例
     * @throws SerializationException 如果未找到对应的序列化器
     */
    public static Serializer getRequiredSerializer(byte codecType) throws SerializationException {
        Serializer serializer = getSerializer(codecType);
        if (serializer == null) {
            throw new SerializationException("No serializer found for codec type: " + codecType);
        }
        return serializer;
    }
    
    /**
     * 获取默认序列化器
     * 
     * @return 默认序列化器实例
     */
    public static Serializer getDefaultSerializer() {
        return defaultSerializer;
    }
    
    /**
     * 设置默认序列化器
     * 
     * @param serializer 要设置为默认的序列化器
     * @throws IllegalArgumentException 如果序列化器为null或未注册
     */
    public static void setDefaultSerializer(Serializer serializer) {
        if (serializer == null) {
            throw new IllegalArgumentException("Default serializer cannot be null");
        }
        
        // 确保序列化器已注册
        if (!SERIALIZERS.containsValue(serializer)) {
            throw new IllegalArgumentException("Serializer must be registered before setting as default");
        }
        
        Serializer oldDefault = defaultSerializer;
        defaultSerializer = serializer;
        
        logger.info("Default serializer changed from {} to {}", 
            oldDefault != null ? oldDefault.getName() : "null", 
            serializer.getName());
    }
    
    /**
     * 移除指定编码类型的序列化器
     * 
     * @param codecType 编码类型
     * @return 被移除的序列化器，如果不存在返回null
     */
    public static Serializer removeSerializer(byte codecType) {
        Serializer removed = SERIALIZERS.remove(codecType);
        if (removed != null) {
            logger.info("Removed serializer {} for codec type {}", removed.getName(), codecType);
            
            // 如果移除的是默认序列化器，需要重新设置默认序列化器
            if (removed == defaultSerializer) {
                // 尝试设置第一个可用的序列化器为默认
                Serializer firstAvailable = SERIALIZERS.values().iterator().hasNext() 
                    ? SERIALIZERS.values().iterator().next() 
                    : null;
                defaultSerializer = firstAvailable;
                
                logger.warn("Removed default serializer, new default is: {}", 
                    firstAvailable != null ? firstAvailable.getName() : "null");
            }
        }
        return removed;
    }
    
    /**
     * 检查是否存在指定编码类型的序列化器
     * 
     * @param codecType 编码类型
     * @return true-存在，false-不存在
     */
    public static boolean hasSerializer(byte codecType) {
        return SERIALIZERS.containsKey(codecType);
    }
    
    /**
     * 获取已注册的序列化器数量
     * 
     * @return 序列化器数量
     */
    public static int getSerializerCount() {
        return SERIALIZERS.size();
    }
    
    /**
     * 清空所有序列化器（主要用于测试）
     */
    public static void clear() {
        SERIALIZERS.clear();
        defaultSerializer = null;
        logger.warn("All serializers cleared");
    }
    
    /**
     * 获取所有已注册的编码类型
     * 
     * @return 编码类型数组
     */
    public static byte[] getSupportedCodecTypes() {
        return SERIALIZERS.keySet().stream()
            .mapToInt(Byte::intValue)
            .sorted()
            .collect(StringBuilder::new, 
                (sb, i) -> sb.append((byte) i), 
                StringBuilder::append)
            .toString()
            .getBytes();
    }
    
    // 私有构造函数，防止实例化
    private SerializerManager() {
        throw new UnsupportedOperationException("SerializerManager is a utility class");
    }
}