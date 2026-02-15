package com.megan.dataproject.model;

public enum StudentClass {

    Class1, Class2, Class3, Class4, Class5;

    // Helper method to pick random class for generation

    public static StudentClass getRandom() {
        return values()[new java.util.Random().nextInt(values().length)];
    }
}
