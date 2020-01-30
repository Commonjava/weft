package org.commonjava.cdi.util.weft;

public interface ThreadContextualizer
{
    String getId();

    Object extractCurrentContext();

    void setChildContext( Object parentContext );

    void clearContext();
}
