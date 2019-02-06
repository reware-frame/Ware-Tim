package com.ten.cim.client.service.impl.command;

import com.ten.cim.client.service.InnerCommand;
import com.ten.cim.client.service.MsgLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Function:
 *
 * @author ten
 * Date: 2019-01-27 19:37
 * @since JDK 1.8
 */
@Service
public class QueryHistoryCommand implements InnerCommand {
    private final static Logger LOGGER = LoggerFactory.getLogger(QueryHistoryCommand.class);


    @Autowired
    private MsgLogger msgLogger ;

    @Override
    public void process(String msg) {
        String[] split = msg.split(" ");
        String res = msgLogger.query(split[1]);
        System.out.println(res);
    }
}
