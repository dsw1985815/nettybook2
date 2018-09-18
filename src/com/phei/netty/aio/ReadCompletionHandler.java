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
package com.phei.netty.aio;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * @author lilinfeng
 * @date 2014年2月16日
 * @version 1.0
 */
public class ReadCompletionHandler implements
	CompletionHandler<Integer, ByteBuffer> {

    private AsynchronousSocketChannel channel;

    public ReadCompletionHandler(AsynchronousSocketChannel channel) {
	if (this.channel == null)
	    this.channel = channel;
    }

    @Override
    public void completed(Integer result, ByteBuffer attachment) {
    	//对数据进行flip操作，为后续从缓冲区读取数据做准备
	attachment.flip();
	byte[] body = new byte[attachment.remaining()];
	attachment.get(body);
	try {
	    String req = new String(body, "UTF-8");
	    System.out.println("The time server receive order : " + req);
	    String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(req) ? new java.util.Date(
		    System.currentTimeMillis()).toString() : "BAD ORDER";
	    doWrite(currentTime);
	} catch (UnsupportedEncodingException e) {
	    e.printStackTrace();
	}
    }

    //处理返回值
    private void doWrite(String currentTime) {
	if (currentTime != null && currentTime.trim().length() > 0) {
	    byte[] bytes = (currentTime).getBytes();
	    ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
	    writeBuffer.put(bytes);
	    writeBuffer.flip();
	    //将字符串转化成Byte数组，并放入到缓冲区对象中，进行发送，由于channel是异步非阻塞的，所以可能发送多次，直到发送成功。
	    channel.write(writeBuffer, writeBuffer,
		    new CompletionHandler<Integer, ByteBuffer>() {
			@Override
			public void completed(Integer result, ByteBuffer buffer) {
			    // 如果没有发送完成，继续发送
			    if (buffer.hasRemaining())
				channel.write(buffer, buffer, this);
			}

			@Override
			public void failed(Throwable exc, ByteBuffer attachment) {
			    try {
					//发生异常关闭链路
					channel.close();
			    } catch (IOException e) {
				// ingnore on close
			    }
			}
		    });
	}
    }

    @Override
    public void failed(Throwable exc, ByteBuffer attachment) {
	try {
		//发生异常关闭链路
	    this.channel.close();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

}
