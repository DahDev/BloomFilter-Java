/**
 * MIT License
 *
 * Copyright (c) 2017 Damian Stygar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package pl.dahdev.bloomfilters;


import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.BitSet;
import java.util.Collection;

/**
 * Abstract Class of Bloom Filter contains implemented all basic methods.
 *
 * A Bloom filter is a space-efficient probabilistic data structure
 * that is used to test whether an element is a member of a set.
 * A query returns either "possibly in set" or "definitely not in set".
 * Elements can be added to the set, but not removed.
 *
 * @param <E> - the type of elements in this Bloom Filter.
 *
 * @author Damian Stygar
 */
public abstract class AbstractBloomFilter<E> implements BloomFilter<E> {

    final int size;
    final int expectedNumberOfElements;
    BitSet bitSet;
    int numberOfHash;
    int numberOfElements;
    double bitsPerElement;

    /**
     * Bloom filter constructor.
     *
     * Number of hash functions is estimated from:
     * k = (m/n)ln(2),
     * where k is number of hash functions, m is size of Bloom Filter, n is number of expected elements.
     *
     * Bits per element are estimated from:
     * pbs = m/n,
     * where m is size of Bloom Filter, n is number of expected elements.
     *
     * Size of Bloom Filter and number of expected elements should be greater than 0.
     *
     * @param size                     size of Bloom Filter.
     * @param expectedNumberOfElements expected number of elements to be inserted to Bloom Filter.
     */
    public AbstractBloomFilter(int size, int expectedNumberOfElements) {
        if (expectedNumberOfElements <= 0) {
            throw new IllegalArgumentException("Expected number of elements should be greater than 0!");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Size of Bloom Filter should be greater than 0!");
        }
        this.expectedNumberOfElements = expectedNumberOfElements;
        this.size = size;
        this.numberOfHash = (int) Math.ceil((this.size / this.expectedNumberOfElements) * Math.log(2));
        this.bitsPerElement = (double) size / (double) expectedNumberOfElements;
        this.bitSet = new BitSet(size);
        this.numberOfElements = 0;
    }

    /**
     * Bloom filter constructor.
     *
     * Size of Bloom Filter is estimated from:
     * m = (-n*ln(p))/(ln(2))^2,
     * where m is size of Bloom Filter, n is number of expected elements, p is probability of false positives.
     *
     * @param probabilityOfFalsePositives probability of false positives.
     * @param expectedNumberOfElements    expected number of elements to be inserted to Bloom Filter.
     */
    public AbstractBloomFilter(double probabilityOfFalsePositives, int expectedNumberOfElements) {
        this(
                (int) Math.ceil((-expectedNumberOfElements * Math.log(probabilityOfFalsePositives)) / Math.pow(Math.log(2), 2)),
                expectedNumberOfElements
        );
    }

    /**
     * The addAll method enables you to insert each element from collection to Bloom Filter.
     *
     * @param collection a collection with elements to be inserted to Bloom Filter.
     */
    public void addAll(Collection<? extends E> collection) {
        for (E item : collection) {
            add(item);
        }
    }

    /**
     * The add method enables you to insert element to Bloom Filter.
     *
     * @param item an element to be inserted to Bloom Filter.
     */
    public void add(E item) {
        add(item.toString().getBytes());
    }

    /**
     * The add method enables you to insert element to Bloom Filter.
     *
     * @param bytes the bytes array of element to be inserted to Bloom Filter.
     */
    public void add(byte[] bytes) {
        int[] hashes = createHashes(bytes, numberOfHash);
        for (int hash : hashes) {
            bitSet.set(hash, true);
        }
        numberOfElements++;
    }

    /**
     * The mightContains method enables you to check if Bloom Filter may contains element.
     *
     * @param element an element to be checked.
     * @return True if Bloom Filter can contains element (Remember that can be false positive result).
     * False if Bloom Filter cannot contains element.
     */
    public boolean mightContains(E element) {
        return contains(element.toString().getBytes());
    }

    /**
     * The contains method enables you to check if Bloom Filter may contains element.
     *
     * @param bytes the bytes array of element to be checked.
     * @return True if all bits in Bloom Filter are set (Remember that can be false positive result).
     * False if at least one bit is not set.
     */
    private boolean contains(byte[] bytes) {
        int[] hashes = createHashes(bytes, numberOfHash);
        for (int hash : hashes) {
            if (!bitSet.get(hash)) {
                return false;
            }
        }
        return true;
    }

