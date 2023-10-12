package com.manga.harbour.mh.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MangaDownloadDetails {
    private List<MangaVolume> mangaData;
    private String method;
    private long folderSize;
    private Path folderPath;
    private String name;
    List<String> zipPaths;

    public void updateZipPath(String newPath) {
        zipPaths.add(newPath);
    }

}
