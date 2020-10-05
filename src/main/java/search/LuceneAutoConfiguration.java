package search;

import lombok.extern.log4j.Log4j2;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

@Log4j2
@Configuration
class LuceneAutoConfiguration {

	LuceneAutoConfiguration() {
		log.debug("launching " + this.getClass().getName() + '.');
	}

	@Bean
	IndexSearcher indexSearcher(IndexReader reader) {
		return new IndexSearcher(reader);
	}

	@Bean
	Analyzer analyzer() {
		return new Analyzer() {
			@Override
			protected TokenStreamComponents createComponents(String fieldName) {
				var tokenizer = new StandardTokenizer();
				tokenizer.setMaxTokenLength(StandardAnalyzer.DEFAULT_MAX_TOKEN_LENGTH);
				var filters = new StopFilter(new ASCIIFoldingFilter(new LowerCaseFilter(tokenizer)),
						CharArraySet.EMPTY_SET);
				return new TokenStreamComponents(tokenizer, filters);
			}
		};

	}

	LuceneAutoConfiguration(@Value("${search.index-directory-resource}") Resource indexDirectory) throws Exception {
		var directoryFile = indexDirectory.getFile();
		Assert.isTrue(directoryFile.exists() || directoryFile.mkdirs(),
				() -> directoryFile.getAbsolutePath() + " does not exist");
	}

	@Bean
	IndexReader indexReader(@Value("${search.index-directory-resource}") Resource indexDirectory) throws Exception {
		return DirectoryReader.open(FSDirectory.open(indexDirectory.getFile().toPath()));
	}

	@Bean(destroyMethod = "close")
	IndexWriter indexWriter(Analyzer analyzer, @Value("${search.index-directory-resource}") Resource indexDirectory)
			throws Exception {
		var dir = FSDirectory.open(indexDirectory.getFile().toPath());
		var iwc = new IndexWriterConfig(analyzer);
		iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
		return new IndexWriter(dir, iwc);
	}

}
