package com.manga.harbour.mh.controller;

import com.manga.harbour.mh.entity.MangaVolume;
import com.manga.harbour.mh.service.MangaDownloaderService;
import com.manga.harbour.mh.service.MangaService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@CrossOrigin(value = {"*"}, exposedHeaders = {"Content-Disposition"})
@RequestMapping("/manga/download")
public class MangaDownloaderController {
    @Autowired
    private MangaService mangaService;
    @Autowired
    private MangaDownloaderService mangaDownloaderService;

    @GetMapping("/{mangaId}")
    public void getManga(@PathVariable String mangaId, HttpServletResponse response) {
        List<MangaVolume> mangaVolumes = mangaService.getMangaVolumesById(mangaId, null, null);
        mangaDownloaderService.generateMangaAsZip(mangaId, mangaVolumes, "byVolume", response);
    }

    @GetMapping("{mangaId}/{volume}")
    public void downloadVolume(@PathVariable("mangaId") String mangaId, @PathVariable("volume") String volume, HttpServletResponse response) {
        List<MangaVolume> mangaVolumes = mangaService.getMangaVolumesById(mangaId, volume, null);
        mangaDownloaderService.generateMangaAsZip(mangaId, mangaVolumes, "byVolume", response);
    }

    @GetMapping("{mangaId}/{volume}/{chapter}")
    public void downloadChapter(@PathVariable("mangaId") String mangaId, @PathVariable("volume") String volume,
                                @PathVariable("chapter") String chapter, HttpServletResponse response) {
        List<MangaVolume> mangaVolumes = mangaService.getMangaVolumesById(mangaId, volume, chapter);
        mangaDownloaderService.generateMangaAsZip(mangaId, mangaVolumes, "byChapter", response);
    }
}
