package com.rpcframework.rpc.protocol;

import com.rpcframework.rpc.model.RpcRequest;
import com.rpcframework.rpc.model.RpcResponse;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.*;

/**
 * 编解码器集成测试类
 * 
 * <p>测试编码器和解码器的协同工作，验证完整的编解码流程。
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class CodecIntegrationTest {
    
    private EmbeddedChannel channel;
    
    @Before
    public void setUp() {
        // 创建包含编解码器的通道
        channel = new EmbeddedChannel(new RpcEncoder(), new RpcDecoder());
    }
    
    @Test
    public void testRpcRequestCodecRoundTrip() {
        // 创建原始请求
        RpcRequest originalRequest = new RpcRequest();
        originalRequest.setRequestId("integration-test-001");
        originalRequest.setInterfaceName("com.example.UserService");
        originalRequest.setMethodName("getUserById");
        originalRequest.setParameterTypes(new Class[]{Long.class, String.class});
        originalRequest.setParameters(new Object[]{12345L, "testUser"});
        originalRequest.setVersion("2.0.0");
        originalRequest.setGroup("production");
        originalRequest.setTimeout(10000L);
        
        // 编码 -> 解码
        assertTrue("Should encode request successfully", 
            channel.writeOutbound(originalRequest));
        
        Object encodedData = channel.readOutbound();
        assertTrue("Should decode request successfully", 
            channel.writeInbound(encodedData));
        
        // 读取解码结果
        Object decodedObject = channel.readInbound();
        assertTrue("Decoded object should be RpcRequest", decodedObject instanceof RpcRequest);
        RpcRequest decodedRequest = (RpcRequest) decodedObject;
        assertNotNull("Decoded request should not be null", decodedRequest);
        
        // 验证所有字段
        assertEquals("Request ID should match", 
            originalRequest.getRequestId(), decodedRequest.getRequestId());
        assertEquals("Interface name should match", 
            originalRequest.getInterfaceName(), decodedRequest.getInterfaceName());
        assertEquals("Method name should match", 
            originalRequest.getMethodName(), decodedRequest.getMethodName());
        assertArrayEquals("Parameter types should match", 
            originalRequest.getParameterTypes(), decodedRequest.getParameterTypes());
        assertEquals("Version should match", 
            originalRequest.getVersion(), decodedRequest.getVersion());
        assertEquals("Group should match", 
            originalRequest.getGroup(), decodedRequest.getGroup());
        assertEquals("Timeout should match", 
            originalRequest.getTimeout(), decodedRequest.getTimeout());
        
        // 验证参数值（注意：JSON反序列化可能改变数值类型）
        assertNotNull("Parameters should not be null", decodedRequest.getParameters());
        assertEquals("Parameter count should match", 
            originalRequest.getParameters().length, decodedRequest.getParameters().length);
    }
    
    @Test
    public void testRpcResponseCodecRoundTrip() {
        // 创建成功响应
        RpcResponse originalResponse = RpcResponse.success("resp-001", "Hello Integration Test");
        originalResponse.setProcessingTime(250L);
        
        // 编码 -> 解码
        assertTrue("Should encode response successfully", 
            channel.writeOutbound(originalResponse));
        
        Object encodedData = channel.readOutbound();
        assertTrue("Should decode response successfully", 
            channel.writeInbound(encodedData));
        
        // 读取解码结果
        Object decodedObject = channel.readInbound();
        assertTrue("Decoded object should be RpcResponse", decodedObject instanceof RpcResponse);
        RpcResponse decodedResponse = (RpcResponse) decodedObject;
        assertNotNull("Decoded response should not be null", decodedResponse);
        
        // 验证所有字段
        assertEquals("Request ID should match", 
            originalResponse.getRequestId(), decodedResponse.getRequestId());
        assertEquals("Status should match", 
            originalResponse.getStatus(), decodedResponse.getStatus());
        assertEquals("Message should match", 
            originalResponse.getMessage(), decodedResponse.getMessage());
        assertEquals("Result should match", 
            originalResponse.getResult(), decodedResponse.getResult());
        assertEquals("Processing time should match", 
            originalResponse.getProcessingTime(), decodedResponse.getProcessingTime());
        assertTrue("Should be success", decodedResponse.isSuccess());
        assertFalse("Should not have exception", decodedResponse.hasException());
    }
    
    @Test
    public void testExceptionResponseCodecRoundTrip() {
        // 创建异常响应
        RuntimeException originalException = new RuntimeException("Integration test exception");
        RpcResponse originalResponse = RpcResponse.exception("error-001", originalException);
        
        // 编码 -> 解码
        assertTrue("Should encode exception response successfully", 
            channel.writeOutbound(originalResponse));
        
        Object encodedData = channel.readOutbound();
        assertTrue("Should decode exception response successfully", 
            channel.writeInbound(encodedData));
        
        // 读取解码结果
        Object decodedObject = channel.readInbound();
        assertTrue("Decoded object should be RpcResponse", decodedObject instanceof RpcResponse);
        RpcResponse decodedResponse = (RpcResponse) decodedObject;
        assertNotNull("Decoded response should not be null", decodedResponse);
        
        // 验证基本字段
        assertEquals("Request ID should match", 
            originalResponse.getRequestId(), decodedResponse.getRequestId());
        assertEquals("Status should match", 
            originalResponse.getStatus(), decodedResponse.getStatus());
        assertEquals("Message should match", 
            originalResponse.getMessage(), decodedResponse.getMessage());
        assertFalse("Should not be success", decodedResponse.isSuccess());
    }
    
    @Test
    public void testComplexObjectCodecRoundTrip() {
        // 创建包含复杂对象的请求
        Map<String, Object> complexParam = new HashMap<>();
        complexParam.put("userId", 12345L);
        complexParam.put("userName", "testUser");
        complexParam.put("isActive", true);
        complexParam.put("score", 98.5);
        
        RpcRequest originalRequest = new RpcRequest();
        originalRequest.setRequestId("complex-001");
        originalRequest.setInterfaceName("com.example.UserService");
        originalRequest.setMethodName("updateUser");
        originalRequest.setParameterTypes(new Class[]{Map.class});
        originalRequest.setParameters(new Object[]{complexParam});
        
        // 编码 -> 解码
        assertTrue("Should encode complex request successfully", 
            channel.writeOutbound(originalRequest));
        
        Object encodedData = channel.readOutbound();
        assertTrue("Should decode complex request successfully", 
            channel.writeInbound(encodedData));
        
        // 读取解码结果
        Object decodedObject = channel.readInbound();
        assertTrue("Decoded object should be RpcRequest", decodedObject instanceof RpcRequest);
        RpcRequest decodedRequest = (RpcRequest) decodedObject;
        assertNotNull("Decoded request should not be null", decodedRequest);
        
        // 验证基本字段
        assertEquals("Request ID should match", 
            originalRequest.getRequestId(), decodedRequest.getRequestId());
        assertEquals("Interface name should match", 
            originalRequest.getInterfaceName(), decodedRequest.getInterfaceName());
        assertEquals("Method name should match", 
            originalRequest.getMethodName(), decodedRequest.getMethodName());
        
        // 验证复杂参数
        assertNotNull("Parameters should not be null", decodedRequest.getParameters());
        assertEquals("Should have one parameter", 1, decodedRequest.getParameters().length);
        
        Object decodedParam = decodedRequest.getParameters()[0];
        assertTrue("Parameter should be a Map", decodedParam instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> decodedMap = (Map<String, Object>) decodedParam;
        assertEquals("Map should have correct userName", "testUser", decodedMap.get("userName"));
        assertEquals("Map should have correct isActive", true, decodedMap.get("isActive"));
        assertNotNull("Map should have userId", decodedMap.get("userId"));
        assertNotNull("Map should have score", decodedMap.get("score"));
    }
    
    @Test
    public void testMultipleMessagesCodecRoundTrip() {
        // 创建多个不同类型的消息
        RpcRequest request1 = new RpcRequest();
        request1.setRequestId("multi-req-1");
        request1.setInterfaceName("com.test.Service1");
        request1.setMethodName("method1");
        
        RpcResponse response1 = RpcResponse.success("multi-resp-1", "result1");
        
        RpcRequest request2 = new RpcRequest();
        request2.setRequestId("multi-req-2");
        request2.setInterfaceName("com.test.Service2");
        request2.setMethodName("method2");
        
        RpcResponse response2 = RpcResponse.failure("multi-resp-2", 400, "Bad Request");
        
        // 依次编码所有消息
        assertTrue("Should encode request1", channel.writeOutbound(request1));
        assertTrue("Should encode response1", channel.writeOutbound(response1));
        assertTrue("Should encode request2", channel.writeOutbound(request2));
        assertTrue("Should encode response2", channel.writeOutbound(response2));
        
        // 依次读取编码后的数据并解码
        Object encodedData1 = channel.readOutbound();
        assertNotNull("First encoded data should not be null", encodedData1);
        assertTrue("Should decode first message", channel.writeInbound(encodedData1));
        
        Object encodedData2 = channel.readOutbound();
        assertNotNull("Second encoded data should not be null", encodedData2);
        assertTrue("Should decode second message", channel.writeInbound(encodedData2));
        
        Object encodedData3 = channel.readOutbound();
        assertNotNull("Third encoded data should not be null", encodedData3);
        assertTrue("Should decode third message", channel.writeInbound(encodedData3));
        
        Object encodedData4 = channel.readOutbound();
        assertNotNull("Fourth encoded data should not be null", encodedData4);
        assertTrue("Should decode fourth message", channel.writeInbound(encodedData4));
        
        // 读取并验证所有解码结果
        Object decoded1 = channel.readInbound();
        assertNotNull("First decoded object should not be null", decoded1);
        assertTrue("First decoded should be RpcRequest", decoded1 instanceof RpcRequest);
        RpcRequest decodedReq1 = (RpcRequest) decoded1;
        assertEquals("First request ID should match", "multi-req-1", decodedReq1.getRequestId());
        
        Object decoded2 = channel.readInbound();
        assertNotNull("Second decoded object should not be null", decoded2);
        assertTrue("Second decoded should be RpcResponse", decoded2 instanceof RpcResponse);
        RpcResponse decodedResp1 = (RpcResponse) decoded2;
        assertEquals("First response ID should match", "multi-resp-1", decodedResp1.getRequestId());
        assertTrue("First response should be success", decodedResp1.isSuccess());
        
        Object decoded3 = channel.readInbound();
        assertNotNull("Third decoded object should not be null", decoded3);
        assertTrue("Third decoded should be RpcRequest", decoded3 instanceof RpcRequest);
        RpcRequest decodedReq2 = (RpcRequest) decoded3;
        assertEquals("Second request ID should match", "multi-req-2", decodedReq2.getRequestId());
        
        Object decoded4 = channel.readInbound();
        assertNotNull("Fourth decoded object should not be null", decoded4);
        assertTrue("Fourth decoded should be RpcResponse", decoded4 instanceof RpcResponse);
        RpcResponse decodedResp2 = (RpcResponse) decoded4;
        assertEquals("Second response ID should match", "multi-resp-2", decodedResp2.getRequestId());
        assertFalse("Second response should not be success", decodedResp2.isSuccess());
        assertEquals("Second response status should be 400", 400, decodedResp2.getStatus());
        
        // 确保没有更多消息
        assertNull("Should have no more messages", channel.readInbound());
    }
    
    @Test
    public void testLargeMessageCodecRoundTrip() {
        // 创建大消息（但在限制范围内）
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeContent.append("This is line ").append(i).append(" of large content. ");
        }
        
        RpcResponse originalResponse = RpcResponse.success("large-001", largeContent.toString());
        
        // 编码 -> 解码
        assertTrue("Should encode large response successfully", 
            channel.writeOutbound(originalResponse));
        
        Object encodedData = channel.readOutbound();
        assertTrue("Should decode large response successfully", 
            channel.writeInbound(encodedData));
        
        // 读取解码结果
        Object decodedObject = channel.readInbound();
        assertTrue("Decoded object should be RpcResponse", decodedObject instanceof RpcResponse);
        RpcResponse decodedResponse = (RpcResponse) decodedObject;
        assertNotNull("Decoded response should not be null", decodedResponse);
        
        // 验证大内容
        assertEquals("Request ID should match", 
            originalResponse.getRequestId(), decodedResponse.getRequestId());
        assertEquals("Large content should match", 
            originalResponse.getResult(), decodedResponse.getResult());
        assertTrue("Content should be large", 
            decodedResponse.getResult().toString().length() > 100000);
    }
    
    @Test
    public void testEmptyObjectsCodecRoundTrip() {
        // 测试空对象的编解码
        RpcRequest emptyRequest = new RpcRequest();
        RpcResponse emptyResponse = new RpcResponse();
        
        // 编码 -> 解码空请求
        assertTrue("Should encode empty request", channel.writeOutbound(emptyRequest));
        
        Object encodedRequest = channel.readOutbound();
        assertTrue("Should decode empty request", 
            channel.writeInbound(encodedRequest));
        
        Object decodedReqObject = channel.readInbound();
        assertTrue("Decoded should be RpcRequest", decodedReqObject instanceof RpcRequest);
        RpcRequest decodedRequest = (RpcRequest) decodedReqObject;
        assertNotNull("Decoded empty request should not be null", decodedRequest);
        
        // 编码 -> 解码空响应
        assertTrue("Should encode empty response", channel.writeOutbound(emptyResponse));
        
        Object encodedResponse = channel.readOutbound();
        assertTrue("Should decode empty response", 
            channel.writeInbound(encodedResponse));
        
        Object decodedRespObject = channel.readInbound();
        assertTrue("Decoded should be RpcResponse", decodedRespObject instanceof RpcResponse);
        RpcResponse decodedResponse = (RpcResponse) decodedRespObject;
        assertNotNull("Decoded empty response should not be null", decodedResponse);
    }
    
    @Test
    public void testCodecConsistencyUnderLoad() {
        // 压力测试：多次编解码相同对象，验证一致性
        RpcRequest template = new RpcRequest();
        template.setRequestId("consistency-test");
        template.setInterfaceName("com.test.Service");
        template.setMethodName("testMethod");
        template.setParameterTypes(new Class[]{String.class, Integer.class});
        template.setParameters(new Object[]{"test", 42});
        
        int iterations = 100;
        for (int i = 0; i < iterations; i++) {
            // 创建新的请求对象（避免对象重用）
            RpcRequest request = new RpcRequest();
            request.setRequestId(template.getRequestId() + "-" + i);
            request.setInterfaceName(template.getInterfaceName());
            request.setMethodName(template.getMethodName());
            request.setParameterTypes(template.getParameterTypes());
            request.setParameters(template.getParameters());
            
            // 编码 -> 解码
            assertTrue("Should encode request " + i, channel.writeOutbound(request));
            
            Object encodedData = channel.readOutbound();
            assertTrue("Should decode request " + i, 
                channel.writeInbound(encodedData));
            
            Object decodedObject = channel.readInbound();
            assertTrue("Decoded should be RpcRequest for iteration " + i, decodedObject instanceof RpcRequest);
            RpcRequest decoded = (RpcRequest) decodedObject;
            assertNotNull("Decoded request " + i + " should not be null", decoded);
            assertEquals("Request ID should match for iteration " + i, 
                request.getRequestId(), decoded.getRequestId());
            assertEquals("Interface name should be consistent for iteration " + i, 
                template.getInterfaceName(), decoded.getInterfaceName());
            assertEquals("Method name should be consistent for iteration " + i, 
                template.getMethodName(), decoded.getMethodName());
        }
    }
}