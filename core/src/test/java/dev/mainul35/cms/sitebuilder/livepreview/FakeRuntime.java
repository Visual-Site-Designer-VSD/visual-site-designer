package dev.mainul35.cms.sitebuilder.livepreview;

import com.sun.net.httpserver.HttpServer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * A stand-in for the runtime process: answers on the given port, tells the token it was started with, and can
 * be told to die at startup ("die") or to exit by itself after a moment ("short").
 */
public class FakeRuntime {

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(args[0]);
        String mode = args.length > 1 ? args[1] : "";
        if (mode.equals("die")) {
            System.out.println("boom: cannot start");
            System.exit(3);
        }
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        server.createContext("/", ex -> {
            String path = ex.getRequestURI().getPath();
            byte[] body = (path.equals("/token") ? String.valueOf(System.getenv("VSD_RUNTIME_CONSOLE_ADMINTOKEN")) : "ok")
                    .getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        server.start();
        System.out.println("fake runtime on " + port);
        if (mode.equals("short")) {
            Thread.sleep(1500);
            System.exit(0);
        }
        Thread.sleep(Long.MAX_VALUE);
    }
}
