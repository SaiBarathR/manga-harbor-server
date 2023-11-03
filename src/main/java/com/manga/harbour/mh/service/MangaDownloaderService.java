package com.manga.harbour.mh.service;

import com.manga.harbour.mh.entity.Chapter;
import com.manga.harbour.mh.entity.Image;
import com.manga.harbour.mh.entity.MangaDownloadDetails;
import com.manga.harbour.mh.entity.MangaVolume;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class MangaDownloaderService {
    Logger logger = LoggerFactory.getLogger(MangaDownloaderService.class);

    @Autowired
    private MangaImageService ImageService;

    @Autowired
    private MangaService mangaService;

    public MangaDownloadDetails createFileStructureForManga(String mangaId, List<MangaVolume> mangaVolumes, String method) {
        MangaDownloadDetails mangaDownloadDetails = new MangaDownloadDetails();
        boolean zipByChapter = method.equals("byChapter");
        String uniqueID = UUID.randomUUID().toString();
        String mangaName = mangaService.getMangaName(mangaId);
        String mangaFolderName = uniqueID + " split_here" + mangaName;
        File mangaFolder = new File(mangaFolderName);
        if (mangaFolder.exists() || mangaFolder.mkdirs()) {
            for (MangaVolume mangaVolume : mangaVolumes) {
                String volumeNumber = mangaVolume.getVolume();
                String volumeName = mangaName + " Volume " + volumeNumber;
                File volumeFolder = new File(mangaFolder, volumeName);
                if (volumeFolder.exists() || volumeFolder.mkdirs()) {
                    List<Chapter> chapters = mangaVolume.getChapters();
                    if (chapters.isEmpty()) {
                        continue;
                    }
                    for (Chapter chapter : chapters) {
                        String chapterNumber = chapter.getChapter();
                        String chapterName = mangaName + " Chapter " + chapterNumber;
                        List<Image> images = chapter.getImages();
                        if (!images.isEmpty()) {
                            int imageIndex = 0;
                            File chapterFolder = new File(zipByChapter ? mangaFolder : volumeFolder, chapterName);
                            if (chapterFolder.exists() || chapterFolder.mkdirs()) {

                                for (Image image : images) {
                                    String imageName = volumeName + " Chapter " + chapterNumber + " img " + (++imageIndex) + ".png";
                                    byte[] imageBytes = ImageService.retrieveImageData(image.getUrl()).block();
                                    try (FileOutputStream outputStream = new FileOutputStream(new File((zipByChapter ? chapterFolder : volumeFolder), imageName))) {
                                        assert imageBytes != null;
                                        outputStream.write(imageBytes);
                                    } catch (IOException e) {
                                        logger.trace("Unable to convert image url to byte data", e);
                                    }
                                }

                                logger.info("Saved " + chapterName);
                                if (zipByChapter) {
                                    mangaDownloadDetails.setChapters(chapterNumber);
                                    generateZipFiles(mangaDownloadDetails, mangaFolder.getPath(), chapterFolder.getPath(), chapterFolder.toPath(), chapterName);
                                    break;
                                }
                            }

                        }
                    }
                }
                if (!zipByChapter) {
                    logger.info("Saved " + volumeName);
                    mangaDownloadDetails.updateVolumeList(volumeNumber);
                    generateZipFiles(mangaDownloadDetails, mangaFolder.getPath(), volumeFolder.getPath(), volumeFolder.toPath(), volumeName);
                }
            }
        }
        if (!mangaVolumes.isEmpty()) {
            try {
                long folderSize = Files.walk(mangaFolder.toPath())
                        .filter(p -> p.toFile().isFile())
                        .mapToLong(p -> p.toFile().length())
                        .sum();
                logger.info(mangaName + " Size: " + folderSize);
                mangaDownloadDetails.setMethod(method);
                mangaDownloadDetails.setFolderSize(folderSize);
                mangaDownloadDetails.setFolderPath(mangaFolder.toPath());
                mangaDownloadDetails.setName(mangaFolder.getName());
                return mangaDownloadDetails;
            } catch (IOException | SecurityException e) {
                logger.trace("Unable to determine file size", e);
            }
        }
        return mangaDownloadDetails;
    }

    public void streamZipData(MangaDownloadDetails mangaDetails, HttpServletResponse response) throws UnsupportedEncodingException {
        response.setContentType("application/zip");
        String encodedFilename = URLEncoder.encode(mangaDetails.getName(), StandardCharsets.UTF_8.toString());

        response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFilename  + ".zip\"");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        response.setHeader(HttpHeaders.EXPIRES, "0");
        response.setHeader("file-size", String.valueOf(mangaDetails.getFolderSize()));
        try (ServletOutputStream outputStream = response.getOutputStream()) {
            createZipStream(mangaDetails.getMethod(), mangaDetails.getZipPaths(), outputStream);
            outputStream.flush();
        } catch (IOException e) {
            logger.trace("Unable to stream zip file", e);
        } finally {
            deleteFolderByPath(mangaDetails.getFolderPath(), mangaDetails.getName());
        }
    }

    private void createZipStream(String method, List<String> zipPaths, OutputStream outputStream) {
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
                        logger.trace("Streaming failed while converting file to byte data", e);
                    }
                } else {
                    logger.info("zip file not found: " + zipPath);
                }
            }
        } catch (IOException e) {
            logger.trace("Streaming failed while converting zip file to outputStream", e);
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
                        logger.trace("converting folder to zip failed for:" + zipFileName, e);
                    }
                });
            }
        } catch (IOException e) {
            logger.trace("creating zip from folder failed for:" + zipFileName, e);
        }
    }

    private void generateZipFiles(MangaDownloadDetails mangaDownloadDetails, String parentPath, String childPath, Path path, String fileName) {
        String zipPath = parentPath + File.separator + fileName + ".zip";
        logger.info("Creating Zip for: " + fileName);
        writeFilesInToZipFile(childPath, zipPath);
        mangaDownloadDetails.updateZipPath(zipPath);
        logger.info("Saved Zip: " + fileName);
        deleteFolderByPath(path, fileName);
    }

    private void deleteFolderByPath(Path path, String folderName) {
        try {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            logger.info("Deleted " + folderName + " folder");
        } catch (IOException | SecurityException e) {
            logger.trace("deleteFolderByPath failed for folder: " + folderName, e);
        }
    }

}
