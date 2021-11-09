package com.dreamcloud.esa.tools;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class BZipFileReader {
    public static Reader getFileReader(File file) throws IOException {
        InputStream wikiInputStream = new FileInputStream(file);
        InputStream bufferedInputStream = new BufferedInputStream(wikiInputStream);
        InputStream bzipInputStream = new BZip2CompressorInputStream(bufferedInputStream, false);
        return new InputStreamReader(bzipInputStream, StandardCharsets.UTF_8);
    }
}
