import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Rabbit_broadcast {
    public static String exchange_name = "rabbit_project_broadcast";
    public static class Send implements Runnable{
        private final static String QUEUE_NAME = "hello";
        private String username;

        @Override
        public void run() {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            try(Connection connection = factory.newConnection();
                Channel channel = connection.createChannel()){
                Scanner s = new Scanner(System.in);
                channel.exchangeDeclare(exchange_name, "fanout");
                System.out.println("Podaj nazwę użytkownika:");
                username = s.nextLine();
                while (true) {
                    String message = (username + ": " + s.nextLine());
                    channel.basicPublish(exchange_name, "", null, message.getBytes());
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class Read implements Runnable{
        private final static String QUEUE_NAME = "hello";

        @Override
        public void run() {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            try{
                Connection connection = factory.newConnection();
                Channel channel = connection.createChannel();
                channel.exchangeDeclare(exchange_name, "fanout");
                String queue_name = channel.queueDeclare().getQueue();
                channel.queueBind(queue_name, exchange_name, "");
                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    System.out.println(message);
                };
                channel.basicConsume(queue_name, true, deliverCallback, consumerTag -> { });

            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String[] argv) throws Exception {
        ExecutorService ex = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        ex.submit(new Send());
        ex.submit(new Read());
        ex.shutdown();
        ex.awaitTermination(1, TimeUnit.DAYS);
    }
}
