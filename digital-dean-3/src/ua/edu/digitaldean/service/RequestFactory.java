package ua.edu.digitaldean.service;

import ua.edu.digitaldean.model.AcademicRequest;
import ua.edu.digitaldean.model.StudentId;

public interface RequestFactory {
    AcademicRequest create(StudentId studentId, String specification);
}