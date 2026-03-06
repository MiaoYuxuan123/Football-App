package com.example.football.entity;

public class user {
    private String name; // 对应后端user的username字段
    private String password;
    public user() {}
    public user(String name, String password) {
        this.name = name;
        this. password =  password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
