package org.database.dto;

import jakarta.persistence.*;

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

    public QuoteTagDto(long idTag, long idQuote) {
        this.idTag = idTag;
        this.idQuote = idQuote;
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
