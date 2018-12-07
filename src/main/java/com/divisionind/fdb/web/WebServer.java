/*
 * DIVISION INDUSTRIES CONFIDENTIAL
 * __________________________________
 *
 *  2015-2018 Division Industries LLC
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property of Division Industries LLC
 * and its suppliers, if any. The intellectual and technical concepts contained herein are proprietary
 * to Division Industries LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents
 * in process, and are protected by trade secret or copyright law. Dissemination of this information
 * or reproduction of this material is strictly forbidden unless prior written permission is obtained
 * from Division Industries LLC.
 */

package com.divisionind.fdb.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class WebServer {

    private HttpServer server;

    public WebServer(int port) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext("/", new Handler());
        this.server.setExecutor(null); // creates a default executor
    }

    public HttpServer getServer() {
        return server;
    }

    private static class Handler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            writeResponse(t, "FapBot webserver is working");
        }
    }

    private static Map<String, String> queryToMap(String query){
        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String pair[] = param.split("=");
            if (pair.length>1) {
                result.put(pair[0], pair[1]);
            } else result.put(pair[0], "");
        }
        return result;
    }

    private static void writeResponse(HttpExchange t, String resp) throws IOException {
        byte[] response = resp.getBytes();
        t.sendResponseHeaders(200, response.length);
        OutputStream os = t.getResponseBody();
        os.write(response);
        os.flush();
        os.close();
    }
}