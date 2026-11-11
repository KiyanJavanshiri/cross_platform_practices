package ua.edu.digitaldean.service;

import ua.edu.digitaldean.model.AcademicRequest;
import ua.edu.digitaldean.model.DormitoryRequest;
import ua.edu.digitaldean.model.EnrollmentRequest;
import ua.edu.digitaldean.model.StudentId;
import ua.edu.digitaldean.model.TranscriptRequest;

public final class DelimitedRequestFactory implements RequestFactory {

    @Override
    public AcademicRequest create(
            StudentId studentId,
            String specification) {
        if (specification == null || specification.isBlank()) {
            throw new IllegalArgumentException(
                    "Специфікація заяви не може бути порожньою.");
        }

        String[] fields = specification.split(":", -1);
        String type = fields[0].strip().toUpperCase();

        return switch (type) {
            case "ENROLLMENT" -> createEnrollment(studentId, fields);
            case "TRANSCRIPT" -> createTranscript(studentId, fields);
            case "DORMITORY" -> createDormitory(studentId, fields);
            default -> throw new IllegalArgumentException(
                    "Невідомий тип заяви: " + fields[0] + ".");
        };
    }

    private AcademicRequest createEnrollment(
            StudentId studentId,
            String[] fields) {
        if (fields.length != 3) {
            throw new IllegalArgumentException(
                    "Формат ENROLLMENT: <requestId>:<courseCode>.");
        }

        return new EnrollmentRequest(
                fields[1], studentId, fields[2]);
    }

    private AcademicRequest createTranscript(
            StudentId studentId,
            String[] fields) {
        if (fields.length != 4) {
            throw new IllegalArgumentException(
                    "Формат TRANSCRIPT: <requestId>:<language>:<copies>.");
        }

        int copies = Integer.parseInt(fields[3]);

        return new TranscriptRequest(
                fields[1], studentId, fields[2], copies);
    }

    private AcademicRequest createDormitory(
            StudentId studentId,
            String[] fields) {
        if (fields.length != 4) {
            throw new IllegalArgumentException(
                    "Формат DORMITORY: <requestId>:<building>:<months>.");
        }

        int months = Integer.parseInt(fields[3]);

        return new DormitoryRequest(
                fields[1], studentId, fields[2], months);
    }
}