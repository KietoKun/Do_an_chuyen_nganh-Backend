package com.pizzastore.dto;

public class CommentRequest {
    private String content;
    private Integer rating;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
}
