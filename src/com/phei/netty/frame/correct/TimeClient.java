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
package com.phei.netty.frame.correct;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

/**
 * @author lilinfeng
 * @date 2014年2月14日
 * @version 1.0
 */
public class TimeClient {

    public void connect(int port, String host) throws Exception {
	// 配置客户端NIO线程组
	EventLoopGroup group = new NioEventLoopGroup();
	try {
	    Bootstrap b = new Bootstrap();
	    b.group(group).channel(NioSocketChannel.class)
		    .option(ChannelOption.TCP_NODELAY, true)
		    .handler(new ChannelInitializer<SocketChannel>() {
			@Override
			public void initChannel(SocketChannel ch)
				throws Exception {
			    ch.pipeline().addLast(
			    		//添加换行符解码器，它会遍历ByteBuf的所有字节，找到 \n \r\n 并以此位置为结尾，可读索引到结尾位置形成一行。
						//如果连续读取到的内容超过最大就抛异常，读取到的内容也忽略掉
				    new LineBasedFrameDecoder(1024));
			    //将读取到的二进制码流转化为string
			    ch.pipeline().addLast(new StringDecoder());
			    // LineBasedFrameDecoder + StringDecoder 就是作为按行切换的文本解码器，用来解决TCP协议的粘包拆包问题
			    ch.pipeline().addLast(new TimeClientHandler());
			}
		    });

	    // 发起异步连接操作
	    ChannelFuture f = b.connect(host, port).sync();

	    // 当代客户端链路关闭
	    f.channel().closeFuture().sync();
	} finally {
	    // 优雅退出，释放NIO线程组
	    group.shutdownGracefully();
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
	new TimeClient().connect(port, "127.0.0.1");
    }
}
