package search;

import lombok.extern.log4j.Log4j2;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.FSDirectory;
import org.jsoup.Jsoup;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

@Log4j2
class SearchService {

    private final Collection<Podcast> podcasts = new ConcurrentLinkedDeque<>();
    private final int maxResultsDefault = 50;
    private final RestTemplate restTemplate;
    private final File indexDirectory;
    private final URI podcastsJsonUri;
    private final IndexSearcher indexSearcher;


    // todo move the indxing client outside the service itself
    // todo we should be able to multithread IndexReader, IndexWriter, IndexSearchers
    public void index(Podcast podcast) {

    }

    SearchService(File index, RestTemplate template, URI podcastsJsonUri)
            throws Exception {
        this.indexDirectory = index;
        this.restTemplate = template;
        this.podcastsJsonUri = podcastsJsonUri;
        buildIndex();
        var reader = DirectoryReader.open(FSDirectory.open(indexDirectory.toPath()));
        this.indexSearcher = new IndexSearcher(reader);
        this.podcasts.addAll(this.loadPodcasts());
        log.debug("there are " + this.podcasts.size() + " Podcasts");
    }

    public void buildIndex() {
        try (var dir = FSDirectory.open(indexDirectory.toPath())) {
            var analyzer = buildAnalyzer();
            var iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            var start = new Date();
            try (var writer = new IndexWriter(dir, iwc)) {
                this.podcasts.forEach(podcast -> {
                    try {
                        indexPodcast(writer, podcast);
                    } catch (IOException e) {
                        ReflectionUtils.rethrowRuntimeException(e);
                    }
                });
            }
            var stop = new Date();
            log.info("index time: " + (stop.getTime() - start.getTime()) + "ms");
        } catch (Exception e) {
            ReflectionUtils.rethrowRuntimeException(e);
        }
    }

    private void indexPodcast(IndexWriter writer, Podcast podcast) throws IOException {
        var doc = new Document();
        doc.add(new StringField("id", Integer.toString(podcast.getId()), Field.Store.YES));
        doc.add(new StringField("uid", podcast.getUid(), Field.Store.YES));
        doc.add(new TextField("title", podcast.getTitle(), Field.Store.YES));
        doc.add(new TextField("description", html2text(podcast.getDescription()), Field.Store.YES));
        doc.add(new LongPoint("time", podcast.getDate().getTime()));
        writer.updateDocument(new Term("uid", podcast.getUid()), doc);
    }

    private Collection<Podcast> loadPodcasts() {
        var responseEntity = this.restTemplate
                .exchange(podcastsJsonUri, HttpMethod.GET, null,
                        new ParameterizedTypeReference<Collection<Podcast>>() {
                        });
        Assert.isTrue(responseEntity.getStatusCode().is2xxSuccessful(), () -> "the HTTP response should be 200x");
        return responseEntity.getBody();
    }

    protected Analyzer buildAnalyzer() {
        return new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                StandardTokenizer tokenizer = new StandardTokenizer();
                tokenizer.setMaxTokenLength(StandardAnalyzer.DEFAULT_MAX_TOKEN_LENGTH);
                TokenFilter filters = new StopFilter(new ASCIIFoldingFilter(new LowerCaseFilter(tokenizer)), CharArraySet.EMPTY_SET);
                return new TokenStreamComponents(tokenizer, filters);
            }
        };
    }

    private Query queryParserQueryFor(String queryStr) throws Exception {
        var analyzer = buildAnalyzer();
        var qp = new QueryParser("description", analyzer);
        return qp.parse(queryStr);
    }

    private static String html2text(String html) {
        return Jsoup.parse(html).text();
    }

    private List<String> searchIndex(String queryStr, int maxResults) throws Exception {
        var query = queryParserQueryFor(queryStr);
        var search = indexSearcher.search(query, maxResults);
        var ids = new ArrayList<String>();
        for (var sd : search.scoreDocs) {
            var doc = indexSearcher.doc(sd.doc);
            ids.add(doc.get("uid"));
        }
        return ids;
    }

    public List<Podcast> search(String query) throws Exception {
        var searchUids = this.searchIndex(query, this.maxResultsDefault);
        log.debug("there are " + searchUids.size() + " results for the query [" + query + "]");
        return this.podcasts
                .stream()
                .filter(podcast -> searchUids.contains(podcast.getUid()))
                .collect(Collectors.toList());
    }
}
