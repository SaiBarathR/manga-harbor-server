package com.manga.harbour.mh.service;

import com.manga.harbour.mh.entity.Chapter;
import com.manga.harbour.mh.entity.Image;
import com.manga.harbour.mh.entity.MangaDownloadDetails;
import com.manga.harbour.mh.entity.MangaVolume;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class MangaDownloaderService {

    @Autowired
    private MangaImageService ImageService;
    @Autowired
    private MangaService mangaService;

    public MangaDownloadDetails createFileStructureForManga(String mangaId, List<MangaVolume> mangaVolumes, String method) {
        MangaDownloadDetails mangaDownloadDetails = new MangaDownloadDetails();
        mangaDownloadDetails.setZipPaths(new ArrayList<>());
        boolean zipByChapter = method.equals("byChapter");
        String uniqueID = UUID.randomUUID().toString();
        String mangaName = mangaService.getMangaName(mangaId);
        String mangaFolderName = uniqueID + " split_here" + mangaName;
        File mangaFolder = new File(mangaFolderName);
        mangaFolder.mkdirs();
        for (MangaVolume mangaVolume : mangaVolumes) {
            String volumeNumber = mangaVolume.getVolume();
            String volumeName = mangaName + " Volume " + volumeNumber;
            File volumeFolder = new File(mangaFolder, volumeName);
            volumeFolder.mkdirs();
            List<Chapter> chapters = mangaVolume.getChapters();
            if (chapters.isEmpty()) {
                continue;
            }
            int imageIndex = 0;
            for (Chapter chapter : chapters) {
                String chapterNumber = chapter.getChapter();
                String chapterName = mangaName + " Chapter " + chapterNumber;
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
                        generateZipFiles(mangaDownloadDetails, mangaFolder.getPath(), chapterFolder.getPath(), chapterFolder.toPath(), chapterName);
                        break;
                    }
                }
            }
            if (!zipByChapter) {
                System.out.println("Saved " + volumeName);
                generateZipFiles(mangaDownloadDetails, mangaFolder.getPath(), volumeFolder.getPath(), volumeFolder.toPath(), volumeName);
            }
        }
        if (!mangaVolumes.isEmpty()) {
            try {
                long folderSize = Files.walk(mangaFolder.toPath())
                        .filter(p -> p.toFile().isFile())
                        .mapToLong(p -> p.toFile().length())
                        .sum();
                System.out.println(mangaName + " Size: " + folderSize);

                mangaDownloadDetails.setMangaData(mangaVolumes);
                mangaDownloadDetails.setMethod(method);
                mangaDownloadDetails.setFolderSize(folderSize);
                mangaDownloadDetails.setFolderPath(mangaFolder.toPath());
                mangaDownloadDetails.setName(mangaFolder.getName());
                return mangaDownloadDetails;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return mangaDownloadDetails;
    }

    public void streamZipData(MangaDownloadDetails mangaDetails, HttpServletResponse response) {
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + mangaDetails.getName() + ".zip\"");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        response.setHeader(HttpHeaders.EXPIRES, "0");
        response.setHeader("file-size", String.valueOf(mangaDetails.getFolderSize()));
        try (ServletOutputStream outputStream = response.getOutputStream()) {
            createZipStream(mangaDetails.getMangaData(), mangaDetails.getMethod(), mangaDetails.getZipPaths(), outputStream);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            deleteFolderByPath(mangaDetails.getFolderPath(), mangaDetails.getName());
        }
    }

    private void createZipStream(List<MangaVolume> mangaVolumes, String method, List<String> zipPaths, OutputStream outputStream) {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            for (String zipPath : zipPaths) {
                File zipFile = new File(zipPath);
                if (zipFile.exists()) {
                    try (FileInputStream fis = new FileInputStream(zipFile)) {
                        ZipEntry zipEntry = new ZipEntry(zipFile.getName());
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeFilesInToZipFile(String folderPath, String zipFileName) {
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

    private void generateZipFiles(MangaDownloadDetails mangaDownloadDetails, String parentPath, String childPath, Path path, String fileName) {
        String zipPath = parentPath + File.separator + fileName + ".zip";
        System.out.println("Creating Zip for: " + fileName);
        writeFilesInToZipFile(childPath, zipPath);
        mangaDownloadDetails.updateZipPath(zipPath);
        System.out.println("Saved Zip: " + fileName);
        deleteFolderByPath(path, fileName);
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

}
