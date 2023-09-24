package com.manga.harbour.mh.controller;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.manga.harbour.mh.service.MangaCoverService;

@RestController
@CrossOrigin(origins="*")
@RequestMapping("/manga/cover")
public class MangaCoverController {

	private final MangaCoverService mangaCoverService;

	public MangaCoverController(MangaCoverService mangaCoverService) {
		this.mangaCoverService = mangaCoverService;
	}

	@GetMapping
	public ResponseEntity<Resource> getMangaCover(@RequestParam String url) {
		Resource imageResource = mangaCoverService.convertImageUrlToResource(url);
		return ResponseEntity.ok()
				.contentType(MediaType.IMAGE_JPEG) // Set the content type for JPEG images
				.body(imageResource);
	}
}
