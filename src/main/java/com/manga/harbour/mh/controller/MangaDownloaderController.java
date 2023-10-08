package com.manga.harbour.mh.controller;

import com.manga.harbour.mh.entity.MangaVolume;
import com.manga.harbour.mh.service.MangaDownloaderService;
import com.manga.harbour.mh.service.MangaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/manga/download")
public class MangaDownloaderController {
    @Autowired
    private MangaService mangaService;
    @Autowired
    private MangaDownloaderService mangaDownloaderService;

    @GetMapping("/{mangaId}")
    public ResponseEntity<byte[]> getManga(@PathVariable String mangaId) {
        List<MangaVolume> mangaVolumes = mangaService.getMangaVolumesById(mangaId, null, null);
        return mangaDownloaderService.createFolders(mangaVolumes, "byVolume");
    }

    @GetMapping("{mangaId}/{volume}")
    public ResponseEntity<byte[]> downloadVolume(@PathVariable("mangaId") String mangaId, @PathVariable("volume") String volume) {
        List<MangaVolume> mangaVolumes = mangaService.getMangaVolumesById(mangaId, volume, null);
        return mangaDownloaderService.createFolders(mangaVolumes, "byVolume");
    }

    @GetMapping("{mangaId}/{volume}/{chapter}")
    public ResponseEntity<byte[]> downloadChapter(@PathVariable("mangaId") String mangaId, @PathVariable("volume") String volume, @PathVariable("chapter") String chapter) {
        List<MangaVolume> mangaVolumes = mangaService.getMangaVolumesById(mangaId, volume, chapter);
        return mangaDownloaderService.createFolders(mangaVolumes, "byChapter");
    }
}
