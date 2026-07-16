package com.sakyvo.nestedpackfix.forge189.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"rawtypes", "unchecked"})
public class AvailablePackFilterTest {
    @Test
    public void allModePreservesEveryAvailableEntry() {
        Object ordinary = new Object();
        Classified nested = new Classified(PackWarningKind.NESTED);
        Classified illegal = new Classified(PackWarningKind.ILLEGAL);
        List source = Arrays.asList(ordinary, nested, illegal);
        List target = new ArrayList();

        AvailablePackFilter.refresh(source, target, PackWarningKind.NONE);

        assertEquals(source, target);
    }

    @Test
    public void warningModesOnlyFilterTheDisplayList() {
        Object ordinary = new Object();
        Classified nested = new Classified(PackWarningKind.NESTED);
        Classified illegal = new Classified(PackWarningKind.ILLEGAL);
        List source = new ArrayList(Arrays.asList(ordinary, nested, illegal));
        List target = new ArrayList();

        AvailablePackFilter.refresh(source, target, PackWarningKind.NESTED);
        assertEquals(Arrays.asList(nested), target);
        assertEquals(Arrays.asList(ordinary, nested, illegal), source);

        AvailablePackFilter.refresh(source, target, PackWarningKind.ILLEGAL);
        assertEquals(Arrays.asList(illegal), target);
        assertEquals(Arrays.asList(ordinary, nested, illegal), source);
    }

    @Test
    public void anomalyPresenceIgnoresOrdinaryEntries() {
        List entries = Arrays.asList(
            new Object(),
            new Classified(PackWarningKind.ILLEGAL)
        );

        assertFalse(AvailablePackFilter.contains(entries, PackWarningKind.NESTED));
        assertTrue(AvailablePackFilter.contains(entries, PackWarningKind.ILLEGAL));
    }

    @Test
    public void reinstallRemovesEveryStaleIllegalProjection() {
        Object ordinary = new Object();
        Classified nested = new Classified(PackWarningKind.NESTED);
        List entries = new ArrayList(Arrays.asList(
            new Classified(PackWarningKind.ILLEGAL),
            ordinary,
            new Classified(PackWarningKind.ILLEGAL),
            nested
        ));

        AvailablePackFilter.remove(entries, PackWarningKind.ILLEGAL);

        assertEquals(Arrays.asList(ordinary, nested), entries);
    }

    private static final class Classified implements PackWarningClassifier {
        private final PackWarningKind kind;

        private Classified(PackWarningKind kind) {
            this.kind = kind;
        }

        @Override
        public PackWarningKind nestedpackfix$getWarningKind() {
            return this.kind;
        }
    }
}
