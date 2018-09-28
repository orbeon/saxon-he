package net.sf.saxon.ma.parray;


import java.util.Collection;

/**
 * An immutable, persistent indexed collection.
 *
 * @param <E>
 * @author harold
 */
public interface PSequence<E> {

    public E get(int i);

    public int size();

    //@Override
    public PSequence<E> plus(E e);

    //@Override
    public PSequence<E> plusAll(Collection<? extends E> list);

    /**
     * @param i
     * @param e
     * @return a sequence consisting of the elements of this with e replacing the element at index i.
     * @throws IndexOutOfBoundsException if i&lt;0 || i&gt;=this.size()
     */
    public PSequence<E> with(int i, E e);

    /**
     * @param i
     * @param e non-null
     * @return a sequence consisting of the elements of this with e inserted at index i.
     * @throws IndexOutOfBoundsException if i&lt;0 || i&gt;this.size()
     */
    public PSequence<E> plus(int i, E e);

    /**
     * @param i
     * @param list
     * @return a sequence consisting of the elements of this with list inserted at index i.
     * @throws IndexOutOfBoundsException if i&lt;0 || i&gt;this.size()
     */
    public PSequence<E> plusAll(int i, Collection<? extends E> list);

    /**
     * Returns a sequence consisting of the elements of this without the first occurrence of e.
     */
    //@Override
    public PSequence<E> minus(Object e);

    //@Override
    public PSequence<E> minusAll(Collection<?> list);

    /**
     * @param i
     * @return a sequence consisting of the elements of this with the element at index i removed.
     * @throws IndexOutOfBoundsException if i&lt;0 || i&gt;=this.size()
     */
    public PSequence<E> minus(int i);

    //@Override
    public PSequence<E> subList(int start, int end);


    /**
     * @param start
     * @return subList(start, this.size())
     */
    public PSequence<E> subList(int start);

}
