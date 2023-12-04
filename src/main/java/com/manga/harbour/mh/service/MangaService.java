package com.manga.harbour.mh.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manga.harbour.mh.entity.Chapter;
import com.manga.harbour.mh.entity.Image;
import com.manga.harbour.mh.entity.MangaVolume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class MangaService {
    Logger logger = LoggerFactory.getLogger(MangaService.class);

    private final WebClient client;

    @Autowired
    private MangaImageService ImageService;

    public MangaService(WebClient.Builder webClientBuilder) {
        this.client = webClientBuilder
                .baseUrl("https://api.mangadex.org")
                .codecs(codecs -> codecs.defaultCodecs()
                        .maxInMemorySize(25000 * 1024))
                .build();
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    }

    public List<MangaVolume> getMangaChapterListById(String id) {
        String mangaData = client.get().uri("/manga/" + id + "/aggregate?translatedLanguage[0]=en").retrieve().bodyToMono(String.class).block();
        return prepareChapterList(mangaData);
    }

    public List<MangaVolume> getMangaVolumesById(String id, String selectedVolume, String selectedChapter) {
        try {
            String mangaData = client.get().uri("/manga/" + id + "/aggregate?translatedLanguage[0]=en").retrieve().bodyToMono(String.class).block();
            return parseAndTransformMangaData(mangaData, selectedVolume, selectedChapter);
        }
        catch (Exception e){
            logger.trace("Error",e);
        }
        return null;
    }

    public String getMangaName(String mangaId) {
        Mono<String> responseMono = client.get().uri("/manga/" + mangaId).retrieve().bodyToMono(String.class);
        String responseJson = responseMono.block();
        if (responseJson != null) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                JsonNode rootNode = objectMapper.readTree(responseJson);
                JsonNode dataNode = rootNode.get("data");
                if (dataNode != null) {
                    JsonNode attributesNode = dataNode.get("attributes");
                    if (attributesNode != null) {
                        JsonNode titleNode = attributesNode.get("title");
                        if (titleNode != null && titleNode.has("en")) {
                            return titleNode.get("en").asText();
                        } else {
                            JsonNode altTitlesNode = attributesNode.get("altTitles");
                            if (altTitlesNode != null) {
                                for (JsonNode altTitle : altTitlesNode) {
                                    if (altTitle.has("en")) {
                                        return altTitle.get("en").asText();
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                logger.trace("unable to get manga title for manga with id: " + mangaId, e);
            }
        }
        return "";
    }

    public Mono<Object> getMangaDetails(String title) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl("https://api.mangadex.org")
                .path("manga")
                .queryParam("limit", 4)
                .queryParam("title", title)
                .queryParam("includes[]", "cover_art")
                .queryParam("contentRating[]", "safe", "suggestive");
        Mono<String> mangaResponse = client.get().uri(builder.build().toUriString()).retrieve().bodyToMono(String.class).onErrorResume(throwable -> Mono.empty());
        return constructMangaData(mangaResponse);
    }

    public Mono<Object> getMangaInfoById(String id) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl("https://api.mangadex.org/manga/" + id)
                    .queryParam("includes[]", "manga", "cover_art", "author", "artist", "tag", "creator");
            Mono<String> mangaResponse = client.get().uri(builder.build().toUriString()).retrieve().bodyToMono(String.class);
            logger.info("Fetched manga details from mangadex for Id: " + id);
            return constructMangaData(mangaResponse);
        } catch (Exception e) {
            logger.trace("getMangaInfoById failed for manga with id: " + id, e);
        }
        return null;
    }

    private Mono<Object> constructMangaData(Mono<String> mangaResponse) {
        return mangaResponse.flatMap(mangaData -> {
            MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
//            extractMangaIdsFromResponse(mangaData).forEach(id -> queryParams.add("manga[]", id));
            List<String> mangaIds = extractMangaIdsFromResponse(mangaData);
            mangaIds.forEach(id -> queryParams.add("manga[]", id));
            logger.info("id for stats" + queryParams);
            UriComponentsBuilder statsBuilder = UriComponentsBuilder.fromPath("statistics/manga").queryParams(queryParams);
            Mono<String> statsResponse = client.get().uri(statsBuilder.build().toUriString()).retrieve().bodyToMono(String.class).onErrorResume(throwable -> Mono.empty());
            logger.info("stat resp" + statsResponse);
            return statsResponse.mapNotNull(statsData -> {
                try {
                    logger.info("inside stats resp" + statsData);
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, Object> mangaObject = objectMapper.readValue(mangaData, new TypeReference<Map<String, Object>>() {
                    });
                    Map<String, Object> statsObject = objectMapper.readValue(statsData, new TypeReference<Map<String, Object>>() {
                    });
                    if (mangaObject.containsKey("data")) {
                        if (mangaObject.get("data") instanceof List){
                            List<Map<String, Object>> mangaList = (List<Map<String, Object>>) mangaObject.get("data");
                            for (Map<String, Object> mangaItem : mangaList) {
                                mangaItem.put("image", getCoverArt(mangaItem));
                            }
                        }
                        else{
                            Map<String, Object> mangaItem = (Map<String, Object>) mangaObject.get("data");
                            mangaItem.put("image", getCoverArt(mangaItem));
                        }
                    }
                    Map<String, Object> data = new HashMap<>();
                    data.put("mangaData", mangaObject);
                    data.put("statsResponse", statsObject);
                    return data;
                } catch (Exception e) {
                    logger.trace("unable to construct json for manga data and stats response for response: " + mangaResponse, e);
                }
                return null;
            });
        });

    }

    public String getCoverArt(Map<String, Object> mangaItem) {
        List<Map<String, Object>> relationshipsList = (List<Map<String, Object>>) mangaItem.get("relationships");
        String mangaId = mangaItem.get("id").toString();
        for (Map<String, Object> relationship : relationshipsList) {
            if ("cover_art".equals(relationship.get("type"))) {
                String fileName = ((Map<String, Object>) relationship.get("attributes")).get("fileName").toString();
                String imageUrl = "https://uploads.mangadex.org/covers/" + mangaId + "/" + fileName;
                logger.info("fetched cover art url: " + imageUrl);
                return imageUrl;
            }
        }
        return null;
    }
    private List<String> extractMangaIdsFromResponse(String mangaData) {
        List<String> mangaIds = new ArrayList<>();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(mangaData);

            if (rootNode.has("data") && rootNode.get("data").isArray()) {
                for (JsonNode mangaNode : rootNode.get("data")) {
                    if (mangaNode.has("id")) {
                        mangaIds.add(mangaNode.get("id").asText());
                    }
                }
            }else{
                if(!rootNode.get("data").isArray()){
                    mangaIds.add(rootNode.get("data").get("id").asText());
                }
                else {
                    logger.info("No manga found for search");
                }
            }
        } catch (IOException e) {
            logger.trace("unable prepare manga id's array for stats api request for manga: " + mangaData, e);
        }
        return mangaIds;
    }

    private List<MangaVolume> prepareChapterList(String mangaData) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String, Object> jsonMap = objectMapper.readValue(mangaData, Map.class);
            Map<String, Map<String, Object>> volumesMap = (Map<String, Map<String, Object>>) jsonMap.get("volumes");

            List<MangaVolume> mangaVolumes = new ArrayList<>();

            for (Map.Entry<String, Map<String, Object>> volumeEntry : volumesMap.entrySet()) {
                MangaVolume mangaVolume = new MangaVolume();
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
            logger.trace("unable to prepare chapter list for manga: "+ mangaData,e);
            return Collections.emptyList();
        }
    }

    private List<MangaVolume> parseAndTransformMangaData(String mangaData, String selectedVolume, String selectedChapter) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String, Object> jsonMap = objectMapper.readValue(mangaData, Map.class);
            Map<String, Map<String, Object>> volumesMap = (Map<String, Map<String, Object>>) jsonMap.get("volumes");

            List<MangaVolume> mangaVolumes = new ArrayList<>();
            for (Map.Entry<String, Map<String, Object>> volumeEntry : volumesMap.entrySet()) {
                String volumeNumber = volumeEntry.getKey();
                if ("none".equals(volumeNumber)) {
                    volumeNumber = "0";
                }
                if (selectedVolume != null && !selectedVolume.equals(volumeNumber)) {
                    continue;
                }
                MangaVolume mangaVolume = new MangaVolume();
                mangaVolume.setVolume(volumeNumber);

                Map<String, Object> chaptersMap = (Map<String, Object>) volumeEntry.getValue().get("chapters");
                List<Chapter> chapters = new ArrayList<>();
                for (Map.Entry<String, Object> chapterEntry : chaptersMap.entrySet()) {
                    String chapterNumber = chapterEntry.getKey();
                    if (selectedChapter != null && !selectedChapter.equals(chapterNumber)) {
                        continue;
                    }
                    Map<String, Object> chapterData = (Map<String, Object>) chapterEntry.getValue();
                    Chapter chapterDTO = new Chapter();
                    chapterDTO.setId((String) chapterData.get("id"));
                    chapterDTO.setChapter(chapterNumber);
                    List<String> imageUrls = getImageUrlsForChapter((String) chapterData.get("id"));
                    if (imageUrls.isEmpty()) {
                        for (String otherImageUrl : ((List<String>) chapterData.get("others"))) {
                            if (imageUrls.isEmpty()) {
                                imageUrls = getImageUrlsForChapter(otherImageUrl);
                            }
                        }
                    }
                    List<Image> images = new ArrayList<>();
                    for (String imageUrl : imageUrls) {
                        Image image = new Image();
                        image.setUrl(imageUrl);
                        images.add(image);
                    }
                    logger.info("Fetched Chapter: " + chapterNumber);
                    chapterDTO.setImages(images);
                    chapters.add(chapterDTO);
                }
                if (chapters.isEmpty()) {
                    continue;
                }
                logger.info("Fetched volume: " + volumeNumber);
                mangaVolume.setChapters(chapters);
                mangaVolumes.add(mangaVolume);
            }
            return mangaVolumes;
        } catch (IOException e) {
            logger.trace("unable to construct manga volume list for manga: "+ mangaData,e);
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
                int requestsPerMinute = 35;
                int delayMilliseconds = 60 * 1000 / requestsPerMinute;
                for (String imageData : data) {
                    rateLimitedExecutor.schedule(() -> {
                        String imageUrl = baseUrl + "/data/" + chapterHash + "/" + imageData;
                        imageUrls.add(imageUrl);
                        logger.info("Fetched image: " + imageUrl);
                    }, delayMilliseconds, TimeUnit.MILLISECONDS);
                }
                rateLimitedExecutor.shutdown();
                rateLimitedExecutor.awaitTermination(1, TimeUnit.HOURS);
            } catch (IOException | InterruptedException e) {
                logger.trace("unable to create /at-home/server/ image url list for chapter id: "+ chapterId,e);
            }
        }
        return imageUrls;
    }
}
