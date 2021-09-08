package be.vanoosten.esa.database;

import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.fst.FST;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.Map;

public class VectorRepository {
    Connection con;

    public VectorRepository(Connection con) throws SQLException {
        this.con = con;
    }

    public void saveDocumentVector(DocumentVector vector) throws SQLException {
        Statement statement = con.createStatement();
        for(ConceptWeight weight: vector.conceptWeights) {
            String sql = "insert into vector(`id`, `concept`, `weight`) VALUES(unhex('" + vector.id + "'), '" + weight.concept + "', " + weight.weight + ")";
            System.out.println("sql: " + sql);
            statement.execute(sql);
        }
    }

    public ArrayList<RelatedDocument> getRelatedDocuments(String id, int limit) throws SQLException {
        Statement statement = con.createStatement();
        ResultSet resultSet = statement.executeQuery("""
select
    hex(v1.id) as dream,
    hex(v2.id) as related,
    sum(
        case
            when v1.concept = v2.concept then v1.weight * v2.weight
            else 0
        end
    ) / (norm1.norm * norm2.norm) as cosine_similarity
from
    vector v1
inner join (
    select
        v.id as id,
        sqrt(sum(v.weight * v.weight)) as norm
    from
        vector v
    group by
        v.id
) as norm1 on norm1.id = v1.id
inner join
    vector v2 on v2.concept = v1.concept and  v2.id != v1.id
inner join (
    select
        v.id as id,
        sqrt(sum(v.weight * v.weight)) as norm
    from
        vector v
    group by
        v.id
) as norm2 on norm2.id = v2.id
where
    v1.id = UNHEX('""" + id + """
')
group by
    v1.id, v2.id
order by
    cosine_similarity desc
limit
    """ + limit + """
""");

        ArrayList<RelatedDocument> relatedDocuments = new ArrayList<>();
        while(resultSet.next()) {
            relatedDocuments.add(new RelatedDocument(resultSet.getString(1), resultSet.getFloat(2)));
        }
        return relatedDocuments;
    }
}
