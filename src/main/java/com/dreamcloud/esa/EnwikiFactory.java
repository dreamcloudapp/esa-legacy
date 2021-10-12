package com.dreamcloud.esa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.lucene.analysis.CharArraySet;

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
        CharArraySet stopwords = new CharArraySet(1024, true);
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
                Paths.get(indexRootPath().toString(), String.join(File.separator, getWikiDumpFile(20211))),
                getExtendedStopWords());
    }

    private static Path indexRootPath() {
        return Paths.get(String.join(File.separator, "F:", "dev", "esa", "enwiki"));
    }
}
