package com.manga.harbour.mh.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class MangaDownloadDetails {
    private ArrayList<String> volumes;
    private String chapters;
    private String method;
    private long folderSize;
    private Path folderPath;
    private String name;
    private List<String> zipPaths;

    public MangaDownloadDetails() {
        this.volumes = new ArrayList<>();
        this.chapters = "";
        this.method = "";
        this.folderSize = 0;
        this.folderPath = null;
        this.name = "";
        this.zipPaths = new ArrayList<>();
    }

    public void updateZipPath(String newPath) {
        zipPaths.add(newPath);
    }

    public void updateVolumeList(String volume) {
        volumes.add(volume);
    }

}
