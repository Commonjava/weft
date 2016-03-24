package org.commonjava.cdi.util.weft;

import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

/**
 * This is used to qualify executors that should be injected via Weft, as opposed to other ExecutorService producers.
 * Created by jdcasey on 3/9/16.
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( { METHOD, FIELD, PARAMETER, TYPE } )
@Qualifier
public @interface WeftManaged
{
}
