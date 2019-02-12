package com.ten.cim.client.service;

/**
 *
 */
public interface MsgLogger {

    /**
     * 异步写入消息
     */
    void log(String msg);

    /**
     * 停止写入
     */
    void stop();

    /**
     * 查询聊天记录
     *
     * @param key 关键字
l     */
    String query(String key);
}
