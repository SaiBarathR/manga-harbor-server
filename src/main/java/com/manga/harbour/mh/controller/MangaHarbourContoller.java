package com.manga.harbour.mh.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.manga.harbour.mh.entity.MangaVolumeDTO;
import com.manga.harbour.mh.service.MangaService;

@CrossOrigin(origins="http://localhost:3000")
@RestController
public class MangaHarbourContoller {
	
	@Autowired
	private MangaService mangaVolumeBuiler;
	
	@GetMapping("/")
	public String checkApi() {
		return "Api is connected";
	}

	@GetMapping("/manga/{id}")
	public List<MangaVolumeDTO> getMangaChapterList(@PathVariable String id) {
		return mangaVolumeBuiler.getMangaChapterListById(id);
	}
	
	
	@GetMapping("/manga/download/{id}")
	public List<MangaVolumeDTO> getManga(@PathVariable String id) {
		return mangaVolumeBuiler.getMangaVolumesById(id, null, null);
	}
	
	@GetMapping("/manga/{mangaId}/{volume}/{chapter}")
	public List<MangaVolumeDTO>  getVolumeAndChapterDetails(@PathVariable("mangaId") String mangaId, @PathVariable("volume") String volume, @PathVariable("chapter") String chapter) {
		return  mangaVolumeBuiler.getMangaVolumesById(mangaId,volume,chapter);
	}
	
	@GetMapping("/manga/{mangaId}/volume/{volume}")
	public List<MangaVolumeDTO> getVolumeDetails(@PathVariable("mangaId") String mangaId, @PathVariable("volume") String volume) {
		return mangaVolumeBuiler.getMangaVolumesById(mangaId,volume,null);
	}
	
	
	@GetMapping("/manga/{mangaId}/chatper/{chapter}")
	public List<MangaVolumeDTO>  getChapterDetails(@PathVariable("mangaId") String mangaId, @PathVariable("chapter") String chapter) {
		return  mangaVolumeBuiler.getMangaVolumesById(mangaId,null,chapter);
	}
	
	
}
