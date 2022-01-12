package com.dreamcloud.esa.tfidf;

import java.nio.ByteBuffer;

public class TfIdfByteVector {
    byte[] vector;
    int index = 0;

    public TfIdfByteVector(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Size must be greater than 0.");
        }
        vector = new byte[size];
    }

    public TfIdfByteVector(byte[] bytes) {
        this.vector = bytes;
        index = vector.length;
    }

    public int getSize() {
        return index;
    }

    public int getCapacity() {
        return vector.length;
    }

    public void addByte(byte b) {
        byte[] bytes = new byte[1];
        bytes[0] = b;
        this.addBytes(bytes);
    }

    public void addBytes(byte[] bytes) {
        synchronized (vector) {
            if (bytes.length + index >= vector.length) {
                //Resize to at least bytes.length
                byte[] resized = new byte[Math.max(vector.length * 2, vector.length + bytes.length)];

                //Copy the old vector
                System.arraycopy(vector, 0, resized, 0, vector.length);

                //Add in the new bytes
                System.arraycopy(bytes, 0, resized, index, bytes.length);

                //Update the current index into the vector
                index += bytes.length;

                //Swap them out (this changes the byte[] ref and allows all other synchronized blocks to proceed)
                vector = resized;
            } else {
                //Add in the new bytes
                System.arraycopy(bytes, 0, vector, index, bytes.length);

                //Update the current index into the vector
                index += bytes.length;
            }
        }
    }

    public byte getByte(int index) {
        return vector[index];
    }

    public void setByte(int index, byte b) {
        vector[index] = b;
    }

    public byte[] getBytes() {
        return vector;
    }

    /*
        Buffer methods:
        These are all thread safe (I believe) as you cannot reduce the size of this vector.
        It is possible that someone else changes out the underlying vector data though, but
        that's ok, just be aware.
     */

    public ByteBuffer getByteBuffer(int offset, int length) {
        return ByteBuffer.wrap(vector, offset, length);
    }

    public ByteBuffer getByteBuffer(int offset) {
        return ByteBuffer.wrap(vector, offset, index);
    }

    public ByteBuffer getByteBuffer() {
        return ByteBuffer.wrap(vector, 0, index);
    }
}
