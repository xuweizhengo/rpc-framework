package com.rpcframework.rpc.server;

import com.rpcframework.rpc.config.NetworkConfig;
import com.rpcframework.rpc.exception.NetworkException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * RPC服务端测试类
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class RpcServerTest {
    
    private RpcServer server;
    private NetworkConfig config;
    private int testPort = 18080;
    
    @Before
    public void setUp() {
        config = NetworkConfig.serverConfig().setServerPort(testPort);
        server = new RpcServer(config);
    }
    
    @After
    public void tearDown() {
        if (server != null && server.isStarted()) {
            server.shutdown();
        }
    }
    
    @Test
    public void testServerStartAndShutdown() throws Exception {
        // 测试服务端启动
        assertFalse("Server should not be started initially", server.isStarted());
        assertEquals("Active connections should be 0", 0, server.getActiveConnections());
        
        server.start();
        
        assertTrue("Server should be started", server.isStarted());
        assertNotNull("Local address should not be null", server.getLocalAddress());
        assertEquals("Server port should match", testPort, server.getLocalAddress().getPort());
        
        // 等待服务端完全启动
        Thread.sleep(100);
        
        // 测试服务端关闭
        server.shutdown();
        
        assertFalse("Server should be stopped", server.isStarted());
    }
    
    @Test
    public void testServerStartTwice() throws Exception {
        // 第一次启动
        server.start();
        assertTrue("Server should be started", server.isStarted());
        
        // 第二次启动应该被忽略
        server.start();
        assertTrue("Server should still be started", server.isStarted());
    }
    
    @Test
    public void testServerStartWithSpecificPort() throws Exception {
        int customPort = 18081;
        
        server.start(customPort);
        
        assertTrue("Server should be started", server.isStarted());
        InetSocketAddress address = server.getLocalAddress();
        assertNotNull("Local address should not be null", address);
        assertEquals("Port should match custom port", customPort, address.getPort());
    }
    
    @Test(expected = NetworkException.class)
    public void testServerStartWithOccupiedPort() throws Exception {
        // 启动第一个服务端
        server.start();
        assertTrue("First server should be started", server.isStarted());
        
        // 尝试在相同端口启动第二个服务端
        RpcServer secondServer = new RpcServer(config);
        try {
            secondServer.start();
        } finally {
            if (secondServer.isStarted()) {
                secondServer.shutdown();
            }
        }
    }
    
    @Test
    public void testServerConfiguration() {
        NetworkConfig serverConfig = server.getConfig();
        assertNotNull("Server config should not be null", serverConfig);
        assertEquals("Config should match", config, serverConfig);
    }
    
    @Test
    public void testServerRequestHandler() throws Exception {
        server.start();
        
        // 测试服务注册
        RpcRequestHandler handler = new RpcRequestHandler();
        TestService testService = new TestServiceImpl();
        handler.registerService(TestService.class, testService);
        
        assertTrue("Service should be registered", 
            handler.isServiceRegistered(TestService.class.getName()));
        assertEquals("Service count should be 1", 1, handler.getServiceCount());
        
        // 测试服务注销
        handler.unregisterService(TestService.class);
        assertFalse("Service should be unregistered", 
            handler.isServiceRegistered(TestService.class.getName()));
        assertEquals("Service count should be 0", 0, handler.getServiceCount());
    }
    
    @Test
    public void testServerShutdownTwice() throws Exception {
        server.start();
        assertTrue("Server should be started", server.isStarted());
        
        // 第一次关闭
        server.shutdown();
        assertFalse("Server should be stopped", server.isStarted());
        
        // 第二次关闭应该被忽略（不抛异常）
        server.shutdown();
        assertFalse("Server should still be stopped", server.isStarted());
    }
    
    @Test
    public void testServerWithCustomConfig() throws Exception {
        NetworkConfig customConfig = new NetworkConfig()
            .setServerPort(18082)
            .setBossThreads(2)
            .setWorkerThreads(8)
            .setMaxConnections(100);
        
        RpcServer customServer = new RpcServer(customConfig);
        
        try {
            customServer.start();
            
            assertTrue("Custom server should be started", customServer.isStarted());
            assertEquals("Custom config should match", 
                customConfig, customServer.getConfig());
            
        } finally {
            if (customServer.isStarted()) {
                customServer.shutdown();
            }
        }
    }
    
    @Test
    public void testServerActiveConnections() throws Exception {
        server.start();
        
        // 初始连接数应该为0
        assertEquals("Initial active connections should be 0", 
            0, server.getActiveConnections());
        
        // 这里可以添加客户端连接测试，但需要更复杂的设置
        // 暂时只测试初始状态
    }
    
    // 测试服务接口
    public interface TestService {
        String sayHello(String name);
    }
    
    // 测试服务实现
    public static class TestServiceImpl implements TestService {
        @Override
        public String sayHello(String name) {
            return "Hello, " + name;
        }
    }
}