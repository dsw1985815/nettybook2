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
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Administrator
 * @date 2014年2月16日
 * @version 1.0
 */
public class MultiplexerTimeServer implements Runnable {

    private Selector selector;

    private ServerSocketChannel servChannel;

    private volatile boolean stop;

    /**
     * 初始化多路复用器、绑定监听端口
     * 
     * @param port
     */
    public MultiplexerTimeServer(int port) {
	try {
		//创建selector 多路复用选择器 ，一个selector可以轮询多个channel ，由于JDK使用了epoll()代替了传统的select实现，selector没有句柄限制。
	    selector = Selector.open();
	    //Channel接口分为2大类，一种处理网络读写的SocketChannel和ServerSocketChannel，另一种是文件炒作的FileChannel
		//创建ServerSocketChannel 并设置为异步非阻塞 绑定端口，设置backlog（requested maximum length of the queue of incoming connections.）
	    servChannel = ServerSocketChannel.open();
	    servChannel.configureBlocking(false);
	    servChannel.socket().bind(new InetSocketAddress(port), 1024);
		//把channel注册到selector，监听SelectionKey.OP_ACCEPT操作位
		servChannel.register(selector, SelectionKey.OP_ACCEPT);
	    System.out.println("The time server is start in port : " + port);
	} catch (IOException e) {
	    e.printStackTrace();
	    //发生异常直接退出
	    System.exit(1);
	}
    }

    public void stop() {
	this.stop = true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
	while (!stop) {
	    try {
	    	//每隔1秒就唤醒selector，当有处于就绪状态的Channel时，selector返回Channel的SelectionKey集合，迭代集合进行网络读写
		selector.select(1000);
		Set<SelectionKey> selectedKeys = selector.selectedKeys();
		Iterator<SelectionKey> it = selectedKeys.iterator();
		SelectionKey key = null;
		while (it.hasNext()) {
		    key = it.next();
		    it.remove();
		    try {
			handleInput(key);
		    } catch (Exception e) {
			if (key != null) {
			    key.cancel();
			    if (key.channel() != null)
				key.channel().close();
			}
		    }
		}
	    } catch (Throwable t) {
		t.printStackTrace();
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

    //使用buffer读写，定义一个1M大小的缓冲区用来处理码流。
    private void handleInput(SelectionKey key) throws IOException {

	if (key.isValid()) {
	    // 处理新接入的请求消息
	    if (key.isAcceptable()) {
		// Accept the new connection
		ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
		SocketChannel sc = ssc.accept();
		sc.configureBlocking(false);
		// Add the new connection to the selector
		sc.register(selector, SelectionKey.OP_READ);
	    }
	    if (key.isReadable()) {
		// Read the data
		SocketChannel sc = (SocketChannel) key.channel();
		ByteBuffer readBuffer = ByteBuffer.allocate(1024);
		int readBytes = sc.read(readBuffer);
		if (readBytes > 0) {
		    readBuffer.flip();
		    byte[] bytes = new byte[readBuffer.remaining()];
		    readBuffer.get(bytes);
		    String body = new String(bytes, "UTF-8");
		    System.out.println("The time server receive order : "
			    + body);
		    String currentTime = "QUERY TIME ORDER"
			    .equalsIgnoreCase(body) ? new java.util.Date(
			    System.currentTimeMillis()).toString()
			    : "BAD ORDER";
		    doWrite(sc, currentTime);
		} else if (readBytes < 0) {
		    // 对端链路关闭
		    key.cancel();
		    sc.close();
		} else
		    ; // 读到0字节，忽略
	    }
	}
    }

    //把应答发回给调用方，首先将字符串编码成字节数组，根据字节数组的容量创建ByteBuffer，调用put方法把自己数组复制到缓冲区，然后对
	//缓冲区使用flip()操作，最后调用SocketChannel的write方法将缓冲区中的字节数组发出去。由于Channel是异步非阻塞，并不能一次性把数据都
	//发出去，有可能出现半包问题。
    private void doWrite(SocketChannel channel, String response)
	    throws IOException {
	if (response != null && response.trim().length() > 0) {
	    byte[] bytes = response.getBytes();
	    ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
	    writeBuffer.put(bytes);
	    writeBuffer.flip();
	    channel.write(writeBuffer);
	}
    }
}
