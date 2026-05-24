package ai;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class ApiKeyStore {
    private static final Path API_KEY_FILE = Paths.get("groq-api.properties");
    private static final String API_KEY_PROPERTY = "groq.api.key";

    public String loadApiKey() throws IOException {
        if (!Files.exists(API_KEY_FILE)) {
            return null;
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(API_KEY_FILE)) {
            properties.load(input);
        }

        String apiKey = properties.getProperty(API_KEY_PROPERTY);
        return apiKey == null || apiKey.isBlank() ? null : apiKey.trim();
    }

    public void saveApiKey(String apiKey) throws IOException {
        Properties properties = new Properties();
        properties.setProperty(API_KEY_PROPERTY, apiKey.trim());

        try (OutputStream output = Files.newOutputStream(API_KEY_FILE)) {
            properties.store(output, "Groq API key for FFShare AI chat");
        }
    }

}
