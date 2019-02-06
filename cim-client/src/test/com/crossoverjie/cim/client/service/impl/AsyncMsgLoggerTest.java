package com.ten.cim.client.service.impl;

import com.ten.cim.client.CIMClientApplication;
import com.ten.cim.client.service.MsgLogger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;

@SpringBootTest(classes = CIMClientApplication.class)
@RunWith(SpringRunner.class)
public class AsyncMsgLoggerTest {



    @Autowired
    private MsgLogger msgLogger ;

    @Test
    public void writeLog() throws Exception {
        for (int i = 0; i < 10; i++) {
            msgLogger.log("zhangsan:【asdsd】" + i);
        }

        TimeUnit.SECONDS.sleep(2);
    }



    @Test
    public void query(){
        String ten = msgLogger.query("ten");
        System.out.println(ten);
    }

}