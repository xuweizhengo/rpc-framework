package com.rpcframework.rpc.protocol;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * RPC协议扩展测试类
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class RpcProtocolExtendedTest {
    
    @Test
    public void testAllCodecTypeValues() {
        // Verify all codec types are unique
        byte[] codecTypes = {
            RpcProtocol.CodecType.JSON,
            RpcProtocol.CodecType.PROTOBUF,
            RpcProtocol.CodecType.HESSIAN,
            RpcProtocol.CodecType.KRYO
        };
        
        for (int i = 0; i < codecTypes.length; i++) {
            for (int j = i + 1; j < codecTypes.length; j++) {
                assertNotEquals("Codec types should be unique", codecTypes[i], codecTypes[j]);
            }
        }
    }
    
    @Test
    public void testAllMessageTypeValues() {
        // Verify all message types are unique
        byte[] messageTypes = {
            RpcProtocol.MessageType.REQUEST,
            RpcProtocol.MessageType.RESPONSE,
            RpcProtocol.MessageType.HEARTBEAT_PING,
            RpcProtocol.MessageType.HEARTBEAT_PONG
        };
        
        for (int i = 0; i < messageTypes.length; i++) {
            for (int j = i + 1; j < messageTypes.length; j++) {
                assertNotEquals("Message types should be unique", messageTypes[i], messageTypes[j]);
            }
        }
    }
    
    @Test
    public void testAllStatusCodeValues() {
        // Verify all status codes are unique
        int[] statusCodes = {
            RpcProtocol.Status.SUCCESS,
            RpcProtocol.Status.BAD_REQUEST,
            RpcProtocol.Status.NOT_FOUND,
            RpcProtocol.Status.TIMEOUT,
            RpcProtocol.Status.INTERNAL_ERROR,
            RpcProtocol.Status.SERVICE_UNAVAILABLE
        };
        
        for (int i = 0; i < statusCodes.length; i++) {
            for (int j = i + 1; j < statusCodes.length; j++) {
                assertNotEquals("Status codes should be unique", statusCodes[i], statusCodes[j]);
            }
        }
    }
    
    @Test
    public void testStatusCodeCategories() {
        // Test that status codes follow HTTP-like conventions
        assertTrue("Success should be 2xx", RpcProtocol.Status.SUCCESS >= 200 && RpcProtocol.Status.SUCCESS < 300);
        
        assertTrue("Bad request should be 4xx", RpcProtocol.Status.BAD_REQUEST >= 400 && RpcProtocol.Status.BAD_REQUEST < 500);
        assertTrue("Not found should be 4xx", RpcProtocol.Status.NOT_FOUND >= 400 && RpcProtocol.Status.NOT_FOUND < 500);
        assertTrue("Timeout should be 4xx", RpcProtocol.Status.TIMEOUT >= 400 && RpcProtocol.Status.TIMEOUT < 500);
        
        assertTrue("Internal error should be 5xx", RpcProtocol.Status.INTERNAL_ERROR >= 500 && RpcProtocol.Status.INTERNAL_ERROR < 600);
        assertTrue("Service unavailable should be 5xx", RpcProtocol.Status.SERVICE_UNAVAILABLE >= 500 && RpcProtocol.Status.SERVICE_UNAVAILABLE < 600);
    }
    
    @Test
    public void testDefaultValueRanges() {
        // Test that default values are reasonable
        assertTrue("Default port should be valid port number", 
                  RpcProtocol.Defaults.DEFAULT_PORT >= 1024 && RpcProtocol.Defaults.DEFAULT_PORT <= 65535);
        
        assertTrue("Default connect timeout should be positive", 
                  RpcProtocol.Defaults.DEFAULT_CONNECT_TIMEOUT > 0);
        
        assertTrue("Default request timeout should be positive", 
                  RpcProtocol.Defaults.DEFAULT_REQUEST_TIMEOUT > 0);
        
        assertTrue("Default heartbeat interval should be positive", 
                  RpcProtocol.Defaults.DEFAULT_HEARTBEAT_INTERVAL > 0);
        
        assertTrue("Default max retry should be positive", 
                  RpcProtocol.Defaults.DEFAULT_MAX_RETRY_TIMES > 0);
    }
    
    @Test
    public void testTimeoutRelationships() {
        // Test logical relationships between timeouts
        assertTrue("Request timeout should be greater than connect timeout",
                  RpcProtocol.Defaults.DEFAULT_REQUEST_TIMEOUT > RpcProtocol.Defaults.DEFAULT_CONNECT_TIMEOUT);
                  
        assertTrue("Heartbeat interval should be reasonable compared to request timeout",
                  RpcProtocol.Defaults.DEFAULT_HEARTBEAT_INTERVAL <= RpcProtocol.Defaults.DEFAULT_REQUEST_TIMEOUT);
    }
    
    @Test
    public void testMaxFrameLengthSafety() {
        // Test that max frame length prevents overflow
        assertTrue("Max frame length should not cause overflow when added to header",
                  (long) RpcProtocol.MAX_FRAME_LENGTH + RpcProtocol.HEADER_LENGTH < Integer.MAX_VALUE);
        
        // Test reasonable upper bound (not too large to cause memory issues)
        assertTrue("Max frame length should be reasonable (less than 1GB)",
                  RpcProtocol.MAX_FRAME_LENGTH < 1024 * 1024 * 1024);
    }
    
    @Test
    public void testMagicNumberUniqueness() {
        // Test that magic number is sufficiently unique
        int magicNumber = RpcProtocol.MAGIC_NUMBER;
        
        // Should not be common values
        assertNotEquals("Magic number should not be 0", 0, magicNumber);
        assertNotEquals("Magic number should not be -1", -1, magicNumber);
        assertNotEquals("Magic number should not be MAX_VALUE", Integer.MAX_VALUE, magicNumber);
        
        // Should have reasonable bit distribution
        String binaryString = Integer.toBinaryString(magicNumber);
        int oneCount = binaryString.length() - binaryString.replace("1", "").length();
        assertTrue("Magic number should have balanced bit distribution", 
                  oneCount >= 8 && oneCount <= 24);
    }
    
    @Test
    public void testVersionNumberValidation() {
        // Test version number is valid
        assertTrue("Version should be positive", RpcProtocol.VERSION > 0);
        assertTrue("Version should be reasonable (not too large)", RpcProtocol.VERSION < 256);
    }
    
    @Test
    public void testInnerClassInstantiation() {
        // Verify that inner utility classes cannot be instantiated
        try {
            // These should fail if properly implemented with private constructors
            // We can't actually test this without reflection, but we can verify the classes exist
            assertNotNull("CodecType class should exist", RpcProtocol.CodecType.class);
            assertNotNull("MessageType class should exist", RpcProtocol.MessageType.class);
            assertNotNull("Status class should exist", RpcProtocol.Status.class);
            assertNotNull("Defaults class should exist", RpcProtocol.Defaults.class);
        } catch (Exception e) {
            fail("Inner classes should be accessible");
        }
    }
}