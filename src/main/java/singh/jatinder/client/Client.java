/*
 * 
 * This file is part of Http-Server
 * Copyright (C) 2013  Jatinder
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this library; If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package singh.jatinder.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.internal.AppendableCharSequence;

import java.net.InetSocketAddress;
import java.nio.channels.Channels;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stumbleupon.async.Callback;
import com.stumbleupon.async.Deferred;

/**
 * @author Jatinder
 *
 * Bootstrap class for server
 *
 */
public class Client {
	private static final Logger LOG = LoggerFactory.getLogger(Client.class);
	private final LineParser lineParser = new LineParser(new AppendableCharSequence(64));
	private Bootstrap controller;
	private EventLoopGroup group;
	private int MaxChannelsPerHostPort = 1; 
	private Map<String, Map<Integer, Channel[]>> clientContext = new HashMap<String, Map<Integer,Channel[]>>();
	
	public void init() {
		String os = System.getProperty("os.name").toLowerCase(Locale.UK).trim();
		if (os.startsWith("linux")) {
		    group = (null==group) ? new EpollEventLoopGroup(1):group;
        } else {
            group = (null==group) ? new NioEventLoopGroup(1):group;
        }
		        
		try {
		 // Configure the client.
		    controller = new Bootstrap();
			controller.group(group);
			if (os.startsWith("linux")) {
			    controller.channel(EpollSocketChannel.class);
			    controller.option(EpollChannelOption.TCP_CORK, true);
			} else {
			    controller.channel(NioSocketChannel.class);
			}
			controller.handler(new PipelineFactory(null));
		} catch (Throwable t) {
		    group.shutdownGracefully();
			throw new RuntimeException("Initialization failed", t);
		}
	}
	
	public Deferred<FullHttpResponse> execute(String host, int port, FullHttpRequest request) throws Exception {
	    if (null == clientContext.get(host)) {
	        clientContext.put(host, new HashMap<Integer, Channel[]>());
	    } 
	    if (null == clientContext.get(host).get(port)) {
	        clientContext.get(host).put(port, new Channel[MaxChannelsPerHostPort]);
	    }
	    Channel ch = getAvailableChannel(clientContext.get(host).get(port), host, port, controller); 
	 
        HttpClientHandler handle = (HttpClientHandler) ch.pipeline().get("handle");
        Deferred<FullHttpResponse> toReturn = handle.getDeferred();
        ch.writeAndFlush(request, ch.voidPromise());
        return toReturn;
	}
	
	private Channel getAvailableChannel(Channel[] channels, String host, int port, Bootstrap controller) throws InterruptedException {
	    for (int i=0;i<channels.length;i++) {
	        if (channels[i] == null) {
	            channels[i] = controller.connect(host, port).sync().channel();
	        }
	        if (channels[i].isWritable())
	            return channels[i];
	    }
	    return null;
	}
	
	public void shutDownGracefully() {
		LOG.info("Stop Requested");
		for ( Map<Integer, Channel[]> ports : clientContext.values()) {
		    for (Channel[] channels : ports.values()) {
		        for (int i=0;i<channels.length;i++) {
		            if (null != channels[i]) {
		                channels[i].disconnect();
		            }
		        }
		    }
		}
		if (null!=controller) {
			controller.group().shutdownGracefully().awaitUninterruptibly();
		}
	}
	
	
	public static void main(String[] args) throws Exception {
        final Client c = new Client();
        c.init();
        Deferred<FullHttpResponse> def = c.execute("127.0.0.1", 9900, new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, new AppendableCharSequence("/hello")));
        def.addCallback(new Callback<Object, FullHttpResponse>() {
            public Object call(FullHttpResponse arg) throws Exception {
                c.lineParser.parse(arg.content());
                System.out.println(c.lineParser.getParsedLine());
                return null;
            }
        });
        c.shutDownGracefully();
    }
	
	private final class LineParser implements ByteBufProcessor {
        private final AppendableCharSequence seq;

        LineParser(AppendableCharSequence seq) {
            this.seq = seq;
        }

        public AppendableCharSequence parse(ByteBuf buffer) {
            seq.reset();
            int i = buffer.forEachByte(this);
            buffer.readerIndex(i + 1);
            return seq;
        }
        
        public String getParsedLine() {
            return seq.toString();
        }

        public boolean process(byte value) throws Exception {
            char nextByte = (char) value;
            seq.append(nextByte);
            return true;
        }
    }
}