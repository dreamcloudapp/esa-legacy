package be.vanoosten.esa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;

import static org.apache.lucene.util.Version.LUCENE_48;

/**
 *
 * @author Philip van Oosten
 */
public class EnwikiFactory extends WikiFactory {
    private static String getWikiDumpFile(int year) {
        if (year == 2021) {
            return "enwiki-20210801-pages-articles-multistream.xml.bz2";
        } else if(year == 20211) {
            return "simplewiki-20210101-pages-articles-multistream.xml.bz2";
        } else {
            return "enwiki-20080103-pages-articles.xml.bz2";
        }
    }

    public static CharArraySet getExtendedStopWords() {
        CharArraySet stopwords = new CharArraySet(LUCENE_48, 1024, true);
        File stopFile = new File("./src/data/en-stopwords.txt");
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(stopFile));
            String line = reader.readLine();
            while (line != null) {
                if (!"".equals(line)) {
                    stopwords.add(line);
                }
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stopwords;
    }

    public EnwikiFactory() {
        super(indexRootPath(),
                new File(indexRootPath(), String.join(File.separator, getWikiDumpFile(20211))),
                getExtendedStopWords());
    }

    private static File indexRootPath() {
        return new File(String.join(File.separator, "F:", "dev", "esa", "enwiki"));
    }
}
