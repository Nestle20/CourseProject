package com.example.c1;

import java.io.Serializable;

public class Director implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String name;

    public Director(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }

    @Override
    public String toString() {
        return name;
    }
}