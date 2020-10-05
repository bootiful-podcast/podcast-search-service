package search;

import lombok.extern.log4j.Log4j2;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.web.client.RestTemplate;

@Log4j2
@SpringBootApplication
public class SearchApiApplication {

	@Bean
	RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
		return restTemplateBuilder.build();
	}

	@Bean
	PodcastSearchService podcastSearchService(@Value("${search.index-directory-resource}") Resource indexDirectory,
			RestTemplate restTemplate, Analyzer analyzer, @Value("${search.podcasts-json-resource}") Resource uri)
			throws Exception {
		return new PodcastSearchService(analyzer, restTemplate, uri.getURI(), indexDirectory.getFile());
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(SearchApiApplication.class, args);
	}

}
