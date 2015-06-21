package jar2xml.scrapers;

import java.io.File;
import java.io.IOException;

public class Java7DocScraper extends AndroidDocScraper {
	static final String pattern_head_javadoc = "<td class=\"col.+\"><code><strong><a href=\"[./]*"; // I'm not sure how path could be specified... (./ , ../ , or even /)
	static final String reset_pattern_head_javadoc = "<td><code>";
	static final String parameter_pair_splitter_javadoc = "&nbsp;";

	public Java7DocScraper (File dir) throws IOException {
		super (dir, pattern_head_javadoc, reset_pattern_head_javadoc, parameter_pair_splitter_javadoc, true);
	}
}
