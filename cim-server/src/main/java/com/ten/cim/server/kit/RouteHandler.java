package com.ten.cim.server.kit;

import com.alibaba.fastjson.JSONObject;
import com.crossoverjie.cim.common.pojo.CIMUserInfo;
import com.ten.cim.server.config.AppConfiguration;
import com.ten.cim.server.util.SessionSocketHolder;
import com.ten.cim.server.util.SpringBeanFactory;
import io.netty.channel.socket.nio.NioSocketChannel;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 路由控制器
 */
@Component
public class RouteHandler {
    private final static Logger LOGGER = LoggerFactory.getLogger(RouteHandler.class);

    private final MediaType mediaType = MediaType.parse("application/json");

    /**
     * 用户下线
     */
    public void userOffLine(CIMUserInfo userInfo, NioSocketChannel channel) throws IOException {
        if (userInfo != null) {
            LOGGER.info("用户[{}]下线", userInfo.getUserName());
            SessionSocketHolder.removeSession(userInfo.getUserId());
            //清除路由关系
            clearRouteInfo(userInfo);
        }
        SessionSocketHolder.remove(channel);
    }

    /**
     * 清除路由关系
     */
    private void clearRouteInfo(CIMUserInfo userInfo) throws IOException {
        OkHttpClient okHttpClient = SpringBeanFactory.getBean(OkHttpClient.class);
        AppConfiguration configuration = SpringBeanFactory.getBean(AppConfiguration.class);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("userId", userInfo.getUserId());
        jsonObject.put("msg", "offLine");
        RequestBody requestBody = RequestBody.create(mediaType, jsonObject.toString());

        Request request = new Request.Builder()
                .url(configuration.getClearRouteUrl())
                .post(requestBody)
                .build();

        Response response = null;
        try {
            response = okHttpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
        } finally {
            response.body().close();
        }
    }

}
