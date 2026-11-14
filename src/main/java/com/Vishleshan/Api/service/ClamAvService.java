
package com.Vishleshan.Api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Service
public class ClamAvService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    @Value("${app.clamav.host}")
    private String host;

    @Value("${app.clamav.port}")
    private int port;

    @Value("${app.clamav.connection-timeout:3000}")
    private int connectionTimeout;

    @Value("${app.clamav.read-timeout:30000}")
    private int readTimeout;

    public void scan(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "File cannot be empty"
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "File size cannot exceed 5 MB"
            );
        }

        try (Socket socket = new Socket()) {

            socket.connect(
                    new InetSocketAddress(host, port),
                    connectionTimeout
            );

            socket.setSoTimeout(readTimeout);

            try (
                    InputStream fileInput = file.getInputStream();
                    DataOutputStream output =
                            new DataOutputStream(socket.getOutputStream());
                    InputStream responseInput = socket.getInputStream()
            ) {

                // 1. Start ClamAV INSTREAM command
                output.write(
                        "zINSTREAM\0".getBytes(StandardCharsets.UTF_8)
                );

                // 2. Send file in chunks
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;

                while ((bytesRead = fileInput.read(buffer)) != -1) {

                    totalBytes += bytesRead;

                    if (totalBytes > MAX_FILE_SIZE) {
                        throw new IllegalArgumentException(
                                "File size cannot exceed 5 MB"
                        );
                    }

                    output.writeInt(bytesRead);
                    output.write(buffer, 0, bytesRead);
                }

                // 3. Signal end of file stream
                output.writeInt(0);
                output.flush();

                // 4. Read ClamAV response
                ByteArrayOutputStream responseBuffer =
                        new ByteArrayOutputStream();

                int character;

                while ((character = responseInput.read()) != -1) {

                    if (character == 0) {
                        break;
                    }

                    responseBuffer.write(character);
                }

                if (character == -1) {
                    throw new IOException(
                            "ClamAV closed connection without a complete response"
                    );
                }

                String result = responseBuffer.toString(
                        StandardCharsets.UTF_8
                ).trim();

                // 5. Exact response validation
                if (result.endsWith(": OK")) {
                    return;
                }

                if (result.endsWith(" FOUND")) {
                    throw new IllegalArgumentException(
                            "Malware detected. File upload rejected."
                    );
                }

                throw new IllegalStateException(
                        "ClamAV returned an unexpected scan response"
                );
            }
        }
    }
}
