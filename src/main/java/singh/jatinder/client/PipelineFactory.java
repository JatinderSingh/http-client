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

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * @author Jatinder
 * 
 * Netty Internal pipeline setup for Http server
 */
public class PipelineFactory extends ChannelInitializer<SocketChannel> {

	private final SslContext sslCtx;

	/**
	 * Constructor.
	 */
	public PipelineFactory(SslContext sslCtx) {
	    this.sslCtx = sslCtx;
	}

	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		final ChannelPipeline pipeline = ch.pipeline();
		// Enable HTTPS if necessary.
		if (sslCtx != null) {
		    pipeline.addLast(sslCtx.newHandler(ch.alloc()));
		}
		pipeline.addLast(new HttpClientCodec());
		// Remove the following line if you don't want automatic content decompression.
		pipeline.addLast(new HttpContentDecompressor());
		// Uncomment the following line if you don't want to handle HttpContents.
		pipeline.addLast(new HttpObjectAggregator(1048576));
		HttpClientHandler handler = new HttpClientHandler();
		//handler.
		pipeline.addLast("handle", handler);
	}
}
