package gov.nist.toolkit.saml.util;
import java.util.ArrayList;
import java.util.List;

/**
 * The abstraction this class provides is a push down stack of variable
 * length frames of prefix to namespace mappings.  Used for keeping track
 * of what namespaces are active at any given point as an XML document is
 * traversed or produced.
 * <p/>
 * From a performance point of view, this data will both be modified frequently
 * (at a minimum, there will be one push and pop per XML element processed),
 * and scanned frequently (many of the "good" mappings will be at the bottom
 * of the stack).  The one saving grace is that the expected maximum
 * cardinalities of the number of frames and the number of total mappings
 * is only in the dozens, representing the nesting depth of an XML document
 * and the number of active namespaces at any point in the processing.
 * <p/>
 * Accordingly, this stack is implemented as a single array, will null
 * values used to indicate frame boundaries.
 *
 */
public class NSStack {
    protected static org.apache.commons.logging.Log log =
        org.apache.commons.logging.LogFactory.getLog(NSStack.class);

    private Mapping[] stack;
    private int top = 0;
    private int iterator = 0;
    private int currentDefaultNS = -1;
    // invariant member variable to track low-level logging requirements
    // we cache this once per instance lifecycle to avoid repeated lookups
    // in heavily used code.
    private final boolean traceEnabled = log.isTraceEnabled();

    public NSStack() {
        stack = new Mapping[32];
        stack[0] = null;
    }

    /**
     * Create a new frame at the top of the stack.
     */
    public void push() {
        top++;
        if (top >= stack.length) {
            Mapping newstack[] = new Mapping[stack.length * 2];
            System.arraycopy(stack, 0, newstack, 0, stack.length);
            stack = newstack;
        }
        if (traceEnabled)
            log.trace("NSPush (" + stack.length + ")");
        stack[top] = null;
    }

    /**
     * Remove the top frame from the stack.
     */
    public void pop() {
        clearFrame();
        top--;

        // If we've moved below the current default NS, figure out the new
        // default (if any)
        if (top < currentDefaultNS) {
            // Reset the currentDefaultNS to ignore the frame just removed.
            currentDefaultNS = top;
            while (currentDefaultNS > 0) {
                if (stack[currentDefaultNS] != null &&
                        stack[currentDefaultNS].getPrefix().length() == 0)
                    break;
                currentDefaultNS--;
            }
        }
        if (top == 0) {
            if (traceEnabled)
                log.trace("NSPop (empty)");
            return;
        }
        if (traceEnabled) {
            log.trace("NSPop (" + stack.length + ")");
        }
    }

    /**
     * Return a copy of the current frame.  Returns null if none are present.
     */
    public List<Mapping> cloneFrame() {
        if (stack[top] == null) return null;
        List<Mapping> clone = new ArrayList<Mapping>();
        for (Mapping map = topOfFrame(); map != null; map = next()) {
            clone.add(map);
        }
        return clone;
    }

    /**
     * Remove all mappings from the current frame.
     */
    private void clearFrame() {
        while (stack[top] != null) top--;
    }

    /**
     * Reset the embedded iterator in this class to the top of the current
     * (i.e., last) frame.  Note that this is not threadsafe, nor does it
     * provide multiple iterators, so don't use this recursively.  Nor
     * should you modify the stack while iterating over it.
     */
    public Mapping topOfFrame() {
        iterator = top;
        while (stack[iterator] != null) iterator--;
        iterator++;
        return next();
    }

    /**
     * Return the next namespace mapping in the top frame.
     */
    public Mapping next() {
        if (iterator > top) {
            return null;
        } else {
            return stack[iterator++];
        }
    }

    /**
     * Add a mapping for a namespaceURI to the specified prefix to the top
     * frame in the stack.  If the prefix is already mapped in that frame,
     * remap it to the (possibly different) namespaceURI.
     */
    public void add(String namespaceURI, String prefix) {
        int idx = top;
        try {
            // Replace duplicate prefixes (last wins - this could also fault)
            for (int cursor = top; stack[cursor] != null; cursor--) {
                if (stack[cursor].getPrefix().equals(prefix)) {
                    stack[cursor].setNamespaceURI(namespaceURI);
                    idx = cursor;
                    return;
                }
            }
            push();
            stack[top] = new Mapping(namespaceURI, prefix);
            idx = top;
        } finally {
            // If this is the default namespace, note the new in-scope
            // default is here.
            if (prefix.length() == 0) {
                currentDefaultNS = idx;
            }
        }
    }

    /**
     * Return an active prefix for the given namespaceURI.  NOTE : This
     * may return null even if the namespaceURI was actually mapped further
     * up the stack IF the prefix which was used has been repeated further
     * down the stack.  I.e.:
     * <p/>
     * <pre:outer xmlns:pre="namespace">
     * <pre:inner xmlns:pre="otherNamespace">
     * *here's where we're looking*
     * </pre:inner>
     * </pre:outer>
     * <p/>
     * If we look for a prefix for "namespace" at the indicated spot, we won't
     * find one because "pre" is actually mapped to "otherNamespace"
     */
    public String getPrefix(String namespaceURI, boolean noDefault) {
        if ((namespaceURI == null) || (namespaceURI.equals("")))
            return null;
        int hash = namespaceURI.hashCode();

        // If defaults are OK, and the given NS is the current default,
        // return "" as the prefix to favor defaults where possible.
        if (!noDefault && currentDefaultNS > 0 && stack[currentDefaultNS] != null &&
                namespaceURI.equals(stack[currentDefaultNS].getNamespaceURI()))
            return "";
        for (int cursor = top; cursor > 0; cursor--) {
            Mapping map = stack[cursor];
            if (map == null) continue;
            if (map.getNamespaceHash() == hash &&
                    map.getNamespaceURI().equals(namespaceURI)) {
                String possiblePrefix = map.getPrefix();
                if (noDefault && possiblePrefix.length() == 0) continue;

                // now make sure that this is the first occurance of this 
                // particular prefix
                int ppHash = possiblePrefix.hashCode();
                for (int cursor2 = top; true; cursor2--) {
                    if (cursor2 == cursor) return possiblePrefix;
                    map = stack[cursor2];
                    if (map == null) continue;
                    if (ppHash == map.getPrefixHash() &&
                            possiblePrefix.equals(map.getPrefix()))
                        break;
                }
            }
        }
        return null;
    }

    /**
     * Return an active prefix for the given namespaceURI, including
     * the default prefix ("").
     */
    public String getPrefix(String namespaceURI) {
        return getPrefix(namespaceURI, false);
    }

    /**
     * Given a prefix, return the associated namespace (if any).
     */
    public String getNamespaceURI(String prefix) {
        if (prefix == null)
            prefix = "";
        int hash = prefix.hashCode();
        for (int cursor = top; cursor > 0; cursor--) {
            Mapping map = stack[cursor];
            if (map == null) continue;
            if (map.getPrefixHash() == hash && map.getPrefix().equals(prefix))
                return map.getNamespaceURI();
        }
        return null;
    }

    /**
     * Produce a trace dump of the entire stack, starting from the top and
     * including frame markers.
     */
    public void dump(String dumpPrefix) {
        for (int cursor = top; cursor > 0; cursor--) {
            Mapping map = stack[cursor];
            if (map == null) {
                log.trace(dumpPrefix + "stackFrame00");
            } else {
                log.trace(dumpPrefix + map.getNamespaceURI() + " -> " + map.getPrefix());
            }
        }
    }
}
