package com.manga.harbour.mh.service;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Service
public class MangaCoverService {

	private final WebClient client;

	public MangaCoverService(WebClient.Builder webClientBuilder) {
		this.client = webClientBuilder
				.codecs(codecs -> codecs
						.defaultCodecs()
						.maxInMemorySize(25000 * 1024))
				.build();

	}

	public ResponseEntity<Resource>  convertImageUrlToResource(String imageUrl) {
		byte[] imageData = retrieveImageData(imageUrl).block(); 
		if (imageData != null) {
			InputStream inputStream = new ByteArrayInputStream(imageData);
			Resource imageResource = new InputStreamResource(inputStream);
			return ResponseEntity.ok()
					.contentType(MediaType.IMAGE_JPEG) // Set the content type for JPEG images
					.body(imageResource);
		}
		return null;
	}

	public Mono<byte[]> retrieveImageData(String imageUrl) {
		return client.get()
				.uri(imageUrl)
				.retrieve()
				.bodyToMono(byte[].class);
	}
}
