package com.manga.harbour.mh.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manga.harbour.mh.entity.Chapter;
import com.manga.harbour.mh.entity.MangaVolumeDTO;
import com.manga.harbour.mh.entity.Image;

@Service
public class MangaService {

	private final WebClient client;
	private final ScheduledExecutorService executorService;

	public MangaService(WebClient.Builder webClientBuilder) {
		this.client = webClientBuilder.baseUrl("https://api.mangadex.org").build();
		this.executorService = Executors.newScheduledThreadPool(1);
	}

	public List<MangaVolumeDTO> getMangaChapterListById(String id) {
		String mangaData = client.get().uri("/manga/" + id + "/aggregate?translatedLanguage[0]=en").retrieve().bodyToMono(String.class).block();
		return prepareChapterList(mangaData);
	}

	public List<MangaVolumeDTO> getMangaVolumesById(String id,String selectedVolume, String selectedChapter) {
		String mangaData = client.get().uri("/manga/" + id + "/aggregate?translatedLanguage[0]=en").retrieve().bodyToMono(String.class).block();
		return parseAndTransformMangaData(mangaData,selectedVolume,selectedChapter);
	}

	private List<MangaVolumeDTO> prepareChapterList(String mangaData) {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			Map<String, Object> jsonMap = objectMapper.readValue(mangaData, Map.class);
			Map<String, Map<String, Object>> volumesMap = (Map<String, Map<String, Object>>) jsonMap.get("volumes");

			List<MangaVolumeDTO> mangaVolumes = new ArrayList<>();

			for (Map.Entry<String, Map<String, Object>> volumeEntry : volumesMap.entrySet()) {
				MangaVolumeDTO mangaVolume = new MangaVolumeDTO();
				mangaVolume.setVolume(volumeEntry.getKey());

				Map<String, Object> chaptersMap = (Map<String, Object>) volumeEntry.getValue().get("chapters");
				List<Chapter> chapters = new ArrayList<>();

				for (Map.Entry<String, Object> chapterEntry : chaptersMap.entrySet()) {
					Map<String, Object> chapterData = (Map<String, Object>) chapterEntry.getValue();
					Chapter chapterDTO = new Chapter();
					chapterDTO.setId((String) chapterData.get("id"));
					chapterDTO.setChapter(chapterEntry.getKey());
					chapters.add(chapterDTO);
				}

				mangaVolume.setChapters(chapters);
				mangaVolumes.add(mangaVolume);
			}

			return mangaVolumes;
		} catch (IOException e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

	private List<MangaVolumeDTO> parseAndTransformMangaData(String mangaData, String selectedVolume, String selectedChapter) {

		ObjectMapper objectMapper = new ObjectMapper();

		try {
			Map<String, Object> jsonMap = objectMapper.readValue(mangaData, Map.class);
			Map<String, Map<String, Object>> volumesMap = (Map<String, Map<String, Object>>) jsonMap.get("volumes");

			List<MangaVolumeDTO> mangaVolumes = new ArrayList<>();

			for (Map.Entry<String, Map<String, Object>> volumeEntry : volumesMap.entrySet()) {
				String volumeNumber = volumeEntry.getKey();

				if ("none".equals(volumeNumber)) {
					volumeNumber = "0";
				}

				if (selectedVolume != null && !selectedVolume.equals(volumeNumber)) {
					continue; 
				}

				MangaVolumeDTO mangaVolume = new MangaVolumeDTO();
				mangaVolume.setVolume(volumeNumber);

				Map<String, Object> chaptersMap = (Map<String, Object>) volumeEntry.getValue().get("chapters");

				List<Chapter> chapters = new ArrayList<>();

				for (Map.Entry<String, Object> chapterEntry : chaptersMap.entrySet()) {
					String chapterNumber = chapterEntry.getKey();
					if (selectedChapter != null && !selectedChapter.equals(chapterNumber)) {
						continue; // Skip this chapter if it's not the selected one
					}

					Map<String, Object> chapterData = (Map<String, Object>) chapterEntry.getValue();
					Chapter chapterDTO = new Chapter();
					chapterDTO.setId((String) chapterData.get("id"));
//					chapterDTO.setOthers((List<String>) chapterData.get("others"));
					chapterDTO.setChapter(chapterNumber);
					List<String> imageUrls = getImageUrlsForChapter((String) chapterData.get("id"));
					if(imageUrls.isEmpty()) {
						String otherUrl=((List<String>) chapterData.get("others")).get(0);
						imageUrls=getImageUrlsForChapter(otherUrl);
					}
					List<Image> images = new ArrayList<>();
					for (String imageUrl : imageUrls) {
						Image image = new Image();
						image.setUrl(imageUrl);
						images.add(image);
					}
					chapterDTO.setImages(images);
					chapters.add(chapterDTO);
				}

				if(chapters.isEmpty()) {
					continue;
				}
				mangaVolume.setChapters(chapters);
				mangaVolumes.add(mangaVolume);
			}

			return mangaVolumes;
		} catch (IOException e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
	}



	public List<String> getImageUrlsForChapter(String chapterId) {
		List<String> imageUrls = new ArrayList<>();
		String imageMetadata = client.get().uri("/at-home/server/" + chapterId).retrieve().bodyToMono(String.class).block();
		if (imageMetadata != null) {
			try {
				ObjectMapper objectMapper = new ObjectMapper();
				Map<String, Object> respMap = objectMapper.readValue(imageMetadata, Map.class);
				String baseUrl = respMap.get("baseUrl").toString();
				Map<String, Object> chapterData = (Map<String, Object>) respMap.get("chapter");
				String chapterHash = chapterData.get("hash").toString();
				List<String> data = (List<String>) chapterData.get("data");
				ScheduledExecutorService rateLimitedExecutor = Executors.newScheduledThreadPool(1);
				int requestsPerMinute = 38;
				int delayMilliseconds = 60 * 1000 / requestsPerMinute; 
				for (String imageData : data) {
					rateLimitedExecutor.schedule(() -> {
						String imageUrl = baseUrl + "/data/" + chapterHash + "/" + imageData;
						imageUrls.add(imageUrl);
						System.out.println("Fetched image: " + imageUrl);
					}, delayMilliseconds, TimeUnit.MILLISECONDS);
				}
				rateLimitedExecutor.shutdown();
				rateLimitedExecutor.awaitTermination(1, TimeUnit.HOURS);
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
		return imageUrls;
	}

}
