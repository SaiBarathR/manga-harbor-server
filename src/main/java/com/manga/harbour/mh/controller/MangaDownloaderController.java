package com.manga.harbour.mh.controller;

import com.manga.harbour.mh.entity.MangaDownloadDetails;
import com.manga.harbour.mh.entity.MangaVolume;
import com.manga.harbour.mh.service.MangaDownloaderService;
import com.manga.harbour.mh.service.MangaService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(value = {"*"}, exposedHeaders = {"Content-Disposition", "file-size"})
@RequestMapping("/manga/download")
public class MangaDownloaderController {
    @Autowired
    private MangaService mangaService;
    @Autowired
    private MangaDownloaderService mangaDownloaderService;

    @PostMapping("/")
    public void downloadManga(@RequestBody MangaDownloadDetails mangaDownloadDetails, HttpServletResponse response) {
        mangaDownloaderService.streamZipData(mangaDownloadDetails, response);
    }

    @GetMapping("/{mangaId}")
    public MangaDownloadDetails getManga(@PathVariable String mangaId) {
        try {
            List<MangaVolume> mangaVolumes = mangaService.getMangaVolumesById(mangaId, null, null);
            return mangaDownloaderService.createFileStructureForManga(mangaId, mangaVolumes, "byVolume");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @GetMapping("{mangaId}/{volume}")
    public MangaDownloadDetails downloadVolume(@PathVariable("mangaId") String mangaId, @PathVariable("volume") String volume) {
        try {
            List<MangaVolume> mangaVolumes = mangaService.getMangaVolumesById(mangaId, volume, null);
            return mangaDownloaderService.createFileStructureForManga(mangaId, mangaVolumes, "byVolume");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @GetMapping("{mangaId}/{volume}/{chapter}")
    public MangaDownloadDetails downloadChapter(@PathVariable("mangaId") String mangaId, @PathVariable("volume") String volume, @PathVariable("chapter") String chapter) {
        try {
            List<MangaVolume> mangaVolumes = mangaService.getMangaVolumesById(mangaId, volume, chapter);
            MangaDownloadDetails mangaDownloadDetails = mangaDownloaderService.createFileStructureForManga(mangaId, mangaVolumes, "byChapter");
            System.out.println("mangaDownloadDetails" + mangaDownloadDetails.toString());
            return mangaDownloadDetails;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }
}
