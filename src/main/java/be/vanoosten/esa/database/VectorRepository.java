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
    PreparedStatement  statement;
    Connection con;

    public VectorRepository(Connection con) throws SQLException {
        this.con = con;
    }

    protected PreparedStatement  getStatement() throws SQLException {
        if (statement == null) {
            statement = con.prepareStatement("insert into vector(`id`, `concept`, `weight`) values(?, ?, ?)");
        }
        return statement;
    }

    protected byte hexToByte(String hexString) {
        int firstDigit = toDigit(hexString.charAt(0));
        int secondDigit = toDigit(hexString.charAt(1));
        return (byte) ((firstDigit << 4) + secondDigit);
    }

    protected int toDigit(char hexChar) {
        int digit = Character.digit(hexChar, 16);
        if(digit == -1) {
            throw new IllegalArgumentException(
                    "Invalid Hexadecimal Character: "+ hexChar);
        }
        return digit;
    }

    protected byte[] decodeHexString(String hexString) {
        if (hexString.length() % 2 == 1) {
            throw new IllegalArgumentException(
                    "Invalid hexadecimal String supplied.");
        }

        byte[] bytes = new byte[hexString.length() / 2];
        for (int i = 0; i < hexString.length(); i += 2) {
            bytes[i / 2] = hexToByte(hexString.substring(i, i + 2));
        }
        return bytes;
    }

    public void saveDocumentVector(DocumentVector vector) throws SQLException {
        PreparedStatement  statement = getStatement();
        for(ConceptWeight weight: vector.conceptWeights) {
            statement.setBytes(1, decodeHexString(vector.id));
            statement.setString(2, weight.concept);
            statement.setFloat(3, weight.weight);
            statement.executeUpdate();
        }
    }

    public ArrayList<RelatedDocument> getRelatedDocuments(String id, int limit) throws SQLException {
        Statement statement = con.createStatement();
        ResultSet resultSet = statement.executeQuery("select\n" +
                "                hex(v1.id) as dream,\n" +
                "                hex(v2.id) as related,\n" +
                "                sum(\n" +
                "        case\n" +
                "                when v1.concept = v2.concept then v1.weight * v2.weight\n" +
                "            else 0\n" +
                "        end\n" +
                "    ) / (norm1.norm * norm2.norm) as cosine_similarity\n" +
                "        from\n" +
                "        vector v1\n" +
                "        inner join (\n" +
                "                select\n" +
                "        v.id as id,\n" +
                "                sqrt(sum(v.weight * v.weight)) as norm\n" +
                "        from\n" +
                "        vector v\n" +
                "        group by\n" +
                "        v.id\n" +
                ") as norm1 on norm1.id = v1.id\n" +
                "        inner join\n" +
                "        vector v2 on v2.concept = v1.concept and  v2.id != v1.id\n" +
                "        inner join (\n" +
                "                select\n" +
                "        v.id as id,\n" +
                "                sqrt(sum(v.weight * v.weight)) as norm\n" +
                "        from\n" +
                "        vector v\n" +
                "        group by\n" +
                "        v.id\n" +
                ") as norm2 on norm2.id = v2.id\n" +
                "        where\n" +
                "        v1.id = UNHEX('" + id + "')\n" +
                "        group by\n" +
                "        v1.id, v2.id\n" +
                "        order by\n" +
                "        cosine_similarity desc\n" +
                "    limit " + limit);

        ArrayList<RelatedDocument> relatedDocuments = new ArrayList<>();
        while(resultSet.next()) {
            relatedDocuments.add(new RelatedDocument(resultSet.getString(2), resultSet.getFloat(3)));
        }
        return relatedDocuments;
    }

    public DocumentScore scoreDocuments(String documentId1, String documentId2) {

    }
}
