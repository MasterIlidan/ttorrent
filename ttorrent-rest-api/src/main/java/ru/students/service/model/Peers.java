package ru.students.service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class Peers {
    private int leechers;
    private int seeders;
    public Peers() {
        leechers = 0;
        seeders = 0;
    }
}
