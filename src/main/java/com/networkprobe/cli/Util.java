package com.networkprobe.cli;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static java.lang.String.format;

public class Util {

    public static File checkIsReadable(File file, String name) {
        checkIfExists(file, name);
        if (!file.canRead())
            throw new SecurityException( format("Não é possível ler o arquivo \"%s\".", name) );
        return file;
    }

    public static File checkIfExists(File file, String name) {
        checkIsNotNull(file, name);
        if (!file.exists())
            throw new NullPointerException( format("O arquivo \"%s\" não existe.", name) );
        return file;
    }

    public static <E> E checkIsNotNull(E object, String name) {
        if (object == null)
            throw new NullPointerException( format("O campo \"%s\" não pode ser nulo.", name) );
        return object;
    }

    public static void writeFile(File file, String content) throws IOException {
        checkIsReadable(file, file.getName());
        Files.write(Paths.get(file.toURI()), content.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.WRITE);
    }

    public static String readFile(File file) throws IOException {
        checkIsReadable(file, file.getName());
        return String.join("\n", Files.readAllLines(Paths.get(file.toURI()))).trim();
    }

    public static void handleException(Exception exception, int exitCode, Class<?> originClass) {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("originClass", exception.getClass().getName());
        jsonObject.put("exceptionType", exception.getClass().getName());
        jsonObject.put("exceptionMessage", exception.getMessage());

        if (exception.getCause() != null)
            jsonObject.put("exceptionCauseType", exception.getCause().getClass().getName());

        JSONArray jsonArray = new JSONArray();
        for (StackTraceElement traceElement : exception.getStackTrace())
            jsonArray.put(traceElement.toString());

        jsonObject.put("exceptionStacktrace", jsonArray);
        System.out.println(jsonObject);
        Runtime.getRuntime().exit(exitCode);
    }

}
