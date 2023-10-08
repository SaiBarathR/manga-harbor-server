package com.manga.harbour.mh.service;

import com.manga.harbour.mh.entity.Chapter;
import com.manga.harbour.mh.entity.Image;
import com.manga.harbour.mh.entity.MangaVolume;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class MangaDownloaderService {

    private final List<String> zipPaths = new ArrayList<>();

    @Autowired
    private MangaImageService ImageService;

    public ResponseEntity<byte[]> createFolders(List<MangaVolume> mangaVolumes, String method) {
        boolean zipByChapter = method.equals("byChapter");
        File mangaFolder = new File("Manga");
        mangaFolder.mkdirs();
        for (MangaVolume mangaVolume : mangaVolumes) {
            String volumeNumber = mangaVolume.getVolume();
            String volumeName = "Volume " + volumeNumber;
            File volumeFolder = new File(mangaFolder, volumeName);
            volumeFolder.mkdirs();
            List<Chapter> chapters = mangaVolume.getChapters();
            if (chapters.isEmpty()) {
                continue;
            }
            int imageIndex = 0;
            for (Chapter chapter : chapters) {
                String chapterNumber = chapter.getChapter();
                String chapterName = "Chapter " + chapterNumber;
                List<Image> images = chapter.getImages();
                if (!images.isEmpty()) {
                    imageIndex = zipByChapter ? 0 : imageIndex;
                    File chapterFolder = new File(zipByChapter ? mangaFolder : volumeFolder, chapterName);
                    chapterFolder.mkdirs();
                    for (Image image : images) {
                        String imageName = "Image" + (++imageIndex) + ".png";
                        byte[] imageBytes = ImageService.retrieveImageData(image.getUrl()).block();
                        try (FileOutputStream outputStream = new FileOutputStream(new File((zipByChapter ? chapterFolder : volumeFolder), imageName))) {
                            outputStream.write(imageBytes);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.println("Saved " + chapterName);
                    if (zipByChapter) {
                        String chapterZipPath = mangaFolder.getPath() + File.separator + "Chapter " + chapterNumber + ".zip";
                        System.out.println("Creating Zip for: " + chapterName);
                        createZipFile(chapterFolder.getPath(), chapterZipPath);
                        zipPaths.add(chapterZipPath);
                        System.out.println("Saved Zip: " + chapterName);
                        deleteFolderByPath(chapterFolder.toPath(), chapterName);
                        break;
                    }
                }
            }
            if (!zipByChapter) {
                System.out.println("Saved " + volumeName);
                String volumeZipPath = mangaFolder.getPath() + File.separator + "Volume " + volumeNumber + ".zip";
                System.out.println("Creating Zip for: " + volumeName);
                createZipFile(volumeFolder.getPath(), volumeZipPath);
                zipPaths.add(volumeZipPath);
                System.out.println("Saved " + volumeName);
                deleteFolderByPath(volumeFolder.toPath(), volumeName);
            }
        }
        if (!mangaVolumes.isEmpty()) {
            byte[] mangaZipFile = createMangaZipFile();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "manga.zip");
            deleteFolderByPath(mangaFolder.toPath(), mangaFolder.getName());
            zipPaths.clear();
            return new ResponseEntity<>(mangaZipFile, headers, HttpStatus.OK);
        }
        return null;
    }

    private void deleteFolderByPath(Path path, String folderName) {
        try {
            Path rootPaths = path;
            Files.walk(rootPaths)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            System.out.println("Deleted " + folderName + " folder");
        } catch (IOException e) {
            System.out.println(e);
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

    public byte[] createMangaZipFile() {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(); ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            for (String zipPath : zipPaths) {
                File volumeZipFile = new File(zipPath);
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
                    System.out.println("zip file not found: " + zipPath);
                }
            }
            zipOutputStream.finish();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
