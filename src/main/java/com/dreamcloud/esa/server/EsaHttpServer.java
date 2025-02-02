package com.dreamcloud.esa.server;

import com.dreamcloud.esa.EsaOptions;
import com.dreamcloud.esa.database.DocumentVector;
import com.dreamcloud.esa.database.MySQLConnection;
import com.dreamcloud.esa.database.VectorRepository;
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
        //Connect to MySQL
        Connection con = MySQLConnection.getConnection();
        VectorRepository repository = new VectorRepository(con);

        Gson gson = new Gson();
        Javalin app = Javalin.create(JavalinConfig::enableCorsForAllOrigins).start(port);
        app.post("/vectorize", ctx -> {
            DocumentVectorizationRequestBody requestBody = gson.fromJson(ctx.body(), DocumentVectorizationRequestBody.class);

            if (!nonEmpty(requestBody.documentText) || !nonEmpty(requestBody.documentId)) {
                ctx.res.sendError(400, "Invalid request: documentText and documentId are required fields.");
            } else {
                //Term idTerm = new Term(DreamIndexer.ID_FIELD, requestBody.documentId);
                //String weightedQuery = builder.weight(idTerm, requestBody.documentText);
                ConceptVector vector = vectorizer.vectorize(requestBody.documentText);
                DocumentVector documentVector = new DocumentVector(requestBody.documentId);
               //todo: fix to use document ids (or remove)
                /*Map<Integer, Float> conceptWeights = vector.getConceptWeights();
                for(int concept: conceptWeights.keySet()) {
                    documentVector.addConceptWeight(new ConceptWeight(concept, conceptWeights.get(concept)));
                }*/

                repository.saveDocumentVector(documentVector);
                ctx.status(200);
                System.out.println("Processed dream: " + requestBody.documentText.substring(0, 16) + "...");
            }
        });

        //Gets top related documents
        app.get("/related", ctx -> {
            try {
                String documentId = ctx.queryParam("documentId");
                String limitParam = ctx.queryParam("limit");
                if (documentId == null || limitParam == null) {
                    throw new Exception("Invalid request: document and limit are required fields.");
                }
                int relatedLimit = Integer.parseInt(limitParam);
                ctx.json(repository.getRelatedDocuments(documentId, relatedLimit));
                System.out.println("Related dream: " + documentId);
            } catch (Exception e) {
                System.out.println("Failed to relate dream: " + e.getMessage() + ": " + Arrays.toString(e.getStackTrace()));
                ctx.status(400);
            }
        });

        //Scores two documents relatedness via their IDs
        app.get("/quick-score", ctx -> {
            String documentId1 = ctx.queryParam("documentId1");
            String documentId2 = ctx.queryParam("documentId2");
            ctx.json(repository.scoreDocuments(documentId1, documentId2));
        });

        //Scores two documents relatedness via their texts, supporting all options
        app.post("/similarity", ctx -> {
            ctx.header(Header.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            DocumentSimilarityRequestBody requestBody = gson.fromJson(ctx.body(), DocumentSimilarityRequestBody.class);
            DocumentSimilarityScorer scorer = new DocumentSimilarityScorer(vectorizer);
            ctx.json(scorer.score(requestBody));
        });
    }
}
