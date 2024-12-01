import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;

public class Server {
    private static final int PORT = 8030;
    private static Set<ClientHandler> clientHandlers = new HashSet<>();
    private static List<String> clientNames = new ArrayList<>();
    private static final String CLIENTS_FILE = "src\\clients.txt";

    public static void main(String[] args) {
        loadClientsFromFile(); // Загружаем существующих клиентов из файла
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Сервер запущен на порту " + PORT);
            new Thread(new ConsoleHandler()).start(); // Запускаем обработчик ввода с консоли
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            System.out.println("Ошибка сервера: " + e.getMessage());
        }
    }

    private static void loadClientsFromFile() {
        File file = new File(CLIENTS_FILE);
        if (!file.exists()) {
            try {
                file.createNewFile(); // Создаем новый файл, если он не существует
            } catch (IOException e) {
                System.out.println("Ошибка при создании файла clients.txt: " + e.getMessage());
            }
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(CLIENTS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                clientNames.add(line.trim());
            }
        } catch (IOException e) {
            System.out.println("Ошибка при загрузке клиентов из файла: " + e.getMessage());
        }
    }

    private static void saveClientToFile(String clientName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CLIENTS_FILE, true))) {
            writer.write(clientName);
            writer.newLine();
        } catch (IOException e) {
            System.out.println("Ошибка при сохранении клиента в файл: " + e.getMessage());
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private String clientName;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            clientHandlers.add(this);
        }

        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Получаем имя клиента
                clientName = in.readLine();

                // Проверяем, существует ли имя клиента
                if (!clientNames.contains(clientName)) {
                    clientNames.add(clientName);
                    saveClientToFile(clientName); // Сохраняем новое имя в файл
                }

                System.out.println(clientName + " подключен.");
                sendMessage("Вы подключены как " + clientName);

                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println(clientName + ": " + message);
                }
            } catch (IOException e) {
                System.out.println("Ошибка: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                    clientHandlers.remove(this);
                    clientNames.remove(clientName);
                } catch (IOException e) {
                    System.out.println("Ошибка при закрытии сокета: " + e.getMessage());
                }
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }
    }

    // Обработчик ввода с консоли для отправки сообщений
    private static class ConsoleHandler implements Runnable {
        @Override
        public void run() {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("Введите команду для рассылки сообщения (формат: /send <clientname1> <clientname2> ... '<message>'): ");
                String input = scanner.nextLine();

                if (input.startsWith("/send")) {
                    String regex = "/send\\s+((?:\\S+\\s*){1,100})'(.+)'";
                    Matcher matcher = java.util.regex.Pattern.compile(regex).matcher(input);

                    if (matcher.matches()) {
                        String recipientNamesString = matcher.group(1).trim(); // Имена клиентов
                        String message = matcher.group(2); // Сообщение с пробелами

                        String[] recipientNames = recipientNamesString.split("\\s+");
                        sendMessageToClients(recipientNames, message);
                    } else {
                        System.out.println("Неверный формат команды. Используйте: /send <clientname1> <clientname2> ... '<message>'");
                    }
                } else {
                    System.out.println("Неизвестная команда.");
                }
            }
        }

        private void sendMessageToClients(String[] recipientNames, String message) {

            boolean sendAll = false;
            for (String recipientName : recipientNames)
                if (Objects.equals("&all", recipientName)) {
                    for (ClientHandler handler : clientHandlers)
                        handler.sendMessage("Сообщение от сервера: " + message);
                    sendAll = true;
                }

            if (!sendAll) {
                for (String recipientName : recipientNames) {
                    boolean found = false;
                    for (ClientHandler handler : clientHandlers) {
                        if (handler.clientName.equals(recipientName)) {
                            handler.sendMessage("Сообщение от сервера: " + message);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        System.out.println("Клиент с именем '" + recipientName + "' не найден.");
                    }
                }
            }
        }
    }
}