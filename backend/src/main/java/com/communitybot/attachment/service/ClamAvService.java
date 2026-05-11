package com.communitybot.attachment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Streams file bytes to a running clamd daemon using the INSTREAM protocol.
 *
 * <p>Protocol summary:
 * <ol>
 *   <li>Connect to clamd on TCP port 3310.</li>
 *   <li>Send the null-terminated command {@code zINSTREAM\0}.</li>
 *   <li>Write each chunk as [4-byte big-endian length][chunk bytes].</li>
 *   <li>Terminate the stream with 4 zero bytes.</li>
 *   <li>Read the response line: {@code stream: OK} or {@code stream: <virus> FOUND}.</li>
 * </ol>
 */
@Service
@Slf4j
public class ClamAvService {

    private static final int CHUNK_SIZE = 8192;

    @Value("${app.clamav.host}")
    private String host;

    @Value("${app.clamav.port}")
    private int port;

    @Value("${app.clamav.timeout-seconds:30}")
    private int timeoutSeconds;

    public ScanResult scan(InputStream fileStream) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutSeconds * 1000);
            socket.setSoTimeout(timeoutSeconds * 1000);

            OutputStream out = socket.getOutputStream();
            DataInputStream in  = new DataInputStream(socket.getInputStream());

            // Initiate INSTREAM command
            out.write("zINSTREAM\0".getBytes(StandardCharsets.US_ASCII));

            // Stream file in fixed-size chunks
            byte[] buffer = new byte[CHUNK_SIZE];
            int    read;
            while ((read = fileStream.read(buffer)) > 0) {
                byte[] sizeHeader = ByteBuffer.allocate(4).putInt(read).array();
                out.write(sizeHeader);
                out.write(buffer, 0, read);
            }

            // Signal end of stream
            out.write(new byte[4]);
            out.flush();

            // Read clamd response (single line)
            StringBuilder sb = new StringBuilder();
            int b;
            while ((b = in.read()) != -1 && b != '\n') {
                sb.append((char) b);
            }
            String response = sb.toString().trim();
            boolean clean = response.endsWith("OK");
            log.debug("ClamAV response: {}", response);
            return new ScanResult(clean, response);
        }
    }

    public record ScanResult(boolean clean, String rawResponse) {}
}
