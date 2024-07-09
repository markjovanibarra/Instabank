package com.example.instabank;

public class User {
    public String firstName;
    public String middleName;
    public String lastName;
    public String age;
    public String birthday;
    public String address;
    public String email;
    public String phoneNumber;
    public String pin;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String firstName, String middleName, String lastName, String age, String birthday,
                String address, String email, String phoneNumber, String pin) {
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.age = age;
        this.birthday = birthday;
        this.address = address;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.pin = pin;
    }
}