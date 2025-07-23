package com.rpcframework.rpc.protocol;

import com.rpcframework.rpc.exception.SerializationException;
import com.rpcframework.rpc.model.RpcRequest;
import com.rpcframework.rpc.model.RpcResponse;
import com.rpcframework.rpc.serialize.Serializer;
import com.rpcframework.rpc.serialize.SerializerManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPC协议编码器
 * 
 * <p>负责将RPC请求和响应对象编码为二进制协议格式。编码过程包括：
 * <ol>
 * <li>对象序列化：将Java对象序列化为字节数组</li>
 * <li>协议头构建：写入魔数、版本、序列化类型等协议信息</li>
 * <li>消息体写入：将序列化后的字节数组写入输出缓冲区</li>
 * </ol>
 * 
 * <p>协议格式（12字节头 + N字节消息体）：
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
public class RpcEncoder extends MessageToByteEncoder<Object> {
    
    private static final Logger logger = LoggerFactory.getLogger(RpcEncoder.class);
    
    /**
     * 默认序列化类型，可以通过配置动态修改
     */
    private byte defaultCodecType = RpcProtocol.CodecType.JSON;
    
    public RpcEncoder() {
        super();
    }
    
    public RpcEncoder(byte defaultCodecType) {
        super();
        this.defaultCodecType = defaultCodecType;
    }
    
    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Encoding message: {}", msg.getClass().getSimpleName());
            }
            
            // 1. 确定序列化类型和序列化器
            byte codecType = determineCodecType(msg);
            Serializer serializer = SerializerManager.getRequiredSerializer(codecType);
            
            // 2. 序列化消息体
            byte[] bodyBytes = serializer.serialize(msg);
            
            // 3. 验证消息长度
            if (bodyBytes.length > RpcProtocol.MAX_FRAME_LENGTH) {
                throw new SerializationException(String.format(
                    "Message too large: %d bytes (max: %d bytes)", 
                    bodyBytes.length, RpcProtocol.MAX_FRAME_LENGTH));
            }
            
            // 4. 写入协议头（12字节）
            writeProtocolHeader(out, codecType, msg, bodyBytes.length);
            
            // 5. 写入消息体
            out.writeBytes(bodyBytes);
            
            if (logger.isDebugEnabled()) {
                logger.debug("Encoded message: type={}, codec={}, headerSize={}, bodySize={}, totalSize={}", 
                    msg.getClass().getSimpleName(), codecType, 
                    RpcProtocol.HEADER_LENGTH, bodyBytes.length, 
                    RpcProtocol.HEADER_LENGTH + bodyBytes.length);
            }
            
        } catch (Exception e) {
            logger.error("Failed to encode message: {}", msg.getClass().getSimpleName(), e);
            // 释放已写入的数据
            out.clear();
            throw e;
        }
    }
    
    /**
     * 写入协议头信息
     * 
     * @param out ByteBuf输出缓冲区
     * @param codecType 序列化类型
     * @param msg 消息对象
     * @param bodyLength 消息体长度
     */
    private void writeProtocolHeader(ByteBuf out, byte codecType, Object msg, int bodyLength) {
        // 魔数（4字节）
        out.writeInt(RpcProtocol.MAGIC_NUMBER);
        
        // 版本号（1字节）
        out.writeByte(RpcProtocol.VERSION);
        
        // 序列化类型（1字节）
        out.writeByte(codecType);
        
        // 消息类型（1字节）
        out.writeByte(getMessageType(msg));
        
        // 消息长度（4字节）
        out.writeInt(bodyLength);
        
        // 预留字段（1字节），用于后续协议扩展
        out.writeByte(0);
    }
    
    /**
     * 确定消息的序列化类型
     * 
     * @param msg 消息对象
     * @return 序列化类型
     */
    private byte determineCodecType(Object msg) {
        // 优先使用默认序列化类型
        // 后续可以扩展为根据消息类型、配置等动态选择
        return defaultCodecType;
    }
    
    /**
     * 获取消息类型标识
     * 
     * @param msg 消息对象
     * @return 消息类型常量
     */
    private byte getMessageType(Object msg) {
        if (msg instanceof RpcRequest) {
            return RpcProtocol.MessageType.REQUEST;
        } else if (msg instanceof RpcResponse) {
            return RpcProtocol.MessageType.RESPONSE;
        } else {
            // 对于未知类型，默认作为请求处理
            logger.warn("Unknown message type: {}, treating as REQUEST", 
                msg.getClass().getName());
            return RpcProtocol.MessageType.REQUEST;
        }
    }
    
    /**
     * 设置默认序列化类型
     * 
     * @param codecType 序列化类型
     */
    public void setDefaultCodecType(byte codecType) {
        this.defaultCodecType = codecType;
        logger.info("Default codec type changed to: {}", codecType);
    }
    
    /**
     * 获取默认序列化类型
     * 
     * @return 序列化类型
     */
    public byte getDefaultCodecType() {
        return defaultCodecType;
    }
}