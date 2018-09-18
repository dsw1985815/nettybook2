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

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * @author lilinfeng
 * @date 2014年2月16日
 * @version 1.0
 */
public class AcceptCompletionHandler implements
	CompletionHandler<AsynchronousSocketChannel, AsyncTimeServerHandler> {

    @Override
    public void completed(AsynchronousSocketChannel result,
	    AsyncTimeServerHandler attachment) {
    	//此处继续调用accept方法的原因是，可能有新的客户端连接会继续请求接入，所以当接入一个请求之后，进入到此处，
		//声明channel继续去等待接受新的请求接入，这样形成一个持续接收请求接入的循环
	attachment.asynchronousServerSocketChannel.accept(attachment, this);
	//分配一个1M大的缓冲区
	ByteBuffer buffer = ByteBuffer.allocate(1024);
	//使用该缓冲区处理结果，具体的处理逻辑交给ReadCompletionHandler
	result.read(buffer, buffer, new ReadCompletionHandler(result));
    }

    @Override
    public void failed(Throwable exc, AsyncTimeServerHandler attachment) {
	exc.printStackTrace();
	//请求失败，计数器减一，服务端主线程将关闭
	attachment.latch.countDown();
    }

}
