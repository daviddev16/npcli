package com.networkprobe.cli;

import org.apache.commons.cli.CommandLine;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.StringJoiner;

public class NetworkExchangeClient extends ExecutionWorker {

    public static final int EXCHANGE_TCP_PORT = 14477;

    private final DiscoveryInformation discoveryInformation;
    private final CommandLine commandLine;

    public NetworkExchangeClient(CommandLine commandLine,
                                 DiscoveryInformation discoveryInformation) {
        super("network-exchange-client", false, false);
        this.discoveryInformation = discoveryInformation;
        this.commandLine = commandLine;
    }

    @Override
    protected void onBegin() {

        NetworkTesting.checkTCPPort(discoveryInformation.getAddress(), EXCHANGE_TCP_PORT);

        try (Socket clientSocket = new Socket(discoveryInformation.getAsInetAddress(), EXCHANGE_TCP_PORT))
        {
            clientSocket.setTcpNoDelay(true);
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
            Scanner reader = new Scanner(clientSocket.getInputStream());

            for (String commandName : commandLine.getOptionValues(Launcher.COMMAND_OPTION[0])) {

                long startDispatchTime = System.currentTimeMillis();

                String jsonCommandContent = createJSONOfCommand(commandName, commandLine);
                String commandAnswer = dispatchCommand(jsonCommandContent, reader, writer);
                /*Respondendo a saida padrão */
                System.out.println(commandAnswer);

                if (Launcher.isDebugModeEnabled())
                    System.out.println(" - SEND_RESPONSE_TOOK = " +
                            (System.currentTimeMillis() - startDispatchTime) + "ms");

            }

        }
        catch (Exception exception) {
            exception.printStackTrace();
            Util.handleException(exception, ExitCodes.EXCHANGE_CLIENT_CODE,
                    NetworkExchangeClient.class);
        }
    }

    /*
     * como é um objeto pequeno, será mais otimizado construir a String ao invés de transformar
     * as informações em o JSONObject e depois em um String.
     */
    public String createJSONOfCommand(String commandName, CommandLine commandLine) {
        return String.format("{\"cmd\": \"%s\", \"arguments\": %s}", commandName,
                createJSONArrayOfAllArguments(commandLine));
    }

    private String createJSONArrayOfAllArguments(CommandLine commandLine) {
        StringJoiner commaJoiner = new StringJoiner(", ");
        if (commandLine.hasOption(Launcher.ARGUMENT[0])) {
            for (String argument : commandLine.getOptionValues(Launcher.ARGUMENT[0])) {
                commaJoiner.add(JSONStringer.valueToString(argument));
            }
        }
        return String.format("[%s]", commaJoiner);
    }

    private String dispatchCommand(String command, Scanner reader, PrintWriter writer) {
        writer.println(command);
        return reader.nextLine();
    }

}
