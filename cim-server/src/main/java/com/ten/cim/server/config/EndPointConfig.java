package com.ten.cim.server.config;

import com.ten.cim.server.endpoint.CustomEndpoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 监控端点配置
 */
@Configuration
public class EndPointConfig {

    @Value("${monitor.channel.map.key}")
    private String channelMap;

    @Bean
    public CustomEndpoint buildEndPoint() {
        CustomEndpoint customEndpoint = new CustomEndpoint(channelMap);
        return customEndpoint;
    }
}
