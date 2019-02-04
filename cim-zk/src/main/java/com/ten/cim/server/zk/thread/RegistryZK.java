package com.ten.cim.server.zk.thread;

import com.ten.cim.server.zk.util.AppConfiguration;
import com.ten.cim.server.zk.util.SpringBeanFactory;
import com.ten.cim.server.zk.util.ZKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 注册zookeeper客户端
 */
public class RegistryZK implements Runnable {
    private static Logger logger = LoggerFactory.getLogger(RegistryZK.class);

    private ZKit zkUtil;

    private AppConfiguration appConfiguration;

    private String ip;
    private int port;

    public RegistryZK(String ip, int port) {
        this.ip = ip;
        this.port = port;
        zkUtil = SpringBeanFactory.getBean(ZKit.class);
        appConfiguration = SpringBeanFactory.getBean(AppConfiguration.class);
    }

    @Override
    public void run() {
        // 创建父节点
        zkUtil.createRootNode();

        // 是否要将自己注册到 ZK
        if (appConfiguration.isZkSwitch()) {
            String path = appConfiguration.getZkRoot() + "/ip-" + ip + ":" + port;
            zkUtil.createNode(path, path);
            logger.info("注册 zookeeper 成功，msg=[{}]", path);
        }

        //注册监听服务
        zkUtil.subscribeEvent(appConfiguration.getZkRoot());

    }
}