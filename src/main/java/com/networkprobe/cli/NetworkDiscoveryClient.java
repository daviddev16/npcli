package com.networkprobe.cli;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class NetworkDiscoveryClient extends ExecutionWorker {

    public static final Object LOCK = new Object();

    public static final byte[] HELLO_FLAG = "H".getBytes(StandardCharsets.UTF_8);
    public static final String BROADCAST_ADDRESS = "255.255.255.255";

    public static final int DISCOVERY_UDP_PORT = 14476;
    public static final int ATTEMPT_TIMEOUT = 1;
    public static final int MAX_ATTEMPTS = 3;

    private final AtomicReference<DiscoveryInformation> discoveryInformation = new AtomicReference<>(DiscoveryInformation.FAILED);
    private final DatagramSocket datagramSocket;
    private int attempts;

    public NetworkDiscoveryClient() throws SocketException, UnknownHostException {
        super("network-discovery-client", true, false);
        this.datagramSocket = new DatagramSocket(null);
        this.datagramSocket.bind(new InetSocketAddress("0.0.0.0", 0));
        this.datagramSocket.setBroadcast(true);
    }

    @Override
    protected void onBegin() {
        Thread discoveryListenerThread = new Thread(new NetworkDiscoveryListener());
        discoveryListenerThread.setName("network-discovery-listener");
        discoveryListenerThread.setDaemon(false);
        discoveryListenerThread.start();
    }

    @Override
    protected void onUpdate() {
        try {

            if (++attempts == MAX_ATTEMPTS)
                stop();

            /* Enviando flag de descoberta para todas as interfaces de rede */
            datagramSocket.send(new DatagramPacket(HELLO_FLAG, 0, HELLO_FLAG.length,
                    new InetSocketAddress(BROADCAST_ADDRESS, DISCOVERY_UDP_PORT)));

            Thread.sleep(TimeUnit.SECONDS.toMillis(ATTEMPT_TIMEOUT));
        }
        catch (InterruptedException ignored) {}
        catch (Exception exception)
        {
            Util.handleException(exception, ExitCodes.DISCOVERY_CLIENT_CODE, NetworkExchangeClient.class);
        }
    }

    @Override
    protected void onStop() {
        NetworkUtil.closeQuietly(datagramSocket);
        synchronized (LOCK) { LOCK.notify(); }
    }

    class NetworkDiscoveryListener implements Runnable
    {
        @Override
        public void run() {
            try {
                DatagramPacket incomingPacket = NetworkUtil.createABufferedPacket(HELLO_FLAG.length);
                synchronized (datagramSocket) {

                    if (datagramSocket.isClosed())
                        return;

                    datagramSocket.receive(incomingPacket);
                    byte[] incomingMessage = incomingPacket.getData();

                    if (incomingMessage[0] == HELLO_FLAG[0])
                        discoveryInformation.set(new DiscoveryInformation(incomingPacket.getAddress()
                                .getHostAddress(), System.currentTimeMillis()));

                    if (NetworkDiscoveryClient.this.isAlive())
                        NetworkDiscoveryClient.this.stop();
                }
            }
            /* socket é fechado caso não nas 3 tentativas não tenha recebido nada. Ignorar SocketException já que
            * O socket não ficará aguardando informação para sempre. */
            catch (SocketException ignored) {}
            catch (Exception exception)
            {
                Util.handleException(exception, ExitCodes.DISCOVERY_LISTENER_CODE, NetworkDiscoveryListener.class);
            }
        }
    }

    public DiscoveryInformation getDiscoveryResult() {
        return discoveryInformation.get();
    }

}
