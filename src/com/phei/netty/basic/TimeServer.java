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
package com.phei.netty.basic;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * @author lilinfeng
 * @date 2014年2月14日
 * @version 1.0
 */
public class TimeServer {

    public void bind(int port) throws Exception {
	// 配置服务端的NIO线程组
		//一个用于接受客户端的链接，一个用于SocketChannel的网络读写
	EventLoopGroup bossGroup = new NioEventLoopGroup();
	EventLoopGroup workerGroup = new NioEventLoopGroup();
	try {
		//netty的辅助启动类，目的是为了降低代码复杂度
	    ServerBootstrap b = new ServerBootstrap();
	    b.group(bossGroup, workerGroup)
		    .channel(NioServerSocketChannel.class) //NioServerSocketChannel对应于NIO的ServerSocketChannel
		    .option(ChannelOption.SO_BACKLOG, 1024)
		    .childHandler(new ChildChannelHandler()); //绑定io事件的处理类，它类似于Reactor模式中的Handler，使用内部定义类ChildChannelHandler初始化Handler
	    // 绑定端口bind，同步阻塞等待链接成功sync
	    ChannelFuture f = b.bind(port).sync();
		//直接关闭链路，释放资源
		//f.channel().close();
	    // 等待服务端监听端口关闭，则唤醒当前线程退出main函数
	    f.channel().closeFuture().sync();
	} finally {
	    // 优雅退出，释放线程池资源
	    bossGroup.shutdownGracefully();
	    workerGroup.shutdownGracefully();
	}
    }

    private class ChildChannelHandler extends ChannelInitializer<SocketChannel> {
	@Override
	protected void initChannel(SocketChannel arg0) throws Exception {
		//处理类按照添加顺序执行
	    arg0.pipeline().addLast(new TimeServerHandler());
	}

    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
	int port = 8080;
	if (args != null && args.length > 0) {
	    try {
		port = Integer.valueOf(args[0]);
	    } catch (NumberFormatException e) {
		// 采用默认值
	    }
	}
	new TimeServer().bind(port);
    }
}
