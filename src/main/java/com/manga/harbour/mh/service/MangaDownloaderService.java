package com.manga.harbour.mh.service;

import com.manga.harbour.mh.entity.Chapter;
import com.manga.harbour.mh.entity.Image;
import com.manga.harbour.mh.entity.MangaVolume;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class MangaDownloaderService {

    private final List<String> volumeZipPaths = new ArrayList<>();

    @Autowired
    private MangaImageService ImageService;

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
    public void createFolders(List<MangaVolume> mangaVolumes) {
        File mangaFolder = new File("Manga");
        mangaFolder.mkdirs();
        for (MangaVolume mangaVolume : mangaVolumes) {
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

}
