package com.pizzastore.dto;

import java.util.List;

public class GoongDistanceResponse {
    private List<Row> rows;

    public List<Row> getRows() { return rows; }
    public void setRows(List<Row> rows) { this.rows = rows; }

    public static class Row {
        private List<Element> elements;
        public List<Element> getElements() { return elements; }
        public void setElements(List<Element> elements) { this.elements = elements; }
    }

    public static class Element {
        private String status;
        private Data distance;
        private Data duration; // Thời gian di chuyển

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Data getDistance() { return distance; }
        public void setDistance(Data distance) { this.distance = distance; }
        public Data getDuration() { return duration; }
        public void setDuration(Data duration) { this.duration = duration; }
    }

    public static class Data {
        private String text;
        private int value; // Giá trị bằng mét (hoặc giây)

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
    }
}