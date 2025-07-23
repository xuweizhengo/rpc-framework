package com.rpcframework.rpc.protocol;

import com.rpcframework.rpc.model.RpcRequest;
import com.rpcframework.rpc.model.RpcResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * RPC解码器测试类
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class RpcDecoderTest {
    
    private RpcDecoder decoder;
    private EmbeddedChannel channel;
    
    @Before
    public void setUp() {
        decoder = new RpcDecoder();
        channel = new EmbeddedChannel(decoder);
    }
    
    @Test
    public void testDecodeCompleteMessage() {
        // 创建完整的协议消息
        ByteBuf message = createValidRpcRequestMessage();
        
        // 解码
        assertTrue("Should write message successfully", channel.writeInbound(message));
        
        // 读取解码结果
        RpcRequest decoded = channel.readInbound();
        assertNotNull("Decoded object should not be null", decoded);
        assertEquals("Request ID should match", "test-123", decoded.getRequestId());
        assertEquals("Interface name should match", "com.example.UserService", decoded.getInterfaceName());
        assertEquals("Method name should match", "getUserById", decoded.getMethodName());
        
        // 确保没有更多消息
        assertNull("Should have no more messages", channel.readInbound());
    }
    
    @Test
    public void testDecodeIncompleteHeader() {
        // 创建不完整的协议头（少于12字节）
        ByteBuf incompleteHeader = Unpooled.buffer(8);
        incompleteHeader.writeInt(RpcProtocol.MAGIC_NUMBER);
        incompleteHeader.writeByte(RpcProtocol.VERSION);
        incompleteHeader.writeByte(RpcProtocol.CodecType.JSON);
        // 缺少后面的字段
        
        // 解码
        assertFalse("Should not produce any output for incomplete header", 
            channel.writeInbound(incompleteHeader));
        
        // 确保没有解码出任何对象
        assertNull("Should have no decoded objects", channel.readInbound());
    }
    
    @Test
    public void testDecodeIncompleteBody() {
        // 创建完整的协议头但不完整的消息体
        ByteBuf incompleteMessage = Unpooled.buffer();
        
        // 写入协议头
        incompleteMessage.writeInt(RpcProtocol.MAGIC_NUMBER);
        incompleteMessage.writeByte(RpcProtocol.VERSION);
        incompleteMessage.writeByte(RpcProtocol.CodecType.JSON);
        incompleteMessage.writeByte(RpcProtocol.MessageType.REQUEST);
        incompleteMessage.writeInt(100); // 声明消息体长度为100
        incompleteMessage.writeByte(0); // 预留字段
        
        // 只写入50字节的消息体（少于声明的100字节）
        byte[] partialBody = new byte[50];
        incompleteMessage.writeBytes(partialBody);
        
        // 解码
        assertFalse("Should not produce any output for incomplete body", 
            channel.writeInbound(incompleteMessage));
        
        // 确保没有解码出任何对象
        assertNull("Should have no decoded objects", channel.readInbound());
    }
    
    @Test
    public void testDecodeInvalidMagicNumber() {
        // 创建带有无效魔数的消息
        ByteBuf invalidMessage = Unpooled.buffer();
        invalidMessage.writeInt(0xDEADBEEF); // 错误的魔数
        invalidMessage.writeByte(RpcProtocol.VERSION);
        invalidMessage.writeByte(RpcProtocol.CodecType.JSON);
        invalidMessage.writeByte(RpcProtocol.MessageType.REQUEST);
        invalidMessage.writeInt(0);
        invalidMessage.writeByte(0);
        
        // 解码
        assertFalse("Should not produce any output for invalid magic number", 
            channel.writeInbound(invalidMessage));
        
        // 确保通道已关闭
        assertFalse("Channel should be closed", channel.isActive());
    }
    
    @Test
    public void testDecodeInvalidVersion() {
        // 创建带有无效版本的消息
        ByteBuf invalidMessage = Unpooled.buffer();
        invalidMessage.writeInt(RpcProtocol.MAGIC_NUMBER);
        invalidMessage.writeByte((byte) 99); // 错误的版本号
        invalidMessage.writeByte(RpcProtocol.CodecType.JSON);
        invalidMessage.writeByte(RpcProtocol.MessageType.REQUEST);
        invalidMessage.writeInt(0);
        invalidMessage.writeByte(0);
        
        // 解码
        assertFalse("Should not produce any output for invalid version", 
            channel.writeInbound(invalidMessage));
        
        // 确保通道已关闭
        assertFalse("Channel should be closed", channel.isActive());
    }
    
    @Test
    public void testDecodeExcessiveBodyLength() {
        // 创建带有过大消息体长度的消息
        ByteBuf invalidMessage = Unpooled.buffer();
        invalidMessage.writeInt(RpcProtocol.MAGIC_NUMBER);
        invalidMessage.writeByte(RpcProtocol.VERSION);
        invalidMessage.writeByte(RpcProtocol.CodecType.JSON);
        invalidMessage.writeByte(RpcProtocol.MessageType.REQUEST);
        invalidMessage.writeInt(RpcProtocol.MAX_FRAME_LENGTH + 1); // 超过最大长度
        invalidMessage.writeByte(0);
        
        // 解码
        assertFalse("Should not produce any output for excessive body length", 
            channel.writeInbound(invalidMessage));
        
        // 确保通道已关闭
        assertFalse("Channel should be closed", channel.isActive());
    }
    
    @Test
    public void testDecodeNegativeBodyLength() {
        // 创建带有负数消息体长度的消息
        ByteBuf invalidMessage = Unpooled.buffer();
        invalidMessage.writeInt(RpcProtocol.MAGIC_NUMBER);
        invalidMessage.writeByte(RpcProtocol.VERSION);
        invalidMessage.writeByte(RpcProtocol.CodecType.JSON);
        invalidMessage.writeByte(RpcProtocol.MessageType.REQUEST);
        invalidMessage.writeInt(-1); // 负数长度
        invalidMessage.writeByte(0);
        
        // 解码
        assertFalse("Should not produce any output for negative body length", 
            channel.writeInbound(invalidMessage));
        
        // 确保通道已关闭
        assertFalse("Channel should be closed", channel.isActive());
    }
    
    @Test
    public void testDecodeUnsupportedCodecType() {
        // 创建带有不支持的序列化类型的消息
        ByteBuf invalidMessage = Unpooled.buffer();
        invalidMessage.writeInt(RpcProtocol.MAGIC_NUMBER);
        invalidMessage.writeByte(RpcProtocol.VERSION);
        invalidMessage.writeByte((byte) 99); // 不支持的序列化类型
        invalidMessage.writeByte(RpcProtocol.MessageType.REQUEST);
        invalidMessage.writeInt(0);
        invalidMessage.writeByte(0);
        
        // 解码
        assertFalse("Should not produce any output for unsupported codec type", 
            channel.writeInbound(invalidMessage));
        
        // 确保通道已关闭
        assertFalse("Channel should be closed", channel.isActive());
    }
    
    @Test
    public void testDecodeInvalidMessageType() {
        // 创建带有无效消息类型的消息
        ByteBuf invalidMessage = Unpooled.buffer();
        invalidMessage.writeInt(RpcProtocol.MAGIC_NUMBER);
        invalidMessage.writeByte(RpcProtocol.VERSION);
        invalidMessage.writeByte(RpcProtocol.CodecType.JSON);
        invalidMessage.writeByte((byte) 99); // 无效的消息类型
        invalidMessage.writeInt(0);
        invalidMessage.writeByte(0);
        
        // 解码
        assertFalse("Should not produce any output for invalid message type", 
            channel.writeInbound(invalidMessage));
        
        // 确保通道已关闭
        assertFalse("Channel should be closed", channel.isActive());
    }
    
    @Test
    public void testDecodeRpcResponseMessage() {
        // 创建RPC响应消息
        ByteBuf message = createValidRpcResponseMessage();
        
        // 解码
        assertTrue("Should write response message successfully", channel.writeInbound(message));
        
        // 读取解码结果
        RpcResponse decoded = channel.readInbound();
        assertNotNull("Decoded object should not be null", decoded);
        assertEquals("Request ID should match", "test-456", decoded.getRequestId());
        assertEquals("Status should be success", 200, decoded.getStatus());
        assertEquals("Result should match", "Hello World", decoded.getResult());
        
        // 确保没有更多消息
        assertNull("Should have no more messages", channel.readInbound());
    }
    
    @Test
    public void testDecodeMultipleMessages() {
        // 创建多个消息的组合缓冲区
        ByteBuf request = createValidRpcRequestMessage();
        ByteBuf response = createValidRpcResponseMessage();
        ByteBuf combined = Unpooled.wrappedBuffer(request, response);
        
        // 解码
        assertTrue("Should write combined messages successfully", channel.writeInbound(combined));
        
        // 读取第一个解码结果（请求）
        RpcRequest decodedRequest = channel.readInbound();
        assertNotNull("First decoded object should not be null", decodedRequest);
        assertEquals("Request ID should match", "test-123", decodedRequest.getRequestId());
        
        // 读取第二个解码结果（响应）  
        RpcResponse decodedResponse = channel.readInbound();
        assertNotNull("Second decoded object should not be null", decodedResponse);
        assertEquals("Response request ID should match", "test-456", decodedResponse.getRequestId());
        
        // 确保没有更多消息
        assertNull("Should have no more messages", channel.readInbound());
    }
    
    @Test
    public void testDecodeFragmentedMessage() {
        // 创建完整的消息
        ByteBuf completeMessage = createValidRpcRequestMessage();
        
        // 将消息分成两个片段
        int splitPoint = completeMessage.readableBytes() / 2;
        ByteBuf fragment1 = completeMessage.readSlice(splitPoint);
        ByteBuf fragment2 = completeMessage.readSlice(completeMessage.readableBytes());
        
        // 发送第一个片段
        assertFalse("Should not produce output for first fragment", 
            channel.writeInbound(fragment1.retain()));
        assertNull("Should have no decoded objects after first fragment", channel.readInbound());
        
        // 发送第二个片段
        assertTrue("Should produce output after second fragment", 
            channel.writeInbound(fragment2.retain()));
        
        // 读取解码结果
        RpcRequest decoded = channel.readInbound();
        assertNotNull("Decoded object should not be null", decoded);
        assertEquals("Request ID should match", "test-123", decoded.getRequestId());
        
        completeMessage.release();
    }
    
    /**
     * 创建有效的RPC请求消息
     */
    private ByteBuf createValidRpcRequestMessage() {
        // 创建请求对象
        RpcRequest request = new RpcRequest();
        request.setRequestId("test-123");
        request.setInterfaceName("com.example.UserService");
        request.setMethodName("getUserById");
        request.setParameterTypes(new Class[]{Long.class});
        request.setParameters(new Object[]{123L});
        
        // 使用编码器创建消息
        EmbeddedChannel encoderChannel = new EmbeddedChannel(new RpcEncoder());
        encoderChannel.writeOutbound(request);
        ByteBuf encoded = encoderChannel.readOutbound();
        encoderChannel.close();
        
        return encoded;
    }
    
    /**
     * 创建有效的RPC响应消息
     */
    private ByteBuf createValidRpcResponseMessage() {
        // 创建响应对象
        RpcResponse response = RpcResponse.success("test-456", "Hello World");
        
        // 使用编码器创建消息
        EmbeddedChannel encoderChannel = new EmbeddedChannel(new RpcEncoder());
        encoderChannel.writeOutbound(response);
        ByteBuf encoded = encoderChannel.readOutbound();
        encoderChannel.close();
        
        return encoded;
    }
}