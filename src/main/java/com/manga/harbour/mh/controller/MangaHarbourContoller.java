package com.manga.harbour.mh.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.manga.harbour.mh.entity.MangaVolumeDTO;
import com.manga.harbour.mh.service.MangaService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;

import reactor.core.publisher.Mono;

@CrossOrigin(origins="*")
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

	@GetMapping("/manga/search/{title}")
	public Mono<Object>searchMangaDetails(@PathVariable String title) {
		return mangaVolumeBuiler.getMangaDetails(title);
	}

	@GetMapping("/manga/download/{mangaId}")
	public ResponseEntity<byte[]> getManga(@PathVariable String mangaId) {
		List<MangaVolumeDTO> mangaVolumes = mangaVolumeBuiler.getMangaVolumesById(mangaId, null, null);
		byte[] mangaZipFile = mangaVolumeBuiler.createMangaZipFile();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		headers.setContentDispositionFormData("attachment", "manga.zip");

		return new ResponseEntity<>(mangaZipFile, headers, HttpStatus.OK);
	}

	@GetMapping("/manga/download/{mangaId}/{volume}/{chapter}")
	public List<MangaVolumeDTO>  getVolumeAndChapterDetails(@PathVariable("mangaId") String mangaId, @PathVariable("volume") String volume, @PathVariable("chapter") String chapter) {
		return  mangaVolumeBuiler.getMangaVolumesById(mangaId,volume,chapter);
	}

	@GetMapping("/manga/download/{mangaId}/volume/{volume}")
	public List<MangaVolumeDTO> getVolumeDetails(@PathVariable("mangaId") String mangaId, @PathVariable("volume") String volume) {
		return mangaVolumeBuiler.getMangaVolumesById(mangaId,volume,null);
	}

	@GetMapping("/manga/download/{mangaId}/chatper/{chapter}")
	public List<MangaVolumeDTO>  getChapterDetails(@PathVariable("mangaId") String mangaId, @PathVariable("chapter") String chapter) {
		return  mangaVolumeBuiler.getMangaVolumesById(mangaId,null,chapter);
	}

}
