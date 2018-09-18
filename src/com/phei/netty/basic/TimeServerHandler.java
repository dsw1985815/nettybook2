/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.phei.netty.basic;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author lilinfeng
 * @date 2014年2月14日
 * @version 1.0
 */
public class TimeServerHandler extends ChannelHandlerAdapter {

	//ChannelHandlerAdapter 继承自ChannelHandler
	//本类用于对IO进行读写操作，关注channelRead和exceptionCaught方法

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
	    throws Exception {
    	//msg换成Netty定义的ByteBuf对象
	ByteBuf buf = (ByteBuf) msg;
	byte[] req = new byte[buf.readableBytes()];
	buf.readBytes(req);
	String body = new String(req, "UTF-8");
	System.out.println("The time server receive order : " + body);
	String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body) ? new java.util.Date(
		System.currentTimeMillis()).toString() : "BAD ORDER";
	ByteBuf resp = Unpooled.copiedBuffer(currentTime.getBytes());
	ctx.write(resp);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    	//netty中，ctx的write方法并不会把内容写入到SocketChannel
		//而是写入到buf中，只有调用flush才会写入SocketChannel并发送
	ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
	//发生异常时，释放资源
    	ctx.close();
    }
}
