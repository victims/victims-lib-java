package com.redhat.victims.mock;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;

import org.apache.commons.io.FileUtils;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

@SuppressWarnings("restriction")
public class MockService {
	private static Integer PORT = 1337;
	private static File MOCK_RESPONSE_FILE = new File(
			"testdata/service/test.response");
	private static HttpServer httpd;
	private static boolean running = false;

	public static void start() throws IOException {
		if(!running){
			httpd = HttpServer.create(new InetSocketAddress(PORT), 0);
			HttpHandler update = new HttpHandler() {
				public void handle(HttpExchange exchange) {

					try {
						final byte[] json = FileUtils
								.readFileToByteArray(MOCK_RESPONSE_FILE);
						exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK,
								json.length);
						exchange.getResponseBody().write(json);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			
			HttpHandler remove = new HttpHandler() {
				public void handle(HttpExchange exchange) {

					try {
						final byte[] json = "[]".getBytes();
						exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK,
								json.length);
						exchange.getResponseBody().write(json);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};

			httpd.createContext("/service/update/", update);
			httpd.createContext("/service/remove/", remove);
			httpd.start();
			running = true;
		}
	}

	public static void stop() {
		httpd.stop(0);
		running = false;
	}
	
	public static String uri(){
		return String.format("http://localhost:%d/", PORT);
	}
	
	public static boolean isRunning(){
		return running;
	}
}
