package search;

import lombok.extern.log4j.Log4j2;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.RestTemplate;

@Log4j2
@SpringBootApplication
public class SearchApiApplication {

    @Bean
    RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }

/*
    @Bean
    @Lazy
    IndexSearcher indexSearcher(@Value("${search.index-directory-resource}") Resource indexDir) throws Exception {
        var reader = DirectoryReader.open(FSDirectory.open(indexDir.getFile().toPath()));
        return new IndexSearcher(reader);
    }
*/

    @Bean
    SearchService searchService(
            RestTemplate restTemplate,

            @Value("${search.podcasts-json-resource}") Resource uri,
            @Value("${search.index-directory-resource}") Resource indexDir) throws Exception {
        var file = indexDir.getFile();
        Assert.isTrue(file.exists() || file.mkdirs(), () -> "the directory " + file.getAbsolutePath() + " should exist");
        return new SearchService(file, restTemplate, uri.getURI());
    }

  /*  @Bean
    ApplicationListener<ApplicationReadyEvent> buildIndex(SearchService searchService) {
        return event -> {
            try {
                searchService.buildIndex();
            } catch (Exception exception) {
                ReflectionUtils.rethrowRuntimeException(exception);
            }
        };
    }*/

    public static void main(String[] args) throws Exception {
        SpringApplication.run(SearchApiApplication.class, args);
    }


}

