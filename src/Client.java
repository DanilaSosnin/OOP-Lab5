import java.io.*;
import java.net.*;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8030;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {

            String name;
            while (true) {
                System.out.print("Введите ваше имя (без пробелов): ");
                name = userInput.readLine();

                // Проверяем наличие имени в файле clients.txt
                if (!name.contains(" ")) {
                    break; // Выход из цикла, если имя корректно и не занято
                } else {
                    System.out.println("Имя не должно содержать пробелов. Пожалуйста, попробуйте снова.");
                }
            }

            out.println(name);

            // Создаем поток для получения сообщений от сервера
            new Thread(() -> {
                String response;
                try {
                    while ((response = in.readLine()) != null) {
                        System.out.println(response);
                    }
                } catch (IOException e) {
                    System.out.println("Ошибка при получении сообщений: " + e.getMessage());
                }
            }).start();

            // Отправка сообщений
            String message;
            while ((message = userInput.readLine()) != null) {
                out.println(message);
            }

        } catch (IOException e) {
            System.out.println("Ошибка клиента: " + e.getMessage());
        }
    }

    private static boolean isNameTaken(String name) {
        try (BufferedReader reader = new BufferedReader(new FileReader("src\\clients.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().equals(name)) {
                    return true;
                }
            }
        } catch (IOException e) {
            System.out.println("Ошибка при проверке имени: " + e.getMessage());
        }
        return false;
    }
}