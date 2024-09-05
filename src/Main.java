import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Main {
    public static final int PORT = 8080;
    public static final String YANDEX_KEY = Token.token;

    public static void main(String[] args) throws Exception {
        //запуск сервера
        HttpServer server = HttpServer.create();
        server.bind(new InetSocketAddress(PORT), 0);
        server.createContext("/download", handler -> {
                    try (handler) {
                        var htmlContent = """
                                <html>
                                <head>
                                    <title>Uploader</title>
                                    <script>
                                        function uploadFile() {
                                            let url = prompt("Enter url to upload");
                                            if (url) {
                                                console.log(url);
                                                fetch('/upload', {
                                                    'method': 'POST',
                                                    'body': url
                                                });
                                            }
                                        }
                                    </script>
                                </head>
                                <body>
                                    <button onclick="uploadFile()">Upload</button>
                                </body>
                                </html>
                                """;
                        handler.sendResponseHeaders(200, htmlContent.getBytes(StandardCharsets.UTF_8).length);
                        handler.getResponseBody().write(htmlContent.getBytes(StandardCharsets.UTF_8));
                    } catch (Exception e) {
                        System.out.println("Ошибка загрузки");
                    }
                }
        );

        server.createContext("/upload", handler -> {
            try (handler) {
                String url = new String(handler.getRequestBody().readAllBytes());
                System.out.println(url);
                String[] urlParts = url.split("\\.");
                String ext = urlParts[urlParts.length - 1];
                HttpRequest yaRequest = HttpRequest.newBuilder()
                        .uri(URI.create("https://cloud-api.yandex.net/v1/disk/resources/upload?url=" +
                                URLEncoder.encode(url, Charset.defaultCharset()) + "&path=Uploads/" +
                                System.currentTimeMillis() + "." + ext))
                        .header("Authorization", "OAuth " + YANDEX_KEY)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
                HttpResponse<String> httpResponse = HttpClient.newHttpClient().send(yaRequest, HttpResponse.BodyHandlers.ofString());
                System.out.println(httpResponse.body());
            } catch (InterruptedException e) {
                handler.sendResponseHeaders(500, 0);
            }
        });
        server.start();
        System.out.println("Сервер успешно стартовал на порту " + PORT);
    }
}