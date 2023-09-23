package com.manga.harbour.mh.entity;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;




public class MangaVolumeDTO {
    private String volume;
    private List<Chapter> chapters;

    public MangaVolumeDTO() {
    }

    public MangaVolumeDTO(String volume, List<Chapter> chapters) {
        this.volume = volume;
        this.chapters = chapters;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public List<Chapter> getChapters() {
        return chapters;
    }

    public void setChapters(List<Chapter> chapters) {
        this.chapters = chapters;
    }
}

