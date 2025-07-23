package com.rpcframework.rpc.protocol;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * RPC协议常量测试类
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class RpcProtocolTest {
    
    @Test
    public void testProtocolConstants() {
        // 验证魔数
        assertEquals("Magic number should be 0xCAFEBABE", 0xCAFEBABE, RpcProtocol.MAGIC_NUMBER);
        
        // 验证版本号
        assertEquals("Protocol version should be 1", 1, RpcProtocol.VERSION);
        
        // 验证协议头长度
        assertEquals("Header length should be 12 bytes", 12, RpcProtocol.HEADER_LENGTH);
        
        // 验证最大帧长度
        assertEquals("Max frame length should be 16MB", 16 * 1024 * 1024, RpcProtocol.MAX_FRAME_LENGTH);
    }
    
    @Test
    public void testCodecType() {
        // 验证序列化类型常量
        assertEquals("JSON codec type should be 0", 0, RpcProtocol.CodecType.JSON);
        assertEquals("Protobuf codec type should be 1", 1, RpcProtocol.CodecType.PROTOBUF);
        assertEquals("Hessian codec type should be 2", 2, RpcProtocol.CodecType.HESSIAN);
        assertEquals("Kryo codec type should be 3", 3, RpcProtocol.CodecType.KRYO);
    }
    
    @Test
    public void testMessageType() {
        // 验证消息类型常量
        assertEquals("Request message type should be 0", 0, RpcProtocol.MessageType.REQUEST);
        assertEquals("Response message type should be 1", 1, RpcProtocol.MessageType.RESPONSE);
        assertEquals("Heartbeat ping type should be 2", 2, RpcProtocol.MessageType.HEARTBEAT_PING);
        assertEquals("Heartbeat pong type should be 3", 3, RpcProtocol.MessageType.HEARTBEAT_PONG);
    }
    
    @Test
    public void testStatus() {
        // 验证状态码常量
        assertEquals("Success status should be 200", 200, RpcProtocol.Status.SUCCESS);
        assertEquals("Bad request status should be 400", 400, RpcProtocol.Status.BAD_REQUEST);
        assertEquals("Not found status should be 404", 404, RpcProtocol.Status.NOT_FOUND);
        assertEquals("Timeout status should be 408", 408, RpcProtocol.Status.TIMEOUT);
        assertEquals("Internal error status should be 500", 500, RpcProtocol.Status.INTERNAL_ERROR);
        assertEquals("Service unavailable status should be 503", 503, RpcProtocol.Status.SERVICE_UNAVAILABLE);
    }
    
    @Test
    public void testDefaults() {
        // 验证默认配置常量
        assertEquals("Default port should be 9090", 9090, RpcProtocol.Defaults.DEFAULT_PORT);
        assertEquals("Default connect timeout should be 5000ms", 5000, RpcProtocol.Defaults.DEFAULT_CONNECT_TIMEOUT);
        assertEquals("Default request timeout should be 30000ms", 30000, RpcProtocol.Defaults.DEFAULT_REQUEST_TIMEOUT);
        assertEquals("Default heartbeat interval should be 30000ms", 30000, RpcProtocol.Defaults.DEFAULT_HEARTBEAT_INTERVAL);
        assertEquals("Default max retry times should be 3", 3, RpcProtocol.Defaults.DEFAULT_MAX_RETRY_TIMES);
    }
    
    @Test
    public void testMagicNumberFormat() {
        // 验证魔数的十六进制表示
        String hexMagicNumber = Integer.toHexString(RpcProtocol.MAGIC_NUMBER).toUpperCase();
        assertEquals("Magic number hex should be CAFEBABE", "CAFEBABE", hexMagicNumber);
    }
    
    @Test
    public void testProtocolHeaderSize() {
        // 验证协议头大小计算
        int calculatedHeaderSize = 4 + 1 + 1 + 1 + 4 + 1; // 魔数 + 版本 + 序列化类型 + 消息类型 + 消息长度 + 预留字段
        assertEquals("Calculated header size should match constant", 
                    calculatedHeaderSize, RpcProtocol.HEADER_LENGTH);
    }
    
    @Test
    public void testMaxFrameLengthIsReasonable() {
        // 验证最大帧长度是合理的（大于0，小于Integer.MAX_VALUE）
        assertTrue("Max frame length should be positive", RpcProtocol.MAX_FRAME_LENGTH > 0);
        assertTrue("Max frame length should be less than Integer.MAX_VALUE", 
                  RpcProtocol.MAX_FRAME_LENGTH < Integer.MAX_VALUE);
        
        // 验证是16MB
        int expectedMaxFrameLength = 16 * 1024 * 1024;
        assertEquals("Max frame length should be 16MB", expectedMaxFrameLength, RpcProtocol.MAX_FRAME_LENGTH);
    }
}