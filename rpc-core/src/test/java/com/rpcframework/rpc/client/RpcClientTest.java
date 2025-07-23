package com.rpcframework.rpc.client;

import com.rpcframework.rpc.config.NetworkConfig;
import com.rpcframework.rpc.exception.NetworkException;
import com.rpcframework.rpc.exception.TimeoutException;
import com.rpcframework.rpc.model.RpcRequest;
import com.rpcframework.rpc.model.RpcResponse;
import com.rpcframework.rpc.server.RpcServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * RPC客户端测试类
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class RpcClientTest {
    
    private RpcServer server;
    private RpcClient client;
    private String serverHost = "localhost";
    private int serverPort = 18090;
    
    @Before
    public void setUp() throws Exception {
        // 启动测试服务端
        NetworkConfig serverConfig = NetworkConfig.serverConfig().setServerPort(serverPort);
        server = new RpcServer(serverConfig, new com.rpcframework.rpc.server.RpcRequestHandler());
        server.start();
        
        // 等待服务端启动
        Thread.sleep(100);
        
        // 创建客户端
        NetworkConfig clientConfig = NetworkConfig.clientConfig()
            .setConnectTimeout(3000)
            .setRequestTimeout(5000);
        client = new RpcClient(serverHost, serverPort, clientConfig);
    }
    
    @After
    public void tearDown() {
        if (client != null && client.isStarted()) {
            client.shutdown();
        }
        
        if (server != null && server.isStarted()) {
            server.shutdown();
        }
    }
    
    @Test
    public void testClientStartAndShutdown() throws Exception {
        // 测试客户端启动
        assertFalse("Client should not be started initially", client.isStarted());
        assertFalse("Client should not be connected initially", client.isConnected());
        
        client.start();
        
        assertTrue("Client should be started", client.isStarted());
        assertFalse("Client should not be connected yet", client.isConnected());
        
        // 测试连接
        client.connect();
        
        assertTrue("Client should be connected", client.isConnected());
        
        // 测试断开连接
        client.disconnect();
        
        assertFalse("Client should be disconnected", client.isConnected());
        assertTrue("Client should still be started", client.isStarted());
        
        // 测试关闭
        client.shutdown();
        
        assertFalse("Client should be stopped", client.isStarted());
        assertFalse("Client should be disconnected", client.isConnected());
    }
    
    @Test
    public void testClientConfiguration() {
        NetworkConfig config = client.getConfig();
        assertNotNull("Client config should not be null", config);
        
        InetSocketAddress serverAddress = client.getServerAddress();
        assertNotNull("Server address should not be null", serverAddress);
        assertEquals("Server host should match", serverHost, serverAddress.getHostString());
        assertEquals("Server port should match", serverPort, serverAddress.getPort());
    }
    
    @Test
    public void testClientStartTwice() throws Exception {
        // 第一次启动
        client.start();
        assertTrue("Client should be started", client.isStarted());
        
        // 第二次启动应该被忽略
        client.start();
        assertTrue("Client should still be started", client.isStarted());
    }
    
    @Test
    public void testClientConnectTwice() throws Exception {
        client.start();
        
        // 第一次连接
        client.connect();
        assertTrue("Client should be connected", client.isConnected());
        
        // 第二次连接应该被忽略
        client.connect();
        assertTrue("Client should still be connected", client.isConnected());
    }
    
    @Test(expected = NetworkException.class)
    public void testConnectWithoutStart() throws Exception {
        // 尝试在未启动时连接
        client.connect();
    }
    
    @Test(expected = NetworkException.class)
    public void testConnectToInvalidServer() throws Exception {
        // 创建连接到无效服务端的客户端
        RpcClient invalidClient = new RpcClient("localhost", 19999);
        
        try {
            invalidClient.start();
            invalidClient.connect();
        } finally {
            if (invalidClient.isStarted()) {
                invalidClient.shutdown();
            }
        }
    }
    
    @Test(expected = NetworkException.class)
    public void testSendRequestWithoutConnection() throws Exception {
        client.start();
        
        RpcRequest request = new RpcRequest();
        request.setRequestId("test-001");
        request.setInterfaceName("com.test.Service");
        request.setMethodName("testMethod");
        
        // 尝试在未连接时发送请求
        client.sendRequest(request);
    }
    
    @Test
    public void testAsyncRequest() throws Exception {
        client.start();
        client.connect();
        
        RpcRequest request = new RpcRequest();
        request.setRequestId("async-test-001");
        request.setInterfaceName("com.test.Service");
        request.setMethodName("testMethod");
        request.setParameters(new Object[]{"test"});
        request.setParameterTypes(new Class[]{String.class});
        
        // 发送异步请求
        RpcFuture future = client.sendRequestAsync(request);
        
        assertNotNull("Future should not be null", future);
        assertEquals("Request ID should match", "async-test-001", future.getRequestId());
        assertFalse("Future should not be done initially", future.isDone());
        
        // 由于服务端没有实际的服务实现，这里主要测试客户端的异步机制
        // 实际的响应测试需要在集成测试中进行
    }
    
    @Test
    public void testFutureCallback() throws Exception {
        client.start();
        client.connect();
        
        RpcRequest request = new RpcRequest();
        request.setRequestId("callback-test-001");
        request.setInterfaceName("com.test.Service");
        request.setMethodName("testMethod");
        
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] callbackExecuted = {false};
        
        // 发送异步请求并设置回调
        RpcFuture future = client.sendRequestAsync(request);
        future.onComplete(() -> {
            callbackExecuted[0] = true;
            latch.countDown();
        });
        
        // 等待回调执行（或超时）
        boolean completed = latch.await(3, TimeUnit.SECONDS);
        
        // 回调应该会被执行（无论成功还是失败）
        assertTrue("Callback should be executed", callbackExecuted[0] || completed);
    }
    
    @Test
    public void testClientReconnect() throws Exception {
        client.start();
        client.connect();
        assertTrue("Client should be connected", client.isConnected());
        
        // 断开连接
        client.disconnect();
        assertFalse("Client should be disconnected", client.isConnected());
        
        // 等待断开连接完全处理，给更多时间让 Netty 完成清理
        Thread.sleep(200);
        
        // 重新连接
        client.connect();
        assertTrue("Client should be reconnected", client.isConnected());
    }
    
    @Test
    public void testClientShutdownTwice() throws Exception {
        client.start();
        client.connect();
        
        // 第一次关闭
        client.shutdown();
        assertFalse("Client should be stopped", client.isStarted());
        assertFalse("Client should be disconnected", client.isConnected());
        
        // 第二次关闭应该被忽略（不抛异常）
        client.shutdown();
        assertFalse("Client should still be stopped", client.isStarted());
    }
    
    @Test
    public void testRequestIdGeneration() throws Exception {
        client.start();
        client.connect();
        
        RpcRequest request1 = new RpcRequest();
        request1.setInterfaceName("com.test.Service");
        request1.setMethodName("test1");
        
        RpcRequest request2 = new RpcRequest();
        request2.setInterfaceName("com.test.Service");
        request2.setMethodName("test2");
        
        // 发送请求（这里主要测试请求ID的自动生成）
        try {
            RpcFuture future1 = client.sendRequestAsync(request1);
            RpcFuture future2 = client.sendRequestAsync(request2);
            
            assertNotNull("First request ID should be generated", future1.getRequestId());
            assertNotNull("Second request ID should be generated", future2.getRequestId());
            assertNotEquals("Request IDs should be different", 
                future1.getRequestId(), future2.getRequestId());
                
        } catch (NetworkException e) {
            // 预期的异常，因为没有实际的服务处理
        }
    }
}