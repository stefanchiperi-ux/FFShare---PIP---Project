package ai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Trimite intrebari catre serviciul Groq AI.
 */
public class GroqAiService {
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Trimite o intrebare catre modelul AI si intoarce raspunsul.
     *
     * @param apiKey cheia pentru acces la API
     * @param prompt intrebarea utilizatorului
     * @return raspunsul primit de la AI
     * @throws IOException daca cererea sau raspunsul sunt invalide
     * @throws InterruptedException daca firul este intrerupt
     */
    public String ask(String apiKey, String prompt) throws IOException, InterruptedException {
        String requestBody = """
                {
                  "model": "%s",
                  "messages": [
                    {
                      "role": "system",
                      "content": "Esti asistentul AI integrat intr-o aplicatie personalizata . Raspunde clar, concis si util."
                    },
                    {
                      "role": "user",
                      "content": "%s"
                    }
                  ]
                }
                """.formatted(MODEL, escapeJson(prompt));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("COD: " + response.statusCode());
        }

        return extractContent(response.body());
    }

    /**
     * Extrage textul raspunsului din JSON-ul primit.
     *
     * @param json raspunsul complet de la API
     * @return continutul mesajului AI
     * @throws IOException daca formatul JSON nu este valid
     */
    private String extractContent(String json) throws IOException {
        String target = "\"content\":";
        int contentIndex = json.indexOf(target);

        if (contentIndex == -1) {
            throw new IOException("Nu am gasit raspunsul AI in JSON.");
        }

        int startQuote = json.indexOf('"', contentIndex + target.length());
        if (startQuote == -1) {
            throw new IOException("Raspuns AI invalid.");
        }

        StringBuilder content = new StringBuilder();
        boolean escaped = false;

        for (int i = startQuote + 1; i < json.length(); i++) {
            char current = json.charAt(i);

            if (escaped) {
                content.append(unescapeJsonChar(current));
                escaped = false;
            } else if (current == '\\') {
                escaped = true;
            } else if (current == '"') {
                return content.toString();
            } else {
                content.append(current);
            }
        }

        throw new IOException("Raspuns AI neterminat.");
    }

    /**
     * Pregateste textul pentru a fi pus intr-un JSON.
     *
     * @param text textul original
     * @return textul escapat pentru JSON
     */
    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Transforma un caracter escapat in caracter normal.
     *
     * @param escapedChar caracterul dupa backslash
     * @return caracterul real
     */
    private char unescapeJsonChar(char escapedChar) {
        return switch (escapedChar) {
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case '"' -> '"';
            case '\\' -> '\\';
            default -> escapedChar;
        };
    }
}
