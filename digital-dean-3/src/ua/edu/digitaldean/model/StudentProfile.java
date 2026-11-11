package ua.edu.digitaldean.model;

public record StudentProfile(
        StudentId id,
        String fullName,
        EmailAddress email,
        StudyPlan studyPlan,
        Transcript transcript) {

    public StudentProfile {

        if (id == null) {
            throw new IllegalArgumentException(
                    "Ідентифікатор профілю не може бути null.");
        }

        fullName = ModelRules.requireText(
                fullName, "ПІБ студента не може бути порожнім.");

        if (email == null) {
            throw new IllegalArgumentException(
                    "Електронна адреса профілю не може бути null.");
        }

        if (studyPlan == null) {
            throw new IllegalArgumentException(
                    "Навчальний план профілю не може бути null.");
        }

        if (transcript == null) {
            throw new IllegalArgumentException(
                    "Транскрипт профілю не може бути null.");
        }
    }

    public StudentProfile withEmail(EmailAddress newEmail) {

        return new StudentProfile(
                id, fullName, newEmail, studyPlan, transcript);
    }

    public StudentProfile withTranscript(Transcript newTranscript) {

        return new StudentProfile(
                id, fullName, email, studyPlan, newTranscript);
    }
}