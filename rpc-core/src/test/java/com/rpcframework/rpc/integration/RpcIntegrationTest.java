package com.rpcframework.rpc.integration;

import com.rpcframework.rpc.client.RpcClient;
import com.rpcframework.rpc.client.RpcFuture;
import com.rpcframework.rpc.config.NetworkConfig;
import com.rpcframework.rpc.model.RpcRequest;
import com.rpcframework.rpc.model.RpcResponse;
import com.rpcframework.rpc.server.RpcRequestHandler;
import com.rpcframework.rpc.server.RpcServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * RPC端到端集成测试
 * 
 * <p>测试完整的RPC调用链路，包括服务注册、客户端调用、
 * 服务端处理和响应返回的完整流程。
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class RpcIntegrationTest {
    
    private RpcServer server;
    private RpcClient client;
    private RpcRequestHandler requestHandler;
    
    private final String serverHost = "localhost";
    private final int serverPort = 18100;
    
    @Before
    public void setUp() throws Exception {
        // 配置服务端
        NetworkConfig serverConfig = NetworkConfig.serverConfig()
            .setServerPort(serverPort)
            .setWorkerThreads(4);
        
        // 创建请求处理器并注册服务
        requestHandler = new RpcRequestHandler();
        registerTestServices();
        
        // 启动服务端
        server = new RpcServer(serverConfig, requestHandler);
        server.start();
        
        // 等待服务端完全启动
        Thread.sleep(200);
        
        // 创建并启动客户端
        NetworkConfig clientConfig = NetworkConfig.clientConfig()
            .setConnectTimeout(3000)
            .setRequestTimeout(10000);
        
        client = new RpcClient(serverHost, serverPort, clientConfig);
        client.start();
        client.connect();
        
        // 等待客户端连接完成
        Thread.sleep(100);
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
    
    /**
     * 注册测试服务
     */
    private void registerTestServices() {
        requestHandler.registerService(UserService.class, new UserServiceImpl());
        requestHandler.registerService(CalculatorService.class, new CalculatorServiceImpl());
        requestHandler.registerService(EchoService.class, new EchoServiceImpl());
    }
    
    @Test
    public void testBasicSyncCall() throws Exception {
        // 创建RPC请求
        RpcRequest request = new RpcRequest();
        request.setInterfaceName(UserService.class.getName());
        request.setMethodName("getUserById");
        request.setParameterTypes(new Class[]{Long.class});
        request.setParameters(new Object[]{123L});
        
        // 发送同步请求
        RpcResponse response = client.sendRequest(request);
        
        // 验证响应
        assertNotNull("Response should not be null", response);
        assertTrue("Response should be successful", response.isSuccess());
        assertEquals("Request ID should match", request.getRequestId(), response.getRequestId());
        
        String result = (String) response.getResult();
        assertEquals("Result should match", "User-123", result);
        assertTrue("Processing time should be non-negative", response.getProcessingTime() >= 0);
    }
    
    @Test
    public void testBasicAsyncCall() throws Exception {
        // 创建RPC请求
        RpcRequest request = new RpcRequest();
        request.setInterfaceName(CalculatorService.class.getName());
        request.setMethodName("add");
        request.setParameterTypes(new Class[]{int.class, int.class});
        request.setParameters(new Object[]{10, 20});
        
        // 发送异步请求
        RpcFuture future = client.sendRequestAsync(request);
        
        // 等待响应
        RpcResponse response = future.get(5, TimeUnit.SECONDS);
        
        // 验证响应
        assertNotNull("Response should not be null", response);
        assertTrue("Response should be successful", response.isSuccess());
        
        Integer result = (Integer) response.getResult();
        assertEquals("Calculation result should be correct", Integer.valueOf(30), result);
    }
    
    @Test
    public void testAsyncCallWithCallback() throws Exception {
        // 创建RPC请求
        RpcRequest request = new RpcRequest();
        request.setInterfaceName(EchoService.class.getName());
        request.setMethodName("echo");
        request.setParameterTypes(new Class[]{String.class});
        request.setParameters(new Object[]{"Hello Integration Test"});
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<RpcResponse> responseRef = new AtomicReference<>();
        AtomicReference<Exception> exceptionRef = new AtomicReference<>();
        
        // 发送异步请求并设置回调
        RpcFuture future = client.sendRequestAsync(request);
        future.onSuccess(() -> {
            try {
                responseRef.set(future.getResponse());
            } catch (Exception e) {
                exceptionRef.set(e);
            } finally {
                latch.countDown();
            }
        }).onFailure(() -> {
            try {
                Throwable throwable = future.getException();
                if (throwable instanceof Exception) {
                    exceptionRef.set((Exception) throwable);
                } else {
                    exceptionRef.set(new Exception("RPC call failed", throwable));
                }
            } catch (Exception e) {
                exceptionRef.set(e);
            } finally {
                latch.countDown();
            }
        });
        
        // 等待回调执行
        assertTrue("Callback should be executed within timeout", 
            latch.await(5, TimeUnit.SECONDS));
        
        // 验证结果
        if (exceptionRef.get() != null) {
            throw new AssertionError("Async call failed", exceptionRef.get());
        }
        
        RpcResponse response = responseRef.get();
        assertNotNull("Response should not be null", response);
        assertTrue("Response should be successful", response.isSuccess());
        
        String result = (String) response.getResult();
        assertEquals("Echo result should match", "Hello Integration Test", result);
    }
    
    @Test
    public void testMethodWithException() throws Exception {
        // 创建会抛出异常的RPC请求
        RpcRequest request = new RpcRequest();
        request.setInterfaceName(CalculatorService.class.getName());
        request.setMethodName("divide");
        request.setParameterTypes(new Class[]{int.class, int.class});
        request.setParameters(new Object[]{10, 0}); // 除零异常
        
        // 发送请求
        RpcResponse response = client.sendRequest(request);
        
        // 验证异常响应
        assertNotNull("Response should not be null", response);
        assertFalse("Response should not be successful", response.isSuccess());
        assertTrue("Response should have exception", response.hasException());
        assertEquals("Status should indicate error", 500, response.getStatus());
        
        String message = response.getMessage();
        assertNotNull("Error message should not be null", message);
        assertTrue("Error message should contain exception info", 
            message.contains("ArithmeticException") || message.contains("zero"));
    }
    
    @Test
    public void testMethodNotFound() throws Exception {
        // 创建调用不存在方法的RPC请求
        RpcRequest request = new RpcRequest();
        request.setInterfaceName(UserService.class.getName());
        request.setMethodName("nonExistentMethod");
        request.setParameterTypes(new Class[]{String.class});
        request.setParameters(new Object[]{"test"});
        
        // 发送请求
        RpcResponse response = client.sendRequest(request);
        
        // 验证错误响应
        assertNotNull("Response should not be null", response);
        assertFalse("Response should not be successful", response.isSuccess());
        assertEquals("Status should indicate not found", 404, response.getStatus());
        
        String message = response.getMessage();
        assertNotNull("Error message should not be null", message);
        assertTrue("Error message should mention method not found", 
            message.toLowerCase().contains("method not found"));
    }
    
    @Test
    public void testServiceNotFound() throws Exception {
        // 创建调用不存在服务的RPC请求
        RpcRequest request = new RpcRequest();
        request.setInterfaceName("com.test.NonExistentService");
        request.setMethodName("testMethod");
        request.setParameterTypes(new Class[0]);
        request.setParameters(new Object[0]);
        
        // 发送请求
        RpcResponse response = client.sendRequest(request);
        
        // 验证错误响应
        assertNotNull("Response should not be null", response);
        assertFalse("Response should not be successful", response.isSuccess());
        assertEquals("Status should indicate not found", 404, response.getStatus());
        
        String message = response.getMessage();
        assertNotNull("Error message should not be null", message);
        assertTrue("Error message should mention service not found", 
            message.toLowerCase().contains("service not found"));
    }
    
    @Test
    public void testConcurrentRequests() throws Exception {
        int requestCount = 10;
        CountDownLatch latch = new CountDownLatch(requestCount);
        AtomicReference<Exception> exceptionRef = new AtomicReference<>();
        
        // 并发发送多个请求
        for (int i = 0; i < requestCount; i++) {
            final int index = i;
            
            new Thread(() -> {
                try {
                    RpcRequest request = new RpcRequest();
                    request.setInterfaceName(EchoService.class.getName());
                    request.setMethodName("echo");
                    request.setParameterTypes(new Class[]{String.class});
                    request.setParameters(new Object[]{"Message-" + index});
                    
                    RpcResponse response = client.sendRequest(request);
                    
                    if (!response.isSuccess()) {
                        throw new RuntimeException("Request failed: " + response.getMessage());
                    }
                    
                    String result = (String) response.getResult();
                    if (!("Message-" + index).equals(result)) {
                        throw new RuntimeException("Result mismatch: expected Message-" + 
                            index + ", got " + result);
                    }
                    
                } catch (Exception e) {
                    exceptionRef.compareAndSet(null, e);
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        // 等待所有请求完成
        assertTrue("All requests should complete within timeout", 
            latch.await(10, TimeUnit.SECONDS));
        
        // 检查是否有异常
        if (exceptionRef.get() != null) {
            throw new AssertionError("Concurrent requests failed", exceptionRef.get());
        }
    }
    
    @Test
    public void testRequestTimeout() throws Exception {
        // 创建调用慢方法的RPC请求
        RpcRequest request = new RpcRequest();
        request.setInterfaceName(EchoService.class.getName());
        request.setMethodName("slowEcho");
        request.setParameterTypes(new Class[]{String.class, long.class});
        request.setParameters(new Object[]{"slow", 3000L}); // 3秒延迟
        
        try {
            // 设置较短的超时时间
            client.sendRequest(request, 1000); // 1秒超时
            fail("Should throw timeout exception");
        } catch (Exception e) {
            assertTrue("Should be timeout exception", 
                e instanceof com.rpcframework.rpc.exception.TimeoutException || 
                e.getMessage().contains("timeout"));
        }
    }
    
    // ===================== 测试服务接口和实现 =====================
    
    public interface UserService {
        String getUserById(Long id);
        void updateUser(String name, int age);
    }
    
    public static class UserServiceImpl implements UserService {
        @Override
        public String getUserById(Long id) {
            return "User-" + id;
        }
        
        @Override
        public void updateUser(String name, int age) {
            // Mock implementation
        }
    }
    
    public interface CalculatorService {
        int add(int a, int b);
        int subtract(int a, int b);
        int multiply(int a, int b);
        int divide(int a, int b);
    }
    
    public static class CalculatorServiceImpl implements CalculatorService {
        @Override
        public int add(int a, int b) {
            return a + b;
        }
        
        @Override
        public int subtract(int a, int b) {
            return a - b;
        }
        
        @Override
        public int multiply(int a, int b) {
            return a * b;
        }
        
        @Override
        public int divide(int a, int b) {
            if (b == 0) {
                throw new ArithmeticException("Division by zero");
            }
            return a / b;
        }
    }
    
    public interface EchoService {
        String echo(String message);
        String slowEcho(String message, long delayMs);
    }
    
    public static class EchoServiceImpl implements EchoService {
        @Override
        public String echo(String message) {
            return message;
        }
        
        @Override
        public String slowEcho(String message, long delayMs) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return message;
        }
    }
}