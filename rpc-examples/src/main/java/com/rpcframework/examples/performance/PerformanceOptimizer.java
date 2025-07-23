package com.rpcframework.examples.performance;

import com.rpcframework.rpc.config.NetworkConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

/**
 * 性能优化器
 * 
 * <p>提供RPC框架的性能优化配置和调优建议。
 * 根据系统环境自动生成最优配置，支持不同场景的性能调优。
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class PerformanceOptimizer {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceOptimizer.class);
    
    /**
     * 创建高性能服务端配置
     * 目标：3000+ QPS，低延迟
     */
    public static NetworkConfig createHighPerformanceServerConfig() {
        logger.info("Creating high-performance server configuration...");
        
        SystemInfo systemInfo = analyzeSystemResources();
        
        NetworkConfig config = NetworkConfig.serverConfig()
            .setServerPort(8080)
            .setBossThreads(1)  // Boss线程通常1个就够
            .setWorkerThreads(systemInfo.getCpuCores() * 2)  // Worker线程数 = CPU核心数 * 2
            .setMaxConnections(Math.max(2000, systemInfo.getCpuCores() * 500))  // 根据CPU核心数调整
            .setBacklog(1024)   // 增大连接队列
            .setUseEpoll(systemInfo.isLinux())  // Linux环境启用Epoll
            .setTcpNodelay(true)  // 禁用Nagle算法，降低延迟
            .setKeepAlive(true)   // 启用Keep-Alive
            .setReuseAddr(true)   // 启用地址复用
            .setRecvBufferSize(128 * 1024)  // 128KB接收缓冲区
            .setSendBufferSize(128 * 1024)  // 128KB发送缓冲区
            .setUsePooledAllocator(true)    // 启用内存池
            .setUseDirectMemory(true)       // 启用直接内存
            .setHeartbeatInterval(30000)    // 30秒心跳
            .setHeartbeatTimeout(10000);    // 10秒心跳超时
        
        logConfigurationDetails("High-Performance Server", config, systemInfo);
        return config;
    }
    
    /**
     * 创建高性能客户端配置
     * 目标：高吞吐量，低资源消耗
     */
    public static NetworkConfig createHighPerformanceClientConfig() {
        logger.info("Creating high-performance client configuration...");
        
        SystemInfo systemInfo = analyzeSystemResources();
        
        NetworkConfig config = NetworkConfig.clientConfig()
            .setConnectTimeout(3000)        // 3秒连接超时
            .setRequestTimeout(10000)       // 10秒请求超时
            .setIoThreads(Math.max(2, systemInfo.getCpuCores() / 2))  // IO线程数
            .setMaxPoolSize(Math.max(10, systemInfo.getCpuCores() * 5))  // 连接池大小
            .setMinPoolSize(2)              // 最小连接池大小
            .setUseEpoll(systemInfo.isLinux())  // Linux环境启用Epoll
            .setTcpNodelay(true)            // 禁用Nagle算法
            .setKeepAlive(true)             // 启用Keep-Alive
            .setRecvBufferSize(128 * 1024)  // 128KB接收缓冲区
            .setSendBufferSize(128 * 1024)  // 128KB发送缓冲区
            .setUsePooledAllocator(true)    // 启用内存池
            .setUseDirectMemory(true)       // 启用直接内存
            .setMaxRetries(2)               // 最大重试次数
            .setRetryInterval(500)          // 重试间隔500ms
            .setHeartbeatInterval(30000);   // 30秒心跳
        
        logConfigurationDetails("High-Performance Client", config, systemInfo);
        return config;
    }
    
    /**
     * 创建低延迟配置
     * 目标：延迟优先，P99 < 10ms
     */
    public static NetworkConfig createLowLatencyServerConfig() {
        logger.info("Creating low-latency server configuration...");
        
        SystemInfo systemInfo = analyzeSystemResources();
        
        NetworkConfig config = NetworkConfig.serverConfig()
            .setServerPort(8080)
            .setBossThreads(1)
            .setWorkerThreads(systemInfo.getCpuCores() * 4)  // 更多Worker线程减少排队
            .setMaxConnections(1000)        // 较少连接数，确保每个连接获得更多资源
            .setBacklog(512)
            .setUseEpoll(systemInfo.isLinux())
            .setTcpNodelay(true)            // 关键：禁用Nagle算法
            .setKeepAlive(false)            // 禁用Keep-Alive减少额外开销
            .setReuseAddr(true)
            .setRecvBufferSize(64 * 1024)   // 较小缓冲区，减少内存拷贝
            .setSendBufferSize(64 * 1024)
            .setUsePooledAllocator(true)    // 内存池减少GC
            .setUseDirectMemory(true)       // 直接内存避免用户态拷贝
            .setHeartbeatInterval(60000)    // 较长心跳间隔
            .setHeartbeatTimeout(5000);     // 较短心跳超时
        
        logConfigurationDetails("Low-Latency Server", config, systemInfo);
        return config;
    }
    
    /**
     * 创建高吞吐配置
     * 目标：吞吐量优先，可容忍较高延迟
     */
    public static NetworkConfig createHighThroughputServerConfig() {
        logger.info("Creating high-throughput server configuration...");
        
        SystemInfo systemInfo = analyzeSystemResources();
        
        NetworkConfig config = NetworkConfig.serverConfig()
            .setServerPort(8080)
            .setBossThreads(2)              // 更多Boss线程处理连接
            .setWorkerThreads(systemInfo.getCpuCores() * 3)  // 更多Worker线程
            .setMaxConnections(5000)        // 更多并发连接
            .setBacklog(2048)               // 更大连接队列
            .setUseEpoll(systemInfo.isLinux())
            .setTcpNodelay(false)           // 启用Nagle算法，提高网络利用率
            .setKeepAlive(true)             // 启用Keep-Alive复用连接
            .setReuseAddr(true)
            .setRecvBufferSize(256 * 1024)  // 更大缓冲区，减少系统调用
            .setSendBufferSize(256 * 1024)
            .setUsePooledAllocator(true)
            .setUseDirectMemory(true)
            .setHeartbeatInterval(45000)    // 适中的心跳间隔
            .setHeartbeatTimeout(15000);
        
        logConfigurationDetails("High-Throughput Server", config, systemInfo);
        return config;
    }
    
    /**
     * 分析系统资源
     */
    private static SystemInfo analyzeSystemResources() {
        SystemInfo info = new SystemInfo();
        
        // CPU信息
        info.setCpuCores(Runtime.getRuntime().availableProcessors());
        
        // 内存信息
        Runtime runtime = Runtime.getRuntime();
        info.setMaxMemory(runtime.maxMemory());
        info.setTotalMemory(runtime.totalMemory());
        info.setFreeMemory(runtime.freeMemory());
        
        // 操作系统信息
        String osName = System.getProperty("os.name").toLowerCase();
        info.setLinux(osName.contains("linux"));
        info.setWindows(osName.contains("windows"));
        info.setMac(osName.contains("mac"));
        
        // 系统负载信息（如果可用）
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
            info.setSystemCpuLoad(osBean.getSystemCpuLoad());
            info.setProcessCpuLoad(osBean.getProcessCpuLoad());
            info.setTotalPhysicalMemory(osBean.getTotalPhysicalMemorySize());
            info.setFreePhysicalMemory(osBean.getFreePhysicalMemorySize());
        } catch (Exception e) {
            logger.debug("Failed to get detailed system info", e);
        }
        
        return info;
    }
    
    /**
     * 记录配置详情
     */
    private static void logConfigurationDetails(String configType, NetworkConfig config, SystemInfo systemInfo) {
        logger.info("=== {} Configuration ===", configType);
        logger.info("System Info:");
        logger.info("  CPU Cores: {}", systemInfo.getCpuCores());
        logger.info("  Max Memory: {}MB", systemInfo.getMaxMemory() / 1024 / 1024);
        logger.info("  OS: {}", systemInfo.getOsName());
        logger.info("  Linux: {}", systemInfo.isLinux());
        
        logger.info("Network Config:");
        logger.info("  Boss Threads: {}", config.getBossThreads());
        logger.info("  Worker Threads: {}", config.getWorkerThreads());
        logger.info("  Max Connections: {}", config.getMaxConnections());
        logger.info("  IO Threads: {}", config.getIoThreads());
        logger.info("  Recv Buffer: {}KB", config.getRecvBufferSize() / 1024);
        logger.info("  Send Buffer: {}KB", config.getSendBufferSize() / 1024);
        logger.info("  Use Epoll: {}", config.isUseEpoll());
        logger.info("  TCP NoDelay: {}", config.isTcpNodelay());
        logger.info("  Keep Alive: {}", config.isKeepAlive());
        logger.info("  Pooled Allocator: {}", config.isUsePooledAllocator());
        logger.info("============================");
    }
    
    /**
     * 获取性能调优建议
     */
    public static PerformanceTuningAdvice getPerformanceTuningAdvice() {
        SystemInfo systemInfo = analyzeSystemResources();
        PerformanceTuningAdvice advice = new PerformanceTuningAdvice();
        
        // JVM调优建议
        if (systemInfo.getMaxMemory() < 1024 * 1024 * 1024) { // < 1GB
            advice.addJvmAdvice("增加JVM堆内存: -Xmx2g -Xms2g");
        }
        advice.addJvmAdvice("启用G1垃圾收集器: -XX:+UseG1GC");
        advice.addJvmAdvice("优化GC参数: -XX:MaxGCPauseMillis=20");
        advice.addJvmAdvice("启用压缩指针: -XX:+UseCompressedOops");
        
        // 网络调优建议
        if (systemInfo.isLinux()) {
            advice.addNetworkAdvice("启用Epoll: config.setUseEpoll(true)");
            advice.addNetworkAdvice("调整系统参数: echo 'net.ipv4.tcp_fin_timeout = 30' >> /etc/sysctl.conf");
            advice.addNetworkAdvice("增加文件描述符限制: ulimit -n 65536");
        }
        
        // 应用层调优建议
        advice.addApplicationAdvice("使用连接池复用连接");
        advice.addApplicationAdvice("启用异步调用提高并发");
        advice.addApplicationAdvice("合理设置线程池大小: CPU密集型=CPU核心数，IO密集型=CPU核心数*2");
        
        return advice;
    }
    
    /**
     * 系统信息类
     */
    public static class SystemInfo {
        private int cpuCores;
        private long maxMemory;
        private long totalMemory;
        private long freeMemory;
        private boolean isLinux;
        private boolean isWindows;
        private boolean isMac;
        private double systemCpuLoad;
        private double processCpuLoad;
        private long totalPhysicalMemory;
        private long freePhysicalMemory;
        
        // Getters and Setters
        public int getCpuCores() { return cpuCores; }
        public void setCpuCores(int cpuCores) { this.cpuCores = cpuCores; }
        
        public long getMaxMemory() { return maxMemory; }
        public void setMaxMemory(long maxMemory) { this.maxMemory = maxMemory; }
        
        public long getTotalMemory() { return totalMemory; }
        public void setTotalMemory(long totalMemory) { this.totalMemory = totalMemory; }
        
        public long getFreeMemory() { return freeMemory; }
        public void setFreeMemory(long freeMemory) { this.freeMemory = freeMemory; }
        
        public boolean isLinux() { return isLinux; }
        public void setLinux(boolean linux) { isLinux = linux; }
        
        public boolean isWindows() { return isWindows; }
        public void setWindows(boolean windows) { isWindows = windows; }
        
        public boolean isMac() { return isMac; }
        public void setMac(boolean mac) { isMac = mac; }
        
        public double getSystemCpuLoad() { return systemCpuLoad; }
        public void setSystemCpuLoad(double systemCpuLoad) { this.systemCpuLoad = systemCpuLoad; }
        
        public double getProcessCpuLoad() { return processCpuLoad; }
        public void setProcessCpuLoad(double processCpuLoad) { this.processCpuLoad = processCpuLoad; }
        
        public long getTotalPhysicalMemory() { return totalPhysicalMemory; }
        public void setTotalPhysicalMemory(long totalPhysicalMemory) { this.totalPhysicalMemory = totalPhysicalMemory; }
        
        public long getFreePhysicalMemory() { return freePhysicalMemory; }
        public void setFreePhysicalMemory(long freePhysicalMemory) { this.freePhysicalMemory = freePhysicalMemory; }
        
        public String getOsName() {
            if (isLinux) return "Linux";
            if (isWindows) return "Windows";
            if (isMac) return "macOS";
            return "Unknown";
        }
    }
    
    /**
     * 性能调优建议类
     */
    public static class PerformanceTuningAdvice {
        private java.util.List<String> jvmAdvice = new java.util.ArrayList<>();
        private java.util.List<String> networkAdvice = new java.util.ArrayList<>();
        private java.util.List<String> applicationAdvice = new java.util.ArrayList<>();
        
        public void addJvmAdvice(String advice) {
            jvmAdvice.add(advice);
        }
        
        public void addNetworkAdvice(String advice) {
            networkAdvice.add(advice);
        }
        
        public void addApplicationAdvice(String advice) {
            applicationAdvice.add(advice);
        }
        
        public void printAdvice() {
            logger.info("\\n=== Performance Tuning Advice ===");
            
            logger.info("JVM 调优建议:");
            jvmAdvice.forEach(advice -> logger.info("  - {}", advice));
            
            logger.info("网络调优建议:");
            networkAdvice.forEach(advice -> logger.info("  - {}", advice));
            
            logger.info("应用层调优建议:");
            applicationAdvice.forEach(advice -> logger.info("  - {}", advice));
            
            logger.info("================================");
        }
        
        public java.util.List<String> getJvmAdvice() { return jvmAdvice; }
        public java.util.List<String> getNetworkAdvice() { return networkAdvice; }
        public java.util.List<String> getApplicationAdvice() { return applicationAdvice; }
    }
}