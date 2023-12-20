package com.manga.harbour.mh.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.manga.harbour.mh.entity.MangaVolume;
import com.manga.harbour.mh.service.MangaService;

import reactor.core.publisher.Mono;

@RestController

public class MangaHarbourController {

    @Autowired
    private MangaService mangaVolumeBuilder;

    @GetMapping("/")
    public String checkApi() {
        return "Api is connected";
    }

    @GetMapping("/manga/{id}")
    public Mono<Object> getMangaDetails(@PathVariable String id) {
        return mangaVolumeBuilder.getMangaInfoById(id);
    }

    @GetMapping("/manga/search/{title}")
    public Mono<Object> searchMangaDetails(@PathVariable String title) {
        return mangaVolumeBuilder.getMangaDetails(title);
    }

    @GetMapping("/manga/volumeList/{id}")
    public List<MangaVolume> getMangaChapterList(@PathVariable String id) {
        return mangaVolumeBuilder.getMangaChapterListById(id);
    }

}
