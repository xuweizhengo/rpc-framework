package com.rpcframework.rpc.protocol;

import com.rpcframework.rpc.exception.RpcException;
import com.rpcframework.rpc.exception.SerializationException;
import com.rpcframework.rpc.model.RpcRequest;
import com.rpcframework.rpc.model.RpcResponse;
import com.rpcframework.rpc.serialize.Serializer;
import com.rpcframework.rpc.serialize.SerializerManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * RPC协议解码器
 * 
 * <p>负责将二进制协议数据解码为RPC请求和响应对象。解码过程包括：
 * <ol>
 * <li>协议头解析：读取魔数、版本、序列化类型等协议信息</li>
 * <li>协议验证：验证魔数、版本兼容性、消息长度等</li>
 * <li>消息体读取：根据消息长度读取消息体字节数据</li>
 * <li>对象反序列化：将字节数据反序列化为Java对象</li>
 * </ol>
 * 
 * <p>解码器采用零拷贝设计，支持TCP粘包拆包处理，具备良好的异常恢复能力。
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class RpcDecoder extends ByteToMessageDecoder {
    
    private static final Logger logger = LoggerFactory.getLogger(RpcDecoder.class);
    
    /**
     * 协议头信息存储类
     */
    private static class ProtocolHeader {
        final int magicNumber;
        final byte version;
        final byte codecType;
        final byte messageType;
        final int bodyLength;
        final byte reserved;
        
        ProtocolHeader(int magicNumber, byte version, byte codecType, 
                      byte messageType, int bodyLength, byte reserved) {
            this.magicNumber = magicNumber;
            this.version = version;
            this.codecType = codecType;
            this.messageType = messageType;
            this.bodyLength = bodyLength;
            this.reserved = reserved;
        }
    }
    
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        try {
            // 1. 检查是否有足够的数据读取协议头
            if (in.readableBytes() < RpcProtocol.HEADER_LENGTH) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Insufficient data for header, need {} bytes, available {} bytes", 
                        RpcProtocol.HEADER_LENGTH, in.readableBytes());
                }
                return; // 等待更多数据
            }
            
            // 2. 标记当前读位置，用于重置
            in.markReaderIndex();
            
            // 3. 读取并验证协议头
            ProtocolHeader header = readAndValidateHeader(in, ctx);
            if (header == null) {
                // 验证失败，连接将被关闭
                return;
            }
            
            // 4. 检查消息体数据是否完整
            if (in.readableBytes() < header.bodyLength) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Insufficient data for body, need {} bytes, available {} bytes", 
                        header.bodyLength, in.readableBytes());
                }
                // 重置读位置，等待更多数据
                in.resetReaderIndex();
                return;
            }
            
            // 5. 读取消息体
            byte[] bodyBytes = new byte[header.bodyLength];
            in.readBytes(bodyBytes);
            
            // 6. 反序列化消息
            Object message = deserializeMessage(header, bodyBytes);
            if (message != null) {
                out.add(message);
                
                if (logger.isDebugEnabled()) {
                    logger.debug("Decoded message: type={}, codec={}, bodySize={}", 
                        message.getClass().getSimpleName(), header.codecType, header.bodyLength);
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to decode message", e);
            // 清理状态，关闭连接
            in.clear();
            ctx.close();
            throw e;
        }
    }
    
    /**
     * 读取并验证协议头信息
     * 
     * @param in 输入ByteBuf
     * @param ctx 通道上下文
     * @return 协议头信息，验证失败返回null
     */
    private ProtocolHeader readAndValidateHeader(ByteBuf in, ChannelHandlerContext ctx) {
        // 读取协议头各字段
        int magicNumber = in.readInt();
        byte version = in.readByte();
        byte codecType = in.readByte();
        byte messageType = in.readByte();
        int bodyLength = in.readInt();
        byte reserved = in.readByte();
        
        ProtocolHeader header = new ProtocolHeader(
            magicNumber, version, codecType, messageType, bodyLength, reserved);
        
        // 验证魔数
        if (magicNumber != RpcProtocol.MAGIC_NUMBER) {
            logger.error("Invalid magic number: expected 0x{}, actual 0x{}", 
                Integer.toHexString(RpcProtocol.MAGIC_NUMBER).toUpperCase(),
                Integer.toHexString(magicNumber).toUpperCase());
            ctx.close();
            return null;
        }
        
        // 验证版本兼容性
        if (version != RpcProtocol.VERSION) {
            logger.error("Unsupported protocol version: expected {}, actual {}", 
                RpcProtocol.VERSION, version);
            ctx.close();
            return null;
        }
        
        // 验证消息长度
        if (bodyLength < 0 || bodyLength > RpcProtocol.MAX_FRAME_LENGTH) {
            logger.error("Invalid body length: {} (max: {})", 
                bodyLength, RpcProtocol.MAX_FRAME_LENGTH);
            ctx.close();
            return null;
        }
        
        // 验证序列化类型
        if (!SerializerManager.hasSerializer(codecType)) {
            logger.error("Unsupported codec type: {}", codecType);
            ctx.close();
            return null;
        }
        
        // 验证消息类型
        if (!isValidMessageType(messageType)) {
            logger.error("Invalid message type: {}", messageType);
            ctx.close();
            return null;
        }
        
        return header;
    }
    
    /**
     * 反序列化消息对象
     * 
     * @param header 协议头信息
     * @param bodyBytes 消息体字节数组
     * @return 反序列化后的对象
     * @throws SerializationException 反序列化失败时抛出
     */
    private Object deserializeMessage(ProtocolHeader header, byte[] bodyBytes) 
            throws SerializationException {
        
        // 获取序列化器
        Serializer serializer = SerializerManager.getRequiredSerializer(header.codecType);
        
        // 根据消息类型确定目标类
        Class<?> targetClass = getTargetClass(header.messageType);
        if (targetClass == null) {
            throw new SerializationException("Unknown message type: " + header.messageType);
        }
        
        // 反序列化
        return serializer.deserialize(bodyBytes, targetClass);
    }
    
    /**
     * 根据消息类型获取目标类
     * 
     * @param messageType 消息类型
     * @return 目标类，未知类型返回null
     */
    private Class<?> getTargetClass(byte messageType) {
        switch (messageType) {
            case RpcProtocol.MessageType.REQUEST:
                return RpcRequest.class;
            case RpcProtocol.MessageType.RESPONSE:
                return RpcResponse.class;
            case RpcProtocol.MessageType.HEARTBEAT_PING:
            case RpcProtocol.MessageType.HEARTBEAT_PONG:
                // 心跳消息暂时使用RpcRequest处理
                return RpcRequest.class;
            default:
                return null;
        }
    }
    
    /**
     * 验证消息类型是否有效
     * 
     * @param messageType 消息类型
     * @return true-有效，false-无效
     */
    private boolean isValidMessageType(byte messageType) {
        return messageType == RpcProtocol.MessageType.REQUEST
            || messageType == RpcProtocol.MessageType.RESPONSE
            || messageType == RpcProtocol.MessageType.HEARTBEAT_PING
            || messageType == RpcProtocol.MessageType.HEARTBEAT_PONG;
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Decoder exception caught", cause);
        ctx.close();
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("Channel inactive: {}", ctx.channel());
        }
        super.channelInactive(ctx);
    }
}