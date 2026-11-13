package com.scasp.model;

/** A student is a user with programme and year of study. Identity is inherited from User (userId = studentId). */
public class Student extends User {
    private final String programme;
    private final int yearOfStudy;
    public Student(String id, String name, String programme, int year) {
        super(id, name, Role.STUDENT);
        if (year < 1 || year > 7) throw new IllegalArgumentException("Year of study must be between 1 and 7");
        if (programme == null || programme.isBlank()) throw new IllegalArgumentException("Programme is required");
        this.programme = programme.trim(); this.yearOfStudy = year;
    }
    public String getStudentId() { return getUserId(); }
    public String getProgramme() { return programme; }
    public int getYearOfStudy() { return yearOfStudy; }
}
