package com.scamshield.app.data.model;

import java.io.Serializable;
import java.util.List;

public class EducationArticle implements Serializable {
    private final String id;
    private final String title;
    private final String subtitle;
    private final String category;
    private final String summary;
    private final List<String> redFlags;
    private final List<String> whatToDo;
    private final String realWorldExample;

    public EducationArticle(
            String id,
            String title,
            String subtitle,
            String category,
            String summary,
            List<String> redFlags,
            List<String> whatToDo,
            String realWorldExample) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.category = category;
        this.summary = summary;
        this.redFlags = redFlags;
        this.whatToDo = whatToDo;
        this.realWorldExample = realWorldExample;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getCategory() {
        return category;
    }

    public String getSummary() {
        return summary;
    }

    public List<String> getRedFlags() {
        return redFlags;
    }

    public List<String> getWhatToDo() {
        return whatToDo;
    }

    public String getRealWorldExample() {
        return realWorldExample;
    }
}
