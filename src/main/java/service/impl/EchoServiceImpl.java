package service.impl;

import pojo.ResponseData;
import service.EchoService;

/**
 * @author andrew
 * @date 2019/03/24
 */
public class EchoServiceImpl implements EchoService {

    @Override
    public String sayHello(String msg) {
        return "Hello Client, I Got Message : " + msg;
    }

    @Override
    public String sayHello(String msg, String author) {
        return "Hello " + author + ", I Got Message :" + msg;
    }

    @Override
    public ResponseData sayHello2() {
        final ResponseData responseData = new ResponseData();
        responseData.setReturnType(String.class);
        responseData.setData("Hello 这个是RPC框架原理的简单封装");
        return responseData;
    }
}
