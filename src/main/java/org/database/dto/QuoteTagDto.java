package org.database.dto;

import jakarta.persistence.*;
import org.hibernate.Session;

import java.util.Collection;
import java.util.List;

@Entity
@Table(
        name = QuoteTagDto.QUOTE_TAG_TABLE_NAME,
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {QuoteTagDto.ID_TAG_COLUMN_NAME, QuoteTagDto.ID_QUOTE_COLUMN_NAME})
        }
)
@IdClass(QuoteTagDto.QuoteTagDtoPK.class)
public class QuoteTagDto {
    public static final String QUOTE_TAG_TABLE_NAME = "quote_tag";

    public static final String ID_TAG_COLUMN_NAME = "id_tag";
    public static final String ID_QUOTE_COLUMN_NAME = "id_quote";

    @Id
    @Column(name = QuoteTagDto.ID_TAG_COLUMN_NAME, nullable = false)
    private long idTag;

    @Id
    @Column(name = QuoteTagDto.ID_QUOTE_COLUMN_NAME, nullable = false)
    private long idQuote;

    public QuoteTagDto() {}

    public QuoteTagDto(long idTag, long idQuote) {
        this.idTag = idTag;
        this.idQuote = idQuote;
    }

    public static List<QuoteTagDto> findByTags(Collection<String> tags, Session session) {
        String innerSql = "SELECT " + TagDto.ID_TAG_COLUMN_NAME + " FROM "
                + TagDto.TAG_TABLE_NAME + " WHERE " + TagDto.TAG_COLUMN_NAME
                + " IN :p_tags";
        String sql = "SELECT * FROM " + QuoteTagDto.QUOTE_TAG_TABLE_NAME
                + " WHERE " + QuoteTagDto.ID_TAG_COLUMN_NAME + " IN ("
                + innerSql + ")";
        return session.createNativeQuery(sql, QuoteTagDto.class)
                .setParameter("p_tags", tags)
                .getResultList();
    }

    public long getIdQuote() {
        return this.idQuote;
    }

    protected record QuoteTagDtoPK(long idTag, long idQuote) {
        @Override
        public boolean equals(Object other) {
            if (other instanceof QuoteTagDtoPK(long otherIdTag, long otherIdQuote)) {
                return this.idTag == otherIdTag && this.idQuote == otherIdQuote;
            }

            return false;
        }
    }
}
