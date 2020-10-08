package network;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class WebServer{
    private static final String STATUS_ENDPOINT = "/status";
    private final int port;
    private final OnRequestCallBack onRequestCallBack;
    private HttpServer httpServer;

    public WebServer(int port, OnRequestCallBack onRequestCallBack){
        this.onRequestCallBack = onRequestCallBack;
        this.port = port;
    }

    public void startServer(){
        try {
            this.httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        HttpContext statusContext = this.httpServer.createContext(STATUS_ENDPOINT);
        HttpContext taskContext = this.httpServer.createContext(onRequestCallBack.getEndPoint());

        statusContext.setHandler(this::handleStatusRequest);
        taskContext.setHandler(this::handleTaskRequest);

        httpServer.setExecutor(Executors.newFixedThreadPool(4));
        httpServer.start();
    }

    public void stop(){
        httpServer.stop(0);
    }

    public void handleStatusRequest(HttpExchange exchange) throws IOException {
        if(!exchange.getRequestMethod().equalsIgnoreCase("get")){
            exchange.close();
            return;
        }

        String responseMessage = "The server is alive";
        sendResponse(responseMessage.getBytes(), exchange);
    }

    public void handleTaskRequest(HttpExchange exchange) throws IOException {
        if(!exchange.getRequestMethod().equalsIgnoreCase("post")){
            exchange.close();
            return;
        }

        byte[] requestResponse = onRequestCallBack.handleTask(exchange.getRequestBody().readAllBytes());
        sendResponse(requestResponse, exchange);
    }

    public void sendResponse(byte[] responseMessage, HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(200, responseMessage.length);
        OutputStream responseBody = exchange.getResponseBody();
        responseBody.write(responseMessage);
        responseBody.flush();
        responseBody.close();
    }
}
