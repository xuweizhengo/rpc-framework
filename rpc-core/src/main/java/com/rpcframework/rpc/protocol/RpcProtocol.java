package com.rpcframework.rpc.protocol;

/**
 * RPC协议常量定义
 * 
 * <p>协议格式：
 * <pre>
 * +-------+--------+----------+----------+-------------+----------+--------+
 * | 魔数  | 版本号 | 序列化   | 消息类型 | 消息长度    | 预留字段 | 消息体 |
 * | 4字节 | 1字节  | 1字节    | 1字节    | 4字节       | 1字节    | N字节  |
 * +-------+--------+----------+----------+-------------+----------+--------+
 * </pre>
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public final class RpcProtocol {
    
    /**
     * 协议魔数，用于快速识别RPC协议
     * 0xCAFEBABE - 咖啡宝贝，Java字节码文件头标识
     */
    public static final int MAGIC_NUMBER = 0xCAFEBABE;
    
    /**
     * 协议版本号，支持协议演进
     */
    public static final byte VERSION = 1;
    
    /**
     * 协议头长度（字节）
     * 魔数(4) + 版本(1) + 序列化类型(1) + 消息类型(1) + 消息长度(4) + 预留字段(1) = 12字节
     */
    public static final int HEADER_LENGTH = 12;
    
    /**
     * 最大帧长度，防止恶意大包攻击
     * 16MB = 16 * 1024 * 1024
     */
    public static final int MAX_FRAME_LENGTH = 16 * 1024 * 1024;
    
    /**
     * 序列化类型常量
     */
    public static final class CodecType {
        /** JSON序列化 */
        public static final byte JSON = 0;
        /** Protobuf序列化 */
        public static final byte PROTOBUF = 1;
        /** Hessian序列化 */
        public static final byte HESSIAN = 2;
        /** Kryo序列化 */
        public static final byte KRYO = 3;
        
        private CodecType() {
            // 工具类，禁止实例化
        }
    }
    
    /**
     * 消息类型常量
     */
    public static final class MessageType {
        /** RPC请求消息 */
        public static final byte REQUEST = 0;
        /** RPC响应消息 */
        public static final byte RESPONSE = 1;
        /** 心跳ping消息 */
        public static final byte HEARTBEAT_PING = 2;
        /** 心跳pong消息 */
        public static final byte HEARTBEAT_PONG = 3;
        
        private MessageType() {
            // 工具类，禁止实例化
        }
    }
    
    /**
     * RPC状态码常量
     */
    public static final class Status {
        /** 成功 */
        public static final int SUCCESS = 200;
        /** 客户端请求错误 */
        public static final int BAD_REQUEST = 400;
        /** 请求的服务或方法未找到 */
        public static final int NOT_FOUND = 404;
        /** 请求超时 */
        public static final int TIMEOUT = 408;
        /** 服务端内部错误 */
        public static final int INTERNAL_ERROR = 500;
        /** 服务不可用 */
        public static final int SERVICE_UNAVAILABLE = 503;
        
        private Status() {
            // 工具类，禁止实例化
        }
    }
    
    /**
     * 默认配置常量
     */
    public static final class Defaults {
        /** 默认RPC端口 */
        public static final int DEFAULT_PORT = 9090;
        /** 默认连接超时时间（毫秒） */
        public static final int DEFAULT_CONNECT_TIMEOUT = 5000;
        /** 默认请求超时时间（毫秒） */
        public static final int DEFAULT_REQUEST_TIMEOUT = 30000;
        /** 默认心跳间隔（毫秒） */
        public static final int DEFAULT_HEARTBEAT_INTERVAL = 30000;
        /** 默认最大重试次数 */
        public static final int DEFAULT_MAX_RETRY_TIMES = 3;
        
        private Defaults() {
            // 工具类，禁止实例化
        }
    }
    
    private RpcProtocol() {
        // 工具类，禁止实例化
    }
}