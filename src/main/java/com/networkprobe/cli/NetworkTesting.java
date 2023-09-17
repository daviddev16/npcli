package com.networkprobe.cli;


import org.json.JSONObject;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;

public final class NetworkTesting {

    private static final int SOCKET_TIMEOUT = 5 * 1000;

    @Deprecated
    public static void checkUDPPort(String host, int port) {
        try {
            DatagramSocket datagramSocket = new DatagramSocket(new InetSocketAddress(host, port));
            datagramSocket.setSoTimeout(SOCKET_TIMEOUT);
            datagramSocket.setReuseAddress(true);
            datagramSocket.close();
        } catch (Exception e) {
            handleSocketException(e, host, port, "UDP/IP");
        }
    }

    public static void checkTCPPort(String host, int port) {
        try {
            Socket socket = new Socket(host, port);
            socket.setSoTimeout(SOCKET_TIMEOUT);
            socket.setReuseAddress(true);
            socket.close();
        } catch (Exception e) {
            handleSocketException(e, host, port, "TCP/IP");
        }
    }

    private static void handleSocketException(Exception exception, String host, int port, String protocol) {
        JSONObject jsonObject = new JSONObject()
                .put("message", String.format("Não foi possível estabelecer uma comunicação %s por que" +
                        " não foi possível se conectar a %s:%d.", protocol, host, port))
                .put("reason", exception.getClass().getSimpleName() + " -> " + exception.getMessage());
        System.out.println(jsonObject.toString());
        Runtime.getRuntime().exit(ExitCodes.SOCKET_EXCEPTION_CODE);
    }

}
