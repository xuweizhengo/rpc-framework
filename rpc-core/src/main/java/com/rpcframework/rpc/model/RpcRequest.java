package com.rpcframework.rpc.model;

import java.io.Serializable;
import java.util.Arrays;

/**
 * RPC请求模型
 * 
 * <p>封装RPC调用的所有信息，包括目标服务、方法、参数等。
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class RpcRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 请求唯一标识，用于请求响应匹配
     */
    private String requestId;
    
    /**
     * 目标服务接口全限定名
     */
    private String interfaceName;
    
    /**
     * 目标方法名
     */
    private String methodName;
    
    /**
     * 方法参数类型数组
     */
    private Class<?>[] parameterTypes;
    
    /**
     * 方法参数值数组
     */
    private Object[] parameters;
    
    /**
     * 服务版本号，支持服务多版本
     */
    private String version;
    
    /**
     * 服务分组，支持服务分组隔离
     */
    private String group;
    
    /**
     * 请求超时时间（毫秒）
     */
    private long timeout;
    
    /**
     * 请求时间戳
     */
    private long timestamp;
    
    public RpcRequest() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public String getInterfaceName() {
        return interfaceName;
    }
    
    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }
    
    public String getMethodName() {
        return methodName;
    }
    
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
    
    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }
    
    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }
    
    public Object[] getParameters() {
        return parameters;
    }
    
    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getGroup() {
        return group;
    }
    
    public void setGroup(String group) {
        this.group = group;
    }
    
    public long getTimeout() {
        return timeout;
    }
    
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * 获取服务的唯一标识
     * 格式：interfaceName:version:group
     * 
     * @return 服务唯一标识
     */
    public String getServiceKey() {
        StringBuilder sb = new StringBuilder();
        sb.append(interfaceName);
        if (version != null && !version.isEmpty()) {
            sb.append(":").append(version);
        }
        if (group != null && !group.isEmpty()) {
            sb.append(":").append(group);
        }
        return sb.toString();
    }
    
    /**
     * 获取方法的唯一标识
     * 格式：methodName(paramType1,paramType2,...)
     * 
     * @return 方法唯一标识
     */
    public String getMethodKey() {
        StringBuilder sb = new StringBuilder();
        sb.append(methodName).append("(");
        if (parameterTypes != null) {
            for (int i = 0; i < parameterTypes.length; i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(parameterTypes[i].getName());
            }
        }
        sb.append(")");
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return "RpcRequest{" +
                "requestId='" + requestId + '\'' +
                ", interfaceName='" + interfaceName + '\'' +
                ", methodName='" + methodName + '\'' +
                ", parameterTypes=" + Arrays.toString(parameterTypes) +
                ", parameters=" + Arrays.toString(parameters) +
                ", version='" + version + '\'' +
                ", group='" + group + '\'' +
                ", timeout=" + timeout +
                ", timestamp=" + timestamp +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        RpcRequest that = (RpcRequest) o;
        
        return requestId != null ? requestId.equals(that.requestId) : that.requestId == null;
    }
    
    @Override
    public int hashCode() {
        return requestId != null ? requestId.hashCode() : 0;
    }
}