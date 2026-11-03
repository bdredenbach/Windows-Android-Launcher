package com.nthexperience.desktop;
import java.util.*;
/** Local Activity drag payload; no global URI grants or filesystem side effects at drag start. */
final class FileDrag {
    final List<DocumentOps.Ref> refs;final String sourcePage;
    final VistaExplorer explorer;
    FileDrag(List<DocumentOps.Ref> refs,String sourcePage,VistaExplorer explorer){this.refs=Collections.unmodifiableList(new ArrayList<>(refs));this.sourcePage=sourcePage;this.explorer=explorer;}
}
