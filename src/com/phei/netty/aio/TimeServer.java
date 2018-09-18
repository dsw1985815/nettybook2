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

/**
 * @author lilinfeng
 * @date 2014年2月14日
 * @version 1.0
 */
public class TimeServer {

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
	int port = 8080;
	if (args != null && args.length > 0) {
	    try {
		port = Integer.valueOf(args[0]);
	    } catch (NumberFormatException e) {
		// 采用默认值
	    }
	}
	//创建一个异步io服务端线程并启动
	AsyncTimeServerHandler timeServer = new AsyncTimeServerHandler(port);
	new Thread(timeServer, "AIO-AsyncTimeServerHandler-001").start();
    }

	/**
	 * "Thread-2@754" daemon prio=5 tid=0xf nid=NA runnable
	 java.lang.Thread.State: RUNNABLE
	 at sun.nio.ch.Iocp.getQueuedCompletionStatus(Iocp.java:-1)
	 at sun.nio.ch.Iocp.access$300(Iocp.java:46)
	 at sun.nio.ch.Iocp$EventHandlerTask.run(Iocp.java:333)
	 at sun.nio.ch.AsynchronousChannelGroupImpl$1.run(AsynchronousChannelGroupImpl.java:112)
	 at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
	 at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
	 at java.lang.Thread.run(Thread.java:748)
	 线程堆栈，可以看到最终服务端的执行线程是AsynchronousChannelGroupImpl$1，该类实现了Executor，因此他是由JDK底层线程池实现的一个专门处理
	 异步操作的线程完成的，所以我们不需要像NIO那样自己创建selector线程来处理网络读写的过程。
	 */

}
