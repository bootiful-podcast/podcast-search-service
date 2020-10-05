package search;

import lombok.extern.log4j.Log4j2;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.FileSystemUtils;

import java.io.File;

@Log4j2
@Configuration
class LuceneAutoConfiguration {

	private static final String IW_NAME = "indexWriter";

	private final File indexDirectory;

	LuceneAutoConfiguration(@Value("${search.index-directory-resource}") Resource indexDirectory) throws Exception {
		this.indexDirectory = indexDirectory.getFile();
		/*
			if (this.indexDirectory.exists() && this.indexDirectory.isDirectory()) {
				FileSystemUtils.deleteRecursively(this.indexDirectory);
			}
			Assert.isTrue(this.indexDirectory.exists() || this.indexDirectory.mkdirs(), () -> this.indexDirectory.getAbsolutePath() + " does not exist");
			log.info("created " + this.indexDirectory.getAbsolutePath() + '.');

		*/
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

}
