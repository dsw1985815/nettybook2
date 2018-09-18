/*
 * Copyright 2013-2018 Lilinfeng.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.phei.netty.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Administrator
 * @date 2014年2月16日
 * @version 1.0
 */
public class TimeClientHandle implements Runnable {

    private String host;
    private int port;

    private Selector selector;
    private SocketChannel socketChannel;

    private volatile boolean stop;

    //客户端线程初始化
    public TimeClientHandle(String host, int port) {
	this.host = host == null ? "127.0.0.1" : host;
	this.port = port;
	try {
		//获取多路复用选择器
	    selector = Selector.open();
	    //创建客户端网络处理Channel并设置为非阻塞
	    socketChannel = SocketChannel.open();
	    socketChannel.configureBlocking(false);
	} catch (IOException e) {
	    e.printStackTrace();
	    System.exit(1);
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
	try {
		//客户端线程启动链接服务端
	    doConnect();
	} catch (IOException e) {
	    e.printStackTrace();
	    System.exit(1);
	}
	while (!stop) {
	    try {
	    	//每个1秒唤醒selector
		selector.select(1000);
		//当有处于激活状态的Channel时，selector返回Channel的SelectionKey集合，迭代集合进行网络读写
		Set<SelectionKey> selectedKeys = selector.selectedKeys();
		Iterator<SelectionKey> it = selectedKeys.iterator();
		SelectionKey key = null;
		while (it.hasNext()) {
		    key = it.next();
		    it.remove();
		    try {
		    	//处理服务端的返回内容，可能是CONNECT也可能是READ
			handleInput(key);
		    } catch (Exception e) {
			if (key != null) {
			    key.cancel();
			    if (key.channel() != null)
				key.channel().close();
			}
		    }
		}
	    } catch (Exception e) {
		e.printStackTrace();
		System.exit(1);
	    }
	}

	// 多路复用器关闭后，所有注册在上面的Channel和Pipe等资源都会被自动去注册并关闭，所以不需要重复释放资源
	if (selector != null)
	    try {
		selector.close();
	    } catch (IOException e) {
		e.printStackTrace();
	    }

    }

	//selector轮询到了Channel的READ，处理Channel读取事件，通过SelectionKey获取输入码流，昝定义ByteBuffer为1M
	private void handleInput(SelectionKey key) throws IOException {

	if (key.isValid()) {
	    // 判断是否连接成功
	    SocketChannel sc = (SocketChannel) key.channel();
	    //先处理CONNECT
	    if (key.isConnectable()) {
		if (sc.finishConnect()) {
			//连接成功就准备监听服务端的响应，并发送请求
		    sc.register(selector, SelectionKey.OP_READ);
		    doWrite(sc);
		} else
		    System.exit(1);// 连接失败，进程退出
	    }
	    //再处理READ
	    //当发送请求之前，此时的key是CONNECT转态的，是不可读的，只有当selector监听到READ事件，才是可读，才可以进入下面的代码执行
	    if (key.isReadable()) {
		ByteBuffer readBuffer = ByteBuffer.allocate(1024);
		int readBytes = sc.read(readBuffer);
		if (readBytes > 0) {
		    readBuffer.flip();
		    byte[] bytes = new byte[readBuffer.remaining()];
		    readBuffer.get(bytes);
		    String body = new String(bytes, "UTF-8");
		    System.out.println("Now is : " + body);
		    //输出结果，退出线程
		    this.stop = false;
		} else if (readBytes < 0) {
		    // 对端链路关闭
		    key.cancel();
		    sc.close();
		} else
		    ; // 读到0字节，忽略
	    }
	}

    }

    //连接服务端
    private void doConnect() throws IOException {
	// 如果直接连接成功，则注册到多路复用器上，发送请求消息，读应答
	if (socketChannel.connect(new InetSocketAddress(host, port))) {
		//如果连接成功，将当前Channel注册到selector上，并监听事件SelectionKey.OP_READ，等待服务端的响应返回
	    socketChannel.register(selector, SelectionKey.OP_READ);
	    //发送请求
	    doWrite(socketChannel);
	} else
		//如果没有直接连接成功，将当前Channel注册到selector上，并监听事件SelectionKey.OP_CONNECT，这并不代表连接失败了，只是服务端没有返回
	//TCP握手应答消息，此时我们注册到OP_CONNECT上，当服务响应之后TCP的ack消息后，selector可以轮询到连接就绪状态。
	    socketChannel.register(selector, SelectionKey.OP_CONNECT);
    }

    //处理发送请求，向Channel中写入请求内容，发送给服务端
    private void doWrite(SocketChannel sc) throws IOException {
	byte[] req = "QUERY TIME ORDER".getBytes();
	ByteBuffer writeBuffer = ByteBuffer.allocate(req.length);
	writeBuffer.put(req);
	writeBuffer.flip();
	sc.write(writeBuffer);
	//hasRemainning判断是否已经读完Buffer
	if (!writeBuffer.hasRemaining())
	    System.out.println("Send order 2 server succeed.");
    }

}
