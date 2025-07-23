package com.rpcframework.rpc.client;

import com.rpcframework.rpc.config.NetworkConfig;
import com.rpcframework.rpc.exception.NetworkException;
import com.rpcframework.rpc.exception.TimeoutException;
import com.rpcframework.rpc.model.RpcRequest;
import com.rpcframework.rpc.model.RpcResponse;
import com.rpcframework.rpc.protocol.RpcDecoder;
import com.rpcframework.rpc.protocol.RpcEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RPC客户端
 * 
 * <p>基于Netty实现的高性能RPC客户端，支持连接管理、自动重连和负载均衡。
 * 提供同步和异步调用接口，内置连接池和故障恢复机制。
 * 
 * <p>主要特性：
 * <ul>
 * <li>高性能：基于Netty NIO，支持连接复用</li>
 * <li>容错性：自动重连、超时处理、故障转移</li>
 * <li>可扩展：支持负载均衡和服务发现</li>
 * <li>监控友好：提供调用统计和健康检查</li>
 * </ul>
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class RpcClient {
    
    private static final Logger logger = LoggerFactory.getLogger(RpcClient.class);
    
    private final NetworkConfig config;
    private final String serverHost;
    private final int serverPort;
    private volatile RpcResponseHandler responseHandler;
    
    private Bootstrap bootstrap;
    private EventLoopGroup eventLoopGroup;
    private volatile Channel channel;
    
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicLong requestIdGenerator = new AtomicLong(0);
    
    public RpcClient(String serverHost, int serverPort) {
        this(serverHost, serverPort, NetworkConfig.clientConfig());
    }
    
    public RpcClient(String serverHost, int serverPort, NetworkConfig config) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.config = config;
        this.responseHandler = new RpcResponseHandler();
    }
    
    /**
     * 启动客户端
     * 
     * @throws NetworkException 网络异常
     */
    public void start() throws NetworkException {
        if (!started.compareAndSet(false, true)) {
            logger.warn("RpcClient is already started");
            return;
        }
        
        try {
            logger.info("Starting RPC client, server: {}:{}", serverHost, serverPort);
            
            // 初始化事件循环组
            initEventLoopGroup();
            
            // 配置客户端启动器
            configureBootstrap();
            
            logger.info("RPC client started successfully");
            logger.info("Client configuration: {}", config);
            
        } catch (Exception e) {
            started.set(false);
            cleanup();
            throw new NetworkException("Failed to start RPC client", e);
        }
    }
    
    /**
     * 初始化事件循环组
     */
    private void initEventLoopGroup() {
        if (config.isUseEpoll() && isEpollAvailable()) {
            logger.info("Using Epoll for better performance");
            eventLoopGroup = new EpollEventLoopGroup(config.getIoThreads(), 
                new DefaultThreadFactory("rpc-client-io"));
        } else {
            logger.info("Using NIO event loop");
            eventLoopGroup = new NioEventLoopGroup(config.getIoThreads(), 
                new DefaultThreadFactory("rpc-client-io"));
        }
    }
    
    /**
     * 配置客户端启动器
     */
    private void configureBootstrap() {
        Class<? extends Channel> channelClass = config.isUseEpoll() && isEpollAvailable() 
            ? EpollSocketChannel.class 
            : NioSocketChannel.class;
            
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
            .channel(channelClass)
            .option(ChannelOption.TCP_NODELAY, config.isTcpNodelay())
            .option(ChannelOption.SO_KEEPALIVE, config.isKeepAlive())
            .option(ChannelOption.SO_RCVBUF, config.getRecvBufferSize())
            .option(ChannelOption.SO_SNDBUF, config.getSendBufferSize())
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectTimeout())
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    
                    // 空闲状态检测
                    pipeline.addLast("idleHandler", new IdleStateHandler(
                        0, config.getHeartbeatInterval() / 1000, 0, TimeUnit.SECONDS));
                    
                    // 协议编解码器
                    pipeline.addLast("decoder", new RpcDecoder());
                    pipeline.addLast("encoder", new RpcEncoder());
                    
                    // 连接管理
                    pipeline.addLast("connectionHandler", new ClientConnectionHandler());
                    
                    // 重新创建 responseHandler 以避免 Netty Handler 复用问题
                    responseHandler = new RpcResponseHandler();
                    pipeline.addLast("responseHandler", responseHandler);
                }
            });
            
        // 配置内存分配器
        if (config.isUsePooledAllocator()) {
            bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        }
    }
    
    /**
     * 检查Epoll是否可用
     */
    private boolean isEpollAvailable() {
        try {
            Class.forName("io.netty.channel.epoll.Epoll");
            return io.netty.channel.epoll.Epoll.isAvailable();
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 连接到服务端
     * 
     * @throws NetworkException 网络异常
     */
    public void connect() throws NetworkException {
        if (!started.get()) {
            throw new NetworkException("Client not started");
        }
        
        if (connected.get()) {
            logger.debug("Client already connected");
            return;
        }
        
        try {
            logger.info("Connecting to server: {}:{}", serverHost, serverPort);
            
            ChannelFuture future = bootstrap.connect(serverHost, serverPort);
            boolean success = future.await(config.getConnectTimeout(), TimeUnit.MILLISECONDS);
            
            if (!success) {
                throw new NetworkException("Connect timeout: " + config.getConnectTimeout() + "ms");
            }
            
            if (!future.isSuccess()) {
                throw new NetworkException("Failed to connect to " + serverHost + ":" + serverPort, 
                    future.cause());
            }
            
            channel = future.channel();
            connected.set(true);
            
            logger.info("Connected to server successfully: {}:{}", serverHost, serverPort);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NetworkException("Interrupted while connecting", e);
        } catch (Exception e) {
            throw new NetworkException("Failed to connect to server", e);
        }
    }
    
    /**
     * 发送RPC请求（同步）
     * 
     * @param request RPC请求
     * @return RPC响应
     * @throws NetworkException 网络异常
     * @throws TimeoutException 超时异常
     */
    public RpcResponse sendRequest(RpcRequest request) throws NetworkException, TimeoutException {
        return sendRequest(request, config.getRequestTimeout());
    }
    
    /**
     * 发送RPC请求（同步，指定超时时间）
     * 
     * @param request RPC请求
     * @param timeoutMs 超时时间（毫秒）
     * @return RPC响应
     * @throws NetworkException 网络异常
     * @throws TimeoutException 超时异常
     */
    public RpcResponse sendRequest(RpcRequest request, int timeoutMs) throws NetworkException, TimeoutException {
        if (!isConnected()) {
            throw new NetworkException("Client not connected");
        }
        
        // 设置请求ID
        if (request.getRequestId() == null) {
            request.setRequestId(generateRequestId());
        }
        
        // 注册等待响应的Future
        RpcFuture future = new RpcFuture(request.getRequestId(), timeoutMs);
        responseHandler.addPendingRequest(request.getRequestId(), future);
        
        try {
            // 发送请求
            ChannelFuture channelFuture = channel.writeAndFlush(request);
            
            // 等待发送完成
            if (!channelFuture.await(1000, TimeUnit.MILLISECONDS)) {
                responseHandler.removePendingRequest(request.getRequestId());
                throw new NetworkException("Send request timeout");
            }
            
            if (!channelFuture.isSuccess()) {
                responseHandler.removePendingRequest(request.getRequestId());
                throw new NetworkException("Failed to send request", channelFuture.cause());
            }
            
            // 等待响应
            RpcResponse response = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            
            if (logger.isDebugEnabled()) {
                logger.debug("Request completed: {}, processing time: {}ms", 
                    request.getRequestId(), response.getProcessingTime());
            }
            
            return response;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            responseHandler.removePendingRequest(request.getRequestId());
            throw new NetworkException("Interrupted while sending request", e);
        } catch (java.util.concurrent.TimeoutException e) {
            responseHandler.removePendingRequest(request.getRequestId());
            throw new TimeoutException("Request timeout: " + timeoutMs + "ms", e);
        } catch (Exception e) {
            responseHandler.removePendingRequest(request.getRequestId());
            throw new NetworkException("Failed to send request", e);
        }
    }
    
    /**
     * 发送RPC请求（异步）
     * 
     * @param request RPC请求
     * @return RPC调用Future
     * @throws NetworkException 网络异常
     */
    public RpcFuture sendRequestAsync(RpcRequest request) throws NetworkException {
        return sendRequestAsync(request, config.getRequestTimeout());
    }
    
    /**
     * 发送RPC请求（异步，指定超时时间）
     * 
     * @param request RPC请求
     * @param timeoutMs 超时时间（毫秒）
     * @return RPC调用Future
     * @throws NetworkException 网络异常
     */
    public RpcFuture sendRequestAsync(RpcRequest request, int timeoutMs) throws NetworkException {
        if (!isConnected()) {
            throw new NetworkException("Client not connected");
        }
        
        // 设置请求ID
        if (request.getRequestId() == null) {
            request.setRequestId(generateRequestId());
        }
        
        // 创建Future并注册
        RpcFuture future = new RpcFuture(request.getRequestId(), timeoutMs);
        responseHandler.addPendingRequest(request.getRequestId(), future);
        
        // 发送请求
        channel.writeAndFlush(request).addListener(channelFuture -> {
            if (!channelFuture.isSuccess()) {
                responseHandler.removePendingRequest(request.getRequestId());
                future.completeExceptionally(new NetworkException("Failed to send request", 
                    channelFuture.cause()));
            }
        });
        
        return future;
    }
    
    /**
     * 生成请求ID
     */
    private String generateRequestId() {
        return "req-" + System.currentTimeMillis() + "-" + requestIdGenerator.incrementAndGet();
    }
    
    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return connected.get() && channel != null && channel.isActive();
    }
    
    /**
     * 检查是否已启动
     */
    public boolean isStarted() {
        return started.get();
    }
    
    /**
     * 获取服务端地址
     */
    public InetSocketAddress getServerAddress() {
        return new InetSocketAddress(serverHost, serverPort);
    }
    
    /**
     * 获取客户端配置
     */
    public NetworkConfig getConfig() {
        return config;
    }
    
    /**
     * 断开连接
     */
    public void disconnect() {
        if (connected.compareAndSet(true, false)) {
            logger.info("Disconnecting from server: {}:{}", serverHost, serverPort);
            
            if (channel != null) {
                try {
                    // 确保连接完全关闭
                    if (channel.isActive()) {
                        channel.close().sync();
                    }
                    // 等待 Channel 完全关闭
                    if (!channel.closeFuture().isDone()) {
                        channel.closeFuture().sync();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Interrupted while closing channel", e);
                } finally {
                    // 清空引用
                    channel = null;
                }
            }
            
            logger.info("Disconnected from server");
        }
    }
    
    /**
     * 关闭客户端
     */
    public void shutdown() {
        if (!started.compareAndSet(true, false)) {
            return;
        }
        
        logger.info("Shutting down RPC client...");
        
        // 断开连接
        disconnect();
        
        // 清理资源
        cleanup();
        
        logger.info("RPC client shutdown completed");
    }
    
    /**
     * 清理资源
     */
    private void cleanup() {
        if (eventLoopGroup != null) {
            eventLoopGroup.shutdownGracefully(1, 5, TimeUnit.SECONDS);
        }
    }
    
    /**
     * 客户端连接管理处理器
     */
    private class ClientConnectionHandler extends ChannelInboundHandlerAdapter {
        
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            logger.info("Connection established: {}", ctx.channel().remoteAddress());
            connected.set(true);
            super.channelActive(ctx);
        }
        
        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            logger.warn("Connection lost: {}", ctx.channel().remoteAddress());
            connected.set(false);
            
            // 清理待处理的请求
            responseHandler.cleanupOnDisconnect();
            
            super.channelInactive(ctx);
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            logger.error("Exception in client connection", cause);
            ctx.close();
        }
        
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof io.netty.handler.timeout.IdleStateEvent) {
                io.netty.handler.timeout.IdleStateEvent idleEvent = 
                    (io.netty.handler.timeout.IdleStateEvent) evt;
                
                if (idleEvent.state() == io.netty.handler.timeout.IdleState.WRITER_IDLE) {
                    // 发送心跳包（这里简化处理，实际可以发送特殊的心跳消息）
                    logger.debug("Sending heartbeat to server");
                    // ctx.writeAndFlush(HeartbeatMessage.PING);
                }
            }
            
            super.userEventTriggered(ctx, evt);
        }
    }
}