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
 * RPC编码器测试类
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class RpcEncoderTest {
    
    private RpcEncoder encoder;
    private EmbeddedChannel channel;
    
    @Before
    public void setUp() {
        encoder = new RpcEncoder();
        channel = new EmbeddedChannel(encoder);
    }
    
    @Test
    public void testEncodeRpcRequest() {
        // 创建测试请求
        RpcRequest request = new RpcRequest();
        request.setRequestId("test-123");
        request.setInterfaceName("com.example.UserService");
        request.setMethodName("getUserById");
        request.setParameterTypes(new Class[]{Long.class});
        request.setParameters(new Object[]{123L});
        
        // 编码
        assertTrue("Should write request successfully", channel.writeOutbound(request));
        
        // 读取编码结果
        ByteBuf encoded = channel.readOutbound();
        assertNotNull("Encoded buffer should not be null", encoded);
        
        // 验证协议头
        verifyProtocolHeader(encoded, RpcProtocol.MessageType.REQUEST);
        
        encoded.release();
    }
    
    @Test
    public void testEncodeRpcResponse() {
        // 创建测试响应
        RpcResponse response = RpcResponse.success("test-456", "Hello World");
        
        // 编码
        assertTrue("Should write response successfully", channel.writeOutbound(response));
        
        // 读取编码结果
        ByteBuf encoded = channel.readOutbound();
        assertNotNull("Encoded buffer should not be null", encoded);
        
        // 验证协议头
        verifyProtocolHeader(encoded, RpcProtocol.MessageType.RESPONSE);
        
        encoded.release();
    }
    
    @Test
    public void testEncodeMultipleMessages() {
        // 创建多个消息
        RpcRequest request = new RpcRequest();
        request.setRequestId("req-1");
        request.setInterfaceName("com.test.Service");
        request.setMethodName("test");
        
        RpcResponse response = RpcResponse.success("resp-1", "result");
        
        // 编码多个消息
        assertTrue("Should write request successfully", channel.writeOutbound(request));
        assertTrue("Should write response successfully", channel.writeOutbound(response));
        
        // 读取第一个消息
        ByteBuf encoded1 = channel.readOutbound();
        assertNotNull("First encoded buffer should not be null", encoded1);
        verifyProtocolHeader(encoded1, RpcProtocol.MessageType.REQUEST);
        encoded1.release();
        
        // 读取第二个消息
        ByteBuf encoded2 = channel.readOutbound();
        assertNotNull("Second encoded buffer should not be null", encoded2);
        verifyProtocolHeader(encoded2, RpcProtocol.MessageType.RESPONSE);
        encoded2.release();
        
        // 确保没有更多消息
        assertNull("Should have no more messages", channel.readOutbound());
    }
    
    @Test
    public void testDefaultCodecType() {
        assertEquals("Default codec type should be JSON", 
            RpcProtocol.CodecType.JSON, encoder.getDefaultCodecType());
    }
    
    @Test
    public void testSetDefaultCodecType() {
        // 设置新的默认编码类型
        encoder.setDefaultCodecType(RpcProtocol.CodecType.PROTOBUF);
        assertEquals("Default codec type should be updated", 
            RpcProtocol.CodecType.PROTOBUF, encoder.getDefaultCodecType());
        
        // 创建使用自定义编码器的通道
        EmbeddedChannel customChannel = new EmbeddedChannel(encoder);
        
        RpcRequest request = new RpcRequest();
        request.setRequestId("test-codec");
        request.setInterfaceName("com.test.Service");
        request.setMethodName("test");
        
        // 由于SerializerManager没有注册Protobuf序列化器，这里会失败
        // 编码器尝试使用PROTOBUF编码类型，但会因为未找到对应序列化器而失败
        // 这会导致编码过程中出现异常，Netty会关闭通道并抛出ClosedChannelException
        try {
            customChannel.writeOutbound(request);
            fail("Should fail due to unsupported codec type");
        } catch (Exception e) {
            // 预期的异常：当使用不支持的编码类型时，通道会因为编码异常而关闭
            // 这可能表现为ClosedChannelException或其他Netty相关异常
            assertTrue("Should throw exception when using unsupported codec type", 
                e instanceof RuntimeException || e.getClass().getSimpleName().contains("Channel"));
        }
        
        customChannel.close();
    }
    
    @Test
    public void testEncodeUnknownMessageType() {
        // 创建一个非RpcRequest/RpcResponse的对象
        String unknownMessage = "unknown message";
        
        // 编码
        assertTrue("Should write unknown message successfully", 
            channel.writeOutbound(unknownMessage));
        
        // 读取编码结果
        ByteBuf encoded = channel.readOutbound();
        assertNotNull("Encoded buffer should not be null", encoded);
        
        // 验证协议头（未知类型应该被当作REQUEST处理）
        verifyProtocolHeader(encoded, RpcProtocol.MessageType.REQUEST);
        
        encoded.release();
    }
    
    @Test
    public void testEncodeEmptyRequest() {
        // 创建空的请求对象
        RpcRequest emptyRequest = new RpcRequest();
        
        // 编码
        assertTrue("Should write empty request successfully", 
            channel.writeOutbound(emptyRequest));
        
        // 读取编码结果
        ByteBuf encoded = channel.readOutbound();
        assertNotNull("Encoded buffer should not be null", encoded);
        
        // 验证协议头
        verifyProtocolHeader(encoded, RpcProtocol.MessageType.REQUEST);
        
        // 验证消息体不为空（即使是空对象，JSON序列化后也会有内容）
        int bodyLength = getBodyLength(encoded);
        assertTrue("Body length should be greater than 0", bodyLength > 0);
        
        encoded.release();
    }
    
    @Test
    public void testProtocolHeaderFormat() {
        RpcRequest request = new RpcRequest();
        request.setRequestId("header-test");
        
        // 编码
        assertTrue("Should write request successfully", channel.writeOutbound(request));
        
        // 读取编码结果
        ByteBuf encoded = channel.readOutbound();
        assertNotNull("Encoded buffer should not be null", encoded);
        
        // 记录总字节数
        int totalBytes = encoded.readableBytes();
        
        // 验证魔数
        int magicNumber = encoded.readInt();
        assertEquals("Magic number should match", RpcProtocol.MAGIC_NUMBER, magicNumber);
        
        // 验证版本
        byte version = encoded.readByte();
        assertEquals("Version should match", RpcProtocol.VERSION, version);
        
        // 验证编码类型
        byte codecType = encoded.readByte();
        assertEquals("Codec type should be JSON", RpcProtocol.CodecType.JSON, codecType);
        
        // 验证消息类型
        byte messageType = encoded.readByte();
        assertEquals("Message type should be REQUEST", RpcProtocol.MessageType.REQUEST, messageType);
        
        // 验证消息长度
        int bodyLength = encoded.readInt();
        assertTrue("Body length should be positive", bodyLength > 0);
        
        // 验证预留字段
        byte reserved = encoded.readByte();
        assertEquals("Reserved field should be 0", 0, reserved);
        
        // 验证总字节数 = 协议头长度 + 消息体长度
        assertEquals("Total readable bytes should be header + body", 
            RpcProtocol.HEADER_LENGTH + bodyLength, totalBytes);
        
        // 验证剩余字节数等于消息体长度
        assertEquals("Remaining bytes should equal body length", 
            bodyLength, encoded.readableBytes());
        
        encoded.release();
    }
    
    /**
     * 验证协议头基本信息
     */
    private void verifyProtocolHeader(ByteBuf encoded, byte expectedMessageType) {
        assertTrue("Buffer should have at least header length", 
            encoded.readableBytes() >= RpcProtocol.HEADER_LENGTH);
        
        // 保存当前读位置
        int readerIndex = encoded.readerIndex();
        
        // 验证魔数
        int magicNumber = encoded.readInt();
        assertEquals("Magic number should match", RpcProtocol.MAGIC_NUMBER, magicNumber);
        
        // 验证版本
        byte version = encoded.readByte();
        assertEquals("Version should match", RpcProtocol.VERSION, version);
        
        // 验证编码类型
        byte codecType = encoded.readByte();
        assertEquals("Codec type should be JSON", RpcProtocol.CodecType.JSON, codecType);
        
        // 验证消息类型
        byte messageType = encoded.readByte();
        assertEquals("Message type should match", expectedMessageType, messageType);
        
        // 验证消息长度
        int bodyLength = encoded.readInt();
        assertTrue("Body length should be positive", bodyLength > 0);
        assertTrue("Body length should not exceed max frame length", 
            bodyLength <= RpcProtocol.MAX_FRAME_LENGTH);
        
        // 验证预留字段
        byte reserved = encoded.readByte();
        assertEquals("Reserved field should be 0", 0, reserved);
        
        // 恢复读位置
        encoded.readerIndex(readerIndex);
    }
    
    /**
     * 获取消息体长度
     */
    private int getBodyLength(ByteBuf encoded) {
        int readerIndex = encoded.readerIndex();
        encoded.skipBytes(8); // 跳过魔数、版本、编码类型、消息类型
        int bodyLength = encoded.readInt();
        encoded.readerIndex(readerIndex);
        return bodyLength;
    }
}