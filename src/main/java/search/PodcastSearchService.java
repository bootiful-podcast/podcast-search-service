package search;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.FSDirectory;
import org.jsoup.Jsoup;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
class PodcastSearchService {

	private final int maxResultsDefault = 50;

	private final RestTemplate restTemplate;

	private final URI podcastsJsonUri;

	private final Analyzer analyzer;

	private final IndexWriter writer;

	private final IndexSearcher searcher;

	private final Object monitor = new Object();

	private final Collection<Podcast> podcasts = Collections.synchronizedSet(new HashSet<>());

	private final File indexDirectory;

	PodcastSearchService(Analyzer analyzer, RestTemplate template, URI podcastsJsonUri, File indexDirectory)
			throws Exception {
		this.indexDirectory = indexDirectory;
		Assert.isTrue(this.indexDirectory.exists() || this.indexDirectory.mkdirs(),
				() -> "the directory " + this.indexDirectory.getAbsolutePath() + " should exist");
		this.restTemplate = template;
		this.analyzer = analyzer;
		this.podcastsJsonUri = podcastsJsonUri;
		try (var writer = indexWriter(analyzer) ){
			this.writer = writer ;
			refreshIndex();
		}
		var reader = indexReader();
		this.searcher = indexSearcher(reader);
	}

	public void refreshIndex() {
		synchronized (this.monitor) {
			this.podcasts.clear();
			this.podcasts.addAll(this.loadPodcasts());
			this.podcasts.forEach(p -> indexPodcast(this.writer, p));
		}
	}

	@SneakyThrows
	private void indexPodcast(IndexWriter writer, Podcast podcast) {
		var doc = new Document();
		doc.add(new StringField("id", Integer.toString(podcast.getId()), Field.Store.YES));
		doc.add(new StringField("uid", podcast.getUid(), Field.Store.YES));
		doc.add(new TextField("title", podcast.getTitle(), Field.Store.YES));
		doc.add(new TextField("description", html2text(podcast.getDescription()), Field.Store.YES));
		doc.add(new LongPoint("time", podcast.getDate().getTime()));
		writer.updateDocument(new Term("uid", podcast.getUid()), doc);
	}

	private Collection<Podcast> loadPodcasts() {
		var responseEntity = this.restTemplate.exchange(podcastsJsonUri, HttpMethod.GET, null,
				new ParameterizedTypeReference<Collection<Podcast>>() {
				});
		Assert.isTrue(responseEntity.getStatusCode().is2xxSuccessful(), () -> "the HTTP response should be 200x");
		return responseEntity.getBody();
	}

	private Query queryParserQueryFor(String queryStr) throws Exception {
		var qp = new QueryParser("description", analyzer);
		return qp.parse(queryStr);
	}

	private static String html2text(String html) {
		return Jsoup.parse(html).text();
	}

	private List<String> searchIndex(String queryStr, int maxResults) throws Exception {
		var query = queryParserQueryFor(queryStr);
		var search = searcher.search(query, maxResults);
		var ids = new ArrayList<String>();
		for (var sd : search.scoreDocs) {
			var doc = searcher.doc(sd.doc);
			ids.add(doc.get("uid"));
		}
		return ids;
	}

	public List<Podcast> search(String query) throws Exception {
		var searchUids = this.searchIndex(query, this.maxResultsDefault);
		log.debug("there are " + searchUids.size() + " results for the query [" + query + "]");
		return this.podcasts.stream().filter(podcast -> searchUids.contains(podcast.getUid()))
				.collect(Collectors.toList());
	}

	private IndexWriter indexWriter(Analyzer analyzer) throws Exception {
		var dir = FSDirectory.open(indexDirectory.toPath());
		var iwc = new IndexWriterConfig(analyzer);
		iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
		return new IndexWriter(dir, iwc);
	}

	private IndexReader indexReader() throws Exception {
		return DirectoryReader.open(FSDirectory.open(this.indexDirectory.toPath()));
	}

	private IndexSearcher indexSearcher(@Lazy IndexReader reader) {
		return new IndexSearcher(reader);
	}

}
