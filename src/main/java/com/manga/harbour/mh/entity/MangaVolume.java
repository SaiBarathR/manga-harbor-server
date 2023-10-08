package com.manga.harbour.mh.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MangaVolume {
    private String volume;
    private List<Chapter> chapters;
}

