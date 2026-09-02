package kz.asfendiyarov.heartsimulator;

import android.net.Network;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public final class ApiClient {
    public static final String BASE = "http://192.168.4.1";
    private static volatile Network wifiNetwork;

    private ApiClient() {}

    public static void setNetwork(Network network) {
        wifiNetwork = network;
    }

    private static HttpURLConnection open(String path) throws IOException {
        Network network = wifiNetwork;
        if (network == null) {
            throw new IOException("Wi‑Fi сеть Heart-Simulator ещё не выбрана Android");
        }
        URL url = new URL(BASE + path);
        URLConnection raw = network.openConnection(url);
        if (!(raw instanceof HttpURLConnection)) {
            throw new IOException("Неподдерживаемое сетевое соединение");
        }
        return (HttpURLConnection) raw;
    }

    public static String get(String path) throws IOException {
        return get(path, 1200);
    }

    public static String get(String path, int readTimeoutMs) throws IOException {
        HttpURLConnection c = open(path);
        c.setRequestMethod("GET");
        c.setConnectTimeout(650);
        c.setReadTimeout(Math.max(1000, readTimeoutMs));
        c.setUseCaches(false);
        c.setRequestProperty("Connection", "close");
        return readResponse(c);
    }

    public static JSONObject getJson(String path) throws Exception {
        return new JSONObject(get(path, 1200));
    }

    public static String postJson(String path, String body) throws IOException {
        HttpURLConnection c = open(path);
        c.setRequestMethod("POST");
        c.setConnectTimeout(1000);
        c.setReadTimeout(6000);
        c.setDoOutput(true);
        c.setUseCaches(false);
        c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        c.setFixedLengthStreamingMode(data.length);
        try (OutputStream os = c.getOutputStream()) {
            os.write(data);
        }
        return readResponse(c);
    }

    private static String readResponse(HttpURLConnection c) throws IOException {
        try {
            int code = c.getResponseCode();
            InputStream in = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
            if (in == null) throw new IOException("HTTP " + code);

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line).append('\n');
            }

            if (code < 200 || code >= 300) {
                throw new IOException(sb.toString().trim().isEmpty() ? ("HTTP " + code) : sb.toString().trim());
            }
            return sb.toString().trim();
        } finally {
            c.disconnect();
        }
    }
}
