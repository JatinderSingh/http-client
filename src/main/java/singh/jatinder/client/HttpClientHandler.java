/**
 * 
 */
package singh.jatinder.client;

import com.stumbleupon.async.Deferred;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;

/**
 * @author Jatinder
 *
 */
public class HttpClientHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
    
    volatile Deferred<FullHttpResponse> resp;
    
    public Deferred<FullHttpResponse> getDeferred() {
        resp = new Deferred<FullHttpResponse>();
        return resp;
    }
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        resp.callback(msg);
    }
}