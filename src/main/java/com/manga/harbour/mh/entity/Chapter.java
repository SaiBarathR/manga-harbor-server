package com.manga.harbour.mh.entity;


import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Chapter {
    private String id;
    private String chapter;
    private List<Image> images;

    public Chapter(String id, String chapter) {
        this.id = id;
        this.chapter = chapter;
    }
  
}
