package com.manga.harbour.mh.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.manga.harbour.mh.service.MangaHarbourService;

@CrossOrigin(origins="http://localhost:3000")
@RestController
public class MangaHarbourContoller {
	@Autowired
	private MangaHarbourService mangaService;

	@GetMapping("/")
	public String getManga() {
		return mangaService.getMangaById();
	}

}
