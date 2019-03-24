package client;

import com.alibaba.fastjson.JSONObject;
import pojo.RequestData;
import pojo.ResponseData;
import service.EchoService;
import service.impl.EchoServiceImpl;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * @author andrew
 * @date 2019/03/24
 */
public class EchoClient<S> {

    S proxy(String ip, int port, Class<? extends S> serviceClass) {
        return (S) Proxy.newProxyInstance(serviceClass.getClassLoader(), serviceClass.getInterfaces(),
                (Object obj, Method method, Object[] objects) -> {
                    RequestData requestData = new RequestData();
                    requestData.setInterfaceName(serviceClass.getName());
                    requestData.setMethodName(method.getName());
                    requestData.setParameters(objects);
                    requestData.setParameterTypes(method.getParameterTypes());

                    final Client c = new Client(ip, port, requestData);
                    FutureTask<ResponseData> f = new FutureTask<ResponseData>(c);
                    new Thread(f).start();
                    ResponseData responseData = f.get();
                    return JSONObject.parseObject(responseData.getData(), responseData.getReturnType());
                });
    }

    public static void main(String[] args) {
        final EchoClient<EchoService> echoServiceEchoClient = new EchoClient<>();
        final EchoService proxy = echoServiceEchoClient.proxy("127.0.0.1", 9999, EchoServiceImpl.class);
        String result = proxy.sayHello("第一个方法");
        String result2 = proxy.sayHello("第二个方法", "andrew");
        final ResponseData responseData = proxy.sayHello2();

        System.out.println(result);
        System.out.println(result2);
        System.out.println(responseData.getData());
    }
}
