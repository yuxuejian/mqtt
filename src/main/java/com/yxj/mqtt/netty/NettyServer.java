package com.yxj.mqtt.netty;

import com.yxj.mqtt.config.BrokerProperties;
import com.yxj.mqtt.netty.abs.NettyServerAbstract;
import com.yxj.mqtt.netty.service.NettyService;
import com.yxj.mqtt.process.BrokerProcess;
import com.yxj.mqtt.utils.Epoll;
import com.yxj.mqtt.utils.Pair;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NettyServer extends NettyServerAbstract implements NettyService {

    private BrokerProperties brokerProperties;

    private final ServerBootstrap serverBootstrap;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final ExecutorService publicExecutor;
    private DefaultEventExecutorGroup defaultEventExecutorGroup;
    private Channel channel;

    public NettyServer(BrokerProperties brokerProperties) {
        this.brokerProperties = brokerProperties;
        this.serverBootstrap = new ServerBootstrap();
        this.publicExecutor = Executors.newFixedThreadPool(brokerProperties.getPublicExecutorNum(), new ThreadFactory() {
            private AtomicInteger threadIndex = new AtomicInteger(0);
            private int threadTotal = brokerProperties.getPublicExecutorNum();
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, String.format("publicExecutor_%d_%d", threadTotal, threadIndex.incrementAndGet()));
            }
        });
        bossGroup = new NioEventLoopGroup(1, new ThreadFactory() {
            private AtomicInteger threadIndex = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r,String.format("BossGroup_%d", threadIndex.incrementAndGet()));
            }
        });
        if (useEpoll()) {
            workerGroup = new EpollEventLoopGroup(brokerProperties.getSelectorThreadNum(), new ThreadFactory() {
                private AtomicInteger threadIndex = new AtomicInteger(0);
                private int totalThread = brokerProperties.getWorkerThreadNum();
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, String.format("EpollWorkerGroup_%d_%d", totalThread, threadIndex.incrementAndGet()));
                }
            });
        } else {
            workerGroup = new NioEventLoopGroup(brokerProperties.getSelectorThreadNum(), new ThreadFactory() {
                private AtomicInteger threadIndex = new AtomicInteger(0);
                private int totalThread = brokerProperties.getWorkerThreadNum();
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, String.format("NioWorkerGroup_%d_%d", totalThread, threadIndex.incrementAndGet()));
                }
            });
        }

    }

    @Override
    public void start() {
        this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(brokerProperties.getWorkerThreadNum(), new ThreadFactory() {
            private AtomicInteger threadIndex = new AtomicInteger(0);
            private int totalThread = brokerProperties.getWorkerThreadNum();

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, String.format("WorkerThread_%d_%d", totalThread, threadIndex.incrementAndGet()));
            }
        });
        ServerBootstrap childBootstrap = this.serverBootstrap.group(bossGroup, workerGroup)
                .channel(useEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .option(ChannelOption.SO_BACKLOG, 1024)// 连接客户等待队列的最大长度
                .option(ChannelOption.SO_REUSEADDR, true) //
                .option(ChannelOption.SO_KEEPALIVE, false) //
                .childOption(ChannelOption.TCP_NODELAY, true) // 实时性要求较高时设置为true，false时会将发送的数据拼接到足够大之后再发送
                .childOption(ChannelOption.SO_SNDBUF, brokerProperties.getSendBufSize())
                .childOption(ChannelOption.SO_RCVBUF, brokerProperties.getRecvBufSize())
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        pipeline.addLast(defaultEventExecutorGroup,
                                new IdleStateHandler(0,0,brokerProperties.getKeepAlive()),
                                new MqttDecoder(),
                                MqttEncoder.INSTANCE,
                                new NettyServerHandler());
                    }
                });
        try {
            channel = childBootstrap.bind(brokerProperties.getPort()).sync().channel();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    class NettyServerHandler extends SimpleChannelInboundHandler<MqttMessage> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) throws Exception {
            processMessageReceived(ctx, msg);
        }
    }

    @Override
    public void shutdown() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        if (this.publicExecutor != null) {
            this.publicExecutor.shutdown();
        }
        if (defaultEventExecutorGroup != null) {
            defaultEventExecutorGroup.shutdownGracefully();
        }
        if (channel != null) {
            channel.closeFuture().syncUninterruptibly();
        }
    }

    @Override
    public void registerProcess(BrokerProcess process, ExecutorService executorService) {
        this.defaultRequestProcessor = new Pair<>(process, executorService);
    }

    @Override
    public void registerProcess(int code, BrokerProcess process, ExecutorService executorService) {
        ExecutorService executorService1 = executorService;
        if (null == executorService1) {
            executorService1 = publicExecutor;
        }
        Pair<BrokerProcess, ExecutorService> pair = new Pair<>(process, executorService1);
        processorTable.put(code, pair);
    }

    private boolean useEpoll() {
        return brokerProperties.isUseEpoll();//Epoll.isAvailable() &&
    }
}

