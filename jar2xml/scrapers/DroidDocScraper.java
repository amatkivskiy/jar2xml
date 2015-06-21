package jar2xml.scrapers;

import java.io.File;
import java.io.IOException;

public class DroidDocScraper extends AndroidDocScraper {
	static final String pattern_head_droiddoc = "<span class=\"sympad\"><a href=\".*";

	public DroidDocScraper (File dir) throws IOException {
		super (dir, pattern_head_droiddoc, null, null, false);
	}
}
