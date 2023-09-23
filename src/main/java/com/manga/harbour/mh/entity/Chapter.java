package com.manga.harbour.mh.entity;

import java.util.List;

public class Chapter {
    private String id;
    private String chapter;
    private List<Image> images;

    public Chapter() {
    }

    public Chapter(String id, String chapter) {
        this.id = id;
        this.chapter = chapter;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getChapter() {
        return chapter;
    }

    public void setChapter(String chapter) {
        this.chapter = chapter;
    }

	public List<Image> getImages() {
		return images;
	}

	public void setImages(List<Image> images) {
		this.images = images;
	}
  
}
