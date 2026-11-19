package ua.edu.digitaldean.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import ua.edu.digitaldean.model.AcademicRequest;
import ua.edu.digitaldean.model.RequestKey;
import ua.edu.digitaldean.model.StudentId;
import ua.edu.digitaldean.model.TranscriptRequest;

public final class RequestImporter {
    public List<TranscriptRequest> read(Path path)
            throws IOException, RequestDataException {
        Objects.requireNonNull(path, "Шлях не може бути null.");
        try (BufferedReader reader = Files.newBufferedReader(
                path, StandardCharsets.UTF_8)) {
            return read(reader);
        }
    }

    public List<TranscriptRequest> read(BufferedReader reader)
            throws IOException, RequestDataException {
        Objects.requireNonNull(reader, "Засіб читання не може бути null.");
        List<TranscriptRequest> requests = new ArrayList<>();
        String line;
        int lineNumber = 0;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            requests.add(parse(line, lineNumber));
        }
        return List.copyOf(requests);
    }

    public ImportReport readAll(BufferedReader reader) throws IOException {
        Objects.requireNonNull(reader, "Засіб читання не може бути null.");
        List<TranscriptRequest> accepted = new ArrayList<>();
        List<RequestDataException> rejected = new ArrayList<>();
        String line;
        int lineNumber = 0;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            try {
                accepted.add(parse(line, lineNumber));
            } catch (RequestDataException exception) {
                rejected.add(exception);
            }
        }
        return new ImportReport(accepted, rejected);
    }

    public int registerUnique(
            List<? extends AcademicRequest> requests,
            RequestCatalog<AcademicRequest> catalog) {
        Objects.requireNonNull(requests, "Список заяв не може бути null.");
        Objects.requireNonNull(catalog, "Реєстр не може бути null.");
        for (AcademicRequest request : requests) {
            Objects.requireNonNull(request, "Елемент списку заяв не може бути null.");
        }
        if (requests.isEmpty()) {
            return 0;
        }

        Set<RequestKey> seenKeys = new HashSet<>();
        for (AcademicRequest request : requests) {
            RequestKey key = new RequestKey(request.requestId());
            if (!seenKeys.add(key) || catalog.find(key).isPresent()) {
                throw new DuplicateRequestException(key);
            }
        }

        int addedCount = 0;
        for (AcademicRequest request : requests) {
            if (catalog.add(request)) {
                addedCount++;
            }
        }
        return addedCount;
    }

    private static TranscriptRequest parse(String line, int lineNumber)
            throws RequestDataException {
        try {
            String[] fields = line.split(";", -1);
            if (fields.length != 4) {
                throw new IllegalArgumentException("Очікується чотири поля.");
            }
            String copiesText = fields[3].strip();
            if (!copiesText.matches("[0-9]+")) {
                throw new NumberFormatException("Кількість має бути цілим числом.");
            }
            int copies = Integer.parseInt(copiesText);
            return new TranscriptRequest(
                    fields[0], new StudentId(fields[1]), fields[2], copies);
        } catch (IllegalArgumentException exception) {
            throw new RequestDataException(lineNumber, exception);
        }
    }
}