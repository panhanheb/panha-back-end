package org.example.panhabackend.repository.projection;

/**
 * Projection of "number of rows per class id".
 */
public interface ClassCount {

    Long getClassId();

    Long getTotal();
}
