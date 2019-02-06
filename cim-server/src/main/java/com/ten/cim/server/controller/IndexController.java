package com.ten.cim.server.controller;

import com.ten.cim.common.enums.StatusEnum;
import com.ten.cim.common.res.BaseResponse;
import com.ten.cim.server.vo.req.SendMsgReqVO;
import com.ten.cim.common.constant.Constants;
import com.ten.cim.server.server.CIMServer;
import com.ten.cim.server.vo.res.SendMsgResVO;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/")
public class IndexController {

    @Autowired
    private CIMServer cimServer;

    /**
     * 统计 service
     */
    @Autowired
    private CounterService counterService;

    /**
     * 向服务端发消息
     *
     * @param sendMsgReqVO
     * @return
     */
    @ApiOperation("服务端发送消息")
    @RequestMapping(value = "sendMsg", method = RequestMethod.POST)
    @ResponseBody
    public BaseResponse<SendMsgResVO> sendMsg(@RequestBody SendMsgReqVO sendMsgReqVO) {
        BaseResponse<SendMsgResVO> res = new BaseResponse();
        cimServer.sendMsg(sendMsgReqVO);

        counterService.increment(Constants.COUNTER_SERVER_PUSH_COUNT);

        SendMsgResVO sendMsgResVO = new SendMsgResVO();
        sendMsgResVO.setMsg("OK");
        res.setCode(StatusEnum.SUCCESS.getCode());
        res.setMessage(StatusEnum.SUCCESS.getMessage());
        res.setDataBody(sendMsgResVO);
        return res;
    }

}
