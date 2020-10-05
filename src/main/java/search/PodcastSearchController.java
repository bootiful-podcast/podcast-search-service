package search;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Map;

@Log4j2
@RestController
@RequiredArgsConstructor
class PodcastSearchController {

	private final PodcastSearchService searchService;

	@GetMapping("/podcasts")
	Collection<Podcast> getPodcasts() {
		return this.searchService.getAllPodcasts();
	}

	@GetMapping("/search")
	Map<String, Object> search(@RequestParam String query) throws Exception {
		log.info("running the query:  " + query);
		var results = this.searchService.search(query);
		return Map.of("count", results.size(), "results", results);
	}

	@PostMapping("/refresh")
	ResponseEntity<?> refresh() {

		searchService.refreshIndex();

		return ResponseEntity.ok().build();
	}

}
