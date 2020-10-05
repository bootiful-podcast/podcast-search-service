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
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.io.File;

@Log4j2
@Configuration
class LuceneAutoConfiguration {

	private static final String IW_NAME = "indexWriter";

	private final File indexDirectory;

	LuceneAutoConfiguration(@Value("${search.index-directory-resource}") Resource indexDirectory) throws Exception {
		this.indexDirectory = indexDirectory.getFile();
		this.ensure(this.indexDirectory);
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

	private void ensure(File directoryFile) {
		// if (directoryFile.exists()) directoryFile.delete();
		Assert.isTrue(directoryFile.exists() || directoryFile.mkdirs(),
				() -> directoryFile.getAbsolutePath() + " does not exist");
		log.info("created " + directoryFile.getAbsolutePath() + '.');
	}

	@Bean(name = IW_NAME, destroyMethod = "close")
	IndexWriter indexWriter(Analyzer analyzer) throws Exception {
		var dir = FSDirectory.open(indexDirectory.toPath());
		var iwc = new IndexWriterConfig(analyzer);
		iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
		return new IndexWriter(dir, iwc);
	}

	@Bean
	@Lazy
	@DependsOn(value = IW_NAME)
	IndexReader indexReader() throws Exception {
		return DirectoryReader.open(FSDirectory.open(this.indexDirectory.toPath()));
	}

	@Bean
	@Lazy
	@DependsOn(IW_NAME)
	IndexSearcher indexSearcher(IndexReader reader) {
		return new IndexSearcher(reader);
	}

}
