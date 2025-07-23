package com.rpcframework.rpc.model;

import java.io.Serializable;

/**
 * RPC响应模型
 * 
 * <p>封装RPC调用的响应结果，包括正常结果或异常信息。
 * 
 * @author rpc-framework
 * @since 1.0.0
 */
public class RpcResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 请求唯一标识，与RpcRequest的requestId对应
     */
    private String requestId;
    
    /**
     * 响应状态码
     * @see com.rpcframework.rpc.protocol.RpcProtocol.Status
     */
    private int status;
    
    /**
     * 响应状态描述
     */
    private String message;
    
    /**
     * 方法调用结果
     */
    private Object result;
    
    /**
     * 异常信息
     */
    private Throwable exception;
    
    /**
     * 响应时间戳
     */
    private long timestamp;
    
    /**
     * 服务处理耗时（毫秒）
     */
    private long processingTime;
    
    public RpcResponse() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public RpcResponse(String requestId) {
        this();
        this.requestId = requestId;
    }
    
    /**
     * 创建成功响应
     * 
     * @param requestId 请求ID
     * @param result 结果数据
     * @return 成功响应
     */
    public static RpcResponse success(String requestId, Object result) {
        RpcResponse response = new RpcResponse(requestId);
        response.setStatus(200); // SUCCESS
        response.setMessage("Success");
        response.setResult(result);
        return response;
    }
    
    /**
     * 创建失败响应
     * 
     * @param requestId 请求ID
     * @param status 状态码
     * @param message 错误信息
     * @return 失败响应
     */
    public static RpcResponse failure(String requestId, int status, String message) {
        RpcResponse response = new RpcResponse(requestId);
        response.setStatus(status);
        response.setMessage(message);
        return response;
    }
    
    /**
     * 创建异常响应
     * 
     * @param requestId 请求ID
     * @param exception 异常信息
     * @return 异常响应
     */
    public static RpcResponse exception(String requestId, Throwable exception) {
        RpcResponse response = new RpcResponse(requestId);
        response.setStatus(500); // INTERNAL_ERROR
        response.setMessage(exception.getMessage());
        response.setException(exception);
        return response;
    }
    
    /**
     * 判断响应是否成功
     * 
     * @return true-成功, false-失败
     */
    public boolean isSuccess() {
        return status == 200;
    }
    
    /**
     * 判断响应是否有异常
     * 
     * @return true-有异常, false-无异常
     */
    public boolean hasException() {
        return exception != null;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public int getStatus() {
        return status;
    }
    
    public void setStatus(int status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Object getResult() {
        return result;
    }
    
    public void setResult(Object result) {
        this.result = result;
    }
    
    public Throwable getException() {
        return exception;
    }
    
    public void setException(Throwable exception) {
        this.exception = exception;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public long getProcessingTime() {
        return processingTime;
    }
    
    public void setProcessingTime(long processingTime) {
        this.processingTime = processingTime;
    }
    
    @Override
    public String toString() {
        return "RpcResponse{" +
                "requestId='" + requestId + '\'' +
                ", status=" + status +
                ", message='" + message + '\'' +
                ", result=" + result +
                ", exception=" + exception +
                ", timestamp=" + timestamp +
                ", processingTime=" + processingTime +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        RpcResponse that = (RpcResponse) o;
        
        return requestId != null ? requestId.equals(that.requestId) : that.requestId == null;
    }
    
    @Override
    public int hashCode() {
        return requestId != null ? requestId.hashCode() : 0;
    }
}