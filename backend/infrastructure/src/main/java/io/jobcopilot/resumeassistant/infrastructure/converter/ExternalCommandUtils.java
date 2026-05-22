package io.jobcopilot.resumeassistant.infrastructure.converter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
public class ExternalCommandUtils {

    public static InputStream runPandoc(InputStream source, String sourceExt, String targetExt, String extraArgs) throws IOException {
        File inputFile = File.createTempFile("resume-in-" + UUID.randomUUID(), "." + sourceExt);
        File outputFile = File.createTempFile("resume-out-" + UUID.randomUUID(), "." + targetExt);

        try {
            FileUtils.copyInputStreamToFile(source, inputFile);

            String commandStr = String.format("pandoc %s -o %s %s", inputFile.getAbsolutePath(), outputFile.getAbsolutePath(), extraArgs != null ? extraArgs : "");
            log.info("Executing pandoc: {}", commandStr);

            CommandLine cmdLine = CommandLine.parse(commandStr);
            DefaultExecutor executor = DefaultExecutor.builder().get();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, errorStream);
            executor.setStreamHandler(streamHandler);

            try {
                executor.execute(cmdLine);
            } catch (Exception e) {
                log.error("Pandoc execution failed. Stdout: {}, Stderr: {}", outputStream.toString(), errorStream.toString());
                throw new IOException("Failed to convert with Pandoc. Check logs.", e);
            }

            if (!outputFile.exists() || outputFile.length() == 0) {
                throw new IOException("Pandoc output file is empty or missing.");
            }

            byte[] result = FileUtils.readFileToByteArray(outputFile);
            return new java.io.ByteArrayInputStream(result);

        } finally {
            FileUtils.deleteQuietly(inputFile);
            FileUtils.deleteQuietly(outputFile);
        }
    }

    public static InputStream runLibreOffice(InputStream source, String sourceExt, String targetFormat) throws IOException {
        File inputFile = File.createTempFile("resume-in-" + UUID.randomUUID(), "." + sourceExt);
        // soffice derives the output filename from the input filename automatically | soffice 根据输入文件名自动推导输出文件名
        File outDir = new File(System.getProperty("java.io.tmpdir"));
        String expectedOutName = inputFile.getName().replaceAll("\\." + sourceExt + "$", "") + "." + targetFormat;
        File outputFile = new File(outDir, expectedOutName);

        try {
            FileUtils.copyInputStreamToFile(source, inputFile);

            String commandStr = String.format("soffice --headless --convert-to %s --outdir %s %s",
                    targetFormat, outDir.getAbsolutePath(), inputFile.getAbsolutePath());
            log.info("Executing LibreOffice: {}", commandStr);

            CommandLine cmdLine = CommandLine.parse(commandStr);
            DefaultExecutor executor = DefaultExecutor.builder().get();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, errorStream);
            executor.setStreamHandler(streamHandler);

            try {
                executor.execute(cmdLine);
            } catch (Exception e) {
                log.error("LibreOffice execution failed. Stdout: {}, Stderr: {}", outputStream.toString(), errorStream.toString());
                throw new IOException("Failed to convert with LibreOffice. Check logs.", e);
            }

            if (!outputFile.exists() || outputFile.length() == 0) {
                throw new IOException("LibreOffice output file is empty or missing.");
            }

            byte[] result = FileUtils.readFileToByteArray(outputFile);
            return new java.io.ByteArrayInputStream(result);

        } finally {
            FileUtils.deleteQuietly(inputFile);
            FileUtils.deleteQuietly(outputFile);
        }
    }
}
