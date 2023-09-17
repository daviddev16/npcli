package com.networkprobe.cli;

import java.io.Closeable;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class NetworkUtil {

	public static final int MIN_BUFFER_SIZE = 2;

	public static DatagramPacket createMessagePacket(InetAddress inetAddress, int port, String message) {
		Util.checkIsNotNull(inetAddress, "inetAddress");
		Util.checkIsNotNull(message, "message");
		byte[] buffer = message.getBytes(StandardCharsets.UTF_8);
		return new DatagramPacket(buffer, 0, buffer.length, inetAddress, port);
	}

	public static DatagramPacket createABufferedPacket(int length) {
		byte[] buffer = new byte[ (length < MIN_BUFFER_SIZE) ? MIN_BUFFER_SIZE : length ];
		return new DatagramPacket(buffer, 0, buffer.length);
	}

	public static String getBufferedData(DatagramPacket packet) {
		Util.checkIsNotNull(packet, "packet");
		return new String(packet.getData(), StandardCharsets.UTF_8).trim();
	}

	public static void closeQuietly(Closeable closeable) {
		try {
			if (closeable != null)
				closeable.close();
		} catch (Exception ignored) {/*  */}
	}

}
