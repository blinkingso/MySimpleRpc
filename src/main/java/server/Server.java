package server;

import com.alibaba.fastjson.JSONObject;
import pojo.RequestData;
import pojo.ResponseData;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @author andrew
 * @date 2019/03/24
 */
public class Server implements Runnable {

    private volatile boolean stop;
    private ServerSocketChannel server;
    private Selector selector;

    public void setStop(boolean stop) {
        this.stop = stop;
    }

    /**
     * 初始化服务器
     *
     * @param ip
     * @param port
     */
    public Server(String ip, int port) {
        try {
            this.selector = Selector.open();
            this.server = ServerSocketChannel.open();
            this.server.configureBlocking(false);
            this.server.bind(new InetSocketAddress(ip, port));
            this.server.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("服务器端启动成功");
        } catch (IOException e) {
            e.printStackTrace();
            if (this.server != null) {
                try {
                    this.server.close();
                } catch (IOException ie) {
                    e.printStackTrace();
                }
            }
            if (this.selector != null) {
                try {
                    this.selector.close();
                } catch (IOException ie) {
                    e.printStackTrace();
                }
            }
        }
    }


    public void run() {
        while (!stop) {
            try {
                // returns only after at least one channel is selected  wakeup or thread interrupted
                selector.select();
                // 读取select keys
                final Set<SelectionKey> keys = selector.selectedKeys();
                final Iterator<SelectionKey> keyIterator = keys.iterator();
                while (keyIterator.hasNext()) {
                    final SelectionKey selectionKey = keyIterator.next();
                    keyIterator.remove();
                    if (selectionKey.isValid()) {
                        if (selectionKey.isAcceptable()) {
                            // 处理新接入的请求
                            doAccept(selectionKey);
                        }

                        if (selectionKey.isReadable()) {
                            // 读取客户端buffer数据
                            doRead(selectionKey);
                        }

                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 处理新接入的请求消息
     */
    private void doAccept(SelectionKey selectionKey) throws IOException {
        final ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
        final SocketChannel socketChannel = serverSocketChannel.accept();
        // 配置非阻塞
        socketChannel.configureBlocking(false);

        // 新连接注册到selector上
        socketChannel.register(selector, SelectionKey.OP_READ);
    }

    /**
     * 读取通道中客户端发送的请求数据
     *
     * @param selectionKey
     * @throws IOException
     */
    private void doRead(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        ByteBuffer buf = ByteBuffer.allocate(1024);
        final int read = socketChannel.read(buf);
        if (read > 0) {
            // 读取到数据
            buf.flip();
            byte[] data = new byte[buf.remaining()];
            buf.get(data);
            String request = new String(data, 0, data.length, "utf-8");
            System.out.println("服务器端读取到客户端请求数据为：" + request);

            // 解析数据
            try {
                RequestData modelData = JSONObject.parseObject(request, RequestData.class);
                String className = modelData.getInterfaceName();
                Class<?> cls = Class.forName(className, true, this.getClass().getClassLoader());
                final Method method = cls.getMethod(modelData.getMethodName(), modelData.getParameterTypes());
                Object result = method.invoke(cls.newInstance(), modelData.getParameters());
                String jsonResult = JSONObject.toJSONString(result);
                // 服务器将数据写会到客户端
                ResponseData responseData = new ResponseData();
                responseData.setData(jsonResult);
                responseData.setReturnType(method.getReturnType());
                doWrite(socketChannel, buf, responseData);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // 未读取到数据
            selectionKey.cancel();
            socketChannel.close();
        }
    }

    private void doWrite(SocketChannel socketChannel, ByteBuffer buffer, ResponseData responseData) throws IOException {
        // 清空buffer
        buffer.clear();
        String response = JSONObject.toJSONString(responseData);
        byte[] data = response.getBytes("utf-8");
        buffer.put(data);
        // 调用flip后切换读状态
        buffer.flip();
        socketChannel.write(buffer);
    }

    public static void main(String[] args) {
        Server server = new Server("127.0.0.1", 9999);
        new Thread(server).start();
    }

}
