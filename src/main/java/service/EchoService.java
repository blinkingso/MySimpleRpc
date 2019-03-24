package service;

import pojo.ResponseData;

/**
 * @author andrew
 * @date 2019/03/24
 */
public interface EchoService {

    String sayHello(String msg);

    String sayHello(String msg, String author);

    ResponseData sayHello2();

}
