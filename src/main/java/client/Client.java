package client;

import com.alibaba.fastjson.JSONObject;
import pojo.RequestData;
import pojo.ResponseData;
import service.EchoService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * @author andrew
 * @date 2019/03/24
 */
public class Client implements Callable<ResponseData> {

    private Selector selector;
    private SocketChannel socketChannel;
    private volatile boolean stop;
    private RequestData modelData;

    public void setStop(boolean stop) {
        this.stop = stop;
    }

    public Client(String ip, int port, RequestData modelData) {
        try {
            this.selector = Selector.open();
            this.socketChannel = SocketChannel.open();
            this.socketChannel.configureBlocking(false);
            if (this.socketChannel.connect(new InetSocketAddress(ip, port))) {
                this.socketChannel.register(selector, SelectionKey.OP_READ);
            } else {
                this.socketChannel.register(selector, SelectionKey.OP_CONNECT);
            }
            this.modelData = modelData;
        } catch (IOException e) {
            if (this.socketChannel != null) {
                try {
                    socketChannel.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (this.selector != null) {
                try {
                    this.selector.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }


    @Override
    public ResponseData call() {
        while (!stop) {
            try {
                selector.select();
                final Set<SelectionKey> selectionKeys = selector.selectedKeys();
                final Iterator<SelectionKey> selectionKeyIterator = selectionKeys.iterator();
                while (selectionKeyIterator.hasNext()) {
                    final SelectionKey selectionKey = selectionKeyIterator.next();
                    selectionKeyIterator.remove();
                    if (selectionKey.isValid()) {
                        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                        if (selectionKey.isConnectable() && socketChannel.finishConnect()) {
                            socketChannel.register(selector, SelectionKey.OP_READ);

                            // doWrite 写数据到服务器端
                            doWrite(socketChannel, modelData);
                        }

                        if (selectionKey.isReadable()) {
                            // 读取服务器端返回的消息
                            return doRead(socketChannel);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        return null;
    }

    private ResponseData doRead(SocketChannel socketChannel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        socketChannel.read(buffer);
        buffer.flip();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        String serverResponse = new String(data, StandardCharsets.UTF_8);
        ResponseData responseData = JSONObject.parseObject(serverResponse, ResponseData.class);
        System.out.println("客户端接收到服务器端的响应消息为：" + responseData.getData() + "，消息类型为：" + responseData.getReturnType());
        this.stop = true;
        return responseData;
    }

    private void doWrite(SocketChannel socketChannel, RequestData data) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        String jsonData = JSONObject.toJSONString(data);
        buffer.put(jsonData.getBytes(StandardCharsets.UTF_8));
        buffer.flip();
        socketChannel.write(buffer);
    }
}
