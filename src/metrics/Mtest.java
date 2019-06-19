package metrics;

import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;

public class Mtest {

   private final static int _PORT = 8088;

   public static void main(String[] args) throws Exception {
      HttpServer server = HttpServer.create(new InetSocketAddress(_PORT), 0);
      server.createContext("/metrics", new MetricsHandler());
      server.createContext("/kpis", new KPISHandler());
      server.setExecutor(null);
      server.start();
   }
}
