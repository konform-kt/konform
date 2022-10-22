package io.konform.validation;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class TestSubject {

    public TestSubject() {
        this("logic will get you from A to Z imagination will get you everywhere");
    }

    public TestSubject(String string) {
        this.array = string.split(" ");
    }

    private final String[] array;

    public String[] stringArray() {
        return array;
    }

    public Iterable<String> stringIterable() {
        return Arrays.asList(array);
    }

    public Map<String, String> stringMap() {
        return Arrays.stream(array)
                .collect(Collectors.toMap(s -> s, s -> s, (s1, s2) -> s1));
    }

    @Nullable
    public String nullString() {
        return null;
    }

    @Nullable
    public String notNullString() {
        return "bla";
    }

}
