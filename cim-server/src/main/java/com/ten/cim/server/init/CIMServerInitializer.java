package com.ten.cim.server.init;

import com.ten.cim.common.protocol.CIMRequestProto;
import com.ten.cim.server.handle.CIMServerHandle;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * CIM 服务端 Netty Channel Pipeline 初始化器。
 *
 * <p>Pipeline 处理器顺序（入站从上到下，出站从下到上）：
 * <ol>
 *   <li>{@link IdleStateHandler}：{@code readerIdleTime=11s}，超时触发 READER_IDLE 事件，
 *       由 CIMServerHandle 检测客户端心跳存活状态。</li>
 *   <li>{@link ProtobufVarint32FrameDecoder}：处理 TCP 粘包/拆包问题，按帧长度字段切分数据包。</li>
 *   <li>{@link ProtobufDecoder}：将字节流反序列化为 {@link CIMRequestProto.CIMReqProtocol} 对象。</li>
 *   <li>{@link ProtobufVarint32LengthFieldPrepender}：出站时在消息头部添加变长帧长度字段。</li>
 *   <li>{@link ProtobufEncoder}：将出站 Protobuf 消息序列化为字节流。</li>
 *   <li>{@link CIMServerHandle}：业务逻辑处理器，处理登录、心跳、掉线事件。</li>
 * </ol>
 */
public class CIMServerInitializer extends ChannelInitializer<Channel> {

    private final CIMServerHandle cimServerHandle = new CIMServerHandle();

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ch.pipeline()
                // 11 秒没有向客户端发送消息就发生心跳
                .addLast(new IdleStateHandler(11, 0, 0))
                // google Protobuf 编解码
                .addLast(new ProtobufVarint32FrameDecoder())
                .addLast(new ProtobufDecoder(CIMRequestProto.CIMReqProtocol.getDefaultInstance()))
                .addLast(new ProtobufVarint32LengthFieldPrepender())
                .addLast(new ProtobufEncoder())
                .addLast(cimServerHandle);
    }
}
