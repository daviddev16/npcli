package com.networkprobe.cli;

import org.apache.commons.cli.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

public class Launcher {

    public static final String   SYNC_FILENAME              = ".np-sync";

    public static final String[] COMMAND_OPTION             = {"c", "command"};
    public static final String[] SYNC_OPTION                = {"s", "sync"};
    public static final String[] DISCOVERY_ONLY             = {"d", "discovery-only"};
    public static final String[] DEBUG_MODE                 = {"b", "debug-mode"};
    public static final String[] ARGUMENT                   = {"a", "arg"};

    private static boolean DEBUG_MODE_VALUE = false;

    public static void main(String[] args) {

        try {

            if (System.getenv("STAGING") != null) {
                args = new String[]{"--command", "queryTags", "--arg", "ALTERDATA_PACK", "--sync", "-b"};
                System.out.println("STAGING");
            }

            Options options = new Options();

            options.addOption(COMMAND_OPTION[0], COMMAND_OPTION[1], true,
                    "Comando(s) a ser(em) enviado(s) para o servidor");

            options.addOption(SYNC_OPTION[0], SYNC_OPTION[1], false,
                    "Força o serviço de descoberta de rede e inicia comunicação TCP");

            options.addOption(DISCOVERY_ONLY[0], DISCOVERY_ONLY[1], false,
                    "Força o envio de mensagem de descoberta e não inicia comunicação TCP");

            options.addOption(DEBUG_MODE[0], DEBUG_MODE[1], false, "Modo debug");

            options.addOption(ARGUMENT[0], ARGUMENT[1], true,
                    "Argumentos requeridos pelo comando");

            CommandLineParser commandLineParser = new DefaultParser();
            CommandLine commandLine = commandLineParser.parse(options, args);

            DEBUG_MODE_VALUE = commandLine.hasOption(DEBUG_MODE[0]);

            final File syncFile = new File(System.getProperty("user.home"), SYNC_FILENAME);
            clearCachedSyncInformationIfNecessary(commandLine, syncFile);

            DiscoveryInformation discoveryInformation = getLastestDiscoveryInformation(commandLine, syncFile);

            if (discoveryInformation.equals(DiscoveryInformation.FAILED))
                throw new RuntimeException("O serviço de descoberta não achou nenhum ponto de entrada.");

            storeLastestSyncData(discoveryInformation, syncFile);

            if (commandLine.hasOption(DISCOVERY_ONLY[0]))
                Runtime.getRuntime().exit(ExitCodes.DISCOVERY_DONE_SUCCESSFULLY);

            if (!commandLine.hasOption(COMMAND_OPTION[0]) && !commandLine.hasOption(SYNC_OPTION[0]))
                Runtime.getRuntime().exit(ExitCodes.NO_COMMAND_CODE);


            NetworkExchangeClient exchangeClient = new NetworkExchangeClient(commandLine, discoveryInformation);
            exchangeClient.start();

        } catch (Exception exception)
        {
            Util.handleException(exception, ExitCodes.LAUNCHER_CODE, Launcher.class);
        }

    }

    /*
    *
    * se a flag --discovery-only for passada, independente de ter um arquivo ou não, será feito o processo
    * de descoberta novamente
    *
    * */
    private static DiscoveryInformation getLastestDiscoveryInformation(CommandLine commandLine, File syncFile) {
        try {

            if (syncFile.exists() && !commandLine.hasOption(DISCOVERY_ONLY[0]))
                return new DiscoveryInformation(new JSONObject(Util.readFile(syncFile)));

            NetworkDiscoveryClient networkDiscoveryClient = new NetworkDiscoveryClient();
            networkDiscoveryClient.start();

            synchronized (NetworkDiscoveryClient.LOCK) {
                NetworkDiscoveryClient.LOCK.wait();
            }

            return networkDiscoveryClient.getDiscoveryResult();

        } catch (IOException e) {
            throw new RuntimeException(
                    "Houve um erro ao tentar ler o arquivo de sincronização (message = " + e.getMessage() + ")." +
                            "Isso geralmente ocorrer devido as seguintes circunstâncias: \n" +
                            " - O usuário executando a aplicação não possui permissão de leitura.\n" +
                            " - O arquivo não existe ou não pode ser executado.\n" +
                            " - Verifique as permissões do arquivo ( " + syncFile.getAbsolutePath() + " )");
        } catch (RuntimeSocketException e) {
            throw new RuntimeException(
                    "Houve um erro ao transmitir um pacote de descoberta (message = " + e.getMessage() + ")." +
                            "Isso geralmente ocorrer devido as seguintes circunstâncias: \n" +
                            " - Não foi possível abrir um socket UDP na porta " + NetworkDiscoveryClient.DISCOVERY_UDP_PORT + ".\n" +
                            " - Houve algum erro de entrada/saída na transmissão dos dados pelo socket.\n" +
                            " - Verifique se a porta " + NetworkDiscoveryClient.DISCOVERY_UDP_PORT + " possui o trafego de saída aberto no firewall.\n" +
                            " - Verifique se a aplicação Java possui permissão de entrada no firewall.");
        }
        catch (Exception e) {
            throw new RuntimeException(
                    "Erro não tratado (message = " + e.getMessage() + "). " +
                            "Verifique com o suporte da aplicação.");
        }
    }

    private static void storeLastestSyncData(DiscoveryInformation discoveryInformation,
                                             File syncFile) throws IOException {
        if (!syncFile.exists()) {
            syncFile.createNewFile();
        }
        Util.writeFile(syncFile, discoveryInformation.toJsonString());
    }

    private static void clearCachedSyncInformationIfNecessary(CommandLine commandLine,
                                                              File syncFile) throws IOException
    {
        if ( (commandLine.hasOption(SYNC_OPTION[0]) || commandLine.hasOption(DISCOVERY_ONLY[0])) && syncFile.exists())
            if (!syncFile.delete())
                throw new IOException("Não foi possível apagar o arquivo da última descoberta.");
    }

    public static boolean isDebugModeEnabled() {
        return DEBUG_MODE_VALUE;
    }

}
