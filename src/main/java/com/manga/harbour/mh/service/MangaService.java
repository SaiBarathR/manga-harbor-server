package com.manga.harbour.mh.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manga.harbour.mh.entity.Chapter;
import com.manga.harbour.mh.entity.MangaVolumeDTO;

import reactor.core.publisher.Mono;

import com.manga.harbour.mh.entity.Image;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class MangaService {

    private final WebClient client;
    private final ScheduledExecutorService executorService;
    private final List<String> volumeZipPaths = new ArrayList<>();

    @Autowired
    private MangaCoverService ImageService;

    public MangaService(WebClient.Builder webClientBuilder) {
        this.client = webClientBuilder.baseUrl("https://api.mangadex.org").codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(25000 * 1024)).build();
        this.executorService = Executors.newScheduledThreadPool(1);
    }

    public List<MangaVolumeDTO> getMangaChapterListById(String id) {
        String mangaData = client.get().uri("/manga/" + id + "/aggregate?translatedLanguage[0]=en").retrieve().bodyToMono(String.class).block();
        return prepareChapterList(mangaData);
    }

    public List<MangaVolumeDTO> getMangaVolumesById(String id, String selectedVolume, String selectedChapter) {
        String mangaData = client.get().uri("/manga/" + id + "/aggregate?translatedLanguage[0]=en").retrieve().bodyToMono(String.class).block();
        return parseAndTransformMangaData(mangaData, selectedVolume, selectedChapter);
    }

    public Mono<Object> getMangaDetails(String title) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("https://api.mangadex.org").path("manga").queryParam("limit", 4).queryParam("title", title).queryParam("includes[]", "cover_art").queryParam("contentRating[]", "safe", "suggestive");

        Mono<String> mangaResponse = client.get().uri(builder.build().toUriString()).retrieve().bodyToMono(String.class).onErrorResume(throwable -> Mono.empty());

        return contructMangaData(mangaResponse);
    }

    public Mono<Object> getMangaInfoById(String id) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("https://api.mangadex.org/manga/" + id).queryParam("includes[]", "manga", "cover_art", "author", "artist", "tag", "creator");
            Mono<String> mangaResponse = client.get().uri(builder.build().toUriString()).retrieve().bodyToMono(String.class);
            System.out.println("Retrived manga details from mangadex for Id: " + id);
            return contructMangaData(mangaResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Mono<Object> contructMangaData(Mono<String> mangaResponse) {
        return mangaResponse.flatMap(mangaData -> {
            MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            extractMangaIdsFromResponse(mangaData).forEach(id -> queryParams.add("manga[]", id));
            UriComponentsBuilder statsBuilder = UriComponentsBuilder.fromPath("statistics/manga").queryParams(queryParams);

            Mono<String> statsResponse = client.get().uri(statsBuilder.build().toUriString()).retrieve().bodyToMono(String.class).onErrorResume(throwable -> Mono.empty());

            return statsResponse.map(statsData -> {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, Object> mangaObject = objectMapper.readValue(mangaData, new TypeReference<Map<String, Object>>() {
                    });
                    Map<String, Object> statsObject = objectMapper.readValue(statsData, new TypeReference<Map<String, Object>>() {
                    });
                    if (mangaObject.containsKey("data") && mangaObject.get("data") instanceof List) {
                        List<Map<String, Object>> mangaList = (List<Map<String, Object>>) mangaObject.get("data");
                        for (Map<String, Object> mangaItem : mangaList) {
                            List<Map<String, Object>> relationshipsList = (List<Map<String, Object>>) mangaItem.get("relationships");
                            String mangaId = mangaItem.get("id").toString();
                            for (Map<String, Object> relationship : relationshipsList) {
                                if ("cover_art".equals(relationship.get("type"))) {
                                    String fileName = ((Map<String, Object>) relationship.get("attributes")).get("fileName").toString();
                                    String imageUrl = "https://uploads.mangadex.org/covers/" + mangaId + "/" + fileName;
                                    mangaItem.put("image", imageUrl);
                                    break;
                                }
                            }
                        }
                    }

                    Map<String, Object> data = new HashMap<>();
                    data.put("mangaData", mangaObject);
                    data.put("statsResponse", statsObject);

                    return data;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            });
        });

    }

    public byte[] createMangaZipFile() {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(); ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            for (String volumeZipPath : volumeZipPaths) {
                File volumeZipFile = new File(volumeZipPath);
                if (volumeZipFile.exists()) {
                    try (FileInputStream fis = new FileInputStream(volumeZipFile)) {
                        ZipEntry zipEntry = new ZipEntry(volumeZipFile.getName());
                        zipOutputStream.putNextEntry(zipEntry);

                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = fis.read(buffer)) > 0) {
                            zipOutputStream.write(buffer, 0, length);
                        }

                        zipOutputStream.closeEntry();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Volume zip file not found: " + volumeZipPath);
                }
            }
            zipOutputStream.finish();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
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
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return mangaIds;
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
                        continue;
                    }
                    Map<String, Object> chapterData = (Map<String, Object>) chapterEntry.getValue();
                    Chapter chapterDTO = new Chapter();
                    chapterDTO.setId((String) chapterData.get("id"));
                    chapterDTO.setChapter(chapterNumber);
                    List<String> imageUrls = getImageUrlsForChapter((String) chapterData.get("id"));
                    if (imageUrls.isEmpty()) {
                        for (String otherImageUrl : ((List<String>) chapterData.get("others"))) {
                            if(imageUrls.isEmpty()) {
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
                    System.out.println("Fetched Chapter: " + chapterNumber);
                    chapterDTO.setImages(images);
                    chapters.add(chapterDTO);
                }

                if (chapters.isEmpty()) {
                    continue;
                }
                System.out.println("Fetched volume: " + volumeNumber);
                mangaVolume.setChapters(chapters);
                mangaVolumes.add(mangaVolume);
            }
            return mangaVolumes;
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public void createFolders(List<MangaVolumeDTO> mangaVolumes) {
        File mangaFolder = new File("Manga");
        mangaFolder.mkdirs();

        for (MangaVolumeDTO mangaVolume : mangaVolumes) {
            String volumeNumber = mangaVolume.getVolume();
            File volumeFolder = new File(mangaFolder, "Volume " + volumeNumber);
            volumeFolder.mkdirs();
            List<Chapter> chapters = mangaVolume.getChapters();
            if (chapters.isEmpty()) {
                continue;
            }
            for (Chapter chapter : chapters) {
                String chapterNumber = chapter.getChapter();
                List<Image> images = chapter.getImages();
                if (!images.isEmpty()) {
                    int imageIndex = 0;
                    File chapterFolder = new File(volumeFolder, "Chapter " + chapterNumber);
                    chapterFolder.mkdirs();
                    for (Image image : images) {
                        String imageName = "Image" + (++imageIndex) + ".png";
                        byte[] imageBytes = ImageService.retrieveImageData(image.getUrl()).block();
                        try (FileOutputStream outputStream = new FileOutputStream(new File(chapterFolder, imageName))) {
                            outputStream.write(imageBytes);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                System.out.println("Saved Chapter: " + chapterNumber);
            }
            System.out.println("Saved volume: " + volumeNumber);
            String volumeZipPath = mangaFolder.getPath() + File.separator + "Volume " + volumeNumber + ".zip";
            System.out.println("Creating Zip: " + volumeNumber);
            createZipFile(volumeFolder.getPath(), volumeZipPath);
            volumeZipPaths.add(volumeZipPath);
            System.out.println("Saved Zip: " + volumeNumber);
        }
    }

    private void createZipFile(String folderPath, String zipFileName) {
        try {
            Path sourceFolderPath = new File(folderPath).toPath();
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(Path.of(zipFileName)))) {
                Files.walk(sourceFolderPath).filter(path -> !Files.isDirectory(path)).forEach(path -> {
                    ZipEntry zipEntry = new ZipEntry(sourceFolderPath.relativize(path).toString());
                    try {
                        zipOutputStream.putNextEntry(zipEntry);
                        Files.copy(path, zipOutputStream);
                        zipOutputStream.closeEntry();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //	private void createMangaZipFile(File mangaFolder) {
    //	    try {
    //	        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream("manga.zip"))) {
    //	            for (String volumeZipPath : volumeZipPaths) {
    //	                File volumeZipFile = new File(volumeZipPath);
    //	                if (volumeZipFile.exists()) {
    //	                    try (FileInputStream fis = new FileInputStream(volumeZipFile)) {
    //	                        ZipEntry zipEntry = new ZipEntry(volumeZipFile.getName());
    //	                        zipOutputStream.putNextEntry(zipEntry);
    //
    //	                        byte[] buffer = new byte[1024];
    //	                        int length;
    //	                        while ((length = fis.read(buffer)) > 0) {
    //	                            zipOutputStream.write(buffer, 0, length);
    //	                        }
    //
    //	                        zipOutputStream.closeEntry();
    //	                    } catch (IOException e) {
    //	                        e.printStackTrace();
    //	                    }
    //	                } else {
    //	                    System.out.println("Volume zip file not found: " + volumeZipPath);
    //	                }
    //	            }
    //	        }
    //	    } catch (IOException e) {
    //	        e.printStackTrace();
    //	    }
    //	}

    //	private ZipOutputStream createMangaZipFile(File mangaFolder) {
    //		try {
    //			try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream("manga.zip"))) {
    //				for (String volumeZipPath : volumeZipPaths) {
    //					File volumeZipFile = new File(volumeZipPath);
    //					if (volumeZipFile.exists()) {
    //						try (FileInputStream fis = new FileInputStream(volumeZipFile)) {
    //							ZipEntry zipEntry = new ZipEntry(volumeZipFile.getName());  // Use only the file name, not the path
    //							zipOutputStream.putNextEntry(zipEntry);
    //
    //							byte[] buffer = new byte[1024];
    //							int length;
    //							while ((length = fis.read(buffer)) > 0) {
    //								zipOutputStream.write(buffer, 0, length);
    //							}
    //
    //							zipOutputStream.closeEntry();
    //						} catch (IOException e) {
    //							e.printStackTrace();
    //						}
    //					} else {
    //						System.out.println("Volume zip file not found: " + volumeZipPath);
    //					}
    //				}
    //				return zipOutputStream;
    //			}
    //		} catch (IOException e) {
    //			e.printStackTrace();
    //		}
    //		return null;
    //	}

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
