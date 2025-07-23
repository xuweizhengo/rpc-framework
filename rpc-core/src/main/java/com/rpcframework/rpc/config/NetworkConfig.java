package com.rpcframework.rpc.config;

/**
 * 网络配置类
 * 
 * <p>统一管理RPC框架的网络相关配置参数，包括服务端和客户端的网络设置。
 * 提供默认配置和可定制的配置选项，支持生产环境的性能优化。
 * 
 * <p>主要配置项：
 * <ul>
 * <li>服务端配置：端口、线程池、连接参数</li>
 * <li>客户端配置：连接超时、重试策略、连接池</li>
 * <li>网络参数：缓冲区大小、Keep-Alive设置</li>
 * <li>性能优化：零拷贝、内存池等</li>
 * </ul>
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class NetworkConfig {
    
    // ==================== 服务端配置 ====================
    
    /**
     * 默认服务端端口
     */
    public static final int DEFAULT_SERVER_PORT = 8080;
    
    /**
     * 服务端Boss线程数（处理连接接受）
     */
    public static final int DEFAULT_BOSS_THREADS = 1;
    
    /**
     * 服务端Worker线程数（处理I/O操作）
     */
    public static final int DEFAULT_WORKER_THREADS = Runtime.getRuntime().availableProcessors() * 2;
    
    /**
     * 服务端最大连接数
     */
    public static final int DEFAULT_MAX_CONNECTIONS = 1000;
    
    // ==================== 客户端配置 ====================
    
    /**
     * 客户端连接超时时间（毫秒）
     */
    public static final int DEFAULT_CONNECT_TIMEOUT = 5000;
    
    /**
     * 客户端请求超时时间（毫秒）
     */
    public static final int DEFAULT_REQUEST_TIMEOUT = 30000;
    
    /**
     * 客户端I/O线程数
     */
    public static final int DEFAULT_IO_THREADS = Runtime.getRuntime().availableProcessors();
    
    /**
     * 连接池最大连接数
     */
    public static final int DEFAULT_MAX_POOL_SIZE = 10;
    
    /**
     * 连接池最小空闲连接数
     */
    public static final int DEFAULT_MIN_POOL_SIZE = 2;
    
    // ==================== 网络参数配置 ====================
    
    /**
     * TCP接收缓冲区大小
     */
    public static final int DEFAULT_RECV_BUFFER_SIZE = 64 * 1024; // 64KB
    
    /**
     * TCP发送缓冲区大小
     */
    public static final int DEFAULT_SEND_BUFFER_SIZE = 64 * 1024; // 64KB
    
    /**
     * SO_BACKLOG参数，服务端监听队列大小
     */
    public static final int DEFAULT_BACKLOG = 128;
    
    /**
     * 是否启用TCP_NODELAY（禁用Nagle算法）
     */
    public static final boolean DEFAULT_TCP_NODELAY = true;
    
    /**
     * 是否启用SO_KEEPALIVE
     */
    public static final boolean DEFAULT_KEEP_ALIVE = true;
    
    /**
     * 是否启用SO_REUSEADDR
     */
    public static final boolean DEFAULT_REUSE_ADDR = true;
    
    // ==================== 重试和恢复配置 ====================
    
    /**
     * 最大重试次数
     */
    public static final int DEFAULT_MAX_RETRIES = 3;
    
    /**
     * 重试间隔（毫秒）
     */
    public static final int DEFAULT_RETRY_INTERVAL = 1000;
    
    /**
     * 心跳间隔（毫秒）
     */
    public static final int DEFAULT_HEARTBEAT_INTERVAL = 30000;
    
    /**
     * 心跳超时时间（毫秒）
     */
    public static final int DEFAULT_HEARTBEAT_TIMEOUT = 10000;
    
    // ==================== 性能优化配置 ====================
    
    /**
     * 是否启用直接内存
     */
    public static final boolean DEFAULT_USE_DIRECT_MEMORY = true;
    
    /**
     * 是否启用内存池
     */
    public static final boolean DEFAULT_USE_POOLED_ALLOCATOR = true;
    
    /**
     * 是否启用Epoll（Linux环境下）
     */
    public static final boolean DEFAULT_USE_EPOLL = isLinux();
    
    // ==================== 实例配置 ====================
    
    private int serverPort = DEFAULT_SERVER_PORT;
    private int bossThreads = DEFAULT_BOSS_THREADS;
    private int workerThreads = DEFAULT_WORKER_THREADS;
    private int maxConnections = DEFAULT_MAX_CONNECTIONS;
    
    private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    private int requestTimeout = DEFAULT_REQUEST_TIMEOUT;
    private int ioThreads = DEFAULT_IO_THREADS;
    private int maxPoolSize = DEFAULT_MAX_POOL_SIZE;
    private int minPoolSize = DEFAULT_MIN_POOL_SIZE;
    
    private int recvBufferSize = DEFAULT_RECV_BUFFER_SIZE;
    private int sendBufferSize = DEFAULT_SEND_BUFFER_SIZE;
    private int backlog = DEFAULT_BACKLOG;
    private boolean tcpNodelay = DEFAULT_TCP_NODELAY;
    private boolean keepAlive = DEFAULT_KEEP_ALIVE;
    private boolean reuseAddr = DEFAULT_REUSE_ADDR;
    
    private int maxRetries = DEFAULT_MAX_RETRIES;
    private int retryInterval = DEFAULT_RETRY_INTERVAL;
    private int heartbeatInterval = DEFAULT_HEARTBEAT_INTERVAL;
    private int heartbeatTimeout = DEFAULT_HEARTBEAT_TIMEOUT;
    
    private boolean useDirectMemory = DEFAULT_USE_DIRECT_MEMORY;
    private boolean usePooledAllocator = DEFAULT_USE_POOLED_ALLOCATOR;
    private boolean useEpoll = DEFAULT_USE_EPOLL;
    
    /**
     * 检测是否为Linux环境
     */
    private static boolean isLinux() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("linux");
    }
    
    // ==================== Getter and Setter Methods ====================
    
    public int getServerPort() {
        return serverPort;
    }
    
    public NetworkConfig setServerPort(int serverPort) {
        this.serverPort = serverPort;
        return this;
    }
    
    public int getBossThreads() {
        return bossThreads;
    }
    
    public NetworkConfig setBossThreads(int bossThreads) {
        this.bossThreads = bossThreads;
        return this;
    }
    
    public int getWorkerThreads() {
        return workerThreads;
    }
    
    public NetworkConfig setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
        return this;
    }
    
    public int getMaxConnections() {
        return maxConnections;
    }
    
    public NetworkConfig setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
        return this;
    }
    
    public int getConnectTimeout() {
        return connectTimeout;
    }
    
    public NetworkConfig setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }
    
    public int getRequestTimeout() {
        return requestTimeout;
    }
    
    public NetworkConfig setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
        return this;
    }
    
    public int getIoThreads() {
        return ioThreads;
    }
    
    public NetworkConfig setIoThreads(int ioThreads) {
        this.ioThreads = ioThreads;
        return this;
    }
    
    public int getMaxPoolSize() {
        return maxPoolSize;
    }
    
    public NetworkConfig setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
        return this;
    }
    
    public int getMinPoolSize() {
        return minPoolSize;
    }
    
    public NetworkConfig setMinPoolSize(int minPoolSize) {
        this.minPoolSize = minPoolSize;
        return this;
    }
    
    public int getRecvBufferSize() {
        return recvBufferSize;
    }
    
    public NetworkConfig setRecvBufferSize(int recvBufferSize) {
        this.recvBufferSize = recvBufferSize;
        return this;
    }
    
    public int getSendBufferSize() {
        return sendBufferSize;
    }
    
    public NetworkConfig setSendBufferSize(int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
        return this;
    }
    
    public int getBacklog() {
        return backlog;
    }
    
    public NetworkConfig setBacklog(int backlog) {
        this.backlog = backlog;
        return this;
    }
    
    public boolean isTcpNodelay() {
        return tcpNodelay;
    }
    
    public NetworkConfig setTcpNodelay(boolean tcpNodelay) {
        this.tcpNodelay = tcpNodelay;
        return this;
    }
    
    public boolean isKeepAlive() {
        return keepAlive;
    }
    
    public NetworkConfig setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
        return this;
    }
    
    public boolean isReuseAddr() {
        return reuseAddr;
    }
    
    public NetworkConfig setReuseAddr(boolean reuseAddr) {
        this.reuseAddr = reuseAddr;
        return this;
    }
    
    public int getMaxRetries() {
        return maxRetries;
    }
    
    public NetworkConfig setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
        return this;
    }
    
    public int getRetryInterval() {
        return retryInterval;
    }
    
    public NetworkConfig setRetryInterval(int retryInterval) {
        this.retryInterval = retryInterval;
        return this;
    }
    
    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }
    
    public NetworkConfig setHeartbeatInterval(int heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
        return this;
    }
    
    public int getHeartbeatTimeout() {
        return heartbeatTimeout;
    }
    
    public NetworkConfig setHeartbeatTimeout(int heartbeatTimeout) {
        this.heartbeatTimeout = heartbeatTimeout;
        return this;
    }
    
    public boolean isUseDirectMemory() {
        return useDirectMemory;
    }
    
    public NetworkConfig setUseDirectMemory(boolean useDirectMemory) {
        this.useDirectMemory = useDirectMemory;
        return this;
    }
    
    public boolean isUsePooledAllocator() {
        return usePooledAllocator;
    }
    
    public NetworkConfig setUsePooledAllocator(boolean usePooledAllocator) {
        this.usePooledAllocator = usePooledAllocator;
        return this;
    }
    
    public boolean isUseEpoll() {
        return useEpoll;
    }
    
    public NetworkConfig setUseEpoll(boolean useEpoll) {
        this.useEpoll = useEpoll;
        return this;
    }
    
    /**
     * 创建默认网络配置
     */
    public static NetworkConfig defaultConfig() {
        return new NetworkConfig();
    }
    
    /**
     * 创建服务端优化配置
     */
    public static NetworkConfig serverConfig() {
        NetworkConfig config = new NetworkConfig();
        config.setWorkerThreads(Runtime.getRuntime().availableProcessors() * 4);
        config.setMaxConnections(5000);
        config.setBacklog(256);
        return config;
    }
    
    /**
     * 创建客户端优化配置
     */
    public static NetworkConfig clientConfig() {
        NetworkConfig config = new NetworkConfig();
        config.setIoThreads(Runtime.getRuntime().availableProcessors());
        config.setMaxPoolSize(20);
        config.setConnectTimeout(3000);
        return config;
    }
    
    @Override
    public String toString() {
        return "NetworkConfig{" +
                "serverPort=" + serverPort +
                ", bossThreads=" + bossThreads +
                ", workerThreads=" + workerThreads +
                ", maxConnections=" + maxConnections +
                ", connectTimeout=" + connectTimeout +
                ", requestTimeout=" + requestTimeout +
                ", ioThreads=" + ioThreads +
                ", maxPoolSize=" + maxPoolSize +
                ", minPoolSize=" + minPoolSize +
                ", recvBufferSize=" + recvBufferSize +
                ", sendBufferSize=" + sendBufferSize +
                ", backlog=" + backlog +
                ", tcpNodelay=" + tcpNodelay +
                ", keepAlive=" + keepAlive +
                ", reuseAddr=" + reuseAddr +
                ", maxRetries=" + maxRetries +
                ", retryInterval=" + retryInterval +
                ", heartbeatInterval=" + heartbeatInterval +
                ", heartbeatTimeout=" + heartbeatTimeout +
                ", useDirectMemory=" + useDirectMemory +
                ", usePooledAllocator=" + usePooledAllocator +
                ", useEpoll=" + useEpoll +
                '}';
    }
}