package com.rpcframework.rpc.server;

import com.rpcframework.rpc.config.NetworkConfig;
import com.rpcframework.rpc.exception.NetworkException;
import com.rpcframework.rpc.protocol.RpcDecoder;
import com.rpcframework.rpc.protocol.RpcEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RPC服务端
 * 
 * <p>基于Netty实现的高性能RPC服务端，支持多客户端并发连接。
 * 提供完整的生命周期管理、连接监控和优雅关闭功能。
 * 
 * <p>主要特性：
 * <ul>
 * <li>高性能：基于Netty NIO，支持Epoll优化</li>
 * <li>可扩展：支持自定义请求处理器和中间件</li>
 * <li>监控友好：提供连接数统计和健康检查</li>
 * <li>生产级：支持优雅关闭和异常处理</li>
 * </ul>
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class RpcServer {
    
    private static final Logger logger = LoggerFactory.getLogger(RpcServer.class);
    
    private final NetworkConfig config;
    private final RpcRequestHandler requestHandler;
    
    private ServerBootstrap bootstrap;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    
    public RpcServer() {
        this(NetworkConfig.serverConfig());
    }
    
    public RpcServer(NetworkConfig config) {
        this(config, new RpcRequestHandler());
    }
    
    public RpcServer(NetworkConfig config, RpcRequestHandler requestHandler) {
        this.config = config;
        this.requestHandler = requestHandler;
    }
    
    /**
     * 启动服务端
     * 
     * @throws NetworkException 网络异常
     */
    public void start() throws NetworkException {
        start(config.getServerPort());
    }
    
    /**
     * 启动服务端并绑定指定端口
     * 
     * @param port 服务端口
     * @throws NetworkException 网络异常
     */
    public void start(int port) throws NetworkException {
        if (!started.compareAndSet(false, true)) {
            logger.warn("RpcServer is already started");
            return;
        }
        
        try {
            logger.info("Starting RPC server on port {}", port);
            
            // 初始化线程组
            initEventLoopGroups();
            
            // 配置服务端启动器
            bootstrap = new ServerBootstrap();
            configureBootstrap();
            
            // 绑定端口并启动
            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();
            
            logger.info("RPC server started successfully on port {}", port);
            logger.info("Server configuration: {}", config);
            
            // 注册关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "rpc-server-shutdown"));
            
        } catch (Exception e) {
            started.set(false);
            cleanup();
            throw new NetworkException("Failed to start RPC server on port " + port, e);
        }
    }
    
    /**
     * 初始化事件循环组
     */
    private void initEventLoopGroups() {
        if (config.isUseEpoll() && isEpollAvailable()) {
            logger.info("Using Epoll for better performance");
            bossGroup = new EpollEventLoopGroup(config.getBossThreads(), 
                new DefaultThreadFactory("rpc-server-boss"));
            workerGroup = new EpollEventLoopGroup(config.getWorkerThreads(), 
                new DefaultThreadFactory("rpc-server-worker"));
        } else {
            logger.info("Using NIO event loop");
            bossGroup = new NioEventLoopGroup(config.getBossThreads(), 
                new DefaultThreadFactory("rpc-server-boss"));
            workerGroup = new NioEventLoopGroup(config.getWorkerThreads(), 
                new DefaultThreadFactory("rpc-server-worker"));
        }
    }
    
    /**
     * 配置服务端启动器
     */
    private void configureBootstrap() {
        Class<? extends ServerChannel> channelClass = config.isUseEpoll() && isEpollAvailable() 
            ? EpollServerSocketChannel.class 
            : NioServerSocketChannel.class;
            
        bootstrap.group(bossGroup, workerGroup)
            .channel(channelClass)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    
                    // 空闲状态检测
                    pipeline.addLast("idleHandler", new IdleStateHandler(
                        config.getHeartbeatTimeout() / 1000, 0, 0, TimeUnit.SECONDS));
                    
                    // 协议编解码器
                    pipeline.addLast("decoder", new RpcDecoder());
                    pipeline.addLast("encoder", new RpcEncoder());
                    
                    // 连接管理
                    pipeline.addLast("connectionHandler", new ServerConnectionHandler());
                    
                    // 请求处理器
                    pipeline.addLast("requestHandler", requestHandler);
                }
            })
            .option(ChannelOption.SO_BACKLOG, config.getBacklog())
            .option(ChannelOption.SO_REUSEADDR, config.isReuseAddr())
            .childOption(ChannelOption.TCP_NODELAY, config.isTcpNodelay())
            .childOption(ChannelOption.SO_KEEPALIVE, config.isKeepAlive())
            .childOption(ChannelOption.SO_RCVBUF, config.getRecvBufferSize())
            .childOption(ChannelOption.SO_SNDBUF, config.getSendBufferSize());
            
        // 配置内存分配器
        if (config.isUsePooledAllocator()) {
            bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
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
     * 等待服务端关闭
     */
    public void waitForShutdown() {
        if (serverChannel != null) {
            try {
                serverChannel.closeFuture().sync();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while waiting for server shutdown", e);
            }
        }
    }
    
    /**
     * 优雅关闭服务端
     */
    public void shutdown() {
        if (!started.compareAndSet(true, false)) {
            return;
        }
        
        logger.info("Shutting down RPC server...");
        
        try {
            // 关闭服务端通道
            if (serverChannel != null) {
                serverChannel.close().sync();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while closing server channel", e);
        } finally {
            cleanup();
        }
        
        logger.info("RPC server shutdown completed");
    }
    
    /**
     * 清理资源
     */
    private void cleanup() {
        if (workerGroup != null) {
            workerGroup.shutdownGracefully(1, 5, TimeUnit.SECONDS);
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully(1, 5, TimeUnit.SECONDS);
        }
    }
    
    /**
     * 检查服务端是否已启动
     */
    public boolean isStarted() {
        return started.get();
    }
    
    /**
     * 获取活跃连接数
     */
    public int getActiveConnections() {
        return activeConnections.get();
    }
    
    /**
     * 获取服务端配置
     */
    public NetworkConfig getConfig() {
        return config;
    }
    
    /**
     * 获取服务端地址
     */
    public InetSocketAddress getLocalAddress() {
        if (serverChannel != null && serverChannel.isActive()) {
            return (InetSocketAddress) serverChannel.localAddress();
        }
        return null;
    }
    
    /**
     * 服务端连接管理处理器
     */
    private class ServerConnectionHandler extends ChannelInboundHandlerAdapter {
        
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            int current = activeConnections.incrementAndGet();
            InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            
            logger.debug("New client connected: {}, active connections: {}", 
                remoteAddress, current);
            
            // 检查连接数限制
            if (current > config.getMaxConnections()) {
                logger.warn("Too many connections ({}), closing new connection from {}", 
                    current, remoteAddress);
                ctx.close();
                activeConnections.decrementAndGet();
                return;
            }
            
            super.channelActive(ctx);
        }
        
        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            int current = activeConnections.decrementAndGet();
            InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            
            logger.debug("Client disconnected: {}, active connections: {}", 
                remoteAddress, current);
            
            super.channelInactive(ctx);
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            logger.error("Exception in connection from {}", remoteAddress, cause);
            ctx.close();
        }
        
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof io.netty.handler.timeout.IdleStateEvent) {
                io.netty.handler.timeout.IdleStateEvent idleEvent = 
                    (io.netty.handler.timeout.IdleStateEvent) evt;
                
                if (idleEvent.state() == io.netty.handler.timeout.IdleState.READER_IDLE) {
                    InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
                    logger.warn("Client {} idle timeout, closing connection", remoteAddress);
                    ctx.close();
                }
            }
            
            super.userEventTriggered(ctx, evt);
        }
    }
}