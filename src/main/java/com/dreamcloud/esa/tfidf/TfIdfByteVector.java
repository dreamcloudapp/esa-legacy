package com.dreamcloud.esa.tfidf;

import java.nio.ByteBuffer;

public class TfIdfByteVector {
    byte[] vector;
    int index = 0;

    public TfIdfByteVector(int size) {
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
        if (index == vector.length) {
            //allocate some more space
            byte[] resized = new byte[vector.length * 2];
            System.arraycopy(vector, 0, resized, 0, vector.length);
            vector = resized;
        }
        vector[index++] = b;
    }

    public void addBytes(byte[] bytes) {
        if (bytes.length + index >= vector.length) {
            //Resize to at least bytes.length
            byte[] resized = new byte[Math.max(vector.length * 2, vector.length + bytes.length)];
            System.arraycopy(vector, 0, resized, 0, vector.length);
            vector = resized;
        }
        System.arraycopy(vector, index, bytes, 0, bytes.length);
        index += bytes.length;
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
