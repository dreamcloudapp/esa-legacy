package com.dreamcloud.esa.server;

import com.dreamcloud.esa.EsaOptions;
import com.dreamcloud.esa.vectorizer.ConceptVector;
import com.dreamcloud.esa.vectorizer.TextVectorizer;
import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.core.JavalinConfig;
import io.javalin.core.util.Header;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * This really shouldn't get the ESA options.
 * The ESA options all need to be built from the HTTP request.
 * Probably need to figure out a way to seamlessly build them from either
 * HTTP or command line!
 */
public class EsaHttpServer {
    private final TextVectorizer vectorizer;
    EsaOptions options;

    public static boolean nonEmpty(String s) {
        return s != null && !s.equals("");
    }

    public EsaHttpServer(TextVectorizer vectorizer, EsaOptions options) {
        this.vectorizer = vectorizer;
        this.options = options;
    }
    public void start(int port) throws IOException, SQLException {
        Gson gson = new Gson();
        Javalin app = Javalin.create(JavalinConfig::enableCorsForAllOrigins).start(port);

        //Scores two documents relatedness via their texts, supporting all options
        app.post("/similarity", ctx -> {
            ctx.header(Header.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            DocumentSimilarityRequestBody requestBody = gson.fromJson(ctx.body(), DocumentSimilarityRequestBody.class);
            DocumentSimilarityScorer scorer = new DocumentSimilarityScorer(vectorizer);
            ctx.json(scorer.score(requestBody));
        });
    }
}
