package be.vanoosten.esa.database;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.Map;

public class VectorRepository {
    Connection con;

    public VectorRepository() throws SQLException {
        Map<String, String> env = System.getenv();
        if (!env.containsKey("ODBC_CONNECTION_STRING")) {
            throw new SQLException("The connection string was empty.");
        }
        con = DriverManager.getConnection("jdbc:" + env.get("ODBC_CONNECTION_STRING"));
    }

    public void saveDocumentVector(DocumentVector vector) throws SQLException {
        CallableStatement statement = con.prepareCall("insert into vector(:id, :concept, :weight)");
        for(ConceptWeight weight: vector.conceptWeights) {
            statement.setBytes(":id", vector.id.getBytes(StandardCharsets.UTF_8));
            statement.setString(":concept", weight.concept);
            statement.setFloat(":weight", weight.weight);
            statement.execute();
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
