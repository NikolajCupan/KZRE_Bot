package org.database;

import org.hibernate.Session;
import org.utility.ProcessingContext;

public interface Persistable {
    void persist(ProcessingContext processingContext, Session session);
    void cancelPersist(ProcessingContext processingContext);
}
