package com.manga.harbour.mh.service;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
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

	public Resource convertImageUrlToResource(String imageUrl) {
		byte[] imageData = retrieveImageData(imageUrl).block(); 
		if (imageData != null) {
			InputStream inputStream = new ByteArrayInputStream(imageData);
			return new InputStreamResource(inputStream);
		} else {
			return getDefaultImageResource();
		}
	}

	private Mono<byte[]> retrieveImageData(String imageUrl) {
		return client.get()
				.uri(imageUrl)
				.retrieve()
				.bodyToMono(byte[].class);
	}

	private Resource getDefaultImageResource() {
		return null;
	}
}
