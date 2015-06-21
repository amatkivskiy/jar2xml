package jar2xml.scrapers;

import java.io.File;
import java.io.IOException;

public class JavaDocScraper extends AndroidDocScraper {
	static final String pattern_head_javadoc = "<TD><CODE><B><A HREF=\"[./]*"; // I'm not sure how path could be specified... (./ , ../ , or even /)
	static final String reset_pattern_head_javadoc = "<TD><CODE>";
	static final String parameter_pair_splitter_javadoc = "&nbsp;";

	public JavaDocScraper (File dir) throws IOException {
		super (dir, pattern_head_javadoc, reset_pattern_head_javadoc, parameter_pair_splitter_javadoc, false);
	}
}
