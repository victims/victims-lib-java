package com.redhat.victims.mock;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;

import org.apache.commons.io.FileUtils;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

@SuppressWarnings("restriction")
public class MockService {
	private static Integer PORT = 1337;
	private static String DEFAULT_RESPONSE = "[]";
	private static HttpServer httpd;
	private static boolean running = false;

	public static void start(final File updateResponse,
			final File removeResponse) throws IOException {
		if (!running) {
			httpd = HttpServer.create(new InetSocketAddress(PORT), 0);
			HttpHandler empty = new GetHandler(DEFAULT_RESPONSE);
			HttpHandler update = updateResponse != null ? new GetHandler(
					updateResponse) : empty;
			HttpHandler remove = removeResponse != null ? new GetHandler(
					removeResponse) : empty;
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

	public static String uri() {
		return String.format("http://localhost:%d/", PORT);
	}

	public static boolean isRunning() {
		return running;
	}

	static class GetHandler implements HttpHandler {

		private byte[] json;

		public GetHandler(File response) {
			try {
				json = FileUtils.readFileToByteArray(response);
			} catch (IOException e) {
				json = "".getBytes();
			}
		}

		public GetHandler(String response) {
			json = response.getBytes();
		}

		public void handle(HttpExchange exchange) {
			try {
				Headers headers = exchange.getResponseHeaders();
				headers.add("Content-Type", "application/json");

				exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK,
						json.length);
				OutputStream os = exchange.getResponseBody();
				os.write(json);
				os.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
