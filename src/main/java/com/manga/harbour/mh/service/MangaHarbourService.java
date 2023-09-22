package com.manga.harbour.mh.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class MangaHarbourService {
	
	WebClient client= WebClient.create("https://api.mangadex.org");
	
	public String getMangaById() {	
	return client.get().uri("/manga/random").retrieve().bodyToMono(String.class).block();
	}

}
