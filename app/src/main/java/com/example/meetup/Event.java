package com.example.meetup;

import java.util.ArrayList;
import java.util.List;

public class Event {
    private String id;
    private String name;
    private String date;
    private String time;
    private String place;
    private List<Invitation> invitations;

    public Event() {
        invitations = new ArrayList<>();
    }
    public Event(String eventId, String eventName, String eventDate, String eventTime) {
        invitations = new ArrayList<>();
        id=eventId;
        name=eventName;
        date=eventDate;
        time=eventTime;
    }

    // Геттеры и сеттеры
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public String getPlace() {
        return place;
    }

}