    /**
     * The mightContainsAll method enables you to check if Bloom Filter may contains each element from collection.
     *
     * @param collection a collection with elements to be checked.
     * @return True if Bloom Filter can contains each element (Remember that can be false positive result).
     * False if Bloom Filter cannot contains each element.
     */
    public boolean mightContainsAll(Collection<? extends E> collection) {
        for (E item : collection)
            if (!mightContains(item))
                return false;
        return true;
    }

    /**
     * The createHashes method enables you to create hash functions.
     *
     * @param bytes        the byte array contains data.
     * @param numberOfHash number of hash function.
     * @return int array with result hashes.
     */
    abstract int[] createHashes(byte[] bytes, int numberOfHash);

    /**
     * The getExpectedProbabilityOfFalsePositives method enables you to get expected probability of false positives.
     *
     * @return expected probability of false positives.
     */
    public double getExpectedProbabilityOfFalsePositives() {
        return getProbabilityOfFalsePositives(expectedNumberOfElements);
    }

    /**
     * The getCurrentProbabilityOfFalsePositives method enables you to get actual probability of false positives.
     *
     * @return actual probability of false positives.
     */
    public double getCurrentProbabilityOfFalsePositives() {
        return getProbabilityOfFalsePositives(numberOfElements);
    }

    /**
     * The getProbabilityOfFalsePositives method enables you to get probability of false positives based on parameter.
     *
     * @param numberOfElements a number of elements in Bloom Filter.
     * @return probability of false positives based on parameter.
     */
    public double getProbabilityOfFalsePositives(int numberOfElements) {
        return Math.pow((1 - Math.exp(-numberOfHash * numberOfElements / (double) size)), numberOfHash);
    }

    /**
     * The getSize method enables you to get size of Bloom Filter.
     *
     * @return size of Bloom Filter.
     */
    public int getSize() {
        return this.size;
    }

    /**
     * The clear method enables you to delete all elements from Bloom Filter.
     */
    public void clear() {
        numberOfElements = 0;
        bitSet.clear();
    }

    /**
     * The isEmpty method enables you to check if Bloom Filter is empty.
     *
     * @return True, if Bloom Filter is empty.
     * False, if Bloom Filter is not empty.
     */
    public boolean isEmpty() {
        return (numberOfElements == 0);
    }

    /**
     * The getNumberOfElements method enables you to get number of inserted elements.
     *
     * @return number of inserted elements.
     */
    public int getNumberOfElements() {
        return this.numberOfElements;
    }

    /**
     * The getExpectedNumberOfElements method enables you to get expected number of inserted elements.
     *
     * @return expected number of inserted elements.
     */
    public int getExpectedNumberOfElements() {
        return this.expectedNumberOfElements;
    }

    /**
     * The getExpectedBitsPerElement method enables you to get expected bits per element.
     *
     * @return expected bits per element.
     */
    public double getExpectedBitsPerElement() {
        return this.bitsPerElement;
    }

    /**
     * The getBitsPerElement method enables you to get actual bits per element.
     *
     * @return actual bits per element.
     * @throws Exception when actual number of inserted element = 0.
     */
    public double getBitsPerElement() throws Exception {
        if (numberOfElements <= 0) {
            throw new Exception("Bloom Filter is empty!");
        }
        return this.size / numberOfElements;
    }

    /**
     * The getNumberOfHash method enables you to get number of hash functions.
     *
     * @return number of hash functions.
     */
    public int getNumberOfHash() {
        return this.numberOfHash;
    }

    /**
     * The getValueFromGeneratedHash method enables you to get int value from created hash.
     *
     * @param data         data to hash.
     * @param hashFunction hash function.
     * @return int value from hash.
     */
    int getValueFromGeneratedHash(byte[] data, MessageDigest hashFunction) {
        byte[] resultHashFunction;
        hashFunction.update(data);
        resultHashFunction = hashFunction.digest();
        return Math.abs(new BigInteger(resultHashFunction).mod(BigInteger.valueOf(size)).intValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractBloomFilter<?> that = (AbstractBloomFilter<?>) o;

        if (numberOfHash != that.numberOfHash) return false;
        if (expectedNumberOfElements != that.expectedNumberOfElements) return false;
        if (numberOfElements != that.numberOfElements) return false;
        if (size != that.size) return false;
        if (Double.compare(that.bitsPerElement, bitsPerElement) != 0) return false;
        return bitSet != null ? bitSet.equals(that.bitSet) : that.bitSet == null;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = bitSet != null ? bitSet.hashCode() : 0;
        result = 31 * result + numberOfHash;
        result = 31 * result + expectedNumberOfElements;
        result = 31 * result + numberOfElements;
        result = 31 * result + size;
        temp = Double.doubleToLongBits(bitsPerElement);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

}
