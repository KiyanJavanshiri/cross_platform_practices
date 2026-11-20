package ua.edu.digitaldean.service;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import ua.edu.digitaldean.model.StudentId;
import ua.edu.digitaldean.model.TranscriptRequest;

public record RequestFileRow(
        String requestId,
        StudentId studentId,
        String language,
        int copies,
        String note) {
    public RequestFileRow {
        TranscriptRequest checked = new TranscriptRequest(
                requestId, studentId, language, copies);
        requestId = checked.requestId();
        studentId = checked.studentId();
        language = checked.language();
        Objects.requireNonNull(note, "Примітка не може бути null.");
        if (!StandardCharsets.UTF_8.newEncoder().canEncode(note)
                || note.codePointCount(0, note.length()) > 200
                || note.codePoints().anyMatch(c -> Character.isISOControl(c)
                && c != '\r' && c != '\n')) {
            throw new IllegalArgumentException("Некоректна примітка.");
        }
    }

    public TranscriptRequest toRequest() {
        return new TranscriptRequest(requestId, studentId, language, copies);
    }
}
