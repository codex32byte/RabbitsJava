package com.example.JavaRabbit.controller;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class TCPServer {

    private static List<ObjectOutputStream> outputStreams = new ArrayList<>();
    private static List<Socket> clientSockets = new ArrayList<>();
    private static final int PORT_RANGE_START = 5000;
    private static final int PORT_RANGE_END = 6000;

    public static void main(String[] args) {
        int port = PORT_RANGE_START;

        try {
            ServerSocket serverSocket = null;
            while (serverSocket == null && port <= PORT_RANGE_END) {
                try {
                    serverSocket = new ServerSocket(port);
                } catch (IOException e) {
                    // Port is busy, try the next one
                    port++;
                }
            }

            if (serverSocket == null) {
                System.err.println("Could not find a free port within the specified range.");
                return;
            }

            System.out.println("Server started. Listening on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientSockets.add(clientSocket);
                ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                outputStreams.add(outputStream);
                new Thread(() -> handleClient(clientSocket)).start();
                sendConnectedClientsList();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void handleClient(Socket clientSocket) {
        try {
            ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream());
            while (true) {
                List<Object> receivedData = (List<Object>) inputStream.readObject();
                if (!receivedData.isEmpty()) {
                    transmitSettingsToClients(receivedData);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
                System.out.println("Client connection closed: " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
                clientSockets.remove(clientSocket);
                outputStreams.removeIf(outputStream -> {
                    try {
                        return outputStream.equals(clientSocket.getOutputStream());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                sendConnectedClientsList();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void transmitSettingsToClients(List<Object> settings) {
        for (ObjectOutputStream outputStream : outputStreams) {
            try {
                outputStream.writeObject(settings);
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void sendConnectedClientsList() {
        List<SocketAddress> clientAddresses = new ArrayList<>();
        for (Socket clientSocket : clientSockets) {
            clientAddresses.add(clientSocket.getRemoteSocketAddress());
        }

        for (ObjectOutputStream outputStream : outputStreams) {
            try {
                outputStream.writeObject(clientAddresses);
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}