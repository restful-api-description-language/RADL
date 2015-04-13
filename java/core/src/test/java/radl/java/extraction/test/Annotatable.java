/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.extraction.test;

import java.util.Collection;


interface Annotatable<T> {

  T annotateWith(Collection<Annotation> annotations);

}
