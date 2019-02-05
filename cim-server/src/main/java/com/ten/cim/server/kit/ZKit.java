package com.ten.cim.server.kit;

import com.ten.cim.server.config.AppConfiguration;
import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Zookeeper工具类
 */
@Component
public class ZKit {
    private static Logger logger = LoggerFactory.getLogger(ZKit.class);

    @Autowired
    private ZkClient zkClient;

    @Autowired
    private AppConfiguration appConfiguration;

    /**
     * 创建父级节点
     */
    public void createRootNode() {
        boolean exists = zkClient.exists(appConfiguration.getZkRoot());
        if (exists) {
            return;
        }

        // 创建 root
        zkClient.createPersistent(appConfiguration.getZkRoot());
    }

    /**
     * 写入指定节点 临时目录
     */
    public void createNode(String path) {
        zkClient.createEphemeral(path);
    }

}
