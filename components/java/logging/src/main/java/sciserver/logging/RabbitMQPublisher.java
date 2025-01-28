package sciserver.logging;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RabbitMQPublisher implements Runnable {

    private Connection connection;
    private Channel channel;
    private BlockingQueue<String> queueBuffer;
    private String host;
    private int port;
    private String exchange;
    private String queue;
    private String routingKey = "#";
    private int numRetries = 3;
    private int backoffMultiplier = 250;
    private Thread publisherThread;

    public RabbitMQPublisher(String host, int port, String exchange) {
        this.host = host;
        this.port = port;
        this.exchange = exchange;
        this.queueBuffer = new ArrayBlockingQueue<String>(1024);
        this.publisherThread = new Thread(this);
        this.publisherThread.setDaemon(true);
        this.publisherThread.start();
    }

    private void connect() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(host);
            if (port > 0)
                factory.setPort(port);
            connection = factory.newConnection();
            channel = connection.createChannel();
        } catch (IOException e) {
            Logger.selfDebug("Error connecting to RabbitMQ at " + host + ":" + port + ": " + e);
        }
    }

    private void reconnect() {
        try {
            connection.close();
        } catch (Exception e) {
        }
        connect();
    }

    public void enqueue(String msg) {
        queueBuffer.offer(msg);
    }

    public void run() {
        Logger.selfDebug("RabbitMQ message processor thread started");
        while (true) {
            try {
                String msg = queueBuffer.take();
                if (channel == null)
                    connect();
                int retriesLeft = numRetries;
                while (retriesLeft > 0) {
                    try {
                        channel.basicPublish(exchange, routingKey, null, msg.getBytes());
                        break;
                    } catch (Exception e) {
                        int backoff = backoffMultiplier * (numRetries - retriesLeft--);
                        Logger.selfDebug("Publish Error (" + retriesLeft + " retries, " + backoff + " backoff): " + e);
                        Thread.sleep(backoff);
                        reconnect();
                    }
                }
            } catch (Exception e) {
                Logger.selfDebug("Caught exception in RabbitMQ processor thread code: " + e);
            }
        }
    }
}
