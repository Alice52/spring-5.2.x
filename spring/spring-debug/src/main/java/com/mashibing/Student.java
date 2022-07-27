package com.mashibing;

public class Student {
    private static String gender = "男";

    private int id;

    private String name;

    public static String getGender() {
        return gender;
    }

    public static void setGender(String gender) {
        Student.gender = gender;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
