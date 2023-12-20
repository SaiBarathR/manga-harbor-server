package com.manga.harbour.mh.controller;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.manga.harbour.mh.service.MangaImageService;

@RestController
@RequestMapping("/manga/cover")
public class MangaImageController {

	private final MangaImageService mangaImageService;

	public MangaImageController(MangaImageService mangaImageService) {
		this.mangaImageService = mangaImageService;
	}

	@GetMapping
	public ResponseEntity<Resource> getMangaCover(@RequestParam String url) {
		return mangaImageService.convertImageUrlToResource(url);
	}
}
