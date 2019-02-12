package com.ten.cim.client.service;

import com.ten.cim.client.vo.req.GroupReqVO;
import com.ten.cim.client.vo.req.P2PReqVO;

/**
 * 消息处理器
 */
public interface MsgHandle {

    /**
     * 统一的发送接口，包含了 groupChat p2pChat
     */
    void sendMsg(String msg);

    /**
     * 群聊
     *
     * @param groupReqVO 群聊消息 其中的 userId 为发送者的 userID
     */
    void groupChat(GroupReqVO groupReqVO) throws Exception;

    /**
     * 私聊
     *
     * @param p2PReqVO 私聊请求
     */
    void p2pChat(P2PReqVO p2PReqVO) throws Exception;

    // TODO: 2018/12/26 后续对消息的处理可以优化为责任链模式

    /**
     * 校验消息
     *
     * @return 不能为空，后续可以加上一些敏感词
     */
    boolean checkMsg(String msg);

    /**
     * 执行内部命令
     *
     * @return 是否应当跳过当前消息（包含了":" 就需要跳过）
     */
    boolean innerCommand(String msg);

    /**
     * 关闭系统
     */
    void shutdown();

    /**
     * 开启 AI 模式
     */
    void openAIModel();

    /**
     * 关闭 AI 模式
     */
    void closeAIModel();

}
