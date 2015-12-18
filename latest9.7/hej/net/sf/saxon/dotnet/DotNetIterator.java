package net.sf.saxon.dotnet;

import cli.System.Collections.IEnumerator;

import java.util.Iterator;

/**
 * This class maps a .NET IEnumerator to a Java Iterator. It optionally accepts a mapping function which
 * converts the objects delivered by the .NET IEnumerator to the required Java class.
 */

public class DotNetIterator<T extends Object> implements Iterator<T> {

    public interface Mapper<T> {
        T convert(Object o);
    }

    private IEnumerator enumerator;
    private Mapper<T> mapper;


    public DotNetIterator(IEnumerator enumerator) {
        this.enumerator = enumerator;
    }

    public DotNetIterator(IEnumerator enumerator, Mapper<T> mapper) {
        this.enumerator = enumerator;
        this.mapper = mapper;
    }

    @Override
    public boolean hasNext() {
        return enumerator.MoveNext();
    }

    @Override
    public T next() {
        Object o = enumerator.get_Current();
        if (mapper != null) {
            return mapper.convert(o);
        } else {
            return (T) o;
        }
    }
}

