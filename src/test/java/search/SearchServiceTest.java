package search;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.function.Function;

@Log4j2
@SpringBootTest
public class SearchServiceTest {

    @Autowired
    private SearchService searchService;

    @Test
    public void search() throws Exception {
        query("Eddu", res -> res.size() == 1, searchService);
        query("Spring Cloud", r -> r.size() > 0, searchService);
    }

    private String buildBasicQuery(String query) {
        return "title:\"" + query + "\"~ OR description:\"" + query + "\"~";
    }

    private void query(String query, Function<List<Podcast>, Boolean> test, SearchService searchService) throws Exception {
        var results = searchService.search(buildBasicQuery(query));
        Assertions.assertTrue(test.apply(results), () -> "there should be one match");
        log.info(query);
        log.info("=========================================================================");
        for (var p : results)
            result(p);
    }

    private void result(Podcast podcast) {
        log.info("------------------------------------------------------------------------");
        log.info("(" + podcast.getUid() + ") " + podcast.getTitle());
        log.info(podcast.getDescription());
    }
}
