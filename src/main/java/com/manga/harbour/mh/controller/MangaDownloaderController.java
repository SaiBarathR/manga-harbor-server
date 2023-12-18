package com.manga.harbour.mh.controller;

import com.manga.harbour.mh.entity.ErrorResponse;
import com.manga.harbour.mh.entity.MangaDownloadDetails;
import com.manga.harbour.mh.entity.MangaVolume;
import com.manga.harbour.mh.service.MangaDownloaderService;
import com.manga.harbour.mh.service.MangaService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.util.List;

@RestController
@CrossOrigin(value = {"*"}, exposedHeaders = {"Content-Disposition", "file-size"})
@RequestMapping("/manga/download")
public class MangaDownloaderController {
    Logger logger = LoggerFactory.getLogger(MangaDownloaderService.class);

    @Autowired
    private MangaService mangaService;
    @Autowired
    private MangaDownloaderService mangaDownloaderService;

    @PostMapping("/")
    public void downloadManga(@RequestBody MangaDownloadDetails mangaDownloadDetails, HttpServletResponse response) throws UnsupportedEncodingException {
        mangaDownloaderService.streamZipData(mangaDownloadDetails, response);
    }

    @GetMapping("/{mangaId}")
    public ResponseEntity<?> getManga(@PathVariable String mangaId) {
        try {
            List<MangaVolume> mangaVolumes = mangaService.getMangaVolumesById(mangaId, null, null);
            if(mangaVolumes.isEmpty()){
                return new ResponseEntity<>(new ErrorResponse("No manga volumes found for the manga with id: " + mangaId), HttpStatus.NOT_FOUND);
            }
            MangaDownloadDetails mangaDownloadDetails = mangaDownloaderService.createFileStructureForManga(mangaId, mangaVolumes, "byVolume");
            return new ResponseEntity<>(mangaDownloadDetails, HttpStatus.OK);
        } catch (Exception e) {
            logger.trace("unable to prepare download data for the manga with id: " + mangaId, e);
            return new ResponseEntity<>(new ErrorResponse("Unable to prepare download data for the manga with id: " + mangaId), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("{mangaId}/{volume}")
    public ResponseEntity<?> downloadVolume(@PathVariable("mangaId") String mangaId, @PathVariable("volume") String volume) {
        try {
            List<MangaVolume> mangaVolumes = mangaService.getMangaVolumesById(mangaId, volume, null);
            if(mangaVolumes.isEmpty()){
                return new ResponseEntity<>(new ErrorResponse("No manga volumes found for the manga with id: " + mangaId), HttpStatus.NOT_FOUND);
            }
            MangaDownloadDetails mangaDownloadDetails = mangaDownloaderService.createFileStructureForManga(mangaId, mangaVolumes, "byVolume");
            return new ResponseEntity<>(mangaDownloadDetails, HttpStatus.OK);
        } catch (Exception e) {
            logger.trace("unable to prepare download data for the manga volume with id: " + mangaId + " volume: " + volume, e);
            return new ResponseEntity<>(new ErrorResponse("Unable to prepare download data for the manga volume with id: " + mangaId + " volume: " + volume), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("{mangaId}/{volume}/{chapter}")
    public ResponseEntity<?> downloadChapter(@PathVariable("mangaId") String mangaId, @PathVariable("volume") String volume, @PathVariable("chapter") String chapter) {
        try {
            List<MangaVolume> mangaVolumes = mangaService.getMangaVolumesById(mangaId, volume, chapter);
            if(mangaVolumes.isEmpty()){
                return new ResponseEntity<>(new ErrorResponse("No manga volumes found for the manga with id: " + mangaId), HttpStatus.NOT_FOUND);
            }
            MangaDownloadDetails mangaDownloadDetails = mangaDownloaderService.createFileStructureForManga(mangaId, mangaVolumes, "byChapter");
            return new ResponseEntity<>(mangaDownloadDetails, HttpStatus.OK);
        } catch (Exception e) {
            logger.trace("unable to prepare download data for the manga chapter with id: " + mangaId + " volume: " + volume + " chapter: " + chapter, e);
            return new ResponseEntity<>(new ErrorResponse("Unable to prepare download data for the manga chapter with id: " + mangaId + " volume: " + volume + " chapter: " + chapter), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
