import java.io.*;
import java.net.*;
import java.nio.file.*;
import com.sun.net.httpserver.*;

public class Main {
  public static void main(String[] args) throws IOException {
    // HTTPサーバーの作成
    HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
    
    // ファイルアップロードの処理
    server.createContext("/upload", new HttpHandler() {
      @Override
      public void handle(HttpExchange exchange) throws IOException {
        // ファイルを保存するディレクトリのパス
        String uploadDir = "path/to/upload/directory";
        
        // ファイルを受信して保存する処理
        if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
          Headers headers = exchange.getRequestHeaders();
          String fileName = headers.getFirst("Content-Disposition").split(";")[1].trim().split("=")[1].replaceAll("\"", "");
          InputStream input = exchange.getRequestBody();
          Files.copy(input, Paths.get(uploadDir, fileName), StandardCopyOption.REPLACE_EXISTING);
          exchange.sendResponseHeaders(200, 0);
        } else {
          exchange.sendResponseHeaders(405, -1);
        }
        exchange.close();
      }
    });
    
    // ファイル一覧の取得の処理
    server.createContext("/files", new HttpHandler() {
      @Override
      public void handle(HttpExchange exchange) throws IOException {
        // ファイルを保存するディレクトリのパス
        String uploadDir = "path/to/upload/directory";
        
        // ファイル一覧の取得とJSONレスポンスの作成
        File[] files = new File(uploadDir).listFiles();
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < files.length; i++) {
          json.append("\"").append(files[i].getName()).append("\"");
          if (i < files.length - 1) {
            json.append(",");
          }
        }
        json.append("]");
        
        // JSONレスポンスの送信
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, json.length());
        OutputStream output = exchange.getResponseBody();
        output.write(json.toString().getBytes());
        output.flush();
        exchange.close();
      }
    });
    
    // ファイルダウンロードの処理
    server.createContext("/download", new HttpHandler() {
      @Override
      public void handle(HttpExchange exchange) throws IOException {
        // ファイルを保存するディレクトリのパス
        String uploadDir = "path/to/upload/directory";
        
        // ダウンロード対象のファイル名の取得
        String fileName = exchange.getRequestURI().getQuery().split("=")[1];
        
        // ファイルの存在チェックと送信
        File file = new File(uploadDir, fileName);
        if (file.exists()) {
          exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
          exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
          exchange.sendResponseHeaders(200, file.length());
          OutputStream output = exchange.getResponseBody();
          Files.copy(file.toPath(), output);
          output.flush();
        } else {
          exchange.sendResponseHeaders(404, -1);
        }
        exchange.close();
      }
    });
    
    // サーバーの開始
    server.start();
  }
}
